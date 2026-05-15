package com.energy.audit.service.template;

import com.energy.audit.dao.mapper.extraction.DeSubmissionFieldMapper;
import com.energy.audit.dao.mapper.extraction.DeSubmissionTableMapper;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import com.energy.audit.service.template.impl.DataPersistenceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class DataPersistenceServiceImplTest {

    @Test
    void clearsSavingCalculationDetailEvenWhenProductRowsAreAbsent() throws Exception {
        BusinessTablePersister businessTablePersister = mock(BusinessTablePersister.class);
        DataPersistenceServiceImpl service = new DataPersistenceServiceImpl(
                mock(DeSubmissionFieldMapper.class),
                mock(DeSubmissionTableMapper.class),
                businessTablePersister,
                mock(EnterpriseSettingService.class),
                new ObjectMapper(),
                mock(EnergyFlowPostProcessor.class));

        Method method = DataPersistenceServiceImpl.class.getDeclaredMethod(
                "persistSavingCalculationDetails", Long.class, Long.class, Integer.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(service, 33L, 1L, 2026, Map.of(), "devin");

        verify(businessTablePersister).deleteForReExtraction(
                "de_saving_calculation_detail", 33L, 1L, 2026, "devin");
        verifyNoMoreInteractions(businessTablePersister);
    }

    @Test
    void savingCalculationDetailUsesSecondSheet12IndicatorLabel() throws Exception {
        BusinessTablePersister businessTablePersister = mock(BusinessTablePersister.class);
        DataPersistenceServiceImpl service = new DataPersistenceServiceImpl(
                mock(DeSubmissionFieldMapper.class),
                mock(DeSubmissionTableMapper.class),
                businessTablePersister,
                mock(EnterpriseSettingService.class),
                new ObjectMapper(),
                mock(EnergyFlowPostProcessor.class));

        Method method = DataPersistenceServiceImpl.class.getDeclaredMethod(
                "persistSavingCalculationDetails", Long.class, Long.class, Integer.class, Map.class, String.class);
        method.setAccessible(true);
        method.invoke(service, 33L, 1L, 2026, Map.of(
                "de_product_unit_consumption", List.of(
                        Map.of("indicator_name", "钢单产综合能耗", "denominator_unit", "吨",
                                "output", 100, "prev_output", 100, "unit_consumption", 1234.5, "prev_unit_consumption", 1500),
                        Map.of("indicator_name", "EA052RT-产品B单位产量综合能耗", "denominator_unit", "件",
                                "output", 200, "prev_output", 200, "unit_consumption", 2345.6, "prev_unit_consumption", 2600)
                )), "devin");

        verify(businessTablePersister).deleteForReExtraction(
                "de_saving_calculation_detail", 33L, 1L, 2026, "devin");
        verify(businessTablePersister).persistTableRows(
                eq("de_saving_calculation_detail"),
                eq(33L),
                eq(1L),
                eq(2026),
                argThat(rows -> rows.size() == 4
                        && rows.stream().anyMatch(row ->
                        "EA052RT-产品B".equals(row.get("product_name"))
                                && "EA052RT-产品B单耗（吨标煤/件）".equals(row.get("row_label"))
                                && row.get("current_value").toString().equals("2.3456"))),
                eq("devin"));
    }
}
