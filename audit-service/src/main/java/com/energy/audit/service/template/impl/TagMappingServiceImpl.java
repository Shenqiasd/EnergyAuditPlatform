package com.energy.audit.service.template.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplTagMappingMapper;
import com.energy.audit.model.dto.DiscoveredField;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.service.template.SpreadsheetDataExtractor;
import com.energy.audit.service.template.TagMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagMappingServiceImpl implements TagMappingService {

    private static final Logger log = LoggerFactory.getLogger(TagMappingServiceImpl.class);

    private final TplTagMappingMapper tagMappingMapper;
    private final SpreadsheetDataExtractor extractor;

    public TagMappingServiceImpl(TplTagMappingMapper tagMappingMapper,
                                 SpreadsheetDataExtractor extractor) {
        this.tagMappingMapper = tagMappingMapper;
        this.extractor = extractor;
    }

    @Override
    @Transactional
    public void replaceAll(Long templateVersionId, List<TplTagMapping> mappings) {
        String operator = SecurityUtils.getRequiredCurrentUsername();
        tagMappingMapper.deleteByVersionId(templateVersionId, operator);
        if (mappings != null && !mappings.isEmpty()) {
            for (TplTagMapping m : mappings) {
                m.setTemplateVersionId(templateVersionId);
                m.setCreateBy(operator);
                m.setUpdateBy(operator);
                validateMapping(m);
            }
            tagMappingMapper.batchInsert(mappings);
        }
    }

    @Override
    public List<TplTagMapping> listByVersionId(Long templateVersionId) {
        return tagMappingMapper.selectListByVersionId(templateVersionId);
    }

    @Override
    @Transactional
    public void syncFromTemplateJson(Long versionId, String templateJson) {
        String operator = SecurityUtils.getRequiredCurrentUsername();
        List<DiscoveredField> discoveredFields = extractor.discoverFields(templateJson);

        Map<String, DiscoveredField> discoveredByTag = discoveredFields.stream()
                .collect(Collectors.toMap(
                        DiscoveredField::getTagName,
                        f -> f,
                        (first, dup) -> {
                            log.warn("syncFromTemplateJson: duplicate tag '{}' in template, keeping first", first.getTagName());
                            return first;
                        }
                ));

        Map<String, TplTagMapping> existingByTag = tagMappingMapper
                .selectListByVersionId(versionId)
                .stream()
                .collect(Collectors.toMap(
                        TplTagMapping::getTagName,
                        m -> m,
                        (first, dup) -> {
                            log.warn("syncFromTemplateJson: duplicate active tag '{}' for versionId={}", first.getTagName(), versionId);
                            return first;
                        }
                ));

        for (Map.Entry<String, DiscoveredField> entry : discoveredByTag.entrySet()) {
            String tagName = entry.getKey();
            DiscoveredField df = entry.getValue();

            if (!existingByTag.containsKey(tagName)) {
                TplTagMapping m = new TplTagMapping();
                m.setTemplateVersionId(versionId);
                m.setTagName(tagName);
                m.setFieldName(tagName);
                m.setDataType("STRING");
                m.setRequired(0);
                m.setSheetIndex(df.getSheetIndex() != null ? df.getSheetIndex() : 0);
                m.setMappingType(df.getMappingType());
                m.setSourceType(df.getSourceType());
                if ("TABLE".equals(df.getMappingType()) && df.getCellRange() != null) {
                    m.setCellRange(df.getCellRange());
                }
                m.setCreateBy(operator);
                m.setUpdateBy(operator);
                tagMappingMapper.insert(m);
            } else {
                TplTagMapping existing = existingByTag.get(tagName);
                boolean needsUpdate = false;
                if (!df.getSourceType().equals(existing.getSourceType())) {
                    existing.setSourceType(df.getSourceType());
                    needsUpdate = true;
                }
                if (existing.getSheetIndex() == null || !existing.getSheetIndex().equals(df.getSheetIndex())) {
                    existing.setSheetIndex(df.getSheetIndex());
                    needsUpdate = true;
                }
                if ("NAMED_RANGE".equals(df.getSourceType())) {
                    if (!df.getMappingType().equals(existing.getMappingType())) {
                        existing.setMappingType(df.getMappingType());
                        needsUpdate = true;
                    }
                    if ("TABLE".equals(df.getMappingType()) && df.getCellRange() != null) {
                        existing.setCellRange(df.getCellRange());
                        needsUpdate = true;
                    }
                }
                if (needsUpdate) {
                    existing.setUpdateBy(operator);
                    tagMappingMapper.updateById(existing);
                }
            }
        }

        existingByTag.forEach((tagName, mapping) -> {
            if (!discoveredByTag.containsKey(tagName)) {
                tagMappingMapper.softDeleteById(mapping.getId(), operator);
            }
        });
    }

    private void validateMapping(TplTagMapping m) {
        if ("TABLE".equalsIgnoreCase(m.getMappingType())
                && "CELL_TAG".equalsIgnoreCase(m.getSourceType())) {
            if (m.getCellRange() == null || m.getCellRange().isBlank()) {
                throw new BusinessException("Cell Tag 类型的 TABLE 映射「" + m.getTagName() + "」必须配置 cellRange");
            }
            if (m.getHeaderRow() == null) {
                throw new BusinessException("Cell Tag 类型的 TABLE 映射「" + m.getTagName() + "」必须配置 headerRow");
            }
        }
    }
}
