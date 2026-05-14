package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplTagMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration coverage for GRA-72 / EA-CUST-031:
 *   Spreadsheet TABLE rows mapped to {@code de_tech_reform_history} must be
 *   persisted into the business table for the same enterprise/submission/audit
 *   year so the {@code /api/extracted-data/de_tech_reform_history} endpoint and
 *   regulated chart table2 / Excel export can read the marker rows.
 *
 * <p>The test wires {@link SpreadsheetDataExtractor} →
 * {@link BusinessTablePersister} against an H2 (MySQL mode) datasource that
 * mirrors the production {@code de_tech_reform_history} schema so the
 * end-to-end persistence/query path can be exercised without Spring Boot
 * auto-configuration overhead.
 */
class BusinessTablePersisterTechReformHistoryTest {

    private static final String SHEET_NAME = "1.十四五已实施节能技改项目";
    private static final String CELL_RANGE = "A3:K202";
    private static final String TARGET_TABLE = "de_tech_reform_history";

    /** Mirrors the live tpl_tag_mapping.columnMappings JSON for tag
     *  "0506_表1_已实施节能技改项目" (GRA-72 evidence). */
    private static final String COLUMN_MAPPINGS_JSON =
            "[{\"col\":0,\"field\":\"seq_no\",\"type\":\"NUMBER\"},"
            + "{\"col\":1,\"field\":\"project_name\",\"type\":\"STRING\"},"
            + "{\"col\":2,\"field\":\"project_type\",\"type\":\"STRING\"},"
            + "{\"col\":3,\"field\":\"main_content\",\"type\":\"STRING\"},"
            + "{\"col\":4,\"field\":\"investment\",\"type\":\"NUMBER\"},"
            + "{\"col\":5,\"field\":\"designed_saving\",\"type\":\"NUMBER\"},"
            + "{\"col\":6,\"field\":\"payback_period\",\"type\":\"NUMBER\"},"
            + "{\"col\":7,\"field\":\"completion_date\",\"type\":\"STRING\"},"
            + "{\"col\":8,\"field\":\"actual_saving\",\"type\":\"NUMBER\"},"
            + "{\"col\":9,\"field\":\"is_contract_energy\",\"type\":\"STRING\"},"
            + "{\"col\":10,\"field\":\"remark\",\"type\":\"STRING\"}]";

    private NamedParameterJdbcTemplate jdbcTemplate;
    private BusinessTablePersister persister;
    private SpreadsheetDataExtractor extractor;

