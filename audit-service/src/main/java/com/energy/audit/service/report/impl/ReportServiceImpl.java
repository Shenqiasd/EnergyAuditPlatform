package com.energy.audit.service.report.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.dao.mapper.report.ArReportMapper;
import com.energy.audit.dao.mapper.report.ArReportTemplateMapper;
import com.energy.audit.model.entity.report.ArReport;
import com.energy.audit.model.entity.report.ArReportTemplate;
import com.energy.audit.service.report.ReportService;
import com.energy.audit.service.report.TemplateBasedReportBuilder;
import com.energy.audit.service.report.WordReportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);
    private static final int STUCK_TIMEOUT_MINUTES = 10;

    @Autowired
    private ArReportMapper reportMapper;

    @Autowired(required = false)
    private ArReportTemplateMapper reportTemplateMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${app.report.upload-dir:upload/report}")
    private String uploadDir;

    @Override
    public ArReport generateReport(Long enterpriseId, Integer auditYear, String username) {
        // Phase 1: Init record in its own transaction (commits immediately, releases row lock)
        ArReport record = new TransactionTemplate(transactionManager).execute(status -> {
            ArReport existing = reportMapper.selectByEnterpriseAndYear(enterpriseId, auditYear, 1);
            if (existing != null && existing.getStatus() == 1) {
                if (existing.getUpdateTime() != null &&
                    existing.getUpdateTime().plusMinutes(STUCK_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
                    log.warn("Report generation stuck for {}min, resetting status for enterprise={} year={}",
                        STUCK_TIMEOUT_MINUTES, enterpriseId, auditYear);
                    existing.setStatus(3);
                    existing.setUpdateBy(username);
                    reportMapper.update(existing);
                } else {
                    throw new RuntimeException("报告正在生成中，请稍候");
                }
            }

            if (existing == null) {
                existing = new ArReport();
                existing.setEnterpriseId(enterpriseId);
                existing.setAuditYear(auditYear);
                existing.setReportType(1);
                existing.setStatus(1);
                existing.setCreateBy(username);
                existing.setUpdateBy(username);
                reportMapper.insert(existing);
                return reportMapper.selectById(existing.getId());
            } else {
                existing.setStatus(1);
                existing.setUpdateBy(username);
                reportMapper.update(existing);
                return existing;
            }
        });

        // Phase 2: Heavy work outside any transaction (no row lock held)
        try {
            Map<String, Object> reportData = collectReportData(enterpriseId, auditYear);

            String enterpriseName = (String) reportData.getOrDefault("enterpriseName", "企业");
            String reportName = enterpriseName + " " + auditYear + "年度能源审计报告";

            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            String fileName = "report_" + enterpriseId + "_" + auditYear + "_" + System.currentTimeMillis() + ".docx";
            Path filePath = dirPath.resolve(fileName).normalize();

            WordReportBuilder.buildReport(filePath, reportName, auditYear, reportData);

            // Phase 3: Commit success in its own transaction
            final ArReport finalRecord = record;
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                finalRecord.setStatus(2);
                finalRecord.setReportName(reportName);
                finalRecord.setGeneratedFilePath(filePath.toString());
                finalRecord.setGenerateTime(LocalDateTime.now());
                finalRecord.setUpdateBy(username);
                reportMapper.update(finalRecord);
            });
            return reportMapper.selectById(record.getId());
        } catch (Exception e) {
            log.error("Report generation failed for enterprise={} year={}", enterpriseId, auditYear, e);
            markReportFailed(record.getId(), username);
            throw new RuntimeException("报告生成失败，请稍后重试");
        }
    }

    @Override
    public List<ArReport> listReports(Long enterpriseId, Integer auditYear) {
        return reportMapper.selectByEnterprise(enterpriseId, auditYear);
    }

    @Override
    public ArReport getReport(Long id) {
        return reportMapper.selectById(id);
    }

    @Override
    public byte[] downloadReport(Long id) {
        ArReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new RuntimeException("报告不存在");
        }
        String path = report.getGeneratedFilePath();
        if (path == null || path.isEmpty()) {
            path = report.getUploadedFilePath();
        }
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("报告文件尚未生成");
        }
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolved = Paths.get(path).toAbsolutePath().normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new RuntimeException("非法文件路径");
        }
        try {
            return Files.readAllBytes(resolved);
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> collectReportData(Long enterpriseId, Integer auditYear) {
        Map<String, Object> data = new HashMap<>();

        try {
            Map<String, Object> enterprise = jdbcTemplate.queryForMap(
                "SELECT enterprise_name, credit_code, contact_person, contact_phone, contact_email, remark " +
                "FROM ent_enterprise WHERE id = ? AND deleted = 0", enterpriseId);
            data.put("enterpriseName", enterprise.getOrDefault("ENTERPRISE_NAME",
                enterprise.getOrDefault("enterprise_name", "")));
            data.put("enterprise", enterprise);
        } catch (Exception e) {
            log.warn("Failed to load enterprise info for id={}", enterpriseId, e);
            data.put("enterpriseName", "企业");
            data.put("enterprise", new HashMap<>());
        }

        try {
            Map<String, Object> setting = jdbcTemplate.queryForMap(
                "SELECT legal_representative, enterprise_address, postal_code, fax, " +
                "industry_category, industry_code, industry_name, compiler_name, " +
                "compiler_contact, registered_capital, energy_cert " +
                "FROM ent_enterprise_setting WHERE enterprise_id = ? AND deleted = 0", enterpriseId);
            data.put("enterpriseSetting", setting);
        } catch (Exception e) {
            log.warn("Failed to load enterprise setting for id={}", enterpriseId, e);
            data.put("enterpriseSetting", new HashMap<>());
        }

        try {
            Map<String, Object> overview = jdbcTemplate.queryForMap(
                "SELECT energy_leader_name, energy_leader_position, energy_dept_name, " +
                "energy_dept_leader, fulltime_staff_count, parttime_staff_count, " +
                "five_year_target_name, five_year_target_value, five_year_target_dept " +
                "FROM de_company_overview WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0",
                enterpriseId, auditYear);
            data.put("companyOverview", overview);
        } catch (Exception e) {
            log.warn("Failed to load company overview for enterprise={} year={}", enterpriseId, auditYear, e);
            data.put("companyOverview", new HashMap<>());
        }

        try {
            List<Map<String, Object>> indicators = jdbcTemplate.queryForList(
                "SELECT * FROM de_tech_indicator WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY indicator_year", enterpriseId, auditYear);
            data.put("techIndicators", indicators);
        } catch (Exception e) {
            log.warn("Failed to load tech indicators", e);
            data.put("techIndicators", List.of());
        }

        try {
            List<Map<String, Object>> balance = jdbcTemplate.queryForList(
                "SELECT * FROM de_energy_balance WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY standard_coal_equiv DESC", enterpriseId, auditYear);
            data.put("energyBalance", balance);
        } catch (Exception e) {
            log.warn("Failed to load energy balance", e);
            data.put("energyBalance", List.of());
        }

        try {
            List<Map<String, Object>> products = jdbcTemplate.queryForList(
                "SELECT * FROM de_product_unit_consumption WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY product_name, year_type", enterpriseId, auditYear);
            data.put("productConsumption", products);
        } catch (Exception e) {
            log.warn("Failed to load product consumption", e);
            data.put("productConsumption", List.of());
        }

        try {
            List<Map<String, Object>> ghg = jdbcTemplate.queryForList(
                "SELECT * FROM de_ghg_emission WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY emission_type, annual_emission DESC", enterpriseId, auditYear);
            data.put("ghgEmission", ghg);
        } catch (Exception e) {
            log.warn("Failed to load GHG emission", e);
            data.put("ghgEmission", List.of());
        }

        try {
            List<Map<String, Object>> flows = jdbcTemplate.queryForList(
                "SELECT * FROM de_energy_flow WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "ORDER BY flow_stage, seq_no", enterpriseId, auditYear);
            data.put("energyFlow", flows);
        } catch (Exception e) {
            log.warn("Failed to load energy flow", e);
            data.put("energyFlow", List.of());
        }

        return data;
    }

    // ====== Template-based report generation (Phase 1) ======

    @Override
    public ArReport generateReportFromTemplate(Long submissionId, Long callerEnterpriseId, byte[] flowChartImage, String username) {
        // 1. Load submission and verify status = 2 (approved)
        Map<String, Object> submission;
        try {
            submission = jdbcTemplate.queryForMap(
                "SELECT s.id, s.enterprise_id, s.audit_year, s.submission_json, s.status, " +
                "e.enterprise_name, e.credit_code " +
                "FROM tpl_submission s " +
                "LEFT JOIN ent_enterprise e ON e.id = s.enterprise_id " +
                "WHERE s.id = ? AND s.deleted = 0", submissionId);
        } catch (Exception e) {
            throw new BusinessException("填报记录不存在: submissionId=" + submissionId);
        }

        Integer submissionStatus = ((Number) submission.get("status")).intValue();
        if (submissionStatus != 2) {
            throw new BusinessException("填报必须通过审核后才能生成报告（当前状态: " + submissionStatus + "）");
        }

        Long enterpriseId = ((Number) submission.get("enterprise_id")).longValue();

        // Verify the submission belongs to the calling user's enterprise
        if (callerEnterpriseId != null && !callerEnterpriseId.equals(enterpriseId)) {
            throw new BusinessException("无权访问其他企业的填报记录");
        }
        Integer auditYear = ((Number) submission.get("audit_year")).intValue();
        String submissionJson = (String) submission.get("submission_json");
        String enterpriseName = (String) submission.getOrDefault("enterprise_name", "企业");
        String creditCode = (String) submission.getOrDefault("credit_code", "");

        if (submissionJson == null || submissionJson.isEmpty()) {
            throw new BusinessException("填报数据为空，无法生成报告");
        }

        // 2. Load active report template
        if (reportTemplateMapper == null) {
            throw new BusinessException("报告模板模块未初始化");
        }
        ArReportTemplate template = reportTemplateMapper.selectActive();
        if (template == null) {
            throw new BusinessException("未找到可用的报告模板，请联系管理员上传模板");
        }
        String templatePath = template.getTemplateFilePath();
        if (templatePath == null || !Files.exists(Paths.get(templatePath))) {
            throw new BusinessException("报告模板文件不存在: " + templatePath);
        }

        // 3. Create/update report record and COMMIT in a short transaction.
        //    This releases the row lock so that markReportFailed (REQUIRES_NEW)
        //    won't deadlock if the generation phase fails.
        final Long reportId;
        {
            TransactionTemplate initTx = new TransactionTemplate(transactionManager);
            reportId = initTx.execute(status -> {
                ArReport record = reportMapper.selectByEnterpriseAndYear(enterpriseId, auditYear, 2);
                if (record != null && record.getStatus() == 1) {
                    if (record.getUpdateTime() != null &&
                        record.getUpdateTime().plusMinutes(STUCK_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
                        record.setStatus(3);
                        record.setUpdateBy(username);
                        reportMapper.update(record);
                    } else {
                        throw new BusinessException("报告正在生成中，请稍候");
                    }
                }

                if (record == null) {
                    record = new ArReport();
                    record.setEnterpriseId(enterpriseId);
                    record.setAuditYear(auditYear);
                    record.setReportType(2); // type 2 = template-based
                    record.setStatus(1); // generating
                    record.setTemplateId(template.getId());
                    record.setSubmissionId(submissionId);
                    record.setCreateBy(username);
                    record.setUpdateBy(username);
                    reportMapper.insert(record);
                } else {
                    record.setStatus(1);
                    record.setTemplateId(template.getId());
                    record.setSubmissionId(submissionId);
                    record.setUpdateBy(username);
                    reportMapper.update(record);
                }
                return record.getId();
            });
        }

        // 4. Heavy generation work runs OUTSIDE any transaction.
        //    Row lock from step 3 is already released.
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("year", String.valueOf(auditYear));
            metadata.put("enterpriseCode", creditCode);
            metadata.put("enterpriseName", enterpriseName);

            byte[] docxBytes;
            try (InputStream templateIs = new FileInputStream(templatePath)) {
                docxBytes = TemplateBasedReportBuilder.buildReport(
                    templateIs, submissionJson, flowChartImage, metadata);
            }

            Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dirPath);
            String fileName = "report_template_" + enterpriseId + "_" + auditYear + "_" +
                System.currentTimeMillis() + ".docx";
            Path filePath = dirPath.resolve(fileName).normalize();
            Files.write(filePath, docxBytes);

            String reportHtml = TemplateBasedReportBuilder.convertDocxToHtml(docxBytes);

            String flowChartFilePath = null;
            if (flowChartImage != null && flowChartImage.length > 0) {
                String imgFileName = "flow_chart_" + enterpriseId + "_" + auditYear + "_" +
                    System.currentTimeMillis() + ".png";
                Path imgPath = dirPath.resolve(imgFileName).normalize();
                Files.write(imgPath, flowChartImage);
                flowChartFilePath = imgPath.toString();
            }

            // 5. Commit success in a short transaction
            final String finalFlowChartPath = flowChartFilePath;
            TransactionTemplate successTx = new TransactionTemplate(transactionManager);
            successTx.executeWithoutResult(status -> {
                String reportName = enterpriseName + " " + auditYear + "年度能源审计报告";
                ArReport record = reportMapper.selectById(reportId);
                record.setStatus(2); // generated
                record.setReportName(reportName);
                record.setGeneratedFilePath(filePath.toString());
                record.setReportHtml(reportHtml);
                record.setFlowChartPath(finalFlowChartPath);
                record.setGenerateTime(LocalDateTime.now());
                record.setUpdateBy(username);
                reportMapper.update(record);
            });

            log.info("[ReportService] Template-based report generated: enterprise={} year={} file={}",
                enterpriseId, auditYear, filePath);
            return reportMapper.selectById(reportId);

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Template-based report generation failed for submission={}", submissionId, e);
            // No deadlock: the status=1 row is already committed, no outer tx holds the lock
            markReportFailed(reportId, username);
            throw new BusinessException("报告生成失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ArReport saveReportHtml(Long reportId, String html, String username) {
        ArReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (report.getStatus() != null && (report.getStatus() == 4 || report.getStatus() == 5)) {
            throw new BusinessException(report.getStatus() == 5 ? "报告已审核通过，不可编辑" : "报告已提交审核，不可编辑");
        }
        report.setReportHtml(html);
        report.setUpdateBy(username);
        reportMapper.update(report);
        return reportMapper.selectById(reportId);
    }

    @Override
    @Transactional
    public ArReport submitForReview(Long reportId, String username) {
        ArReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (report.getStatus() == null || report.getStatus() < 2 || report.getStatus() == 3) {
            throw new BusinessException("报告尚未生成，不可提交审核");
        }
        if (report.getStatus() == 4) {
            throw new BusinessException("报告已提交审核，请勿重复提交");
        }
        if (report.getStatus() == 5) {
            throw new BusinessException("报告已审核通过，无需重复提交");
        }
        report.setStatus(4); // submitted_for_review
        report.setSubmitTime(LocalDateTime.now());
        report.setUpdateBy(username);
        reportMapper.update(report);
        return reportMapper.selectById(reportId);
    }

    /**
     * Mark a report as failed.
     * Uses REQUIRES_NEW propagation so the status=3 update persists even when
     * the caller's transaction (e.g. generateReport) rolls back.
     * For generateReportFromTemplate, the outer tx is already committed so
     * REQUIRES_NEW is harmless but still correct.
     */
    private void markReportFailed(Long reportId, String username) {
        try {
            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            txTemplate.executeWithoutResult(status -> {
                int updated = jdbcTemplate.update(
                    "UPDATE ar_report SET status = 3, update_by = ?, update_time = NOW() " +
                    "WHERE id = ? AND deleted = 0", username, reportId);
                if (updated == 0) {
                    log.warn("[ReportService] markReportFailed: no record found for id={}", reportId);
                }
            });
        } catch (Exception ex) {
            log.warn("[ReportService] Failed to mark report {} as failed: {}", reportId, ex.getMessage());
        }
    }

    @Override
    public List<ArReportTemplate> listTemplates() {
        if (reportTemplateMapper == null) {
            return List.of();
        }
        return reportTemplateMapper.selectAll();
    }

    // ====== Phase 3: Report Review Workflow (auditor side) ======

    @Override
    public List<ArReport> listReportsForReview(Integer status, Integer auditYear) {
        return reportMapper.selectByStatus(status, auditYear);
    }

    @Override
    @Transactional
    public ArReport approveReport(Long reportId, String reviewComment, Long reviewerId, String username) {
        ArReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 4) {
            throw new BusinessException("只有已提交审核的报告才能审批（当前状态: " + report.getStatus() + "）");
        }
        report.setStatus(5); // review_approved
        report.setReviewComment(reviewComment);
        report.setReviewerId(reviewerId);
        report.setUpdateBy(username);
        reportMapper.update(report);
        log.info("[ReportService] Report approved: id={} reviewer={}", reportId, username);
        return reportMapper.selectById(reportId);
    }

    @Override
    @Transactional
    public ArReport rejectReport(Long reportId, String reviewComment, Long reviewerId, String username) {
        ArReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (report.getStatus() == null || report.getStatus() != 4) {
            throw new BusinessException("只有已提交审核的报告才能退回（当前状态: " + report.getStatus() + "）");
        }
        if (reviewComment == null || reviewComment.trim().isEmpty()) {
            throw new BusinessException("退回报告必须填写退回理由");
        }
        report.setStatus(6); // review_rejected
        report.setReviewComment(reviewComment);
        report.setReviewerId(reviewerId);
        report.setUpdateBy(username);
        reportMapper.update(report);
        log.info("[ReportService] Report rejected: id={} reviewer={} reason={}", reportId, username, reviewComment);
        return reportMapper.selectById(reportId);
    }
}
