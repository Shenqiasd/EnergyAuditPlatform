package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.enterprise.EntEnterpriseSetting;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import com.energy.audit.service.setting.EnergySettingService;
import com.energy.audit.service.setting.ProductSettingService;
import com.energy.audit.service.setting.UnitSettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EnterpriseSettingController}.
 *
 * <p>Covers the save-time required-field validation added for GRA-67 so that
 * direct callers cannot bypass the frontend by submitting a setting payload
 * with blank {@code industryCode} / {@code industryName}.
 */
class EnterpriseSettingControllerTest {

    private EnterpriseSettingService settingService;
    private EnterpriseSettingController controller;

    @BeforeEach
    void setUp() {
        settingService = mock(EnterpriseSettingService.class);
        controller = new EnterpriseSettingController(
                settingService,
                mock(EnergySettingService.class),
                mock(ProductSettingService.class),
                mock(UnitSettingService.class),
                new ObjectMapper());
        SecurityUtils.setContext(1L, "enterprise", 3, 42L);
    }

    @AfterEach
    void tearDown() {
        SecurityUtils.clear();
    }

    @Test
    void saveRejectsBlankIndustryCode() {
        EntEnterpriseSetting setting = baseSetting();
        setting.setIndustryCode(null);

        assertThatThrownBy(() -> controller.save(setting))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("行业分类不能为空");

        verify(settingService, never()).save(setting);
    }

    @Test
    void saveRejectsBlankIndustryName() {
        EntEnterpriseSetting setting = baseSetting();
        setting.setIndustryName("   ");

        assertThatThrownBy(() -> controller.save(setting))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("行业分类不能为空");

        verify(settingService, never()).save(setting);
    }

    @Test
    void saveAcceptsPopulatedIndustryClassification() {
        EntEnterpriseSetting setting = baseSetting();

        R<Void> response = controller.save(setting);

        assertThat(response.getCode()).isEqualTo(200);
        // Enterprise id from the security context must be stamped onto the entity.
        assertThat(setting.getEnterpriseId()).isEqualTo(42L);
        verify(settingService, times(1)).save(setting);
    }

    // ── Draft save endpoint tests ──

    @Test
    void saveDraftAllowsBlankIndustryCode() {
        EntEnterpriseSetting setting = new EntEnterpriseSetting();
        setting.setIndustryCode(null);
        setting.setIndustryName(null);

        R<Void> response = controller.saveDraft(setting);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(setting.getEnterpriseId()).isEqualTo(42L);
        verify(settingService, times(1)).save(setting);
    }

    @Test
    void saveDraftAllowsPartialData() {
        EntEnterpriseSetting setting = new EntEnterpriseSetting();
        setting.setFax("qa-codex-test-marker");

        R<Void> response = controller.saveDraft(setting);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(setting.getEnterpriseId()).isEqualTo(42L);
        verify(settingService, times(1)).save(setting);
    }

    @Test
    void saveDraftStampsEnterpriseId() {
        EntEnterpriseSetting setting = baseSetting();

        controller.saveDraft(setting);

        assertThat(setting.getEnterpriseId()).isEqualTo(42L);
    }

    @Test
    void strictSaveStillRejectsBlankIndustryAfterDraftEndpointExists() {
        EntEnterpriseSetting setting = new EntEnterpriseSetting();
        setting.setFax("some-value");

        assertThatThrownBy(() -> controller.save(setting))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("行业分类不能为空");

        verify(settingService, never()).save(setting);
    }

    private static EntEnterpriseSetting baseSetting() {
        EntEnterpriseSetting setting = new EntEnterpriseSetting();
        setting.setIndustryCode("C281");
        setting.setIndustryName("锅炉及原动设备制造");
        return setting;
    }
}
