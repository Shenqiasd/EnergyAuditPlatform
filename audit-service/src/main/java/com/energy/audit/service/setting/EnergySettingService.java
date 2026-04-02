package com.energy.audit.service.setting;

import com.energy.audit.model.entity.setting.BsEnergy;

import java.util.List;

/**
 * Energy setting service interface
 */
public interface EnergySettingService {

    BsEnergy getByIdForEnterprise(Long id, Long enterpriseId);

    List<BsEnergy> list(BsEnergy query);

    void create(BsEnergy energy);

    void update(BsEnergy energy);

    void delete(Long id);

    /**
     * Import energy entries from global catalog into the current enterprise's energy list.
     * @param catalogIds IDs from bs_energy_catalog to import
     */
    void importFromCatalog(List<Long> catalogIds);
}
