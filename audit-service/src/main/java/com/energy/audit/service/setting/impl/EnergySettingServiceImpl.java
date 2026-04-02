package com.energy.audit.service.setting.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.setting.BsEnergyCatalogMapper;
import com.energy.audit.dao.mapper.setting.BsEnergyMapper;
import com.energy.audit.model.entity.setting.BsEnergy;
import com.energy.audit.model.entity.setting.BsEnergyCatalog;
import com.energy.audit.service.setting.EnergySettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Energy setting service implementation — all mutations are tenant-scoped.
 */
@Service
public class EnergySettingServiceImpl implements EnergySettingService {

    private static final Logger log = LoggerFactory.getLogger(EnergySettingServiceImpl.class);

    private final BsEnergyMapper energyMapper;
    private final BsEnergyCatalogMapper catalogMapper;

    public EnergySettingServiceImpl(BsEnergyMapper energyMapper, BsEnergyCatalogMapper catalogMapper) {
        this.energyMapper = energyMapper;
        this.catalogMapper = catalogMapper;
    }

    /**
     * Tenant-safe getById: verifies that the record belongs to the current enterprise.
     */
    @Override
    @Cacheable(cacheNames = "energyCache", key = "#id + '_' + #enterpriseId")
    public BsEnergy getByIdForEnterprise(Long id, Long enterpriseId) {
        BsEnergy energy = energyMapper.selectByIdAndEnterprise(id, enterpriseId);
        if (energy == null) {
            throw new BusinessException("Energy not found or access denied: " + id);
        }
        return energy;
    }

    @Override
    public List<BsEnergy> list(BsEnergy query) {
        // Always enforce current enterprise — ignore any client-supplied enterpriseId
        query.setEnterpriseId(SecurityUtils.getRequiredCurrentEnterpriseId());
        return energyMapper.selectList(query);
    }

    @Override
    @CacheEvict(cacheNames = "energyCache", allEntries = true)
    public void create(BsEnergy energy) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        String operator = SecurityUtils.getCurrentUsername();
        energy.setCreateBy(operator);
        energy.setUpdateBy(operator);
        // Always derive enterpriseId from JWT — never trust caller
        energy.setEnterpriseId(enterpriseId);
        if (energy.getIsActive() == null) {
            energy.setIsActive(1);
        }
        energyMapper.insert(energy);
    }

    @Override
    @CacheEvict(cacheNames = "energyCache", allEntries = true)
    public void update(BsEnergy energy) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        // Ownership check: ensure record belongs to current enterprise
        getByIdForEnterprise(energy.getId(), enterpriseId);
        energy.setUpdateBy(SecurityUtils.getCurrentUsername());
        energyMapper.updateById(energy);
    }

    @Override
    @CacheEvict(cacheNames = "energyCache", allEntries = true)
    public void delete(Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        // Ownership check
        getByIdForEnterprise(id, enterpriseId);
        energyMapper.deleteById(id, SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "energyCache", allEntries = true)
    public void importFromCatalog(List<Long> catalogIds) {
        if (catalogIds == null || catalogIds.isEmpty()) {
            return;
        }
        String operator = SecurityUtils.getCurrentUsername();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        for (Long catalogId : catalogIds) {
            BsEnergyCatalog catalog = catalogMapper.selectById(catalogId);
            if (catalog == null) {
                log.warn("Catalog entry {} not found, skipping", catalogId);
                continue;
            }
            BsEnergy energy = new BsEnergy();
            energy.setEnterpriseId(enterpriseId);
            energy.setName(catalog.getName());
            energy.setCategory(catalog.getCategory());
            energy.setMeasurementUnit(catalog.getMeasurementUnit());
            energy.setEquivalentValue(catalog.getEquivalentValue());
            energy.setEqualValue(catalog.getEqualValue());
            energy.setLowHeatValue(catalog.getLowHeatValue());
            energy.setCarbonContent(catalog.getCarbonContent());
            energy.setOxidationRate(catalog.getOxidationRate());
            energy.setColor(catalog.getColor());
            energy.setIsActive(1);
            energy.setCreateBy(operator);
            energy.setUpdateBy(operator);
            energyMapper.insert(energy);
        }
    }
}
