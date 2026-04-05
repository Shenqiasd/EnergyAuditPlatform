package com.energy.audit.model.dto;

import lombok.Data;

@Data
public class SaveDraftRequest {
    private Long templateId;
    private Integer auditYear;
    private String submissionJson;
    private Integer templateVersion;
}
