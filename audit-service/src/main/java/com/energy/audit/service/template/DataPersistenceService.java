package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplTagMapping;

import java.util.List;
import java.util.Map;

public interface DataPersistenceService {

    void persistExtractedData(Long submissionId, Long enterpriseId, Integer auditYear,
                              Map<String, Object> extractedData,
                              List<TplTagMapping> mappings);
}
