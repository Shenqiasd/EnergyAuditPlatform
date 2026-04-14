package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.enterprise.EntEnterpriseSetting;
import com.energy.audit.model.entity.setting.BsEnergy;
import com.energy.audit.model.entity.setting.BsProduct;
import com.energy.audit.model.entity.setting.BsUnit;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import com.energy.audit.service.setting.EnergySettingService;
import com.energy.audit.service.setting.ProductSettingService;
import com.energy.audit.service.setting.UnitSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise setting controller — enterprise user views and updates own settings.
 */
@Tag(name = "Enterprise Settings", description = "Enterprise profile / setting upsert")
@RestController
@RequestMapping("/enterprise/setting")
public class EnterpriseSettingController {

    private final EnterpriseSettingService settingService;
    private final EnergySettingService energySettingService;
    private final ProductSettingService productSettingService;
    private final UnitSettingService unitSettingService;
    private final ObjectMapper objectMapper;

    public EnterpriseSettingController(EnterpriseSettingService settingService,
                                       EnergySettingService energySettingService,
                                       ProductSettingService productSettingService,
                                       UnitSettingService unitSettingService,
                                       ObjectMapper objectMapper) {
        this.settingService = settingService;
        this.energySettingService = energySettingService;
        this.productSettingService = productSettingService;
        this.unitSettingService = unitSettingService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Get own enterprise setting")
    @GetMapping
    public R<EntEnterpriseSetting> get() {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(settingService.get(enterpriseId));
    }

    @Operation(summary = "Save (upsert) own enterprise setting")
    @PutMapping
    public R<Void> save(@RequestBody EntEnterpriseSetting setting) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        setting.setEnterpriseId(enterpriseId);
        settingService.save(setting);
        return R.ok();
    }

    /**
     * Returns enterprise setting as a flat field map (camelCase keys) for SpreadJS pre-fill.
     * This enables bidirectional sync: enterprise settings page → SpreadJS template.
     * Null fields are excluded from the map.
     */
    @Operation(summary = "Get enterprise setting as flat map for SpreadJS pre-fill")
    @GetMapping("/prefill")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> prefill() {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        EntEnterpriseSetting setting = settingService.get(enterpriseId);
        if (setting == null) {
            return R.ok(Collections.emptyMap());
        }
        Map<String, Object> map = objectMapper.convertValue(setting, Map.class);
        // Remove system/internal fields that are not part of the spreadsheet template
        map.remove("id");
        map.remove("enterpriseId");
        map.remove("createBy");
        map.remove("createTime");
        map.remove("updateBy");
        map.remove("updateTime");
        map.remove("deleted");
        // Remove null values to keep response clean
        map.values().removeIf(v -> v == null);
        return R.ok(map);
    }

    /**
     * Returns all config data (energy types + products) for CONFIG_PREFILL tag mappings.
     * No pagination — config data is small (typically < 50 records per table).
     */
    @Operation(summary = "Get config data for CONFIG_PREFILL (energy types + products + units)")
    @GetMapping("/config-prefill-data")
    public R<Map<String, Object>> getConfigPrefillData() {
        BsEnergy energyQuery = new BsEnergy();
        List<BsEnergy> energies = energySettingService.list(energyQuery);

        BsProduct productQuery = new BsProduct();
        List<BsProduct> products = productSettingService.list(productQuery);

        BsUnit unitQuery = new BsUnit();
        List<BsUnit> units = unitSettingService.list(unitQuery);

        Map<String, Object> result = new HashMap<>();
        result.put("bs_energy", energies);
        result.put("bs_product", products);
        result.put("bs_unit", units);
        return R.ok(result);
    }
}
