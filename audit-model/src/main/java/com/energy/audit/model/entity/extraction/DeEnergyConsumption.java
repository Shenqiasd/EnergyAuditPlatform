package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEnergyConsumption extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String energyCode;
    private String energyName;
    private String measurementUnit;
    private BigDecimal openingStock;
    private BigDecimal purchaseTotal;
    private BigDecimal purchaseFromProvince;
    private BigDecimal purchaseAmount;
    private BigDecimal industrialConsumption;
    private BigDecimal materialConsumption;
    private BigDecimal transportConsumption;
    private BigDecimal closingStock;
    private BigDecimal externalSupply;
    private BigDecimal equivFactor;
    private BigDecimal equalFactor;
    private BigDecimal standardCoal;
}
