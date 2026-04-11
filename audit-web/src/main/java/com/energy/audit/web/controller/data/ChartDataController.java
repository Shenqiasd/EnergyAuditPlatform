package com.energy.audit.web.controller.data;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "ChartData", description = "Chart data aggregation API")
@RestController
@RequestMapping("/chart-data")
public class ChartDataController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new com.energy.audit.common.exception.BusinessException("仅企业用户可访问图表数据");
        }
    }

    @Operation(summary = "Energy consumption structure (pie chart)")
    @GetMapping("/energy-structure")
    public R<List<Map<String, Object>>> energyStructure(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT energy_name AS name, standard_coal AS value " +
            "FROM de_energy_consumption WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
            "AND standard_coal IS NOT NULL AND standard_coal > 0 " +
            "ORDER BY standard_coal DESC",
            enterpriseId, auditYear
        );
        return R.ok(rows);
    }

    @Operation(summary = "Energy consumption trends (line chart) — mapping C2 + C7")
    @GetMapping("/energy-trend")
    public R<List<Map<String, Object>>> energyTrend(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT indicator_year AS \"year\", total_energy_equiv AS totalEnergy, " +
            "total_energy_equal AS totalEnergyEqual, " +
            "total_energy_excl_material AS totalEnergyExclMaterial, " +
            "unit_output_energy AS unitEnergy, " +
            "gross_output AS grossOutput, sales_revenue AS salesRevenue, " +
            "tax_paid AS taxPaid, production_cost AS productionCost, " +
            "energy_total_cost AS energyTotalCost, energy_cost_ratio AS energyCostRatio " +
            "FROM de_tech_indicator WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
            "ORDER BY indicator_year ASC",
            enterpriseId, auditYear
        );
        return R.ok(rows);
    }

    @Operation(summary = "Product unit consumption comparison (bar chart)")
    @GetMapping("/product-consumption")
    public R<List<Map<String, Object>>> productConsumption(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT p.name AS productName, d.year_type AS yearType, " +
            "d.output, d.energy_consumption AS energyConsumption, d.unit_consumption AS unitConsumption " +
            "FROM de_product_unit_consumption d " +
            "JOIN bs_product p ON p.id = d.product_id " +
            "WHERE d.enterprise_id = ? AND d.audit_year = ? AND d.deleted = 0 " +
            "ORDER BY p.name, d.year_type",
            enterpriseId, auditYear
        );
        return R.ok(rows);
    }

    @Operation(summary = "GHG emission composition (pie + bar chart)")
    @GetMapping("/ghg-emission")
    public R<List<Map<String, Object>>> ghgEmission(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT d.emission_category AS emissionType, d.source_name AS energyName, " +
            "d.co2_emission AS annualEmission " +
            "FROM de_carbon_emission d " +
            "WHERE d.enterprise_id = ? AND d.audit_year = ? AND d.deleted = 0 " +
            "ORDER BY d.emission_category, d.co2_emission DESC",
            enterpriseId, auditYear
        );
        return R.ok(rows);
    }

    @Operation(summary = "Dashboard summary data")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> indicator = jdbcTemplate.queryForMap(
                "SELECT total_energy_equiv, total_energy_equal, total_energy_excl_material, " +
                "unit_output_energy, gross_output, sales_revenue, tax_paid, " +
                "energy_total_cost, production_cost, energy_cost_ratio, " +
                "saving_project_count, saving_invest_total, saving_capacity, saving_benefit, " +
                "coal_target, coal_actual " +
                "FROM de_tech_indicator WHERE enterprise_id = ? AND audit_year = ? AND indicator_year = ? AND deleted = 0",
                enterpriseId, auditYear, auditYear
            );
            result.put("indicator", indicator);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            result.put("indicator", null);
        }

        try {
            Map<String, Object> ghgTotal = jdbcTemplate.queryForMap(
                "SELECT SUM(co2_emission) AS totalEmission, COUNT(*) AS sourceCount " +
                "FROM de_carbon_emission WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0",
                enterpriseId, auditYear
            );
            result.put("ghgTotal", ghgTotal);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            result.put("ghgTotal", null);
        }

        return R.ok(result);
    }
}
