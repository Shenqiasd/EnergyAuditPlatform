package com.energy.audit.model.entity.report;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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

    private transient String enterpriseName;
}
