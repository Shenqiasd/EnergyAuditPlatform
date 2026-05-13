package com.energy.audit.service.setting.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.setting.BsEnergyMapper;
import com.energy.audit.dao.mapper.setting.BsUnitEnergyMapper;
import com.energy.audit.dao.mapper.setting.BsUnitMapper;
import com.energy.audit.model.entity.setting.BsUnit;
import com.energy.audit.model.entity.setting.BsUnitEnergy;
import com.energy.audit.service.setting.UnitSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class UnitSettingServiceImpl implements UnitSettingService {

    /** unitType=3 (terminal use) sub-category whitelist — kept in sync with audit-ui (GRA-70). */
    private static final int END_USE_UNIT_TYPE = 3;
    private static final Set<String> END_USE_SUB_CATEGORIES =
            Set.of("生产系统", "辅助系统", "非生产系统", "外供");

    private final BsUnitMapper unitMapper;
    private final BsUnitEnergyMapper unitEnergyMapper;
    private final BsEnergyMapper energyMapper;

    public UnitSettingServiceImpl(BsUnitMapper unitMapper,
                                   BsUnitEnergyMapper unitEnergyMapper,
                                   BsEnergyMapper energyMapper) {
        this.unitMapper = unitMapper;
        this.unitEnergyMapper = unitEnergyMapper;
        this.energyMapper = energyMapper;
    }

    @Override
    public BsUnit getByIdForEnterprise(Long id, Long enterpriseId) {
        BsUnit unit = unitMapper.selectByIdAndEnterprise(id, enterpriseId);
        if (unit == null) {
            throw new BusinessException("Unit not found or access denied: " + id);
        }
        return unit;
    }

    @Override
    public List<BsUnit> list(BsUnit query) {
        query.setEnterpriseId(SecurityUtils.getRequiredCurrentEnterpriseId());
        return unitMapper.selectList(query);
    }

    @Override
    @Transactional
    public void create(BsUnit unit) {
        validateSubCategory(unit);
        String operator = SecurityUtils.getCurrentUsername();
        unit.setCreateBy(operator);
        unit.setUpdateBy(operator);
        unit.setEnterpriseId(SecurityUtils.getRequiredCurrentEnterpriseId());
        unitMapper.insert(unit);
    }

    @Override
    public void update(BsUnit unit) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        BsUnit existing = getByIdForEnterprise(unit.getId(), enterpriseId);
        // BsUnitMapper.updateById guards every column with <if test="x != null">,
        // so PATCH-style requests legitimately omit fields they don't want to
        // change. Mirror the persisted unitType / subCategory before validating
        // so partial updates on end-use units don't trip the GRA-70 whitelist
        // just because the caller didn't re-send unchanged fields.
        if (unit.getUnitType() == null) {
            unit.setUnitType(existing.getUnitType());
        }
        if (unit.getSubCategory() == null) {
            unit.setSubCategory(existing.getSubCategory());
        }
        validateSubCategory(unit);
        unit.setUpdateBy(SecurityUtils.getCurrentUsername());
        unitMapper.updateById(unit);
    }

    /**
     * Enforce GRA-70: terminal-use (unitType=3) units must carry a sub-category
     * from the fixed whitelist. Non-terminal units are unconstrained.
     */
    private void validateSubCategory(BsUnit unit) {
        if (unit.getUnitType() == null || unit.getUnitType() != END_USE_UNIT_TYPE) {
            return;
        }
        String subCategory = unit.getSubCategory();
        if (subCategory == null || subCategory.isBlank()) {
            throw new BusinessException("终端使用单元的子类别不能为空");
        }
        if (!END_USE_SUB_CATEGORIES.contains(subCategory)) {
            throw new BusinessException("终端使用单元的子类别必须为：生产系统/辅助系统/非生产系统/外供");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        getByIdForEnterprise(id, enterpriseId);
        String operator = SecurityUtils.getCurrentUsername();
        unitMapper.deleteById(id, operator);
        unitEnergyMapper.deleteAllByUnitId(id, operator);
    }

    @Override
    public List<BsUnitEnergy> getUnitEnergies(Long unitId) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        getByIdForEnterprise(unitId, enterpriseId);
        return unitEnergyMapper.selectByUnitId(unitId);
    }

    @Override
    @Transactional
    public void addUnitEnergy(Long unitId, Long energyId) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        getByIdForEnterprise(unitId, enterpriseId);
        if (energyMapper.selectByIdAndEnterprise(energyId, enterpriseId) == null) {
            throw new BusinessException("Energy not found or access denied: " + energyId);
        }
        if (unitEnergyMapper.selectByUnitIdAndEnergyId(unitId, energyId) != null) {
            throw new BusinessException("Energy already associated with this unit");
        }
        String operator = SecurityUtils.getCurrentUsername();
        BsUnitEnergy ue = new BsUnitEnergy();
        ue.setUnitId(unitId);
        ue.setEnergyId(energyId);
        ue.setCreateBy(operator);
        ue.setUpdateBy(operator);
        unitEnergyMapper.insert(ue);
    }

    @Override
    @Transactional
    public void removeUnitEnergy(Long unitId, Long energyId) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        getByIdForEnterprise(unitId, enterpriseId);
        unitEnergyMapper.deleteByUnitIdAndEnergyId(unitId, energyId, SecurityUtils.getCurrentUsername());
    }
}
