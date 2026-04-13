package com.energy.audit.model.entity.template;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Template version entity — maps to tpl_template_version
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TplTemplateVersion extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Template ID -> tpl_template.id */
    private Long templateId;

    /** Version number */
    private Integer version;

    /** SpreadJS template JSON (LONGTEXT) */
    private String templateJson;

    /** Change description */
    private String changeLog;

    /** Published flag (0=draft, 1=published) */
    private Integer published;

    /** Publish timestamp */
    private LocalDateTime publishTime;

    /** Protection enabled flag (0=disabled, 1=enabled). Default 1. */
    private Integer protectionEnabled;
}
