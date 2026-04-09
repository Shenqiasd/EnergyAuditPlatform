package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.enterprise.EntEnterpriseSetting;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Enterprise setting controller — enterprise user views and updates own settings.
 */
@Tag(name = "Enterprise Settings", description = "Enterprise profile / setting upsert")
@RestController
@RequestMapping("/enterprise/setting")
public class EnterpriseSettingController {

    private final EnterpriseSettingService settingService;
    private final ObjectMapper objectMapper;

    public EnterpriseSettingController(EnterpriseSettingService settingService,
                                       ObjectMapper objectMapper) {
        this.settingService = settingService;
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
}
