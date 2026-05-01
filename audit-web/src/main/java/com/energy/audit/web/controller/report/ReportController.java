package com.energy.audit.web.controller.report;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.report.ArReport;
import com.energy.audit.model.entity.report.ArReportTemplate;
import com.energy.audit.service.report.ActiveTemplateDownload;
import com.energy.audit.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "Report", description = "Audit report management")
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new BusinessException("仅企业用户可访问报告功能");
        }
    }

    @Operation(summary = "List enterprise reports")
    @GetMapping("/list")
    public R<List<ArReport>> list(@RequestParam(required = false) Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<ArReport> reports = reportService.listReports(enterpriseId, auditYear);
        return R.ok(reports);
    }

    @Operation(summary = "Get report detail")
    @GetMapping("/{id}")
    public R<ArReport> detail(@PathVariable Long id) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        ArReport report = reportService.getReport(id);
        if (report == null || !report.getEnterpriseId().equals(enterpriseId)) {
            return R.fail("报告不存在");
        }
        return R.ok(report);
    }

    @Operation(summary = "Download report file")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || (userType != 1 && userType != 2 && userType != 3)) {
            throw new BusinessException("无权下载报告");
        }
        ArReport report = reportService.getReport(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        // Enterprise users can only download their own reports
        if (userType == 3) {
            Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
            if (!report.getEnterpriseId().equals(enterpriseId)) {
                return ResponseEntity.notFound().build();
            }
        }
        byte[] content = reportService.downloadReport(id);
        String fileName = report.getReportName() + ".docx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }

    // ====== Template-based report generation (Phase 1) ======

    @Operation(summary = "Generate report from Word template using SpreadJS submission data")
    @PostMapping("/generate-from-template")
    public R<ArReport> generateFromTemplate(
            @RequestParam Long submissionId,
            @RequestPart(required = false) MultipartFile flowChartImage) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        String username = SecurityUtils.getCurrentUsername();
        byte[] imageBytes = null;
        if (flowChartImage != null && !flowChartImage.isEmpty()) {
            try {
                imageBytes = flowChartImage.getBytes();
            } catch (Exception e) {
                return R.fail("能源流向图上传失败");
            }
        }
        ArReport report = reportService.generateReportFromTemplate(submissionId, enterpriseId, imageBytes, username);
        return R.ok(report);
    }

    @Operation(summary = "Save edited report HTML (from TinyMCE editor)")
    @PostMapping("/{id}/edit")
    public R<ArReport> editHtml(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        ArReport report = reportService.getReport(id);
        if (report == null || !report.getEnterpriseId().equals(enterpriseId)) {
            return R.fail("报告不存在");
        }
        String html = body.get("html");
        if (html == null) {
            return R.fail("缺少 html 字段");
        }
        String username = SecurityUtils.getCurrentUsername();
        ArReport updated = reportService.saveReportHtml(id, html, username);
        return R.ok(updated);
    }

    @Operation(summary = "Submit report for auditor review")
    @PostMapping("/{id}/submit-for-review")
    public R<ArReport> submitForReview(@PathVariable Long id) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        ArReport report = reportService.getReport(id);
        if (report == null || !report.getEnterpriseId().equals(enterpriseId)) {
            return R.fail("报告不存在");
        }
        String username = SecurityUtils.getCurrentUsername();
        ArReport updated = reportService.submitForReview(id, username);
        return R.ok(updated);
    }

    @Operation(summary = "List available report templates")
    @GetMapping("/templates")
    public R<List<ArReportTemplate>> listTemplates() {
        return R.ok(reportService.listTemplates());
    }

    // ====== Enterprise-side: download the active report template ======

    @Operation(summary = "Get currently active report template metadata (enterprise users only)")
    @GetMapping("/template/active")
    public R<ArReportTemplate> getActiveTemplate() {
        requireEnterprise();
        // Returns null when no active template exists — frontend shows disabled state.
        return R.ok(reportService.getActiveTemplate());
    }

    @Operation(summary = "Download the active report template (.docx) for enterprise users")
    @GetMapping("/template/active/download")
    public ResponseEntity<byte[]> downloadActiveTemplate() {
        requireEnterprise();
        // Single service call returns the metadata + bytes from one DB read so the
        // Content-Disposition filename and the body cannot drift if an admin swaps
        // the active template between requests.
        ActiveTemplateDownload download = reportService.downloadActiveTemplate();
        ArReportTemplate template = download.getTemplate();
        String fileName = template.getOriginalFileName();
        if (fileName == null || fileName.isEmpty()) {
            String tplName = template.getTemplateName() != null && !template.getTemplateName().isEmpty()
                ? template.getTemplateName() : "report-template";
            fileName = tplName + ".docx";
        }
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(download.getBytes());
    }

    // ====== Phase 4: Admin Report Template Management ======

    private void requireAdmin() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 1) {
            throw new BusinessException("该操作仅管理员可执行");
        }
    }

    @Operation(summary = "Upload a new report template (.docx)")
    @PostMapping("/template/upload")
    public R<ArReportTemplate> uploadTemplate(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String templateName) {
        requireAdmin();
        if (file == null || file.isEmpty()) {
            return R.fail("请选择要上传的文件");
        }
        String username = SecurityUtils.getCurrentUsername();
        try {
            ArReportTemplate template = reportService.uploadTemplate(
                file.getOriginalFilename(), file.getBytes(), templateName, username);
            return R.ok(template);
        } catch (BusinessException be) {
            return R.fail(be.getMessage());
        } catch (Exception e) {
            return R.fail("文件上传失败");
        }
    }

    @Operation(summary = "Get report template detail")
    @GetMapping("/template/{id}")
    public R<ArReportTemplate> getTemplate(@PathVariable Long id) {
        requireAdmin();
        ArReportTemplate template = reportService.getReportTemplate(id);
        if (template == null) {
            return R.fail("模板不存在");
        }
        return R.ok(template);
    }

    @Operation(summary = "Activate a report template (set as current active)")
    @PostMapping("/template/{id}/activate")
    public R<ArReportTemplate> activateTemplate(@PathVariable Long id) {
        requireAdmin();
        String username = SecurityUtils.getCurrentUsername();
        ArReportTemplate template = reportService.activateTemplate(id, username);
        return R.ok(template);
    }

    @Operation(summary = "Deactivate a report template")
    @PostMapping("/template/{id}/deactivate")
    public R<ArReportTemplate> deactivateTemplate(@PathVariable Long id) {
        requireAdmin();
        String username = SecurityUtils.getCurrentUsername();
        ArReportTemplate template = reportService.deactivateTemplate(id, username);
        return R.ok(template);
    }

    @Operation(summary = "Delete a report template (soft delete)")
    @DeleteMapping("/template/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        requireAdmin();
        String username = SecurityUtils.getCurrentUsername();
        reportService.deleteReportTemplate(id, username);
        return R.ok(null);
    }

    // ====== Phase 3: Report Review Workflow (auditor side) ======

    private void requireAuditorOrAdmin() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || (userType != 1 && userType != 2)) {
            throw new BusinessException("仅管理员或审核员可执行此操作");
        }
    }

    @Operation(summary = "List reports for review (auditor/admin)")
    @GetMapping("/review/list")
    public R<List<ArReport>> listForReview(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer auditYear) {
        requireAuditorOrAdmin();
        List<ArReport> reports = reportService.listReportsForReview(status, auditYear);
        return R.ok(reports);
    }

    @Operation(summary = "Get report detail for review (auditor/admin)")
    @GetMapping("/review/{id}")
    public R<ArReport> reviewDetail(@PathVariable Long id) {
        requireAuditorOrAdmin();
        ArReport report = reportService.getReport(id);
        if (report == null) {
            return R.fail("报告不存在");
        }
        return R.ok(report);
    }

    @Operation(summary = "Approve a submitted report (status 4 -> 5)")
    @PostMapping("/review/{id}/approve")
    public R<ArReport> approveReport(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body) {
        requireAuditorOrAdmin();
        Long reviewerId = SecurityUtils.getRequiredCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        String comment = body != null ? body.get("reviewComment") : null;
        ArReport updated = reportService.approveReport(id, comment, reviewerId, username);
        return R.ok(updated);
    }

    @Operation(summary = "Reject/return a submitted report (status 4 -> 6)")
    @PostMapping("/review/{id}/reject")
    public R<ArReport> rejectReport(@PathVariable Long id,
                                     @RequestBody Map<String, String> body) {
        requireAuditorOrAdmin();
        Long reviewerId = SecurityUtils.getRequiredCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        String comment = body != null ? body.get("reviewComment") : null;
        ArReport updated = reportService.rejectReport(id, comment, reviewerId, username);
        return R.ok(updated);
    }
}
