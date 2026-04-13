package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplTemplateVersion;

import java.util.List;

/**
 * Service for managing template versions (tpl_template_version)
 */
public interface TemplateVersionService {

    /** Create an initial draft version (version=1, published=0) for a newly-created template */
    TplTemplateVersion createDraftVersion(Long templateId);

    /** Update the SpreadJS JSON content of a draft version */
    void saveJson(Long versionId, String templateJson, String changeLog);

    /** Update the SpreadJS JSON content + protection flag of a draft version */
    default void saveJson(Long versionId, String templateJson, String changeLog, Integer protectionEnabled) {
        saveJson(versionId, templateJson, changeLog);
    }

    /** List all versions of a template ordered by version DESC (includes template_json) */
    List<TplTemplateVersion> listVersions(Long templateId);

    /** List versions without the LONGTEXT template_json — for list/table views */
    List<TplTemplateVersion> listVersionsMeta(Long templateId);

    /** Return the latest published version, or null if none */
    TplTemplateVersion getPublished(Long templateId);

    TplTemplateVersion getById(Long versionId);

    /**
     * Publish the specified version:
     *  1. Sets published=1 / publish_time=now on the version row
     *  2. Updates tpl_template.current_version to this version number
     */
    void publish(Long templateId, Long versionId);

    /**
     * Delete a version (soft-delete).
     * Published versions cannot be deleted.
     * Also soft-deletes all associated tag mappings.
     */
    void deleteVersion(Long templateId, Long versionId);
}
