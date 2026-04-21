package com.energy.audit.service.report.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.dao.mapper.report.ArReportMapper;
import com.energy.audit.dao.mapper.report.ArReportTemplateMapper;
import com.energy.audit.model.entity.report.ArReport;
import com.energy.audit.model.entity.report.ArReportTemplate;
import com.energy.audit.service.report.ReportService;
import com.energy.audit.service.report.TemplateBasedReportBuilder;
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

    // ====== Template-based report generation ======

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
     * For generateReportFromTemplate, the outer tx is already committed so
     * REQUIRES_NEW ensures the failure status persists.
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

    // ====== Phase 4: Admin Report Template Management ======

    @Override
    @Transactional
    public ArReportTemplate uploadTemplate(String fileName, byte[] fileBytes, String templateName, String username) {
        if (reportTemplateMapper == null) {
            throw new BusinessException("报告模板模块未初始化");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException("文件不能为空");
        }
        if (!fileName.toLowerCase().endsWith(".docx") && !fileName.toLowerCase().endsWith(".doc")) {
            throw new BusinessException("仅支持 .docx 或 .doc 格式的模板文件");
        }

        try {
            Path dirPath = Paths.get(uploadDir, "templates").toAbsolutePath().normalize();
            Files.createDirectories(dirPath);

            String safeFileName = "template_" + System.currentTimeMillis() + "_" +
                fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path filePath = dirPath.resolve(safeFileName).normalize();
            if (!filePath.startsWith(dirPath)) {
                throw new BusinessException("非法文件名");
            }
            Files.write(filePath, fileBytes);

            // Determine next version number
            List<ArReportTemplate> existing = reportTemplateMapper.selectAll();
            int maxVersion = existing.stream()
                .mapToInt(t -> t.getVersion() != null ? t.getVersion() : 0)
                .max().orElse(0);

            ArReportTemplate template = new ArReportTemplate();
            template.setTemplateName(templateName != null && !templateName.isEmpty() ? templateName : fileName);
            template.setTemplateFilePath(filePath.toString());
            template.setVersion(maxVersion + 1);
            template.setStatus(0); // draft by default
            template.setCreateBy(username);
            template.setUpdateBy(username);
            reportTemplateMapper.insert(template);

            log.info("[ReportService] Template uploaded: id={} name={} path={}", template.getId(), templateName, filePath);
            return reportTemplateMapper.selectById(template.getId());
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to upload report template", e);
            throw new BusinessException("模板上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ArReportTemplate activateTemplate(Long templateId, String username) {
        if (reportTemplateMapper == null) {
            throw new BusinessException("报告模板模块未初始化");
        }
        ArReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // Deactivate all other templates first
        List<ArReportTemplate> all = reportTemplateMapper.selectAll();
        for (ArReportTemplate t : all) {
            if (t.getStatus() != null && t.getStatus() == 1 && !t.getId().equals(templateId)) {
                t.setStatus(0);
                t.setUpdateBy(username);
                reportTemplateMapper.update(t);
            }
        }

        // Activate the target template
        template.setStatus(1);
        template.setUpdateBy(username);
        reportTemplateMapper.update(template);

        log.info("[ReportService] Template activated: id={} name={}", templateId, template.getTemplateName());
        return reportTemplateMapper.selectById(templateId);
    }

    @Override
    @Transactional
    public ArReportTemplate deactivateTemplate(Long templateId, String username) {
        if (reportTemplateMapper == null) {
            throw new BusinessException("报告模板模块未初始化");
        }
        ArReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        template.setStatus(0);
        template.setUpdateBy(username);
        reportTemplateMapper.update(template);

        log.info("[ReportService] Template deactivated: id={} name={}", templateId, template.getTemplateName());
        return reportTemplateMapper.selectById(templateId);
    }

    @Override
    @Transactional
    public void deleteReportTemplate(Long templateId, String username) {
        if (reportTemplateMapper == null) {
            throw new BusinessException("报告模板模块未初始化");
        }
        ArReportTemplate template = reportTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        if (template.getStatus() != null && template.getStatus() == 1) {
            throw new BusinessException("已激活的模板不可删除，请先停用");
        }
        reportTemplateMapper.softDelete(templateId, username);
        log.info("[ReportService] Template deleted: id={} name={}", templateId, template.getTemplateName());
    }

    @Override
    public ArReportTemplate getReportTemplate(Long templateId) {
        if (reportTemplateMapper == null) {
            return null;
        }
        return reportTemplateMapper.selectById(templateId);
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
