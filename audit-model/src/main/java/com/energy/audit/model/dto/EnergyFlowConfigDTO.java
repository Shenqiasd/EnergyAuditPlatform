package com.energy.audit.model.dto;

import com.energy.audit.model.entity.extraction.DeEnergyFlow;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Full energy flow config response — returned by GET /energy-flow/config.
 */
@Data
public class EnergyFlowConfigDTO implements Serializable {

    private EnterpriseInfoDTO enterpriseInfo;
    private List<UnitInfoDTO> units;
    private List<EnergyInfoDTO> energies;
    private List<ProductInfoDTO> products;
    private List<DeEnergyFlow> flowRecords;
    private DiagramConfigDTO diagram;
    private ValidationResultDTO validation;

    @Data
    public static class EnterpriseInfoDTO implements Serializable {
        private Long id;
        private String name;
        private String address;
        private String industry;
    }

    @Data
    public static class UnitInfoDTO implements Serializable {
        private Long id;
        private String name;
        private Integer unitType;
        private String subCategory;
    }

    @Data
    public static class EnergyInfoDTO implements Serializable {
        private Long id;
        private String name;
        private String category;
        private String measurementUnit;
        private java.math.BigDecimal equivalentValue;
    }

    @Data
    public static class ProductInfoDTO implements Serializable {
        private Long id;
        private String name;
        private String measurementUnit;
        private java.math.BigDecimal unitPrice;
    }

    @Data
    public static class ValidationResultDTO implements Serializable {
        private boolean valid;
        private boolean exportReady;
        private boolean enterpriseComplete;
        private boolean hasUnits;
        private boolean hasEnergies;
        private boolean hasProducts;
        private List<String> warnings;
        /** Blocking errors that prevent PNG export */
        private List<String> exportErrors;
    }
}
