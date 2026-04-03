package com.energy.audit.web.controller.data;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.service.template.BusinessTablePersister;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Extracted Data", description = "Query extracted business data from de_* tables")
@RestController
@RequestMapping("/extracted-data")
public class ExtractedDataController {

    private static final int MAX_ROWS = 1000;

    private static final Map<String, String> TABLE_LABELS;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("de_company_overview",         "企业概况");
        m.put("de_tech_indicator",           "技术经济指标");
        m.put("de_energy_consumption",       "能源消费量");
        m.put("de_energy_conversion",        "能源加工转换");
        m.put("de_product_unit_consumption", "产品单耗");
        m.put("de_equipment_detail",         "设备明细");
        m.put("de_carbon_emission",          "碳排放");
        m.put("de_energy_balance",           "能源平衡");
        m.put("de_energy_flow",              "能源流向");
        m.put("de_five_year_target",         "十四五目标");
        m.put("de_tech_reform_history",      "节能改造历史");
        m.put("de_saving_project",           "节能项目");
        m.put("de_product_output",           "产品产量");
        m.put("de_meter_instrument",         "计量器具");
        m.put("de_meter_config_rate",        "计量器具配备率");
        m.put("de_obsolete_equipment",       "淘汰设备");
        m.put("de_product_energy_cost",      "产品能源成本");
        m.put("de_saving_calculation",       "节能量计算");
        m.put("de_management_policy",        "管理制度");
        m.put("de_saving_potential",         "节能潜力");
        m.put("de_management_suggestion",    "管理建议");
        m.put("de_tech_reform_suggestion",   "技改建议");
        m.put("de_rectification",            "整改措施");
        m.put("de_report_text",             "报告文本");
        TABLE_LABELS = Collections.unmodifiableMap(m);
    }

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ExtractedDataController(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new BusinessException(403, "该操作仅企业用户可执行");
        }
    }

    @Operation(summary = "List available business tables with labels and record counts")
    @GetMapping("/tables")
    public R<List<Map<String, Object>>> listTables() {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> e : TABLE_LABELS.entrySet()) {
            String countSql = "SELECT COUNT(*) FROM " + e.getKey()
                    + " WHERE enterprise_id = :enterpriseId AND deleted = 0";
            MapSqlParameterSource params = new MapSqlParameterSource("enterpriseId", enterpriseId);
            int count = jdbcTemplate.queryForObject(countSql, params, Integer.class);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tableName", e.getKey());
            item.put("label", e.getValue());
            item.put("count", count);
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "Query data from a specific business table")
    @GetMapping("/{tableName}")
    public R<List<Map<String, Object>>> queryTable(
            @PathVariable String tableName,
            @RequestParam(required = false) Integer auditYear) {

        requireEnterprise();

        String table = tableName.toLowerCase();
        if (!BusinessTablePersister.ALLOWED_TABLES.contains(table)) {
            return R.fail("Invalid table name: " + tableName);
        }

        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(table)
                .append(" WHERE enterprise_id = :enterpriseId AND deleted = 0");
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("enterpriseId", enterpriseId);

        if (auditYear != null) {
            sql.append(" AND audit_year = :auditYear");
            params.addValue("auditYear", auditYear);
        }

        sql.append(" ORDER BY id ASC LIMIT ").append(MAX_ROWS);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);
        return R.ok(rows);
    }
}
