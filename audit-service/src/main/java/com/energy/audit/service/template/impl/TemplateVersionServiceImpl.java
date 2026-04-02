package com.energy.audit.service.template.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplTemplateMapper;
import com.energy.audit.dao.mapper.template.TplTemplateVersionMapper;
import com.energy.audit.model.entity.template.TplTemplate;
import com.energy.audit.model.entity.template.TplTemplateVersion;
import com.energy.audit.service.template.TemplateVersionService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateVersionServiceImpl implements TemplateVersionService {

    private final TplTemplateVersionMapper versionMapper;
    private final TplTemplateMapper templateMapper;

    public TemplateVersionServiceImpl(TplTemplateVersionMapper versionMapper,
                                      TplTemplateMapper templateMapper) {
        this.versionMapper = versionMapper;
        this.templateMapper = templateMapper;
    }

    @Override
    public TplTemplateVersion createDraftVersion(Long templateId) {
        String operator = SecurityUtils.getCurrentUsername();
        Integer max = versionMapper.selectMaxVersion(templateId);
        int nextVersion = (max == null ? 0 : max) + 1;
        TplTemplateVersion v = new TplTemplateVersion();
        v.setTemplateId(templateId);
        v.setVersion(nextVersion);
        v.setTemplateJson("{}");
        v.setPublished(0);
        v.setCreateBy(operator);
        v.setUpdateBy(operator);
        versionMapper.insert(v);
        return v;
    }

    @Override
    public void saveJson(Long versionId, String templateJson, String changeLog) {
        TplTemplateVersion existing = getById(versionId);
        if (existing.getPublished() == 1) {
            throw new BusinessException("已发布的版本不可修改，请先创建新版本");
        }
        TplTemplateVersion upd = new TplTemplateVersion();
        upd.setId(versionId);
        upd.setTemplateJson(templateJson);
        upd.setChangeLog(changeLog);
        upd.setUpdateBy(SecurityUtils.getCurrentUsername());
        versionMapper.updateById(upd);
    }

    @Override
    public List<TplTemplateVersion> listVersions(Long templateId) {
        return versionMapper.selectListByTemplateId(templateId);
    }

    @Override
    public TplTemplateVersion getPublished(Long templateId) {
        return versionMapper.selectPublishedByTemplateId(templateId);
    }

    @Override
    public TplTemplateVersion getById(Long versionId) {
        TplTemplateVersion v = versionMapper.selectById(versionId);
        if (v == null) {
            throw new BusinessException("模板版本不存在: " + versionId);
        }
        return v;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "templateCache", allEntries = true)
    public void publish(Long templateId, Long versionId) {
        TplTemplateVersion v = getById(versionId);
        if (!templateId.equals(v.getTemplateId())) {
            throw new BusinessException("版本不属于该模板");
        }
        String operator = SecurityUtils.getCurrentUsername();
        versionMapper.updatePublishStatus(versionId, operator);

        TplTemplate upd = new TplTemplate();
        upd.setId(templateId);
        upd.setCurrentVersion(v.getVersion());
        upd.setUpdateBy(operator);
        templateMapper.updateById(upd);
    }
}
