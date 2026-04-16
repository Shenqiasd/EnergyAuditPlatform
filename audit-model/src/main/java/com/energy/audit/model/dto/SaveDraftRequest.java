package com.energy.audit.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /template/submission/draft
 */
public class SaveDraftRequest {

    @NotNull(message = "templateId 不能为空")
    private Long templateId;

    @NotNull(message = "auditYear 不能为空")
    private Integer auditYear;

    @NotNull(message = "templateVersion 不能为空")
    private Integer templateVersion;

    private String submissionJson;

    /** Optional: when provided, data extraction runs on save (best-effort). */
    private Long templateVersionId;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Integer getAuditYear() { return auditYear; }
    public void setAuditYear(Integer auditYear) { this.auditYear = auditYear; }

    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }

    public String getSubmissionJson() { return submissionJson; }
    public void setSubmissionJson(String submissionJson) { this.submissionJson = submissionJson; }

    public Long getTemplateVersionId() { return templateVersionId; }
    public void setTemplateVersionId(Long templateVersionId) { this.templateVersionId = templateVersionId; }
}
