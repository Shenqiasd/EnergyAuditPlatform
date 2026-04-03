package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEnergyConversion extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String energyName;
    private String measurementUnit;
    private BigDecimal industrialConsumption;
    private BigDecimal conversionInputTotal;
    private BigDecimal convPowerGen;
    private BigDecimal convHeating;
    private BigDecimal convCoalWashing;
    private BigDecimal convCoking;
    private BigDecimal convRefining;
    private BigDecimal convGasMaking;
    private BigDecimal convLng;
    private BigDecimal convCoalProduct;
    private BigDecimal conversionOutput;
    private BigDecimal conversionOutputStd;
    private BigDecimal recoveryUtilization;
    private BigDecimal equivFactor;
    private BigDecimal equalFactor;
}
