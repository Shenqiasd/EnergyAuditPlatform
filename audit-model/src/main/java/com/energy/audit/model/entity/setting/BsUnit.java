package com.energy.audit.model.entity.setting;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Unit (process unit) entity — matches bs_unit production schema.
 * unitType: 1=Processing/Conversion, 2=Distribution/Transport, 3=Terminal Use
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BsUnit extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Enterprise ID */
    private Long enterpriseId;

    /** Unit name */
    private String name;

    /** Unit type (1=processing/conversion 2=distribution/transport 3=terminal use) */
    private Integer unitType;

    /** Sub-category (dictionary, used when unitType=3) */
    private String subCategory;

    /** Remark */
    private String remark;
}
