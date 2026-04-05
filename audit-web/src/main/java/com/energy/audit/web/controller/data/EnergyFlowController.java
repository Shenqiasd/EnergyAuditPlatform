package com.energy.audit.web.controller.data;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.data.DeEnergyFlow;
import com.energy.audit.service.data.EnergyFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public EnergyFlowController(EnergyFlowService energyFlowService) {
        this.energyFlowService = energyFlowService;
    }

    @Operation(summary = "Get energy flow data for enterprise and year")
    @GetMapping("/list")
    public R<List<DeEnergyFlow>> list(@RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(energyFlowService.listByEnterpriseAndYear(enterpriseId, auditYear));
    }

    @Operation(summary = "Save energy flow data (replace all for given year)")
    @PostMapping("/save")
    public R<Void> save(@RequestParam Integer auditYear,
                        @RequestBody List<DeEnergyFlow> flows) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        energyFlowService.saveBatch(enterpriseId, auditYear, flows);
        return R.ok();
    }

    @Operation(summary = "Delete a single energy flow record")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        energyFlowService.deleteByIdAndEnterprise(id, enterpriseId);
        return R.ok();
    }
}
