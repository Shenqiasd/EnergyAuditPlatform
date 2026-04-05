package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplTemplateVersion;

import java.util.List;

public interface TemplateVersionService {

    TplTemplateVersion getById(Long id);

    List<TplTemplateVersion> listByTemplateId(Long templateId);

    List<TplTemplateVersion> listVersionsMeta(Long templateId);

    TplTemplateVersion getLatest(Long templateId);

    TplTemplateVersion getPublished(Long templateId);

    void create(TplTemplateVersion version);

    TplTemplateVersion createDraftVersion(Long templateId);

    void publish(Long templateId, Long versionId);

    void saveJson(Long versionId, String json, String changeLog);

    void update(TplTemplateVersion version);

    void delete(Long id);
}
