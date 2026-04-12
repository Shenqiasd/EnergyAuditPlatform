package com.energy.audit.service.template;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class BusinessTablePersister {

    private static final Logger log = LoggerFactory.getLogger(BusinessTablePersister.class);

    public static final Set<String> ALLOWED_TABLES = Set.of(
            "de_company_overview", "de_tech_indicator", "de_energy_consumption",
            "de_energy_conversion", "de_product_unit_consumption", "de_equipment_detail",
            "de_carbon_emission", "de_ghg_emission", "de_energy_balance", "de_energy_flow",
            "de_five_year_target", "de_tech_reform_history", "de_saving_project",
            "de_product_output", "de_meter_instrument", "de_meter_config_rate",
            "de_obsolete_equipment", "de_product_energy_cost", "de_saving_calculation",
            "de_management_policy", "de_saving_potential", "de_management_suggestion",
            "de_tech_reform_suggestion", "de_rectification", "de_report_text",
            "de_equipment_benchmark", "de_equipment_summary", "de_equipment_test",
            "de_equipment_energy", "de_carbon_peak_info",
            "de_energy_ghg_source"
    );

    private static final Set<String> SYSTEM_COLUMNS = Set.of(
            "submission_id", "enterprise_id", "audit_year",
            "create_by", "create_time", "update_by", "update_time", "deleted"
    );

    /**
     * Table-specific NOT NULL columns that should default to audit_year
     * when not explicitly provided by the extraction data.
     * Key = table name, Value = set of column names to auto-fill with auditYear.
     */
    private static final Map<String, Set<String>> YEAR_COLUMNS_BY_TABLE = Map.of(
            "de_tech_indicator", Set.of("indicator_year")
    );

    private static final java.util.regex.Pattern SAFE_COLUMN_PATTERN =
            java.util.regex.Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** Tables confirmed to have the submission_id column in the live DB. */
    private Set<String> tablesWithSubmissionId = Collections.emptySet();

    /** Known columns per table — probed at startup to skip unknown columns gracefully. */
    private Map<String, Set<String>> knownColumnsByTable = Collections.emptyMap();

    public BusinessTablePersister(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void probeTableMetadata() {
        Set<String> confirmedSid = new HashSet<>();
        Map<String, Set<String>> columnMap = new HashMap<>();

        for (String table : ALLOWED_TABLES) {
            try {
                List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                        "SELECT * FROM " + table + " WHERE 1=0",
                        new MapSqlParameterSource());
                // cols is empty list but the metadata is available from column names
                Set<String> colNames = new HashSet<>();
                try {
                    // Use INFORMATION_SCHEMA for reliable column discovery
                    List<Map<String, Object>> infoRows = jdbcTemplate.queryForList(
                            "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = :table",
                            new MapSqlParameterSource("table", table));
                    for (Map<String, Object> row : infoRows) {
                        Object cn = row.get("COLUMN_NAME");
                        if (cn != null) colNames.add(cn.toString().toLowerCase());
                    }
                } catch (Exception e) {
                    log.debug("INFORMATION_SCHEMA probe failed for '{}', skipping column filter", table);
                }
                if (!colNames.isEmpty()) {
                    columnMap.put(table, colNames);
                }
                if (colNames.contains("submission_id")) {
                    confirmedSid.add(table);
                }
            } catch (Exception e) {
                log.warn("Table '{}' does not exist or is inaccessible — skipping", table);
            }
        }

        this.tablesWithSubmissionId = Collections.unmodifiableSet(confirmedSid);
        this.knownColumnsByTable = Collections.unmodifiableMap(columnMap);
        log.info("BusinessTablePersister: {}/{} tables probed, {}/{} have submission_id",
                columnMap.size(), ALLOWED_TABLES.size(),
                confirmedSid.size(), ALLOWED_TABLES.size());
    }

    private boolean hasSubmissionId(String tableName) {
        return tablesWithSubmissionId.contains(tableName.toLowerCase());
    }

    public boolean isBusinessTable(String tableName) {
        return tableName != null && ALLOWED_TABLES.contains(tableName.toLowerCase());
    }

    /**
     * Check if a column exists in the given table.
     * Returns true if column metadata is unknown (not probed) to avoid false negatives.
     */
    private boolean isKnownColumn(String tableName, String columnName) {
        Set<String> cols = knownColumnsByTable.get(tableName.toLowerCase());
        if (cols == null) return true; // no metadata — assume column exists
        return cols.contains(columnName.toLowerCase());
    }

    public void deleteBySubmissionId(String tableName, Long submissionId, String operator) {
        if (!isBusinessTable(tableName)) return;

        String sql;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("updateBy", operator);

        if (hasSubmissionId(tableName)) {
            sql = "UPDATE " + tableName + " SET deleted = 1, update_by = :updateBy, update_time = NOW() " +
                    "WHERE submission_id = :submissionId AND deleted = 0";
            params.addValue("submissionId", submissionId);
        } else {
            log.warn("Table '{}' missing submission_id — skipping deleteBySubmissionId (use deleteForReExtraction instead)", tableName);
            return;
        }
        int deleted = jdbcTemplate.update(sql, params);
        log.debug("Soft-deleted {} rows from {} for submission {}", deleted, tableName, submissionId);
    }

    /**
     * Unified pre-extraction delete: uses submission_id when available,
     * falls back to enterprise_id+audit_year otherwise.
     */
    public void deleteForReExtraction(String tableName, Long submissionId,
                                       Long enterpriseId, Integer auditYear, String operator) {
        if (!isBusinessTable(tableName)) return;

        if (hasSubmissionId(tableName)) {
            deleteBySubmissionId(tableName, submissionId, operator);
        } else {
            deleteByEnterpriseAndYear(tableName, enterpriseId, auditYear, operator);
        }
    }

    public void deleteByEnterpriseAndYear(String tableName, Long enterpriseId, Integer auditYear, String operator) {
        if (!isBusinessTable(tableName)) return;

        String sql = "UPDATE " + tableName + " SET deleted = 1, update_by = :updateBy, update_time = NOW() " +
                "WHERE enterprise_id = :enterpriseId AND audit_year = :auditYear AND deleted = 0";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("enterpriseId", enterpriseId);
        params.addValue("auditYear", auditYear);
        params.addValue("updateBy", operator);
        int deleted = jdbcTemplate.update(sql, params);
        log.debug("Soft-deleted {} rows from {} for enterprise {} year {}", deleted, tableName, enterpriseId, auditYear);
    }

    /**
     * Returns false if the column doesn't exist in the table and the caller
     * should fall back to generic storage.
     */
    public boolean persistScalar(String tableName, Long submissionId, Long enterpriseId,
                               Integer auditYear, String fieldName, Object value, String operator) {
        if (!isBusinessTable(tableName)) return false;

        String columnName = camelToSnake(fieldName);
        if (!SAFE_COLUMN_PATTERN.matcher(columnName).matches()) {
            log.warn("Skipping unsafe column name '{}' for scalar persist to '{}'", columnName, tableName);
            return false;
        }
        if (!isKnownColumn(tableName, columnName)) {
            log.warn("Column '{}' does not exist in table '{}' — falling back to generic storage",
                    columnName, tableName);
            return false;
        }
        Map<String, Object> row = new HashMap<>();
        if (hasSubmissionId(tableName)) {
            row.put("submission_id", submissionId);
        }
        row.put("enterprise_id", enterpriseId);
        row.put("audit_year", auditYear);
        row.put(columnName, convertValue(value));
        row.put("create_by", operator);
        row.put("update_by", operator);
        row.put("deleted", 0);
        fillRequiredYearColumns(tableName, row, auditYear);

        try {
            insertOrMergeRow(tableName, submissionId, enterpriseId, auditYear, row);
        } catch (Exception e) {
            log.warn("persistScalar failed for table '{}' column '{}' — falling back to generic storage: {}",
                    tableName, columnName, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean persistTableRows(String tableName, Long submissionId, Long enterpriseId,
                                  Integer auditYear, List<Map<String, Object>> rows, String operator) {
        if (!isBusinessTable(tableName)) return false;

        boolean hasSid = hasSubmissionId(tableName);
        List<Map<String, Object>> dbRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> dbRow = new HashMap<>();
            if (hasSid) {
                dbRow.put("submission_id", submissionId);
            }
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
                    if (!SAFE_COLUMN_PATTERN.matcher(colName).matches()) {
                        log.warn("Skipping unsafe column name '{}' for table '{}'", colName, tableName);
                        continue;
                    }
                    if (!isKnownColumn(tableName, colName)) {
                        log.debug("Skipping unknown column '{}' for table '{}'", colName, tableName);
                        continue;
                    }
                    dbRow.put(colName, convertValue(entry.getValue()));
                }
            }
            // Fill required year columns AFTER user data so putIfAbsent
            // restores the default when extraction provides null
            fillRequiredYearColumns(tableName, dbRow, auditYear);
            dbRows.add(dbRow);
        }

        if (dbRows.isEmpty()) return true;

        // Collect the UNION of all column keys across all rows so that
        // heterogeneous rows (e.g. different device types in EQUIPMENT_BENCHMARK)
        // produce a single INSERT statement with NULL for missing columns.
        Set<String> allColumns = new java.util.LinkedHashSet<>();
        for (Map<String, Object> dbRow : dbRows) {
            allColumns.addAll(dbRow.keySet());
        }
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

        try {
            jdbcTemplate.batchUpdate(sql.toString(), batchParams);
            log.info("Inserted {} rows into {} for submission {}", dbRows.size(), tableName, submissionId);
            return true;
        } catch (Exception e) {
            log.warn("persistTableRows failed for table '{}' ({} rows) — falling back to generic storage: {}",
                    tableName, dbRows.size(), e.getMessage());
            return false;
        }
    }

    private void insertOrMergeRow(String tableName, Long submissionId, Long enterpriseId,
                                   Integer auditYear, Map<String, Object> row) {
        String checkSql;
        MapSqlParameterSource checkParams = new MapSqlParameterSource();
        if (hasSubmissionId(tableName)) {
            checkSql = "SELECT id FROM " + tableName +
                    " WHERE submission_id = :submissionId AND deleted = 0 LIMIT 1";
            checkParams.addValue("submissionId", submissionId);
        } else {
            checkSql = "SELECT id FROM " + tableName +
                    " WHERE enterprise_id = :enterpriseId AND audit_year = :auditYear AND deleted = 0 LIMIT 1";
            checkParams.addValue("enterpriseId", enterpriseId);
            checkParams.addValue("auditYear", auditYear);
        }
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(checkSql, checkParams);

        if (!existing.isEmpty()) {
            Long existingId = ((Number) existing.get(0).get("id")).longValue();
            StringBuilder updateSql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
            List<String> setClauses = new ArrayList<>();
            MapSqlParameterSource updateParams = new MapSqlParameterSource();
            Set<String> autoYearCols = YEAR_COLUMNS_BY_TABLE.getOrDefault(
                    tableName.toLowerCase(), Collections.emptySet());
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String col = entry.getKey();
                if ("submission_id".equals(col) || "enterprise_id".equals(col) ||
                    "audit_year".equals(col) || "create_by".equals(col) || "deleted".equals(col)) {
                    continue;
                }
                // Skip auto-filled year columns on UPDATE to avoid overwriting existing values
                if (autoYearCols.contains(col)) {
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

    /**
     * Auto-populate table-specific NOT NULL year columns (e.g. indicator_year)
     * from auditYear when they are not already present in the row.
     */
    private void fillRequiredYearColumns(String tableName, Map<String, Object> row, Integer auditYear) {
        Set<String> yearCols = YEAR_COLUMNS_BY_TABLE.get(tableName.toLowerCase());
        if (yearCols == null) return;
        for (String col : yearCols) {
            row.putIfAbsent(col, auditYear);
        }
    }

    private Object convertValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        String str = value.toString();
        // Coerce blank strings to null — MySQL rejects '' for numeric/integer columns
        if (str.isBlank()) return null;
        // Attempt numeric coercion for strings that look like numbers
        // (SpreadJS often returns numeric values as strings)
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException ignored) {
            // Not a number — return as-is
        }
        return str;
    }

    static String camelToSnake(String camel) {
        if (camel == null) return null;
        if (camel.contains("_")) return camel.toLowerCase();
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
