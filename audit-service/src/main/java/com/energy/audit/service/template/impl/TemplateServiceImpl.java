package com.energy.audit.service.template.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplTemplateMapper;
import com.energy.audit.dao.mapper.template.TplTemplateVersionMapper;
import com.energy.audit.model.entity.template.TplTemplate;
import com.energy.audit.model.entity.template.TplTemplateVersion;
import com.energy.audit.service.template.TemplateService;
import com.energy.audit.service.template.TemplateVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Template service implementation
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private final TplTemplateMapper templateMapper;
    private final TplTemplateVersionMapper versionMapper;
    private final TemplateVersionService versionService;

    public TemplateServiceImpl(TplTemplateMapper templateMapper,
                               TplTemplateVersionMapper versionMapper,
                               TemplateVersionService versionService) {
        this.templateMapper = templateMapper;
        this.versionMapper = versionMapper;
        this.versionService = versionService;
    }

    @Override
    @Cacheable(cacheNames = "templateCache", key = "#id")
    public TplTemplate getById(Long id) {
        TplTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("模板不存在: " + id);
        }
        return template;
    }

    @Override
    @Cacheable(cacheNames = "templateCache", key = "'code:' + #templateCode")
    public TplTemplate getByCode(String templateCode) {
        TplTemplate template = templateMapper.selectByCode(templateCode);
        if (template == null) {
            throw new BusinessException("模板不存在，code=" + templateCode);
        }
        return template;
    }

    @Override
    public List<TplTemplate> list(TplTemplate query) {
        return templateMapper.selectList(query);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void create(TplTemplate template) {
        String operator = SecurityUtils.getRequiredCurrentUsername();
        template.setCurrentVersion(1);
        template.setStatus(0);
        template.setCreateBy(operator);
        template.setUpdateBy(operator);
        templateMapper.insert(template);
        versionService.createDraftVersion(template.getId());
        log.info("Template created: id={} code={}", template.getId(), template.getTemplateCode());
    }

    @Override
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void update(TplTemplate template) {
        getById(template.getId());
        template.setUpdateBy(SecurityUtils.getRequiredCurrentUsername());
        templateMapper.updateById(template);
    }

    @Override
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void delete(Long id) {
        getById(id);
        templateMapper.deleteById(id, SecurityUtils.getRequiredCurrentUsername());
    }

    /**
     * Publish the latest draft version of a template.
     * Use POST /versions/{versionId}/publish for a specific version.
     */
    @Override
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void publish(Long templateId) {
        List<TplTemplateVersion> versions = versionMapper.selectListByTemplateId(templateId);
        TplTemplateVersion draft = versions.stream()
                .filter(v -> v.getPublished() == 0)
                .findFirst()
                .orElseThrow(() -> new BusinessException("没有可发布的草稿版本"));
        versionService.publish(templateId, draft.getId());
        log.info("Template published: id={} version={}", templateId, draft.getVersion());
    }
}
