package com.energy.audit.model.entity.report;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Audit report entity — maps to ar_report.
 * Status: 0=draft, 1=generating, 2=generated, 3=failed, 4=submitted_for_review,
 *         5=review_approved, 6=review_rejected
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArReport extends BaseEntity {

    private Long enterpriseId;
    private Integer auditYear;
    private String reportName;
    private Integer reportType;
    private Integer status;
    private String generatedFilePath;
    private String uploadedFilePath;
    private String onlyofficeDocKey;
    private LocalDateTime generateTime;
    private LocalDateTime submitTime;

    /** HTML content for TinyMCE online editing */
    private String reportHtml;

    /** Template ID used for generation -> ar_report_template.id */
    private Long templateId;

    /** Submission ID that was used for data extraction -> tpl_submission.id */
    private Long submissionId;

    /** Path to energy flow chart image (PNG) */
    private String flowChartPath;

    /** Review comment from auditor (for review workflow) */
    private String reviewComment;

    /** Reviewer user ID */
    private Long reviewerId;

    private transient String enterpriseName;
}
