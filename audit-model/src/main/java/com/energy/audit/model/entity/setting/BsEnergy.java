package com.energy.audit.model.entity.setting;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Energy type base setting entity — matches bs_energy production schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BsEnergy extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Enterprise ID */
    private Long enterpriseId;

    /** Energy name */
    private String name;

    /** Energy category (dictionary: solid/liquid/gas/electricity/heat etc.) */
    private String category;

    /** Measurement unit */
    private String measurementUnit;

    /** Standard coal equivalent value */
    private BigDecimal equivalentValue;

    /** Equal value */
    private BigDecimal equalValue;

    /** Low heat value */
    private BigDecimal lowHeatValue;

    /** Carbon content per unit heat value */
    private BigDecimal carbonContent;

    /** Oxidation rate */
    private BigDecimal oxidationRate;

    /** Color */
    private String color;

    /** Active flag (0=inactive, 1=active) */
    private Integer isActive;

    /** Remark */
    private String remark;
}
