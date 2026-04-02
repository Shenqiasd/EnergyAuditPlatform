package com.energy.audit.model.entity.system;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * System role entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRole extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Role name */
    private String roleName;

    /** Role key */
    private String roleKey;

    /** Sort order */
    private Integer sortOrder;

    /** Status (0=disabled, 1=enabled) */
    private Integer status;

    /** Remark */
    private String remark;
}
