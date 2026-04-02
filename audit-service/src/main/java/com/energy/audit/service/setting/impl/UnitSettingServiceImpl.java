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

/**
 * Unit setting service implementation — all mutations are tenant-scoped.
 */
@Service
public class UnitSettingServiceImpl implements UnitSettingService {

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

    /**
     * Tenant-safe getById: verifies that the record belongs to the current enterprise.
     */
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
        // Always enforce current enterprise — ignore any client-supplied enterpriseId
        query.setEnterpriseId(SecurityUtils.getRequiredCurrentEnterpriseId());
        return unitMapper.selectList(query);
    }

    @Override
    @Transactional
    public void create(BsUnit unit) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        String operator = SecurityUtils.getCurrentUsername();
        unit.setCreateBy(operator);
        unit.setUpdateBy(operator);
        // Always derive enterpriseId from JWT — never trust caller
        unit.setEnterpriseId(enterpriseId);
        unitMapper.insert(unit);
    }

    @Override
    public void update(BsUnit unit) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        // Ownership check
        getByIdForEnterprise(unit.getId(), enterpriseId);
        unit.setUpdateBy(SecurityUtils.getCurrentUsername());
        unitMapper.updateById(unit);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        // Ownership check
        getByIdForEnterprise(id, enterpriseId);
        String operator = SecurityUtils.getCurrentUsername();
        unitMapper.deleteById(id, operator);
        unitEnergyMapper.deleteAllByUnitId(id, operator);
    }

    @Override
    public List<BsUnitEnergy> getUnitEnergies(Long unitId) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        // Verify caller owns this unit before returning its energy list
        getByIdForEnterprise(unitId, enterpriseId);
        return unitEnergyMapper.selectByUnitId(unitId);
    }

    @Override
    @Transactional
    public void addUnitEnergy(Long unitId, Long energyId) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        // Both unit and energy must belong to current enterprise
        getByIdForEnterprise(unitId, enterpriseId);
        if (energyMapper.selectByIdAndEnterprise(energyId, enterpriseId) == null) {
            throw new BusinessException("Energy not found or access denied: " + energyId);
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
        // Verify caller owns this unit
        getByIdForEnterprise(unitId, enterpriseId);
        unitEnergyMapper.deleteByUnitIdAndEnergyId(unitId, energyId, SecurityUtils.getCurrentUsername());
    }
}
