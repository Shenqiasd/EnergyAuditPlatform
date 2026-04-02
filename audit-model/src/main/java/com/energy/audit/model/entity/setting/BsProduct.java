package com.energy.audit.model.entity.setting;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Product base setting entity — matches bs_product production schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BsProduct extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Enterprise ID */
    private Long enterpriseId;

    /** Product name */
    private String name;

    /** Measurement unit */
    private String measurementUnit;

    /** Unit price (10k CNY) */
    private BigDecimal unitPrice;

    /** Remark */
    private String remark;
}
