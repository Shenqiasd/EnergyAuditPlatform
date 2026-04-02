package com.energy.audit.service.system.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.system.SysDictDataMapper;
import com.energy.audit.dao.mapper.system.SysDictTypeMapper;
import com.energy.audit.model.entity.system.SysDictData;
import com.energy.audit.model.entity.system.SysDictType;
import com.energy.audit.service.system.SysDictService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * System dictionary service implementation with Ehcache caching
 */
@Service
public class SysDictServiceImpl implements SysDictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;

    public SysDictServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictDataMapper dictDataMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
    }

    // ---- Dict Type ----

    @Override
    public SysDictType getTypeById(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException("Dict type not found: " + id);
        }
        return type;
    }

    @Override
    public List<SysDictType> listTypes(SysDictType query) {
        return dictTypeMapper.selectList(query);
    }

    @Override
    public void createType(SysDictType dictType) {
        String operator = SecurityUtils.getCurrentUsername();
        dictType.setCreateBy(operator);
        dictType.setUpdateBy(operator);
        if (dictType.getStatus() == null) {
            dictType.setStatus(1);
        }
        dictTypeMapper.insert(dictType);
    }

    @Override
    @CacheEvict(value = "dictCache", key = "#dictType.dictType", condition = "#dictType.dictType != null")
    public void updateType(SysDictType dictType) {
        String operator = SecurityUtils.getCurrentUsername();
        dictType.setUpdateBy(operator);
        dictTypeMapper.updateById(dictType);
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        SysDictType type = getTypeById(id);
        String operator = SecurityUtils.getCurrentUsername();
        // Delete associated data first
        dictDataMapper.deleteByDictType(type.getDictType(), operator);
        dictTypeMapper.deleteById(id, operator);
    }

    // ---- Dict Data ----

    @Override
    public SysDictData getDataById(Long id) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data == null) {
            throw new BusinessException("Dict data not found: " + id);
        }
        return data;
    }

    @Override
    public List<SysDictData> listData(SysDictData query) {
        return dictDataMapper.selectList(query);
    }

    @Override
    @Cacheable(value = "dictCache", key = "#dictType")
    public List<SysDictData> getDataByType(String dictType) {
        return dictDataMapper.selectByDictType(dictType);
    }

    @Override
    @CacheEvict(value = "dictCache", key = "#dictData.dictType", condition = "#dictData.dictType != null")
    public void createData(SysDictData dictData) {
        String operator = SecurityUtils.getCurrentUsername();
        dictData.setCreateBy(operator);
        dictData.setUpdateBy(operator);
        if (dictData.getStatus() == null) {
            dictData.setStatus(1);
        }
        if (dictData.getDictSort() == null) {
            dictData.setDictSort(0);
        }
        dictDataMapper.insert(dictData);
    }

    @Override
    @CacheEvict(value = "dictCache", key = "#dictData.dictType", condition = "#dictData.dictType != null")
    public void updateData(SysDictData dictData) {
        String operator = SecurityUtils.getCurrentUsername();
        dictData.setUpdateBy(operator);
        dictDataMapper.updateById(dictData);
    }

    @Override
    public void deleteData(Long id) {
        SysDictData data = getDataById(id);
        String operator = SecurityUtils.getCurrentUsername();
        dictDataMapper.deleteById(id, operator);
        // Evict cache for this dict type
        evictDictCache(data.getDictType());
    }

    @CacheEvict(value = "dictCache", key = "#dictType")
    public void evictDictCache(String dictType) {
        // Cache eviction handled by annotation
    }
}
