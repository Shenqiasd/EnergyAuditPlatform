package com.energy.audit.service.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class BusinessTablePersister {

    private static final Logger log = LoggerFactory.getLogger(BusinessTablePersister.class);

    private static final Set<String> ALLOWED_TABLES = Set.of(
            "de_company_overview", "de_tech_indicator", "de_energy_consumption",
            "de_energy_conversion", "de_product_unit_consumption", "de_equipment_detail",
            "de_carbon_emission", "de_energy_balance", "de_energy_flow", "de_five_year_target"
    );

    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "submission_id", "enterprise_id", "audit_year",
            "create_by", "create_time", "update_by", "update_time", "deleted"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BusinessTablePersister(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isBusinessTable(String tableName) {
        return tableName != null && ALLOWED_TABLES.contains(tableName.toLowerCase());
    }

    public void deleteBySubmissionId(String tableName, Long submissionId, String operator) {
        if (!isBusinessTable(tableName)) return;

        String sql = "UPDATE " + tableName + " SET deleted = 1, update_by = :updateBy, update_time = NOW() " +
                "WHERE submission_id = :submissionId AND deleted = 0";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("submissionId", submissionId);
        params.addValue("updateBy", operator);
        int deleted = jdbcTemplate.update(sql, params);
        log.debug("Soft-deleted {} rows from {} for submission {}", deleted, tableName, submissionId);
    }

    public void persistScalar(String tableName, Long submissionId, Long enterpriseId,
                               Integer auditYear, String fieldName, Object value, String operator) {
        if (!isBusinessTable(tableName)) return;

        String columnName = camelToSnake(fieldName);
        Map<String, Object> row = new HashMap<>();
        row.put("submission_id", submissionId);
        row.put("enterprise_id", enterpriseId);
        row.put("audit_year", auditYear);
        row.put(columnName, convertValue(value));
        row.put("create_by", operator);
        row.put("update_by", operator);
        row.put("deleted", 0);

        insertOrMergeRow(tableName, submissionId, enterpriseId, auditYear, row);
    }

    public void persistTableRows(String tableName, Long submissionId, Long enterpriseId,
                                  Integer auditYear, List<Map<String, Object>> rows, String operator) {
        if (!isBusinessTable(tableName)) return;

        List<Map<String, Object>> dbRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> dbRow = new HashMap<>();
            dbRow.put("submission_id", submissionId);
            dbRow.put("enterprise_id", enterpriseId);
            dbRow.put("audit_year", auditYear);
            dbRow.put("create_by", operator);
            dbRow.put("update_by", operator);
            dbRow.put("deleted", 0);

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("_")) continue;
                String colName = camelToSnake(key);
                if (!SYSTEM_COLUMNS.contains(colName)) {
                    dbRow.put(colName, convertValue(entry.getValue()));
                }
            }
            dbRows.add(dbRow);
        }

        if (dbRows.isEmpty()) return;

        Set<String> allColumns = dbRows.get(0).keySet();
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        List<String> colList = new ArrayList<>(allColumns);
        sql.append(String.join(", ", colList));
        sql.append(") VALUES (");
        List<String> placeholders = new ArrayList<>();
        for (String col : colList) {
            placeholders.add(":" + col);
        }
        sql.append(String.join(", ", placeholders));
        sql.append(")");

        @SuppressWarnings("unchecked")
        MapSqlParameterSource[] batchParams = dbRows.stream()
                .map(row -> {
                    MapSqlParameterSource ps = new MapSqlParameterSource();
                    for (String col : colList) {
                        ps.addValue(col, row.get(col));
                    }
                    return ps;
                })
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql.toString(), batchParams);
        log.info("Inserted {} rows into {} for submission {}", dbRows.size(), tableName, submissionId);
    }

    private void insertOrMergeRow(String tableName, Long submissionId, Long enterpriseId,
                                   Integer auditYear, Map<String, Object> row) {
        String checkSql = "SELECT id FROM " + tableName +
                " WHERE submission_id = :submissionId AND deleted = 0 LIMIT 1";
        MapSqlParameterSource checkParams = new MapSqlParameterSource();
        checkParams.addValue("submissionId", submissionId);
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, checkParams);

        if (!existing.isEmpty()) {
            Long existingId = ((Number) existing.get(0).get("id")).longValue();
            StringBuilder updateSql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
            List<String> setClauses = new ArrayList<>();
            MapSqlParameterSource updateParams = new MapSqlParameterSource();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String col = entry.getKey();
                if ("submission_id".equals(col) || "enterprise_id".equals(col) ||
                    "audit_year".equals(col) || "create_by".equals(col) || "deleted".equals(col)) {
                    continue;
                }
                setClauses.add(col + " = :" + col);
                updateParams.addValue(col, entry.getValue());
            }
            setClauses.add("update_time = NOW()");
            updateSql.append(String.join(", ", setClauses));
            updateSql.append(" WHERE id = :id AND deleted = 0");
            updateParams.addValue("id", existingId);
            jdbcTemplate.update(updateSql.toString(), updateParams);
        } else {
            List<String> colList = new ArrayList<>(row.keySet());
            StringBuilder insertSql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
            insertSql.append(String.join(", ", colList));
            insertSql.append(") VALUES (");
            List<String> placeholders = new ArrayList<>();
            for (String col : colList) {
                placeholders.add(":" + col);
            }
            insertSql.append(String.join(", ", placeholders));
            insertSql.append(")");

            MapSqlParameterSource insertParams = new MapSqlParameterSource();
            for (String col : colList) {
                insertParams.addValue(col, row.get(col));
            }
            jdbcTemplate.update(insertSql.toString(), insertParams);
        }
    }

    private Object convertValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return value.toString();
    }

    static String camelToSnake(String camel) {
        if (camel == null) return null;
        if (camel.contains("_")) return camel;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
