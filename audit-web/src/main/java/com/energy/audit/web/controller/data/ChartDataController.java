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

    @Operation(summary = "Energy consumption structure (pie chart)")
    @GetMapping("/energy-structure")
    public R<List<Map<String, Object>>> energyStructure(@RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT energy_name AS name, standard_coal_equiv AS value, energy_category AS category " +
            "FROM de_energy_balance WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
            "ORDER BY standard_coal_equiv DESC",
            enterpriseId, auditYear
        );
        return R.ok(rows);
    }

    @Operation(summary = "Energy consumption trends (line chart) — mapping C2 + C7")
    @GetMapping("/energy-trend")
    public R<List<Map<String, Object>>> energyTrend(@RequestParam Integer auditYear) {
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
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT product_name AS productName, year_type AS yearType, " +
            "output, energy_consumption AS energyConsumption, unit_consumption AS unitConsumption " +
            "FROM de_product_unit_consumption WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
            "ORDER BY product_name, year_type",
            enterpriseId, auditYear
        );
        return R.ok(rows);
    }

    @Operation(summary = "GHG emission composition (pie + bar chart)")
    @GetMapping("/ghg-emission")
    public R<List<Map<String, Object>>> ghgEmission(@RequestParam Integer auditYear) {
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT emission_type AS emissionType, energy_name AS energyName, " +
            "annual_emission AS annualEmission, main_equipment AS mainEquipment " +
            "FROM de_ghg_emission WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
            "ORDER BY emission_type, annual_emission DESC",
            enterpriseId, auditYear
        );
        return R.ok(rows);
    }

    @Operation(summary = "Dashboard summary data")
    @GetMapping("/summary")
    public R<Map<String, Object>> summary(@RequestParam Integer auditYear) {
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
        } catch (Exception e) {
            result.put("indicator", null);
        }

        try {
            Map<String, Object> ghgTotal = jdbcTemplate.queryForMap(
                "SELECT SUM(annual_emission) AS totalEmission, COUNT(*) AS sourceCount " +
                "FROM de_ghg_emission WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0",
                enterpriseId, auditYear
            );
            result.put("ghgTotal", ghgTotal);
        } catch (Exception e) {
            result.put("ghgTotal", null);
        }

        return R.ok(result);
    }
}
