package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEnergyFlowDiagram extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long enterpriseId;
    private Integer auditYear;
    private Integer diagramType;
    private String diagramData;
    private String name;
    private Integer canvasWidth;
    private Integer canvasHeight;
    private String backgroundColor;
}
