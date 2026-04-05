package com.energy.audit.model.entity.setting;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class BsUnitEnergy extends BaseEntity {
    private Long unitId;
    private Long energyId;
    private String energyName;
    private BigDecimal annualConsumption;
    private String measurementUnit;
}
