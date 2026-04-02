package com.energy.audit.model.entity.system;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * System dictionary type entity — matches sys_dict_type production schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictType extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Dictionary name */
    private String dictName;

    /** Dictionary type identifier (unique) */
    private String dictType;

    /** Status (0=disabled, 1=enabled) */
    private Integer status;

    /** Remark */
    private String remark;
}
