package com.energy.audit.model.entity.system;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * System dictionary data entity — matches sys_dict_data production schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictData extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Dict type (FK to sys_dict_type.dict_type) */
    private String dictType;

    /** Display label */
    private String dictLabel;

    /** Stored value */
    private String dictValue;

    /** Sort order */
    private Integer dictSort;

    /** CSS class / tag type (success/warning/danger/info) */
    private String cssClass;

    /** Status (0=disabled, 1=enabled) */
    private Integer status;

    /** Remark */
    private String remark;
}
