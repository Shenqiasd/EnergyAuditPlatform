package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeTechIndicator extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private Integer indicatorYear;
    private BigDecimal grossOutput;
    private BigDecimal salesRevenue;
    private BigDecimal taxPaid;
    private BigDecimal energyTotalCost;
    private BigDecimal productionCost;
    private BigDecimal energyCostRatio;
    private BigDecimal totalEnergyEquiv;
    private BigDecimal totalEnergyEqual;
    private BigDecimal totalEnergyExclMaterial;
    private BigDecimal unitOutputEnergyEquiv;
    private BigDecimal unitOutputEnergyEqual;
    private Integer savingProjectCount;
    private BigDecimal savingInvestTotal;
    private BigDecimal savingCapacity;
    private BigDecimal savingBenefit;
    private BigDecimal coalTarget;
    private BigDecimal coalActual;
    private Integer employeeCount;
    private Integer energyManagerCount;
    private BigDecimal totalEnergyEquivExclGreen;
    private BigDecimal totalEnergyEqualExclGreen;
    private BigDecimal rawMaterialEnergy;
    private BigDecimal electrificationRate;
    private BigDecimal totalEnergyEqualExclMaterial;
}
