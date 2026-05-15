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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

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

    // ── Draft save endpoint tests (patch/merge semantics) ──

    @Test
    void saveDraftAllowsBlankIndustryCodeForNewRecord() {
        // No existing record → new enterprise, sparse payload is fine
        when(settingService.get(42L)).thenReturn(null);

        EntEnterpriseSetting setting = new EntEnterpriseSetting();
        setting.setFax("qa-codex-test-marker");

        R<Void> response = controller.saveDraft(setting);

        assertThat(response.getCode()).isEqualTo(200);

        ArgumentCaptor<EntEnterpriseSetting> captor = ArgumentCaptor.forClass(EntEnterpriseSetting.class);
        verify(settingService, times(1)).save(captor.capture());
        EntEnterpriseSetting saved = captor.getValue();
        assertThat(saved.getEnterpriseId()).isEqualTo(42L);
        assertThat(saved.getFax()).isEqualTo("qa-codex-test-marker");
    }

    @Test
    void saveDraftMergesWithExistingRecordPreservingUntouchedFields() {
        // Existing record with many fields populated
        EntEnterpriseSetting existing = new EntEnterpriseSetting();
        existing.setId(1L);
        existing.setEnterpriseId(42L);
        existing.setRegion("上海市");
        existing.setIndustryCode("281");
        existing.setIndustryName("锅炉及原动设备制造");
        existing.setLegalRepresentative("张三");
        existing.setFax("021-12345678");
        when(settingService.get(42L)).thenReturn(existing);

        // Sparse incoming payload: only fax changed
        EntEnterpriseSetting incoming = new EntEnterpriseSetting();
        incoming.setFax("qa-codex-20260515-ea-cust-042-marker");

        R<Void> response = controller.saveDraft(incoming);

        assertThat(response.getCode()).isEqualTo(200);

        ArgumentCaptor<EntEnterpriseSetting> captor = ArgumentCaptor.forClass(EntEnterpriseSetting.class);
        verify(settingService, times(1)).save(captor.capture());
        EntEnterpriseSetting saved = captor.getValue();
        // Incoming field is updated
        assertThat(saved.getFax()).isEqualTo("qa-codex-20260515-ea-cust-042-marker");
        // Existing fields are preserved (NOT wiped to null)
        assertThat(saved.getRegion()).isEqualTo("上海市");
        assertThat(saved.getIndustryCode()).isEqualTo("281");
        assertThat(saved.getIndustryName()).isEqualTo("锅炉及原动设备制造");
        assertThat(saved.getLegalRepresentative()).isEqualTo("张三");
        assertThat(saved.getEnterpriseId()).isEqualTo(42L);
    }

    @Test
    void saveDraftMergesFullPayloadCorrectly() {
        // Existing record
        EntEnterpriseSetting existing = new EntEnterpriseSetting();
        existing.setId(1L);
        existing.setEnterpriseId(42L);
        existing.setRegion("上海市");
        existing.setFax("old-fax");
        when(settingService.get(42L)).thenReturn(existing);

        // Full payload from the frontend (all fields present)
        EntEnterpriseSetting incoming = new EntEnterpriseSetting();
        incoming.setRegion("北京市");
        incoming.setFax("new-fax");
        incoming.setIndustryCode("282");
        incoming.setIndustryName("金属加工机械制造");

        R<Void> response = controller.saveDraft(incoming);

        assertThat(response.getCode()).isEqualTo(200);

        ArgumentCaptor<EntEnterpriseSetting> captor = ArgumentCaptor.forClass(EntEnterpriseSetting.class);
        verify(settingService, times(1)).save(captor.capture());
        EntEnterpriseSetting saved = captor.getValue();
        assertThat(saved.getRegion()).isEqualTo("北京市");
        assertThat(saved.getFax()).isEqualTo("new-fax");
        assertThat(saved.getIndustryCode()).isEqualTo("282");
        assertThat(saved.getIndustryName()).isEqualTo("金属加工机械制造");
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
