package com.energy.audit.web.controller.setting;

import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.dto.ImportCatalogRequest;
import com.energy.audit.model.entity.setting.BsEnergy;
import com.energy.audit.service.setting.EnergySettingService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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

/**
 * Energy setting controller
 */
@Tag(name = "Energy Settings", description = "Enterprise energy type CRUD and catalog import")
@RestController
@RequestMapping("/setting/energy")
public class EnergySettingController {

    private final EnergySettingService energySettingService;

    public EnergySettingController(EnergySettingService energySettingService) {
        this.energySettingService = energySettingService;
    }

    @Operation(summary = "Get energy setting by ID (tenant-scoped)")
    @GetMapping("/{id}")
    public R<BsEnergy> getById(@PathVariable Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(energySettingService.getByIdForEnterprise(id, enterpriseId));
    }

    @Operation(summary = "List energy settings with pagination")
    @GetMapping
    public R<PageResult<BsEnergy>> list(BsEnergy query,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BsEnergy> list = energySettingService.list(query);
        PageInfo<BsEnergy> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "Create energy setting")
    @PostMapping
    public R<Void> create(@RequestBody BsEnergy energy) {
        energySettingService.create(energy);
        return R.ok();
    }

    @Operation(summary = "Update energy setting")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody BsEnergy energy) {
        energy.setId(id);
        energySettingService.update(energy);
        return R.ok();
    }

    @Operation(summary = "Delete energy setting")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        energySettingService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Import energy types from global catalog")
    @PostMapping("/import-from-catalog")
    public R<Void> importFromCatalog(@RequestBody(required = false) ImportCatalogRequest request) {
        if (request == null || request.getCatalogIds() == null || request.getCatalogIds().isEmpty()) {
            return R.ok();
        }
        energySettingService.importFromCatalog(request.getCatalogIds());
        return R.ok();
    }
}
