package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEnergyFlowEdge extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long diagramId;
    private String edgeId;
    private String sourceNodeId;
    private String targetNodeId;
    private Long energyId;
    private Long productId;
    private BigDecimal physicalAmount;
    private String remark;

    // v2 columns
    private Long flowRecordId;
    private String itemType;
    private Long itemId;
    private BigDecimal physicalQuantity;
    private BigDecimal calculatedValue;
    private String labelText;
    private String color;
    private Double lineWidth;
    private String routePoints;
    private Integer visible;
}
