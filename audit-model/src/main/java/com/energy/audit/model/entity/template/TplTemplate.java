package com.energy.audit.model.entity.template;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Template entity
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TplTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Template code */
    private String templateCode;

    /** Template name */
    private String templateName;

    /** Module type */
    private String moduleType;

    /** Description */
    private String description;

    /** Current version number */
    private Integer currentVersion;

    /** Status (0=draft, 1=published, 2=archived) */
    private Integer status;
}
