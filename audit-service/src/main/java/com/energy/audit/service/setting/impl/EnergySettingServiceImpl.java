package com.energy.audit.service.setting.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.setting.BsEnergyMapper;
import com.energy.audit.model.entity.setting.BsEnergy;
import com.energy.audit.service.setting.EnergySettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Energy setting service implementation
 */
@Service
public class EnergySettingServiceImpl implements EnergySettingService {

    private static final Logger log = LoggerFactory.getLogger(EnergySettingServiceImpl.class);

    private final BsEnergyMapper energyMapper;

    public EnergySettingServiceImpl(BsEnergyMapper energyMapper) {
        this.energyMapper = energyMapper;
    }

    @Override
    @Cacheable(cacheNames = "energyCache", key = "#id")
    public BsEnergy getById(Long id) {
        BsEnergy energy = energyMapper.selectById(id);
        if (energy == null) {
            throw new BusinessException("Energy setting not found: " + id);
        }
        return energy;
    }

    @Override
    public List<BsEnergy> list(BsEnergy query) {
        return energyMapper.selectList(query);
    }

    @Override
    @CacheEvict(cacheNames = "energyCache", allEntries = true)
    public void create(BsEnergy energy) {
        // TODO: validate unique energy code
        energyMapper.insert(energy);
    }

    @Override
    @CacheEvict(cacheNames = "energyCache", key = "#energy.id")
    public void update(BsEnergy energy) {
        // TODO: validate energy exists
        energyMapper.updateById(energy);
    }

    @Override
    @CacheEvict(cacheNames = "energyCache", key = "#id")
    public void delete(Long id) {
        // TODO: validate energy exists, check dependencies
        String operator = SecurityUtils.getCurrentUsername();
        energyMapper.deleteById(id, operator);
    }
}
