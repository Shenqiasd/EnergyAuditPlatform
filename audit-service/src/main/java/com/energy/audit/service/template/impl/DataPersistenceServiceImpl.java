package com.energy.audit.service.template.impl;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.extraction.DeSubmissionFieldMapper;
import com.energy.audit.dao.mapper.extraction.DeSubmissionTableMapper;
import com.energy.audit.model.entity.enterprise.EntEnterpriseSetting;
import com.energy.audit.model.entity.extraction.DeSubmissionField;
import com.energy.audit.model.entity.extraction.DeSubmissionTable;
import com.energy.audit.model.entity.template.TplTagMapping;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import com.energy.audit.service.template.BusinessTablePersister;
import com.energy.audit.service.template.DataPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DataPersistenceServiceImpl implements DataPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(DataPersistenceServiceImpl.class);

    private static final String ENT_ENTERPRISE_SETTING = "ent_enterprise_setting";

    private final DeSubmissionFieldMapper fieldMapper;
    private final DeSubmissionTableMapper tableMapper;
    private final BusinessTablePersister businessTablePersister;
    private final EnterpriseSettingService enterpriseSettingService;
    private final ObjectMapper objectMapper;

    public DataPersistenceServiceImpl(DeSubmissionFieldMapper fieldMapper,
                                       DeSubmissionTableMapper tableMapper,
                                       BusinessTablePersister businessTablePersister,
                                       EnterpriseSettingService enterpriseSettingService,
                                       ObjectMapper objectMapper) {
        this.fieldMapper = fieldMapper;
        this.tableMapper = tableMapper;
        this.businessTablePersister = businessTablePersister;
        this.enterpriseSettingService = enterpriseSettingService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void persistExtractedData(Long submissionId, Long enterpriseId, Integer auditYear,
                                      Map<String, Object> extractedData,
                                      List<TplTagMapping> mappings) {
        String operator = SecurityUtils.getRequiredCurrentUsername();

        fieldMapper.deleteBySubmissionId(submissionId, operator);
        tableMapper.deleteBySubmissionId(submissionId, operator);

        // Collect fields targeting ent_enterprise_setting for batch upsert
        Map<String, Object> entSettingFields = new HashMap<>();

        Set<String> clearedBusinessTables = new HashSet<>();
        for (TplTagMapping mapping : mappings) {
            String targetTable = mapping.getTargetTable();
            if (targetTable != null && !targetTable.isBlank()
                    && businessTablePersister.isBusinessTable(targetTable)
                    && !clearedBusinessTables.contains(targetTable)) {
                businessTablePersister.deleteForReExtraction(
                        targetTable, submissionId, enterpriseId, auditYear, operator);
                clearedBusinessTables.add(targetTable);
            }
        }

        List<DeSubmissionField> fallbackScalars = new ArrayList<>();
        List<DeSubmissionTable> fallbackTableRows = new ArrayList<>();

        for (TplTagMapping mapping : mappings) {
            String fieldName = mapping.getFieldName();
            Object value = extractedData.get(fieldName);
            if (value == null) continue;

            String mappingType = mapping.getMappingType() != null ? mapping.getMappingType() : "SCALAR";
            String targetTable = mapping.getTargetTable();

            // Route ent_enterprise_setting fields to dedicated upsert (no submission_id/audit_year)
            if (ENT_ENTERPRISE_SETTING.equalsIgnoreCase(targetTable)) {
                entSettingFields.put(fieldName, value);
                continue;
            }

            boolean hasBusinessTable = targetTable != null && !targetTable.isBlank()
                    && businessTablePersister.isBusinessTable(targetTable);

            if ("TABLE".equalsIgnoreCase(mappingType) && value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) value;

                if (hasBusinessTable) {
                    businessTablePersister.persistTableRows(
                            targetTable, submissionId, enterpriseId, auditYear, rows, operator);
                } else {
                    for (int i = 0; i < rows.size(); i++) {
                        Map<String, Object> row = rows.get(i);
                        DeSubmissionTable tableRow = new DeSubmissionTable();
                        tableRow.setSubmissionId(submissionId);
                        tableRow.setEnterpriseId(enterpriseId);
                        tableRow.setAuditYear(auditYear);
                        tableRow.setTagName(mapping.getTagName());
                        tableRow.setRowIndex(i);
                        Object rowKey = row.get("_rowKey");
                        tableRow.setRowKey(rowKey != null ? rowKey.toString() : null);
                        try {
                            tableRow.setColumnValues(objectMapper.writeValueAsString(row));
                        } catch (Exception e) {
                            log.warn("Failed to serialize table row for tag '{}', row {}", mapping.getTagName(), i);
                            tableRow.setColumnValues("{}");
                        }
                        tableRow.setCreateBy(operator);
                        tableRow.setUpdateBy(operator);
                        fallbackTableRows.add(tableRow);
                    }
                }
            } else {
                if (hasBusinessTable) {
                    businessTablePersister.persistScalar(
                            targetTable, submissionId, enterpriseId, auditYear,
                            fieldName, value, operator);
                } else {
                    DeSubmissionField field = new DeSubmissionField();
                    field.setSubmissionId(submissionId);
                    field.setEnterpriseId(enterpriseId);
                    field.setAuditYear(auditYear);
                    field.setTagName(mapping.getTagName());
                    field.setFieldName(fieldName);
                    field.setCreateBy(operator);
                    field.setUpdateBy(operator);

                    String valueStr = value.toString();
                    if (value instanceof Number) {
                        field.setValueNumber(new BigDecimal(valueStr));
                        field.setValueText(valueStr);
                    } else {
                        field.setValueText(valueStr);
                    }
                    if ("DATE".equalsIgnoreCase(mapping.getDataType())) {
                        field.setValueDate(valueStr);
                    }
                    fallbackScalars.add(field);
                }
            }
        }

        // Persist ent_enterprise_setting fields via dedicated mapper (bidirectional sync)
        if (!entSettingFields.isEmpty()) {
            syncToEnterpriseSetting(enterpriseId, entSettingFields);
        }

        if (!fallbackScalars.isEmpty()) {
            fieldMapper.batchInsert(fallbackScalars);
            log.info("Persisted {} scalar fields to generic storage for submission {}", fallbackScalars.size(), submissionId);
        }
        if (!fallbackTableRows.isEmpty()) {
            tableMapper.batchInsert(fallbackTableRows);
            log.info("Persisted {} table rows to generic storage for submission {}", fallbackTableRows.size(), submissionId);
        }
    }

    /**
     * Sync extracted SpreadJS fields into ent_enterprise_setting via the dedicated mapper.
     * This enables bidirectional sync: SpreadJS template → enterprise settings page.
     * Uses BeanWrapper for safe, reflection-based property setting with type conversion.
     */
    private void syncToEnterpriseSetting(Long enterpriseId, Map<String, Object> fields) {
        EntEnterpriseSetting setting = enterpriseSettingService.get(enterpriseId);
        if (setting == null) {
            setting = new EntEnterpriseSetting();
            setting.setEnterpriseId(enterpriseId);
        }

        BeanWrapper wrapper = new BeanWrapperImpl(setting);
        wrapper.setAutoGrowNestedPaths(true);
        int updated = 0;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String property = entry.getKey();
            Object value = entry.getValue();
            if (!wrapper.isWritableProperty(property)) {
                log.warn("syncToEnterpriseSetting: skipping unknown property '{}'", property);
                continue;
            }
            try {
                Class<?> propType = wrapper.getPropertyType(property);
                if (propType != null && LocalDate.class.isAssignableFrom(propType) && value instanceof String) {
                    wrapper.setPropertyValue(property, LocalDate.parse((String) value));
                } else if (propType != null && BigDecimal.class.isAssignableFrom(propType) && value instanceof Number) {
                    wrapper.setPropertyValue(property, new BigDecimal(value.toString()));
                } else if (propType != null && Integer.class.isAssignableFrom(propType) && value instanceof String) {
                    // Convert Chinese yes/no strings to Integer 1/0 for DICT-type fields
                    Integer intVal = convertChineseBoolToInt((String) value);
                    if (intVal != null) {
                        wrapper.setPropertyValue(property, intVal);
                    } else {
                        // Try parsing as a plain integer string (e.g. "1", "0")
                        wrapper.setPropertyValue(property, Integer.valueOf((String) value));
                    }
                } else {
                    wrapper.setPropertyValue(property, value);
                }
                updated++;
            } catch (Exception e) {
                log.warn("syncToEnterpriseSetting: failed to set property '{}' = '{}': {}",
                        property, value, e.getMessage());
            }
        }

        if (updated > 0) {
            enterpriseSettingService.save(setting);
            log.info("Synced {} fields from SpreadJS to ent_enterprise_setting for enterprise {}",
                    updated, enterpriseId);
        }
    }

    /**
     * Convert Chinese boolean strings (是/否) to Integer (1/0).
     * Returns null if the string is not a recognized boolean value.
     */
    private static Integer convertChineseBoolToInt(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if ("是".equals(trimmed)) return 1;
        if ("否".equals(trimmed)) return 0;
        return null;
    }
}
