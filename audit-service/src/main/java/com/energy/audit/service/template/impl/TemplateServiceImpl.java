package com.energy.audit.service.template.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplTemplateMapper;
import com.energy.audit.model.entity.template.TplTemplate;
import com.energy.audit.service.template.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Template service implementation
 */
@Service
public class TemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private final TplTemplateMapper templateMapper;

    public TemplateServiceImpl(TplTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    @Override
    @Cacheable(cacheNames = "templateCache", key = "#id")
    public TplTemplate getById(Long id) {
        TplTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException("Template not found: " + id);
        }
        return template;
    }

    @Override
    @Cacheable(cacheNames = "templateCache", key = "'code:' + #templateCode")
    public TplTemplate getByCode(String templateCode) {
        TplTemplate template = templateMapper.selectByCode(templateCode);
        if (template == null) {
            throw new BusinessException("Template not found: " + templateCode);
        }
        return template;
    }

    @Override
    public List<TplTemplate> list(TplTemplate query) {
        return templateMapper.selectList(query);
    }

    @Override
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void create(TplTemplate template) {
        // TODO: validate unique template code, set initial version
        template.setCurrentVersion(1);
        template.setStatus(0); // draft
        templateMapper.insert(template);
    }

    @Override
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void update(TplTemplate template) {
        // TODO: validate template exists
        templateMapper.updateById(template);
    }

    @Override
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void delete(Long id) {
        // TODO: validate template exists, check dependencies
        String operator = SecurityUtils.getCurrentUsername();
        templateMapper.deleteById(id, operator);
    }

    @Override
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void publish(Long templateId) {
        TplTemplate template = getById(templateId);
        // TODO: create new template version, update status to published
        template.setStatus(1); // published
        template.setCurrentVersion(template.getCurrentVersion() + 1);
        templateMapper.updateById(template);
        log.info("Template published: id={}, version={}", templateId, template.getCurrentVersion());
    }
}
