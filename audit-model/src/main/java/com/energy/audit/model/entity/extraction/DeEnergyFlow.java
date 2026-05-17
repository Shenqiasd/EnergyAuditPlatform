package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEnergyFlow extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String flowStage;
    private Integer seqNo;
    private String sourceUnit;
    private Long sourceUnitId;
    private String targetUnit;
    private Long targetUnitId;
    private String energyProduct;
    private BigDecimal physicalQuantity;
    private BigDecimal standardQuantity;
    private String remark;

    // v2 typed columns
    private String sourceType;
    private Long sourceRefId;
    private String targetType;
    private Long targetRefId;
    private String itemType;
    private Long itemId;
    private BigDecimal calculatedValue;
}