    @BeforeEach
    void setUp() throws Exception {
        // Each test gets its own in-memory DB so probeTableMetadata sees the
        // de_tech_reform_history schema fresh and there are no rows from
        // prior submissions contaminating the SELECT … WHERE deleted = 0
        // query under test.
        String dbName = "tech_reform_history_" + UUID.randomUUID().toString().replace('-', '_');
        // Use H2's default (upper-cased) identifier mode on purpose:
        //
        // BusinessTablePersister.probeTableMetadata() must work regardless
        // of how the underlying database folds identifiers. The JDBC
        // ResultSet metadata path that this test exercises bypasses any
        // INFORMATION_SCHEMA case mismatch, so the persister can correctly
        // tell that de_tech_reform_history does NOT have a row_seq column
        // and therefore must not inject one into the INSERT batch. The
        // GRA-72 bug surfaces here when the probe over-reports columns and
        // the resulting INSERT fails with "bad SQL grammar" — every row in
        // the batch then falls back to de_submission_table, leaving the
        // /api/extracted-data/de_tech_reform_history endpoint empty.
        DriverManagerDataSource ds = new DriverManagerDataSource(
                "jdbc:h2:mem:" + dbName + ";MODE=MySQL;" +
                        "DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE",
                "sa", "");
        ds.setDriverClassName("org.h2.Driver");
        this.jdbcTemplate = new NamedParameterJdbcTemplate(ds);

        // Mirror the production de_tech_reform_history schema (see
        // audit-web/src/main/resources/schema-h2.sql §33).
        jdbcTemplate.getJdbcTemplate().execute(
                "CREATE TABLE de_tech_reform_history (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT, " +
                        "submission_id BIGINT NOT NULL DEFAULT 0, " +
                        "enterprise_id BIGINT NOT NULL, " +
                        "audit_year INT NOT NULL, " +
                        "seq_no INT, " +
                        "project_name VARCHAR(256), " +
                        "project_type VARCHAR(64), " +
                        "main_content CLOB, " +
                        "investment DECIMAL(18,4), " +
                        "designed_saving DECIMAL(18,4), " +
                        "payback_period DECIMAL(8,2), " +
                        "completion_date VARCHAR(32), " +
                        "actual_saving DECIMAL(18,4), " +
                        "is_contract_energy VARCHAR(8), " +
                        "remark VARCHAR(512), " +
                        "create_by VARCHAR(64), " +
                        "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "update_by VARCHAR(64), " +
                        "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "deleted TINYINT NOT NULL DEFAULT 0, " +
                        "PRIMARY KEY (id))");

        this.persister = new BusinessTablePersister(jdbcTemplate);
        // BusinessTablePersister probes column metadata via @PostConstruct in
        // Spring; invoke directly so the persister's per-table column filter
        // matches production behaviour.
        Method probe = BusinessTablePersister.class.getDeclaredMethod("probeTableMetadata");
        probe.setAccessible(true);
        probe.invoke(persister);

        this.extractor = new SpreadsheetDataExtractor(new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        if (jdbcTemplate != null) {
            jdbcTemplate.getJdbcTemplate().execute("DROP TABLE de_tech_reform_history");
        }
    }

    private TplTagMapping table1Mapping() {
        TplTagMapping m = new TplTagMapping();
        m.setId(101L);
        m.setTagName("0506_表1_已实施节能技改项目");
        m.setFieldName(TARGET_TABLE);
        m.setTargetTable(TARGET_TABLE);
        m.setDataType("STRING");
        m.setSheetIndex(1);
        m.setSheetName(SHEET_NAME);
        m.setCellRange(CELL_RANGE);
        m.setMappingType("TABLE");
        m.setSourceType("CELL_RANGE");
        m.setColumnMappings(COLUMN_MAPPINGS_JSON);
        return m;
    }

    /**
     * Build a SpreadJS-style submission JSON with the given row data inside
     * sheet "1.十四五已实施节能技改项目".
     */
    private String buildSubmission(String dataTableJson) {
        return "{\"sheets\":{\"" + SHEET_NAME + "\":{\"data\":{\"dataTable\":"
                + dataTableJson + "}}}}";
    }

    /** Cell helper — emits {"col":{"value":val}} pairs separated by commas. */
    private static String cellJson(int col, Object value) {
        String literal;
        if (value instanceof Number) {
            literal = value.toString();
        } else {
            literal = "\"" + value.toString().replace("\"", "\\\"") + "\"";
        }
        return "\"" + col + "\":{\"value\":" + literal + "}";
    }

    /** Replicates the prod query that ExtractedDataController.queryTable runs. */
    private List<Map<String, Object>> queryExtractedTable(long enterpriseId, int auditYear) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM de_tech_reform_history " +
                        "WHERE enterprise_id = :enterpriseId AND audit_year = :auditYear " +
                        "AND deleted = 0 ORDER BY id DESC",
                new MapSqlParameterSource()
                        .addValue("enterpriseId", enterpriseId)
                        .addValue("auditYear", auditYear));
    }

    @Test
    void persistsSparseTechReformHistoryRowFromExtractedSubmission() {
        // Build a SpreadJS dataTable mirroring the GRA-72 marker scenario:
        //   row 2 (UI row 3) carries a unique marker remark plus a few business
        //   fields. Other columns are left blank — this is the typical shape
        //   for a quick verification row written by the QA tool. The bug
        //   reported by Codex is that this row never makes it into
        //   de_tech_reform_history and therefore is invisible to the
        //   /api/extracted-data endpoint and the regulated chart table2 page.
        String marker = "qa-codex-gra72-" + UUID.randomUUID();
        String dataTable = "{"
                + "\"2\":{"
                + cellJson(0, 1) + ","
                + cellJson(1, "qa-codex-gra72-project") + ","
                + cellJson(2, "电机系统节能") + ","
                + cellJson(3, "GRA-72 端到端验证") + ","
                + cellJson(10, marker)
                + "}"
                + "}";

        Map<String, Object> extracted = extractor.extractData(
                buildSubmission(dataTable), List.of(table1Mapping()));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) extracted.get(TARGET_TABLE);
        // Extractor must see the marker row.
        assertThat(rows).as("Extractor produced rows for de_tech_reform_history").isNotNull();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("remark", marker);

        long submissionId = 9001L;
        long enterpriseId = 42L;
        int auditYear = 2026;
        boolean ok = persister.persistTableRows(
                TARGET_TABLE, submissionId, enterpriseId, auditYear, rows, "qa-test");

        assertThat(ok).as("persistTableRows returned a success").isTrue();

        List<Map<String, Object>> persisted = queryExtractedTable(enterpriseId, auditYear);
        assertThat(persisted)
                .as("Persisted rows visible via /api/extracted-data scope (enterprise_id, audit_year, deleted = 0)")
                .hasSize(1);
        Map<String, Object> row = persisted.get(0);
        assertThat(row).containsEntry("remark", marker);
        assertThat(row).containsEntry("project_name", "qa-codex-gra72-project");
        assertThat(row).containsEntry("project_type", "电机系统节能");
        assertThat(((Number) row.get("submission_id")).longValue()).isEqualTo(submissionId);
        assertThat(((Number) row.get("enterprise_id")).longValue()).isEqualTo(enterpriseId);
        assertThat(((Number) row.get("audit_year")).intValue()).isEqualTo(auditYear);
    }

    @Test
    void reExtractionReplacesOnlyCurrentSubmissionRowsAndKeepsMarker() {
        // Simulate the GRA-72 flow:
        //   1. Prior submission produced legacy instruction rows.
        //   2. New submission with the same submission_id is re-extracted,
        //      which soft-deletes the current submission's rows then INSERTs
        //      the freshly extracted marker row.
        //   3. /api/extracted-data must surface ONLY the latest non-deleted
        //      rows for this submission.
        long submissionId = 9002L;
        long enterpriseId = 99L;
        int auditYear = 2026;

        // Seed a legacy row attributed to a different submission so we verify
        // the soft-delete strategy is scoped correctly (does not nuke other
        // submissions' rows, but also does not surface stale data from
        // current submission).
        jdbcTemplate.update(
                "INSERT INTO de_tech_reform_history " +
                        "(submission_id, enterprise_id, audit_year, project_name, create_by, update_by, deleted) " +
                        "VALUES (:sid, :eid, :year, :name, 'seed', 'seed', 0)",
                new MapSqlParameterSource()
                        .addValue("sid", submissionId)
                        .addValue("eid", enterpriseId)
                        .addValue("year", auditYear)
                        .addValue("name", "legacy-instruction-row"));

        persister.deleteForReExtraction(TARGET_TABLE, submissionId, enterpriseId, auditYear, "qa-test");

        // Re-extract marker row and persist.
        String marker = "qa-codex-gra72-reextract-" + UUID.randomUUID();
        String dataTable = "{"
                + "\"3\":{"
                + cellJson(0, 7) + ","
                + cellJson(1, "重新抽取-marker-项目") + ","
                + cellJson(10, marker)
                + "}"
                + "}";
        Map<String, Object> extracted = extractor.extractData(
                buildSubmission(dataTable), List.of(table1Mapping()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) extracted.get(TARGET_TABLE);
        boolean ok = persister.persistTableRows(
                TARGET_TABLE, submissionId, enterpriseId, auditYear, rows, "qa-test");
        assertThat(ok).isTrue();

        List<Map<String, Object>> persisted = queryExtractedTable(enterpriseId, auditYear);
        assertThat(persisted)
                .as("Only the freshly-inserted marker row is visible after re-extraction")
                .hasSize(1);
        assertThat(persisted.get(0)).containsEntry("remark", marker);
        assertThat(persisted.get(0)).containsEntry("project_name", "重新抽取-marker-项目");
    }

    @Test
    void preservesNonBlankRowsWithSparseColumnsAcrossBatch() {
        // Regression net for GRA-72: a batch that mixes rows with different
        // non-null column subsets must persist every row whose extracted data
        // contained at least one non-blank business value. The bug
        // hypothesis was that the all-empty row filter (GRA-73) was dropping
        // rows whose only populated column happened to be VARCHAR-heavy
        // (e.g. remark only).
        String marker1 = "qa-codex-row-with-remark-only";
        String marker2 = "qa-codex-row-with-project-name-only";
        String dataTable = "{"
                + "\"2\":{" + cellJson(10, marker1) + "},"
                + "\"3\":{" + cellJson(1, marker2) + "},"
                + "\"4\":{" + cellJson(0, 3) + ","
                        + cellJson(1, "完整行") + ","
                        + cellJson(4, 1234.5) + "},"
                // Row 5: entirely blank — must be filtered out by extractor.
                + "\"5\":{}"
                + "}";

        Map<String, Object> extracted = extractor.extractData(
                buildSubmission(dataTable), List.of(table1Mapping()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) extracted.get(TARGET_TABLE);
        assertThat(rows).hasSize(3);

        long submissionId = 9003L;
        long enterpriseId = 7L;
        int auditYear = 2027;
        boolean ok = persister.persistTableRows(
                TARGET_TABLE, submissionId, enterpriseId, auditYear, rows, "qa-test");
        assertThat(ok).isTrue();

        List<Map<String, Object>> persisted = queryExtractedTable(enterpriseId, auditYear);
        assertThat(persisted)
                .as("All three non-empty rows must persist, even when each only fills a different column subset")
                .hasSize(3);
        assertThat(persisted)
                .extracting(r -> r.get("remark"))
                .contains(marker1);
        assertThat(persisted)
                .extracting(r -> r.get("project_name"))
                .contains(marker2, "完整行");
    }

    @Test
    void doesNotInjectRowSeqIntoTablesThatLackIt() {
        // Regression net for GRA-72 root cause: BusinessTablePersister used
        // to inject row_seq into every TABLE INSERT whenever the metadata
        // probe could not confirm the absence of the column. For tables
        // that legitimately have no row_seq column (e.g.
        // de_tech_reform_history), the resulting INSERT failed with a
        // bad-SQL-grammar exception and the entire batch fell back to
        // de_submission_table — making the data invisible to
        // /api/extracted-data/de_tech_reform_history, the regulated chart
        // table2 page, and Excel export. The fix only injects row_seq
        // when the column has been positively confirmed by the probe.
        String marker = "qa-codex-row-seq-guard-" + UUID.randomUUID();
        String dataTable = "{\"4\":{" + cellJson(0, 1) + ","
                + cellJson(1, "row-seq-guard") + ","
                + cellJson(10, marker) + "}}";

        Map<String, Object> extracted = extractor.extractData(
                buildSubmission(dataTable), List.of(table1Mapping()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) extracted.get(TARGET_TABLE);

        long submissionId = 9004L;
        long enterpriseId = 11L;
        int auditYear = 2028;
        boolean ok = persister.persistTableRows(
                TARGET_TABLE, submissionId, enterpriseId, auditYear, rows, "qa-test");
        assertThat(ok)
                .as("persistTableRows succeeds without injecting row_seq into a table that lacks it")
                .isTrue();

        // de_tech_reform_history must NOT have a row_seq column in production —
        // confirm here so a future schema migration can't silently mask the
        // regression.
        List<Map<String, Object>> meta = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE UPPER(TABLE_NAME) = 'DE_TECH_REFORM_HISTORY' " +
                        "AND UPPER(COLUMN_NAME) = 'ROW_SEQ'",
                new MapSqlParameterSource());
        assertThat(meta).isEmpty();

        // And the marker row should be queryable via the same scope as
        // ExtractedDataController.queryTable.
        List<Map<String, Object>> persisted = queryExtractedTable(enterpriseId, auditYear);
        assertThat(persisted).hasSize(1);
        assertThat(persisted.get(0)).containsEntry("remark", marker);
    }
}
