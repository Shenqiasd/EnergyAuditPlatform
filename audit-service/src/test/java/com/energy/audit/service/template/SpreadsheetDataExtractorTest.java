package com.energy.audit.service.template;

import com.energy.audit.model.entity.template.TplTagMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpreadsheetDataExtractorTest {

    private static final String SAVING_POTENTIAL_COLUMNS =
            "[{\"col\":0,\"field\":\"seq_no\",\"type\":\"NUMBER\"},"
                    + "{\"col\":1,\"field\":\"project_type\",\"type\":\"STRING\"},"
                    + "{\"col\":2,\"field\":\"project_name\",\"type\":\"STRING\"},"
                    + "{\"col\":3,\"field\":\"main_content\",\"type\":\"STRING\"},"
                    + "{\"col\":4,\"field\":\"saving_potential\",\"type\":\"NUMBER\"},"
                    + "{\"col\":5,\"field\":\"carbon_reduction\",\"type\":\"NUMBER\"},"
                    + "{\"col\":6,\"field\":\"investment\",\"type\":\"NUMBER\"},"
                    + "{\"col\":7,\"field\":\"calc_description\",\"type\":\"STRING\"},"
                    + "{\"col\":8,\"field\":\"remark\",\"type\":\"STRING\"}]";

    private static final String EQUIPMENT_ENERGY_COLUMNS =
            "[{\"col\":0,\"field\":\"seq_no\",\"type\":\"NUMBER\"},"
                    + "{\"col\":1,\"field\":\"device_name\",\"type\":\"STRING\"},"
                    + "{\"col\":2,\"field\":\"model_spec\",\"type\":\"STRING\"},"
                    + "{\"col\":3,\"field\":\"nameplate_output\",\"type\":\"STRING\"},"
                    + "{\"col\":4,\"field\":\"main_energy_name\",\"type\":\"STRING\"},"
                    + "{\"col\":5,\"field\":\"main_energy_consumption\",\"type\":\"NUMBER\"},"
                    + "{\"col\":6,\"field\":\"avg_operating_efficiency\",\"type\":\"NUMBER\"},"
                    + "{\"col\":7,\"field\":\"residual_heat_energy\",\"type\":\"NUMBER\"},"
                    + "{\"col\":8,\"field\":\"available_residual_heat_energy\",\"type\":\"NUMBER\"},"
                    + "{\"col\":9,\"field\":\"utilized_residual_heat_energy\",\"type\":\"NUMBER\"},"
                    + "{\"col\":10,\"field\":\"recovery_utilization_rate\",\"type\":\"NUMBER\"},"
                    + "{\"col\":11,\"field\":\"statistical_load_rate\",\"type\":\"NUMBER\"},"
                    + "{\"col\":12,\"field\":\"test_efficiency\",\"type\":\"NUMBER\"},"
                    + "{\"col\":13,\"field\":\"flue_gas_loss_rate\",\"type\":\"NUMBER\"},"
                    + "{\"col\":14,\"field\":\"heat_loss_rate\",\"type\":\"NUMBER\"},"
                    + "{\"col\":15,\"field\":\"other_loss\",\"type\":\"NUMBER\"},"
                    + "{\"col\":16,\"field\":\"test_date\",\"type\":\"DATE\"}]";

    private SpreadsheetDataExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new SpreadsheetDataExtractor(new ObjectMapper());
    }

    private TplTagMapping savingPotentialMapping() {
        TplTagMapping m = new TplTagMapping();
        m.setId(1L);
        m.setTagName("表16_节能潜力");
        m.setFieldName("de_saving_potential");
        m.setTargetTable("de_saving_potential");
        m.setDataType("STRING");
        m.setSheetIndex(0);
        m.setSheetName("16,节能潜力明细");
        m.setCellRange("A3:I8");
        m.setMappingType("TABLE");
        m.setSourceType("CELL_RANGE");
        m.setRowKeyColumn(0);
        // 表16_节能潜力 has header_row=NULL in prod (the template header lives
        // in row 1, not inside the A3:I102 data range), so we leave it null.
        m.setColumnMappings(SAVING_POTENTIAL_COLUMNS);
        return m;
    }

    private TplTagMapping equipmentEnergyMapping() {
        TplTagMapping m = new TplTagMapping();
        m.setId(2L);
        m.setTagName("0511_表7_重点设备能耗和效率");
        m.setFieldName("de_equipment_energy");
        m.setTargetTable("de_equipment_energy");
        m.setDataType("STRING");
        m.setSheetIndex(0);
        m.setSheetName("7.重点设备能耗和效率");
        m.setCellRange("A3:Q52");
        m.setMappingType("TABLE");
        m.setSourceType("CELL_RANGE");
        m.setRowKeyColumn(0);
        m.setColumnMappings(EQUIPMENT_ENERGY_COLUMNS);
        return m;
    }

    /**
     * Build a SpreadJS-style submission JSON. {@code dataTable} keys are
     * 0-based row indices; for cell range A3:I8 these are rows 2..7.
     */
    private String buildSubmission(String dataTableJson) {
        return "{\"sheets\":{\"16,节能潜力明细\":{\"data\":{\"dataTable\":" + dataTableJson + "}}}}";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(String dataTableJson) {
        Map<String, Object> out = extractor.extractData(
                buildSubmission(dataTableJson), List.of(savingPotentialMapping()));
        return (List<Map<String, Object>>) out.get("de_saving_potential");
    }

    @Test
    void rejectsAllNullPlaceholderRows() {
        // Row 2 (UI row 3) has a real qa-codex record.
        // Rows 3..7 have only style/format metadata (no value) — these are the
        // SpreadJS placeholder rows the bug used to persist.
        String dataTable = "{"
                + "\"2\":{"
                + "  \"0\":{\"value\":1},"
                + "  \"1\":{\"value\":\"电机系统节能\"},"
                + "  \"2\":{\"value\":\"qa-codex-20260513-ea-cust-027\"},"
                + "  \"3\":{\"value\":\"表13端到端验证\"},"
                + "  \"4\":{\"value\":321312},"
                + "  \"5\":{\"value\":123123},"
                + "  \"6\":{\"value\":12312},"
                + "  \"7\":{\"value\":\"节能潜力计算说明-qa\"},"
                + "  \"8\":{\"value\":\"qa cleanup required\"}"
                + "},"
                + "\"3\":{\"0\":{\"style\":\"s1\"}},"
                + "\"4\":{\"0\":{\"style\":\"s1\"},\"3\":{\"style\":\"s2\"}},"
                + "\"5\":{},"
                + "\"6\":{\"0\":{\"style\":\"s1\"}},"
                + "\"7\":{\"0\":{\"style\":\"s1\"}}"
                + "}";

        List<Map<String, Object>> rows = extractRows(dataTable);

        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(((Number) row.get("seq_no")).intValue()).isEqualTo(1);
        assertThat(row).containsEntry("project_type", "电机系统节能");
        assertThat(row).containsEntry("project_name", "qa-codex-20260513-ea-cust-027");
        assertThat(row).containsEntry("main_content", "表13端到端验证");
        assertThat(((Number) row.get("saving_potential")).longValue()).isEqualTo(321312L);
        assertThat(((Number) row.get("carbon_reduction")).longValue()).isEqualTo(123123L);
        assertThat(((Number) row.get("investment")).longValue()).isEqualTo(12312L);
        assertThat(row).containsEntry("calc_description", "节能潜力计算说明-qa");
        assertThat(row).containsEntry("remark", "qa cleanup required");
    }

    @Test
    void rejectsEmptyAndWhitespaceOnlyStringCells() {
        // Rows of explicit empty / whitespace-only string values must not
        // count as non-empty (the original bug — SpreadJS submits these for
        // cells that were touched and then cleared).
        String dataTable = "{"
                + "\"2\":{"
                + "  \"0\":{\"value\":\"\"},\"1\":{\"value\":\"\"},\"2\":{\"value\":\"\"},"
                + "  \"3\":{\"value\":\"\"},\"4\":{\"value\":\"\"},\"5\":{\"value\":\"\"},"
                + "  \"6\":{\"value\":\"\"},\"7\":{\"value\":\"\"},\"8\":{\"value\":\"\"}"
                + "},"
                + "\"3\":{"
                + "  \"0\":{\"value\":\"   \"},\"1\":{\"value\":\"\\t\"},\"2\":{\"value\":\"\\n\"},"
                + "  \"3\":{\"value\":\"  \\t  \"}"
                + "}"
                + "}";

        List<Map<String, Object>> rows = extractRows(dataTable);

        assertThat(rows).isEmpty();
    }

    @Test
    void keepsRowWithMissingSeqNoButPopulatedBusinessFields() {
        // seq_no blank but project_name populated → row must be kept.
        String dataTable = "{"
                + "\"2\":{"
                + "  \"0\":{\"value\":\"\"},"
                + "  \"2\":{\"value\":\"only-name\"}"
                + "}"
                + "}";

        List<Map<String, Object>> rows = extractRows(dataTable);

        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row).containsEntry("project_name", "only-name");
        assertThat(row).containsEntry("seq_no", null);
    }

    @Test
    void normalizeBlankCollapsesWhitespaceStringsToNull() {
        assertThat(SpreadsheetDataExtractor.normalizeBlank(null)).isNull();
        assertThat(SpreadsheetDataExtractor.normalizeBlank("")).isNull();
        assertThat(SpreadsheetDataExtractor.normalizeBlank("   ")).isNull();
        assertThat(SpreadsheetDataExtractor.normalizeBlank("\t\n ")).isNull();
        assertThat(SpreadsheetDataExtractor.normalizeBlank("x")).isEqualTo("x");
        assertThat(SpreadsheetDataExtractor.normalizeBlank("  x  ")).isEqualTo("  x  ");
        assertThat(SpreadsheetDataExtractor.normalizeBlank(0L)).isEqualTo(0L);
        assertThat(SpreadsheetDataExtractor.normalizeBlank(0.0)).isEqualTo(0.0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractsEquipmentEnergyAThroughQWithDate() {
        String json = "{\"sheets\":{\"7.重点设备能耗和效率\":{\"data\":{\"dataTable\":{"
                + "\"2\":{"
                + "\"0\":{\"value\":1},\"1\":{\"value\":\"设备A\"},\"2\":{\"value\":\"M1\"},"
                + "\"3\":{\"value\":\"250kW\"},\"4\":{\"value\":\"电力\"},\"5\":{\"value\":123.45},"
                + "\"6\":{\"value\":88.8},\"7\":{\"value\":10},\"8\":{\"value\":9},\"9\":{\"value\":8},"
                + "\"10\":{\"value\":80},\"11\":{\"value\":75},\"12\":{\"value\":91.2},"
                + "\"13\":{\"value\":1.1},\"14\":{\"value\":2.2},\"15\":{\"value\":3.3},"
                + "\"16\":{\"value\":\"2026/05/15\"}"
                + "},"
                + "\"3\":{\"0\":{\"value\":2},\"1\":{\"value\":\"设备B\"},\"16\":{\"value\":46157}}"
                + "}}}}}";

        Map<String, Object> out = extractor.extractData(json, List.of(equipmentEnergyMapping()));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) out.get("de_equipment_energy");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("device_name", "设备A");
        assertThat(rows.get(0)).containsEntry("main_energy_name", "电力");
        assertThat(rows.get(0)).containsEntry("test_date", "2026-05-15");
        assertThat(rows.get(1)).containsEntry("device_name", "设备B");
        assertThat(rows.get(1)).containsEntry("test_date", "2026-05-15");
    }
}
