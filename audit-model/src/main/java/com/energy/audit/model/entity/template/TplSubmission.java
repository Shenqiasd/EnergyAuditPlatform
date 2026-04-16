package com.energy.audit.model.entity.template;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Template submission entity — maps to tpl_submission
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TplSubmission extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Enterprise ID -> ent_enterprise.id */
    private Long enterpriseId;

    /** Template ID -> tpl_template.id */
    private Long templateId;

    /** Template version number (snapshot at submission time) */
    private Integer templateVersion;

    /** Audit year */
    private Integer auditYear;

    /** Raw SpreadJS JSON as filled by the enterprise (LONGTEXT) */
    private String submissionJson;

    /** Extracted structured data as JSON string (MySQL JSON / H2 CLOB) */
    private String extractedData;

    /** Status (0=draft, 1=submitted, 2=approved, 3=rejected) */
    private Integer status;

    /** Review comment from auditor (set when status changes to 3) */
    private String reviewComment;

    /** Submission timestamp (set when status changes to 1) */
    private LocalDateTime submitTime;
}
