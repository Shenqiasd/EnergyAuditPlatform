package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeFiveYearTarget extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String sectionType;
    private String yearLabel;
    private BigDecimal grossOutput;
    private BigDecimal energyEquiv;
    private BigDecimal energyEqual;
    private BigDecimal unitEnergyEquiv;
    private BigDecimal unitEnergyEqual;
    private BigDecimal declineRate;
    private String productName;
    private String indicatorName;
    private BigDecimal indicatorValue;
    private BigDecimal actualValue;
    private BigDecimal energyControlTotal;
    private BigDecimal productUnitConsumption;
    private BigDecimal savingAmount;
}
