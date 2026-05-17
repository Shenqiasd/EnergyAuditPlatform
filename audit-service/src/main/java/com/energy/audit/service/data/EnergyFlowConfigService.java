package com.energy.audit.service.data;

import com.energy.audit.model.dto.EnergyFlowConfigDTO;
import com.energy.audit.model.dto.SaveEnergyFlowConfigDTO;

public interface EnergyFlowConfigService {

    EnergyFlowConfigDTO getConfig(Long enterpriseId, Integer auditYear);

    void saveConfig(Long enterpriseId, Integer auditYear, SaveEnergyFlowConfigDTO dto);
}
