package com.energy.audit.service.setting.impl;

import com.energy.audit.model.entity.setting.BsUnit;
import com.energy.audit.model.entity.setting.BsUnitEnergy;
import com.energy.audit.service.setting.UnitSettingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UnitSettingServiceImpl implements UnitSettingService {

    @Override
    public BsUnit getById(Long id) {
        return null;
    }

    @Override
    public BsUnit getByIdForEnterprise(Long id, Long enterpriseId) {
        return null;
    }

    @Override
    public List<BsUnit> list(BsUnit query) {
        return new ArrayList<>();
    }

    @Override
    public void create(BsUnit unit) {
    }

    @Override
    public void update(BsUnit unit) {
    }

    @Override
    public void delete(Long id) {
    }

    @Override
    public List<BsUnitEnergy> getUnitEnergies(Long unitId) {
        return new ArrayList<>();
    }

    @Override
    public void addUnitEnergy(Long unitId, Long energyId) {
    }

    @Override
    public void removeUnitEnergy(Long unitId, Long energyId) {
    }
}
