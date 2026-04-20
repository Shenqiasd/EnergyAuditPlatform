package com.energy.audit.service.report;

import com.energy.audit.model.entity.report.ArReport;
import com.energy.audit.model.entity.report.ArReportTemplate;

import java.util.List;

public interface ReportService {

    ArReport generateReport(Long enterpriseId, Integer auditYear, String username);

    List<ArReport> listReports(Long enterpriseId, Integer auditYear);

    ArReport getReport(Long id);

    byte[] downloadReport(Long id);

    // ====== Template-based report generation (Phase 1) ======

    /**
     * Generate a report from a Word template using SpreadJS submission data.
     * The submission must have status=2 (approved) before generation is allowed.
     *
     * @param submissionId   the tpl_submission ID with approved fill data
     * @param flowChartImage PNG bytes of the energy flow diagram (from AntV X6 screenshot), nullable
     * @param username       current user
     * @return the generated report record
     */
    ArReport generateReportFromTemplate(Long submissionId, Long callerEnterpriseId, byte[] flowChartImage, String username);

    /**
     * Save edited HTML content for a report (from TinyMCE editor).
     */
    ArReport saveReportHtml(Long reportId, String html, String username);

    /**
     * Submit a report for auditor review.
     */
    ArReport submitForReview(Long reportId, String username);

    /**
     * List available report templates.
     */
    List<ArReportTemplate> listTemplates();
}
