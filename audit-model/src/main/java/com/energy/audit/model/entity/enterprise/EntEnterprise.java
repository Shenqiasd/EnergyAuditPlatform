package com.energy.audit.model.entity.enterprise;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Enterprise entity — matches ent_enterprise production schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EntEnterprise extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Enterprise name */
    private String enterpriseName;

    /** Unified social credit code */
    private String creditCode;

    /** Contact person */
    private String contactPerson;

    /** Contact email */
    private String contactEmail;

    /** Contact phone */
    private String contactPhone;

    /** Remark */
    private String remark;

    /** Expiry date */
    private LocalDate expireDate;

    /** Locked flag (0=unlocked, 1=locked) */
    private Integer isLocked;

    /** Active flag (0=inactive, 1=active) */
    private Integer isActive;

    /** Last login time */
    private LocalDateTime lastLoginTime;

    /** Sort order */
    private Integer sortOrder;
}
