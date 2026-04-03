package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeCarbonEmission extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String emissionCategory;
    private String sourceName;
    private String measurementUnit;
    private BigDecimal emissionFactor;
    private BigDecimal activityData;
    private BigDecimal co2Emission;
    private String remark;
}
