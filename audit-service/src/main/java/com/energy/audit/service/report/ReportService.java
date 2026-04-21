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

    // ====== Phase 3: Report Review Workflow (auditor side) ======

    /**
     * List reports for auditor review.
     * @param status   filter by status (4=pending, 5=approved, 6=rejected), null=all
     * @param auditYear filter by audit year, null=all
     */
    List<ArReport> listReportsForReview(Integer status, Integer auditYear);

    /**
     * Approve a submitted report (status 4 -> 5).
     * @param reportId  the report to approve
     * @param reviewComment optional approval comment
     * @param reviewerId the auditor's user ID
     * @param username   the auditor's username
     */
    ArReport approveReport(Long reportId, String reviewComment, Long reviewerId, String username);

    /**
     * Reject/return a submitted report (status 4 -> 6).
     * The enterprise can then edit and re-submit.
     * @param reportId  the report to reject
     * @param reviewComment required rejection reason
     * @param reviewerId the auditor's user ID
     * @param username   the auditor's username
     */
    ArReport rejectReport(Long reportId, String reviewComment, Long reviewerId, String username);
}
