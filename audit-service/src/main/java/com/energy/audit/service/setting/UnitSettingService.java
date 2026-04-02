package com.energy.audit.service.setting;

import com.energy.audit.model.entity.setting.BsUnit;
import com.energy.audit.model.entity.setting.BsUnitEnergy;

import java.util.List;

/**
 * Unit setting service
 */
public interface UnitSettingService {

    /**
     * Tenant-scoped lookup — throws BusinessException when id does not belong to enterpriseId.
     */
    BsUnit getByIdForEnterprise(Long id, Long enterpriseId);

    List<BsUnit> list(BsUnit query);

    void create(BsUnit unit);

    void update(BsUnit unit);

    void delete(Long id);

    List<BsUnitEnergy> getUnitEnergies(Long unitId);

    void addUnitEnergy(Long unitId, Long energyId);

    void removeUnitEnergy(Long unitId, Long energyId);
}
