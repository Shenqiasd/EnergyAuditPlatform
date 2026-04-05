package com.energy.audit.service.data;

import com.energy.audit.model.entity.extraction.DeEnergyFlow;

import java.util.List;

public interface EnergyFlowService {

    List<DeEnergyFlow> listByEnterpriseAndYear(Long enterpriseId, Integer auditYear);

    void saveBatch(Long enterpriseId, Integer auditYear, List<DeEnergyFlow> flows);

    void deleteByIdAndEnterprise(Long id, Long enterpriseId);
}
