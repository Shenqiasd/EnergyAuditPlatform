package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.enterprise.EntEnterpriseSetting;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enterprise setting controller — enterprise user views and updates own settings.
 */
@Tag(name = "Enterprise Settings", description = "Enterprise profile / setting upsert")
@RestController
@RequestMapping("/enterprise/setting")
public class EnterpriseSettingController {

    private final EnterpriseSettingService settingService;

    public EnterpriseSettingController(EnterpriseSettingService settingService) {
        this.settingService = settingService;
    }

    @Operation(summary = "Get own enterprise setting")
    @GetMapping
    public R<EntEnterpriseSetting> get() {
        Long enterpriseId = SecurityUtils.getCurrentEnterpriseId();
        return R.ok(settingService.get(enterpriseId));
    }

    @Operation(summary = "Save (upsert) own enterprise setting")
    @PutMapping
    public R<Void> save(@RequestBody EntEnterpriseSetting setting) {
        Long enterpriseId = SecurityUtils.getCurrentEnterpriseId();
        setting.setEnterpriseId(enterpriseId);
        settingService.save(setting);
        return R.ok();
    }
}
