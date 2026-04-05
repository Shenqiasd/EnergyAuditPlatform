package com.energy.audit.service.data.impl;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.data.DeEnergyFlowMapper;
import com.energy.audit.model.entity.data.DeEnergyFlow;
import com.energy.audit.service.data.EnergyFlowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnergyFlowServiceImpl implements EnergyFlowService {

    private final DeEnergyFlowMapper energyFlowMapper;

    public EnergyFlowServiceImpl(DeEnergyFlowMapper energyFlowMapper) {
        this.energyFlowMapper = energyFlowMapper;
    }

    @Override
    public List<DeEnergyFlow> listByEnterpriseAndYear(Long enterpriseId, Integer auditYear) {
        return energyFlowMapper.selectByEnterpriseAndYear(enterpriseId, auditYear);
    }

    @Override
    @Transactional
    public void saveBatch(Long enterpriseId, Integer auditYear, List<DeEnergyFlow> flows) {
        String operator = SecurityUtils.getCurrentUsername();
        energyFlowMapper.deleteByEnterpriseAndYear(enterpriseId, auditYear, operator);
        if (flows != null && !flows.isEmpty()) {
            for (int i = 0; i < flows.size(); i++) {
                DeEnergyFlow flow = flows.get(i);
                flow.setEnterpriseId(enterpriseId);
                flow.setAuditYear(auditYear);
                flow.setSeqNo(i + 1);
                flow.setCreateBy(operator);
                flow.setUpdateBy(operator);
                energyFlowMapper.insert(flow);
            }
        }
    }
}
