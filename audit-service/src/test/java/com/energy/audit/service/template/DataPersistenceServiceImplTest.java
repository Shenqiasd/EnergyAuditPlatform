package com.energy.audit.service.template;

import com.energy.audit.dao.mapper.extraction.DeSubmissionFieldMapper;
import com.energy.audit.dao.mapper.extraction.DeSubmissionTableMapper;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import com.energy.audit.service.template.impl.DataPersistenceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

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
}
