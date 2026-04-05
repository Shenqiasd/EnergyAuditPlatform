package com.energy.audit.service.template.impl;

import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.service.template.TagMappingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagMappingServiceImpl implements TagMappingService {

    @Override
    public List<TplTagMapping> listByTemplateId(Long templateId) {
        return new ArrayList<>();
    }

    @Override
    public List<TplTagMapping> listByVersionId(Long versionId) {
        return new ArrayList<>();
    }

    @Override
    public void saveBatch(Long templateId, List<TplTagMapping> mappings) {
    }

    @Override
    public void replaceAll(Long versionId, List<TplTagMapping> mappings) {
    }

    @Override
    public void syncFromTemplateJson(Long versionId, String templateJson) {
    }
}
