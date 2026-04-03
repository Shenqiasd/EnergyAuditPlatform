package com.energy.audit.web.controller.data;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.PageResult;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.service.template.BusinessTablePersister;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Tag(name = "ExtractedData", description = "抽取数据总览")
@RestController
@RequestMapping("/extracted-data")
public class ExtractedDataController {

    private static final Map<String, String> TABLE_LABELS = new LinkedHashMap<>();

    static {
        TABLE_LABELS.put("de_company_overview",         "企业概况");
        TABLE_LABELS.put("de_tech_indicator",           "技术经济指标");
        TABLE_LABELS.put("de_energy_consumption",       "能源消费量");
        TABLE_LABELS.put("de_energy_conversion",        "能源加工转换");
        TABLE_LABELS.put("de_product_unit_consumption", "产品单耗");
        TABLE_LABELS.put("de_equipment_detail",         "设备明细");
        TABLE_LABELS.put("de_carbon_emission",          "碳排放");
        TABLE_LABELS.put("de_energy_balance",           "能源平衡");
        TABLE_LABELS.put("de_energy_flow",              "能源流向");
        TABLE_LABELS.put("de_five_year_target",         "十四五目标");
        TABLE_LABELS.put("de_tech_reform_history",      "节能改造历史");
        TABLE_LABELS.put("de_saving_project",           "节能项目");
        TABLE_LABELS.put("de_product_output",           "产品产量");
        TABLE_LABELS.put("de_meter_instrument",         "计量器具");
        TABLE_LABELS.put("de_meter_config_rate",        "计量器具配备率");
        TABLE_LABELS.put("de_obsolete_equipment",       "淘汰设备");
        TABLE_LABELS.put("de_product_energy_cost",      "产品能源成本");
        TABLE_LABELS.put("de_saving_calculation",       "节能量计算");
        TABLE_LABELS.put("de_management_policy",        "管理制度");
        TABLE_LABELS.put("de_saving_potential",         "节能潜力");
        TABLE_LABELS.put("de_management_suggestion",    "管理建议");
        TABLE_LABELS.put("de_tech_reform_suggestion",   "技改建议");
        TABLE_LABELS.put("de_rectification",            "整改措施");
        TABLE_LABELS.put("de_report_text",              "报告文本");
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

    @Operation(summary = "获取所有抽取表的记录数")
    @GetMapping("/tables")
    public R<List<Map<String, Object>>> listTables(
            @RequestParam(required = false) Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("enterpriseId", enterpriseId);

        String yearFilter = "";
        if (auditYear != null) {
            yearFilter = " AND audit_year = :auditYear";
            params.addValue("auditYear", auditYear);
        }

        StringJoiner unionJoiner = new StringJoiner(" UNION ALL ");
        for (String tableName : TABLE_LABELS.keySet()) {
            unionJoiner.add("SELECT '" + tableName + "' AS table_name, COUNT(*) AS cnt FROM "
                    + tableName + " WHERE enterprise_id = :enterpriseId AND deleted = 0" + yearFilter);
        }

        List<Map<String, Object>> countRows = jdbcTemplate.queryForList(unionJoiner.toString(), params);

        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Map<String, Object> row : countRows) {
            countMap.put((String) row.get("table_name"), ((Number) row.get("cnt")).longValue());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : TABLE_LABELS.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tableName", entry.getKey());
            item.put("label", entry.getValue());
            item.put("count", countMap.getOrDefault(entry.getKey(), 0L));
            result.add(item);
        }
        return R.ok(result);
    }

    @Operation(summary = "分页查询指定抽取表的数据")
    @GetMapping("/{tableName}")
    public R<PageResult<Map<String, Object>>> queryTable(
            @PathVariable String tableName,
            @RequestParam(required = false) Integer auditYear,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();

        if (!BusinessTablePersister.ALLOWED_TABLES.contains(tableName)) {
            throw new BusinessException(400, "不允许查询的表: " + tableName);
        }
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 100) pageSize = 100;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("enterpriseId", enterpriseId);

        StringBuilder where = new StringBuilder(" WHERE enterprise_id = :enterpriseId AND deleted = 0");
        if (auditYear != null) {
            where.append(" AND audit_year = :auditYear");
            params.addValue("auditYear", auditYear);
        }

        String countSql = "SELECT COUNT(*) FROM " + tableName + where;
        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);
        if (total == null) total = 0L;

        long offset = (long)(pageNum - 1) * pageSize;
        String dataSql = "SELECT * FROM " + tableName + where
                + " ORDER BY id DESC LIMIT :limit OFFSET :offset";
        params.addValue("limit", pageSize);
        params.addValue("offset", offset);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(dataSql, params);

        return R.ok(PageResult.of(total, rows));
    }
}
