package com.energy.audit.web.controller.setting;

import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.setting.BsUnit;
import com.energy.audit.model.entity.setting.BsUnitEnergy;
import com.energy.audit.service.setting.UnitSettingService;
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
 * Unit setting controller
 */
@Tag(name = "Unit Settings", description = "Enterprise unit (process/organization) CRUD")
@RestController
@RequestMapping("/setting/unit")
public class UnitSettingController {

    private final UnitSettingService unitSettingService;

    public UnitSettingController(UnitSettingService unitSettingService) {
        this.unitSettingService = unitSettingService;
    }

    @Operation(summary = "Get unit by ID (tenant-scoped)")
    @GetMapping("/{id}")
    public R<BsUnit> getById(@PathVariable Long id) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(unitSettingService.getByIdForEnterprise(id, enterpriseId));
    }

    @Operation(summary = "List units with pagination")
    @GetMapping
    public R<PageResult<BsUnit>> list(BsUnit query,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<BsUnit> list = unitSettingService.list(query);
        PageInfo<BsUnit> pageInfo = new PageInfo<>(list);
        return R.ok(PageResult.of(pageInfo.getTotal(), pageInfo.getList()));
    }

    @Operation(summary = "Create unit")
    @PostMapping
    public R<Void> create(@RequestBody BsUnit unit) {
        unitSettingService.create(unit);
        return R.ok();
    }

    @Operation(summary = "Update unit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody BsUnit unit) {
        unit.setId(id);
        unitSettingService.update(unit);
        return R.ok();
    }

    @Operation(summary = "Delete unit")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        unitSettingService.delete(id);
        return R.ok();
    }

    @Operation(summary = "Get energy associations for a unit")
    @GetMapping("/{unitId}/energies")
    public R<List<BsUnitEnergy>> getUnitEnergies(@PathVariable Long unitId) {
        return R.ok(unitSettingService.getUnitEnergies(unitId));
    }

    @Operation(summary = "Add energy to unit")
    @PostMapping("/{unitId}/energies/{energyId}")
    public R<Void> addUnitEnergy(@PathVariable Long unitId, @PathVariable Long energyId) {
        unitSettingService.addUnitEnergy(unitId, energyId);
        return R.ok();
    }

    @Operation(summary = "Remove energy from unit")
    @DeleteMapping("/{unitId}/energies/{energyId}")
    public R<Void> removeUnitEnergy(@PathVariable Long unitId, @PathVariable Long energyId) {
        unitSettingService.removeUnitEnergy(unitId, energyId);
        return R.ok();
    }
}
