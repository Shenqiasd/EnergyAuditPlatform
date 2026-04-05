package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplTagMapping;

import java.util.List;

public interface TagMappingService {

    List<TplTagMapping> listByTemplateId(Long templateId);

    List<TplTagMapping> listByVersionId(Long versionId);

    void saveBatch(Long templateId, List<TplTagMapping> mappings);

    void replaceAll(Long versionId, List<TplTagMapping> mappings);

    void syncFromTemplateJson(Long versionId, String templateJson);
}
