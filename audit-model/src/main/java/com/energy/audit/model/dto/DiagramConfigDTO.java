package com.energy.audit.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Diagram config (diagram + nodes + edges) — nested in EnergyFlowConfigDTO
 * and also the body of PUT /energy-flow/config for the diagram portion.
 */
@Data
public class DiagramConfigDTO implements Serializable {

    private Long id;
    private String name;
    private Integer diagramType;
    private Integer canvasWidth;
    private Integer canvasHeight;
    private String backgroundColor;
    private List<FlowNodeDTO> nodes;
    private List<FlowEdgeDTO> edges;

    @Data
    public static class FlowNodeDTO implements Serializable {
        private Long id;
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

    @Data
    public static class FlowEdgeDTO implements Serializable {
        private Long id;
        private String edgeId;
        private String sourceNodeId;
        private String targetNodeId;
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
}
