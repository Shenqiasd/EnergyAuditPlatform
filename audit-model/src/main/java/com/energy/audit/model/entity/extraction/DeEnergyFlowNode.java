package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEnergyFlowNode extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long diagramId;
    private String nodeId;
    private String nodeType;
    private String refType;
    private Long refId;
    private String label;
    private Double positionX;
    private Double positionY;
    private Double width;
    private Double height;
    private String color;
    private Integer visible;
    private Integer locked;
}
