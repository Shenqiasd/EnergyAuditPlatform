package com.energy.audit.model.entity.enterprise;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Enterprise registration application entity — matches ent_registration production schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EntRegistration extends BaseEntity {

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

    /** Apply IP */
    private String applyIp;

    /** Apply application number */
    private String applyNo;

    /** Apply time */
    private LocalDateTime applyTime;

    /** Audit status (0=pending, 1=approved, 2=rejected) */
    private Integer auditStatus;

    /** Auditor user ID */
    private Long auditUserId;

    /** Audit time */
    private LocalDateTime auditTime;

    /** Audit remark */
    private String auditRemark;
}
