package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeProductUnitConsumption extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String indicatorName;
    private String indicatorUnit;
    private String numeratorUnit;
    private String denominatorUnit;
    private BigDecimal conversionFactor;
    private BigDecimal currentIndicator;
    private BigDecimal currentNumerator;
    private BigDecimal currentDenominator;
    private BigDecimal previousIndicator;
    private BigDecimal previousNumerator;
    private BigDecimal previousDenominator;
}
