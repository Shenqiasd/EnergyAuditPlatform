package com.energy.audit.service.template.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplTagMappingMapper;
import com.energy.audit.dao.mapper.template.TplTemplateMapper;
import com.energy.audit.dao.mapper.template.TplTemplateVersionMapper;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.model.entity.template.TplTemplate;
import com.energy.audit.model.entity.template.TplTemplateVersion;
import com.energy.audit.service.template.TagMappingService;
import com.energy.audit.service.template.TemplateVersionService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TemplateVersionServiceImpl implements TemplateVersionService {

    private final TplTemplateVersionMapper versionMapper;
    private final TplTemplateMapper templateMapper;
    private final TagMappingService tagMappingService;
    private final TplTagMappingMapper tagMappingMapper;

    public TemplateVersionServiceImpl(TplTemplateVersionMapper versionMapper,
                                      TplTemplateMapper templateMapper,
                                      TagMappingService tagMappingService,
                                      TplTagMappingMapper tagMappingMapper) {
        this.versionMapper = versionMapper;
        this.templateMapper = templateMapper;
        this.tagMappingService = tagMappingService;
        this.tagMappingMapper = tagMappingMapper;
    }

    @Override
    @Transactional
    public TplTemplateVersion createDraftVersion(Long templateId) {
        String operator = SecurityUtils.getRequiredCurrentUsername();
        Integer max = versionMapper.selectMaxVersion(templateId);
        int nextVersion = (max == null ? 0 : max) + 1;

        // ── Inherit templateJson from previous version (prefer latest published, fallback to highest version) ──
        String inheritedJson = "{}";
        TplTemplateVersion previousVersion = null;
        if (max != null && max > 0) {
            // Try the latest published version first
            previousVersion = versionMapper.selectPublishedByTemplateId(templateId);
            if (previousVersion == null) {
                // No published version — use the latest version (highest version number)
                List<TplTemplateVersion> allVersions = versionMapper.selectListByTemplateId(templateId);
                if (!allVersions.isEmpty()) {
                    previousVersion = allVersions.get(0); // ordered by version DESC
                }
            }
            if (previousVersion != null && previousVersion.getTemplateJson() != null
                    && !previousVersion.getTemplateJson().isBlank()) {
                inheritedJson = previousVersion.getTemplateJson();
            }
        }

        TplTemplateVersion v = new TplTemplateVersion();
        v.setTemplateId(templateId);
        v.setVersion(nextVersion);
        v.setTemplateJson(inheritedJson);
        v.setPublished(0);
        v.setCreateBy(operator);
        v.setUpdateBy(operator);
        versionMapper.insert(v);

        // ── Copy tag mappings from the previous version ──
        if (previousVersion != null) {
            List<TplTagMapping> previousTags = tagMappingService.listByVersionId(previousVersion.getId());
            if (previousTags != null && !previousTags.isEmpty()) {
                for (TplTagMapping src : previousTags) {
                    TplTagMapping copy = new TplTagMapping();
                    copy.setTemplateVersionId(v.getId());
                    copy.setTagName(src.getTagName());
                    copy.setFieldName(src.getFieldName());
                    copy.setTargetTable(src.getTargetTable());
                    copy.setDataType(src.getDataType());
                    copy.setDictType(src.getDictType());
                    copy.setRequired(src.getRequired());
                    copy.setSheetIndex(src.getSheetIndex());
                    copy.setSheetName(src.getSheetName());
                    copy.setCellRange(src.getCellRange());
                    copy.setMappingType(src.getMappingType());
                    copy.setSourceType(src.getSourceType());
                    copy.setRowKeyColumn(src.getRowKeyColumn());
                    copy.setColumnMappings(src.getColumnMappings());
                    copy.setHeaderRow(src.getHeaderRow());
                    copy.setRemark(src.getRemark());
                    copy.setCreateBy(operator);
                    copy.setUpdateBy(operator);
                    tagMappingMapper.insert(copy);
                }
            }
        }

        return v;
    }

    @Override
    @Transactional
    public void saveJson(Long versionId, String templateJson, String changeLog) {
        TplTemplateVersion existing = getById(versionId);
        if (existing.getPublished() == 1) {
            throw new BusinessException("已发布的版本不可修改，请先创建新版本");
        }
        TplTemplateVersion upd = new TplTemplateVersion();
        upd.setId(versionId);
        upd.setTemplateJson(templateJson);
        upd.setChangeLog(changeLog);
        upd.setUpdateBy(SecurityUtils.getRequiredCurrentUsername());
        versionMapper.updateById(upd);
        tagMappingService.syncFromTemplateJson(versionId, templateJson);
    }

    @Override
    public List<TplTemplateVersion> listVersions(Long templateId) {
        return versionMapper.selectListByTemplateId(templateId);
    }

    @Override
    public List<TplTemplateVersion> listVersionsMeta(Long templateId) {
        return versionMapper.selectListMetaByTemplateId(templateId);
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
        String operator = SecurityUtils.getRequiredCurrentUsername();
        versionMapper.updatePublishStatus(versionId, operator);

        TplTemplate upd = new TplTemplate();
        upd.setId(templateId);
        upd.setCurrentVersion(v.getVersion());
        upd.setStatus(1);
        upd.setUpdateBy(operator);
        templateMapper.updateById(upd);
    }
}
