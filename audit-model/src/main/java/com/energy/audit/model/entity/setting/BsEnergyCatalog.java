package com.energy.audit.model.entity.setting;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class BsEnergyCatalog extends BaseEntity {

    private String name;
    private String category;
    private String measurementUnit;
    private BigDecimal equivalentValue;
    private BigDecimal equalValue;
    private BigDecimal lowHeatValue;
    private BigDecimal carbonContent;
    private BigDecimal oxidationRate;
    private String color;
    private Integer isActive;
    private Integer sortOrder;
    private String remark;
}
