package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.exception.BusinessException;
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

import java.util.ArrayList;
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
        // Frontend already enforces industry classification as required (GRA-67),
        // but we mirror that here so the save API cannot be bypassed by direct callers.
        if (isBlank(setting.getIndustryCode()) || isBlank(setting.getIndustryName())) {
            throw new BusinessException(400, "行业分类不能为空");
        }
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
     * Checks whether all prerequisite tables are completed before allowing
     * data entry or chart export.
     * Returns a map with:
     *   - passed: boolean (true if all prerequisites met)
     *   - errors: list of error messages for unmet prerequisites
     */
    @Operation(summary = "Check prerequisite completion for data entry / chart export")
    @GetMapping("/prerequisite-check")
    public R<Map<String, Object>> checkPrerequisites() {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<String> errors = new ArrayList<>();

        // 1. Enterprise basic information — all fields except remark must be filled
        EntEnterpriseSetting setting = settingService.get(enterpriseId);
        if (setting == null) {
            errors.add("企业基本信息未填写");
        } else {
            List<String> missing = checkRequiredSettingFields(setting);
            if (!missing.isEmpty()) {
                errors.add("企业基本信息以下字段未填写：" + String.join("、", missing));
            }
        }

        // 2. Energy types — at least 1 record
        BsEnergy energyQuery = new BsEnergy();
        energyQuery.setEnterpriseId(enterpriseId);
        List<BsEnergy> energies = energySettingService.list(energyQuery);
        if (energies == null || energies.isEmpty()) {
            errors.add("能源品种至少需要配置1条记录");
        }

        // 3. Units — at least 1 record
        BsUnit unitQuery = new BsUnit();
        unitQuery.setEnterpriseId(enterpriseId);
        List<BsUnit> units = unitSettingService.list(unitQuery);
        if (units == null || units.isEmpty()) {
            errors.add("用能单元至少需要配置1条记录");
        }

        // 4. Products — at least 1 record
        BsProduct productQuery = new BsProduct();
        productQuery.setEnterpriseId(enterpriseId);
        List<BsProduct> products = productSettingService.list(productQuery);
        if (products == null || products.isEmpty()) {
            errors.add("产品信息至少需要配置1条记录");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("passed", errors.isEmpty());
        result.put("errors", errors);
        return R.ok(result);
    }

    /**
     * Check required fields on EntEnterpriseSetting.
     * All fields except remark are considered required.
     */
    private List<String> checkRequiredSettingFields(EntEnterpriseSetting s) {
        List<String> missing = new ArrayList<>();
        if (isBlank(s.getRegion())) missing.add("所属地区");
        if (isBlank(s.getIndustryName())) missing.add("行业分类");
        if (isBlank(s.getUnitNature())) missing.add("单位类型");
        if (isBlank(s.getLegalRepresentative())) missing.add("法定代表人");
        if (isBlank(s.getLegalPhone())) missing.add("法定代表人电话");
        if (isBlank(s.getEnterpriseAddress())) missing.add("单位地址");
        if (isBlank(s.getPostalCode())) missing.add("邮政编码");
        if (isBlank(s.getEnergyLeaderName())) missing.add("节能领导姓名");
        if (isBlank(s.getEnergyManagerName())) missing.add("能源管理负责人");
        if (isBlank(s.getEnergyManagerMobile())) missing.add("能源管理负责人手机");
        return missing;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Returns all config data (energy types + products) for CONFIG_PREFILL tag mappings.
     * No pagination — config data is small (typically < 50 records per table).
     */
    @Operation(summary = "Get config data for CONFIG_PREFILL (energy types + products + units)")
    @GetMapping("/config-prefill-data")
    public R<Map<String, Object>> getConfigPrefillData() {
        BsEnergy energyQuery = new BsEnergy();
        energyQuery.setIsActive(1);
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
