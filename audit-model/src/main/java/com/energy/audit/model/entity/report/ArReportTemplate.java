package com.energy.audit.model.entity.report;

import com.energy.audit.model.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Report template entity — maps to ar_report_template.
 * Stores uploaded Word (.docx) templates with batch annotations for report generation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArReportTemplate extends BaseEntity {

    /** Template display name */
    private String templateName;

    /** File path to the .docx template on disk (legacy, may be null) */
    private String templateFilePath;

    /** Template file content stored as binary (survives container restarts) */
    @JsonIgnore
    private byte[] templateFileData;

    /** Original file name (e.g. "report_template_0417.doc") */
    private String originalFileName;

    /** Template version number */
    private Integer version;

    /** Status: 0=draft, 1=active */
    private Integer status;
}
