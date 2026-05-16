package com.energy.audit.web.controller.data;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.dto.EnergyFlowConfigDTO;
import com.energy.audit.model.dto.SaveEnergyFlowConfigDTO;
import com.energy.audit.model.entity.extraction.DeEnergyFlow;
import com.energy.audit.service.data.EnergyFlowConfigService;
import com.energy.audit.service.data.EnergyFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "EnergyFlow", description = "Energy flow diagram data")
@RestController
@RequestMapping("/energy-flow")
public class EnergyFlowController {

    private final EnergyFlowService energyFlowService;
    private final EnergyFlowConfigService energyFlowConfigService;

    public EnergyFlowController(EnergyFlowService energyFlowService,
                                EnergyFlowConfigService energyFlowConfigService) {
        this.energyFlowService = energyFlowService;
        this.energyFlowConfigService = energyFlowConfigService;
    }

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new RuntimeException("仅企业用户可操作能源流向数据");
        }
    }

    @Operation(summary = "Get energy flow data for enterprise and year")
    @GetMapping("/list")
    public R<List<DeEnergyFlow>> list(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(energyFlowService.listByEnterpriseAndYear(enterpriseId, auditYear));
    }

    @Operation(summary = "Save energy flow data (replace all for given year)")
    @PostMapping("/save")
    public R<Void> save(@RequestParam Integer auditYear,
                        @RequestBody List<DeEnergyFlow> flows) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        energyFlowService.saveBatch(enterpriseId, auditYear, flows);
        return R.ok();
    }

    @Operation(summary = "Delete a single energy flow record")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        energyFlowService.deleteByIdAndEnterprise(id, enterpriseId);
        return R.ok();
    }

    @Operation(summary = "Get full energy flow config (records + diagram + validation)")
    @GetMapping("/config")
    public R<EnergyFlowConfigDTO> getConfig(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(energyFlowConfigService.getConfig(enterpriseId, auditYear));
    }

    @Operation(summary = "Save energy flow config (records + diagram)")
    @PutMapping("/config")
    public R<Void> saveConfig(@RequestParam Integer auditYear,
                              @RequestBody SaveEnergyFlowConfigDTO dto) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        energyFlowConfigService.saveConfig(enterpriseId, auditYear, dto);
        return R.ok();
    }
}
