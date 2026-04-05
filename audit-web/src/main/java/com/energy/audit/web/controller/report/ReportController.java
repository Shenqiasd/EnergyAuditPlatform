package com.energy.audit.web.controller.report;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.report.ArReport;
import com.energy.audit.service.report.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Report", description = "Audit report management")
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Operation(summary = "Generate audit report")
    @PostMapping("/generate")
    public R<ArReport> generate(@RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        String username = SecurityUtils.getCurrentUsername();
        ArReport report = reportService.generateReport(enterpriseId, auditYear, username);
        return R.ok(report);
    }

    @Operation(summary = "List enterprise reports")
    @GetMapping("/list")
    public R<List<ArReport>> list(@RequestParam(required = false) Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<ArReport> reports = reportService.listReports(enterpriseId, auditYear);
        return R.ok(reports);
    }

    @Operation(summary = "Get report detail")
    @GetMapping("/{id}")
    public R<ArReport> detail(@PathVariable Long id) {
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
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        ArReport report = reportService.getReport(id);
        if (report == null || !report.getEnterpriseId().equals(enterpriseId)) {
            return ResponseEntity.notFound().build();
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
}
