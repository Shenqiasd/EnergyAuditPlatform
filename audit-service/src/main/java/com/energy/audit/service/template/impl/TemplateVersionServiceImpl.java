package com.energy.audit.service.template.impl;

import com.energy.audit.model.entity.template.TplTemplateVersion;
import com.energy.audit.service.template.TemplateVersionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateVersionServiceImpl implements TemplateVersionService {

    @Override
    public TplTemplateVersion getById(Long id) {
        return null;
    }

    @Override
    public List<TplTemplateVersion> listByTemplateId(Long templateId) {
        return new ArrayList<>();
    }

    @Override
    public List<TplTemplateVersion> listVersionsMeta(Long templateId) {
        return new ArrayList<>();
    }

    @Override
    public TplTemplateVersion getLatest(Long templateId) {
        return null;
    }

    @Override
    public TplTemplateVersion getPublished(Long templateId) {
        return null;
    }

    @Override
    public void create(TplTemplateVersion version) {
    }

    @Override
    public TplTemplateVersion createDraftVersion(Long templateId) {
        TplTemplateVersion v = new TplTemplateVersion();
        v.setTemplateId(templateId);
        v.setVersion(1);
        v.setPublished(0);
        return v;
    }

    @Override
    public void publish(Long templateId, Long versionId) {
    }

    @Override
    public void saveJson(Long versionId, String json, String changeLog) {
    }

    @Override
    public void update(TplTemplateVersion version) {
    }

    @Override
    public void delete(Long id) {
    }
}
