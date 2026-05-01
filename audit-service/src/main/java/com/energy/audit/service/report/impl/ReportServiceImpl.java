package com.energy.audit.service.report.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.dao.mapper.report.ArReportMapper;
import com.energy.audit.dao.mapper.report.ArReportTemplateMapper;
import com.energy.audit.model.entity.report.ArReport;
import com.energy.audit.model.entity.report.ArReportTemplate;
import com.energy.audit.service.report.ActiveTemplateDownload;
import com.energy.audit.service.report.ReportFileStore;
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

import java.io.ByteArrayInputStream;
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
import java.util.Optional;

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

    @Autowired
    private ReportFileStore reportFileStore;

    @Value("${app.report.upload-dir:upload/report}")
    private String uploadDir;

    @Value("${app.report.max-upload-size-mb:50}")
    private int maxUploadSizeMb;

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
        // Resolve template InputStream: prefer filesystem (fast), fallback to DB BLOB (survives container restarts)
        InputStream templateStream = resolveTemplateStream(template);
        if (templateStream == null) {
            throw new BusinessException("报告模板文件不存在且数据库中无模板数据，请重新上传模板");
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
            try (InputStream templateIs = templateStream) {
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

    @Override
    public ArReportTemplate getActiveTemplate() {
        if (reportTemplateMapper == null) {
            return null;
        }
        // Light variant: explicit column list excludes the BLOB so the metadata
        // endpoint never pulls a multi-MB byte[] just to drop it on serialization.
        return reportTemplateMapper.selectActiveLight();
    }

    @Override
    @Transactional
    public ArReport uploadFilledReport(Long enterpriseId, Integer auditYear, Integer reportType,
                                       String fileName, byte[] fileBytes, String username) {
        if (enterpriseId == null) {
            throw new BusinessException("未识别到企业身份，请重新登录");
        }
        if (auditYear == null || auditYear < 1900 || auditYear > 9999) {
            throw new BusinessException("请选择正确的审计年度");
        }
        int rt = (reportType == null) ? 2 : reportType;
        if (rt != 1 && rt != 2) {
            throw new BusinessException("reportType 仅支持 1（初报）或 2（终报）");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }
        long maxBytes = (long) maxUploadSizeMb * 1024L * 1024L;
        if (fileBytes.length > maxBytes) {
            throw new BusinessException("文件大小超过限制（" + maxUploadSizeMb + " MB）");
        }
        if (fileName == null || !fileName.toLowerCase().endsWith(".docx")) {
            throw new BusinessException("仅支持 .docx 格式的报告文件");
        }
        // .docx is a ZIP archive — magic bytes 50 4B 03 04 (PK\x03\x04). Reject .doc / random files.
        if (fileBytes.length < 4 ||
            fileBytes[0] != 0x50 || fileBytes[1] != 0x4B ||
            fileBytes[2] != 0x03 || fileBytes[3] != 0x04) {
            if (TemplateBasedReportBuilder.isOle2Format(fileBytes)) {
                throw new BusinessException(
                    "上传的文件为旧版 .doc 格式（Office 97-2003），系统仅支持 .docx 格式（Office 2007+）。" +
                    "请用 Word 打开后「另存为」选择 .docx 格式后重新上传。");
            }
            throw new BusinessException("上传的文件不是有效的 .docx 文档");
        }

        ArReport existing = reportMapper.selectByEnterpriseAndYear(enterpriseId, auditYear, rt);
        if (existing != null && existing.getStatus() != null) {
            int s = existing.getStatus();
            if (s == 4) {
                throw new BusinessException("该年度报告已提交审核，等待审核结果后再重新上传");
            }
            if (s == 5) {
                throw new BusinessException("该年度报告已审核通过，无需再次上传");
            }
        }

        // 1. Write the file bytes to the pluggable store (local fs by default, could be COS later).
        // Fail loud on storage failure rather than persisting a row whose uploaded_file_path
        // is null but whose BLOB has the new bytes — that combination would silently serve
        // the OLD filesystem file on the next download (if the previous path is still
        // present on disk) because the MyBatis update statement guards uploaded_file_path
        // with <if test="uploadedFilePath != null">.
        String newKey = reportFileStore.save(enterpriseId, auditYear, fileName, fileBytes);
        if (newKey == null) {
            throw new BusinessException("文件保存失败，请稍后重试");
        }

        // 2. Upsert the ar_report row + persist BLOB redundancy in one tx.
        Long reportId;
        String oldKey = null;
        if (existing == null) {
            ArReport record = new ArReport();
            record.setEnterpriseId(enterpriseId);
            record.setAuditYear(auditYear);
            record.setReportType(rt);
            record.setStatus(2); // "已生成" — repurposed here as "report content uploaded"
            record.setUploadedFilePath(newKey);
            record.setUploadedFileData(fileBytes);
            record.setUploadedFileSize((long) fileBytes.length);
            record.setUploadedFileName(fileName);
            record.setUploadedAt(LocalDateTime.now());
            record.setReviewComment(null);
            record.setCreateBy(username);
            record.setUpdateBy(username);
            try {
                reportMapper.insert(record);
            } catch (org.springframework.dao.DuplicateKeyException dup) {
                // Another concurrent upload won the race against the
                // uk_ar_report_ent_year_type unique index — translate into a
                // friendly retry message instead of a 500.
                log.warn("[ReportService] Concurrent upload conflict for enterprise={} year={} type={}",
                    enterpriseId, auditYear, rt);
                throw new BusinessException("该年度报告正在被同时上传，请稍后重试");
            }
            reportId = record.getId();
        } else {
            oldKey = existing.getUploadedFilePath();
            existing.setStatus(2); // overwrite back to "uploaded" regardless of prior 0/2/6
            existing.setUploadedFilePath(newKey);
            existing.setUploadedFileData(fileBytes);
            existing.setUploadedFileSize((long) fileBytes.length);
            existing.setUploadedFileName(fileName);
            existing.setUploadedAt(LocalDateTime.now());
            existing.setReviewComment(null);   // clear any prior rejection reason on re-upload
            existing.setReviewerId(null);
            existing.setUpdateBy(username);
            reportMapper.update(existing);
            reportId = existing.getId();
        }

        // 3. Best-effort delete of the previous file (after the new path is persisted).
        if (oldKey != null && !oldKey.equals(newKey)) {
            reportFileStore.delete(oldKey);
        }

        log.info("[ReportService] Enterprise {} uploaded report year={} type={} id={} bytes={} key={}",
            enterpriseId, auditYear, rt, reportId, fileBytes.length, newKey);
        return reportMapper.selectById(reportId);
    }

    @Override
    public byte[] downloadUploadedReportBytes(Long reportId) {
        if (reportId == null) {
            throw new BusinessException("reportId 不能为空");
        }
        ArReport report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        // Filesystem first (fast)
        Optional<byte[]> fsBytes = reportFileStore.load(report.getUploadedFilePath());
        if (fsBytes.isPresent()) {
            return fsBytes.get();
        }
        // BLOB fallback (Railway-restart durable copy)
        byte[] blob = reportMapper.selectUploadedFileBytesById(reportId);
        if (blob != null && blob.length > 0) {
            // Self-heal is best-effort: a write/DB failure here must not abort the
            // download, since the bytes the caller actually wants are already in hand.
            try {
                String newKey = reportFileStore.save(
                    report.getEnterpriseId(),
                    report.getAuditYear() == null ? 0 : report.getAuditYear(),
                    report.getUploadedFileName(),
                    blob);
                if (newKey != null && !newKey.equals(report.getUploadedFilePath())) {
                    // Targeted update — must NOT use reportMapper.update(patch), which would
                    // unconditionally set review_comment = NULL and erase any auditor's rejection reason.
                    reportMapper.updateUploadedFilePathById(reportId, newKey, "system");
                    log.info("[ReportService] Self-healed local cache for report {} -> {}", reportId, newKey);
                }
            } catch (RuntimeException e) {
                log.warn("[ReportService] Self-heal failed for report {} (serving BLOB only): {}",
                    reportId, e.getMessage());
            }
            return blob;
        }
        throw new BusinessException("报告文件不存在，请重新上传");
    }

    @Override
    public ActiveTemplateDownload downloadActiveTemplate() {
        if (reportTemplateMapper == null) {
            throw new BusinessException("报告模板模块未初始化");
        }
        // Single DB read so the filename and bytes returned to the controller
        // are guaranteed to come from the same template row.
        ArReportTemplate template = reportTemplateMapper.selectActive();
        if (template == null) {
            throw new BusinessException("未找到可用的报告模板，请联系管理员上传模板");
        }

        // Filesystem first (fast), then DB BLOB (survives container restarts).
        String fsPath = template.getTemplateFilePath();
        if (fsPath != null && !fsPath.isEmpty()) {
            Path resolved = resolveTemplatePath(fsPath);
            if (resolved != null && Files.exists(resolved)) {
                try {
                    byte[] bytes = Files.readAllBytes(resolved);
                    log.info("[ReportService] Active template served from filesystem: {}", resolved);
                    return new ActiveTemplateDownload(template, bytes);
                } catch (IOException e) {
                    log.warn("[ReportService] Failed to read template file {}, falling back to DB BLOB",
                        resolved, e);
                }
            }
        }
        byte[] data = template.getTemplateFileData();
        if (data != null && data.length > 0) {
            log.info("[ReportService] Active template served from DB BLOB ({} bytes)", data.length);
            return new ActiveTemplateDownload(template, data);
        }
        throw new BusinessException("报告模板文件不存在且数据库中无模板数据，请重新上传模板");
    }

    /**
     * Validate a {@code template_file_path} value coming from the DB before reading it.
     * Returns {@code null} (caller falls back to BLOB) when the path escapes the configured
     * upload root, defending against tampered DB rows pointing at arbitrary container paths.
     */
    private Path resolveTemplatePath(String fsPath) {
        try {
            Path candidate = Paths.get(fsPath).toAbsolutePath().normalize();
            Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!candidate.startsWith(baseDir)) {
                log.warn("[ReportService] Template path {} escapes upload root {}, ignoring",
                    candidate, baseDir);
                return null;
            }
            return candidate;
        } catch (RuntimeException e) {
            log.warn("[ReportService] Invalid template path {}: {}", fsPath, e.getMessage());
            return null;
        }
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
        if (!fileName.toLowerCase().endsWith(".docx")) {
            throw new BusinessException("仅支持 .docx 格式的模板文件");
        }
        // Validate actual file content — reject OLE2 (.doc) even if extension says .docx
        if (TemplateBasedReportBuilder.isOle2Format(fileBytes)) {
            throw new BusinessException(
                "上传的文件为旧版 .doc 格式（Office 97-2003），系统仅支持 .docx 格式（Office 2007+）。" +
                "请用 Word 打开模板后「另存为」选择 .docx 格式，然后重新上传。");
        }

        try {
            // Also write to filesystem as a local cache (may not survive container restart)
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
            template.setTemplateFileData(fileBytes);  // Store in DB for persistence across deploys
            template.setOriginalFileName(fileName);
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

    /**
     * Resolve template as an InputStream.  Priority:
     * 1. Filesystem path (fast, if file still exists — e.g. same container that uploaded it)
     * 2. DB BLOB as ByteArrayInputStream (no temp file, no disk leak)
     */
    private InputStream resolveTemplateStream(ArReportTemplate template) {
        // 1. Try filesystem first
        String fsPath = template.getTemplateFilePath();
        if (fsPath != null && Files.exists(Paths.get(fsPath))) {
            try {
                log.info("[ReportService] Loading template from filesystem: {}", fsPath);
                return new FileInputStream(fsPath);
            } catch (IOException e) {
                log.warn("[ReportService] Filesystem path exists but failed to open: {}", fsPath, e);
                // Fall through to BLOB
            }
        }

        // 2. Fallback: stream directly from DB BLOB (no temp file needed)
        byte[] data = template.getTemplateFileData();
        if (data != null && data.length > 0) {
            log.info("[ReportService] Loading template from DB BLOB ({} bytes)", data.length);
            return new ByteArrayInputStream(data);
        }

        log.warn("[ReportService] Template id={} has no file on disk and no BLOB in DB", template.getId());
        return null;
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
