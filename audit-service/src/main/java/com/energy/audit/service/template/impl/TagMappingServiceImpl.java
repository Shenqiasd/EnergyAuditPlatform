package com.energy.audit.service.template.impl;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplTagMappingMapper;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.service.template.TagMappingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagMappingServiceImpl implements TagMappingService {

    private final TplTagMappingMapper tagMappingMapper;

    public TagMappingServiceImpl(TplTagMappingMapper tagMappingMapper) {
        this.tagMappingMapper = tagMappingMapper;
    }

    @Override
    @Transactional
    public void replaceAll(Long templateVersionId, List<TplTagMapping> mappings) {
        String operator = SecurityUtils.getRequiredCurrentUsername();
        tagMappingMapper.deleteByVersionId(templateVersionId, operator);
        if (mappings != null && !mappings.isEmpty()) {
            mappings.forEach(m -> {
                m.setTemplateVersionId(templateVersionId);
                m.setCreateBy(operator);
                m.setUpdateBy(operator);
            });
            tagMappingMapper.batchInsert(mappings);
        }
    }

    @Override
    public List<TplTagMapping> listByVersionId(Long templateVersionId) {
        return tagMappingMapper.selectListByVersionId(templateVersionId);
    }
}
