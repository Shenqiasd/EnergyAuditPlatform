package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEquipmentDetail extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String equipmentType;
    private String equipmentName;
    private String model;
    private Integer quantity;
    private String capacity;
    private BigDecimal annualRuntimeHours;
    private BigDecimal annualEnergy;
    private String energyUnit;
    private String energyEfficiency;
    private String installLocation;
    private String detailJson;
    private String remark;
}
