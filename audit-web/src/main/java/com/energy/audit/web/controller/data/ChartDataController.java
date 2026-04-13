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
        // Three possible schema variants for de_energy_balance:
        //   1. 00-schema.sql: has standard_coal_equiv + consumption_amount + measurement_unit
        //   2. schema.sql:    has consumption_amount + measurement_unit (no standard_coal_equiv)
        //   3. wave4:         has energy_value only (no consumption_amount, no standard_coal_equiv)
        // Try each in order of preference.
        List<Map<String, Object>> rows;
        try {
            // Attempt 1: Check if standard_coal_equiv exists and is fully populated (tce)
            Integer missingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM de_energy_balance " +
                "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "AND energy_name IS NOT NULL AND energy_name <> '' " +
                "AND (standard_coal_equiv IS NULL OR standard_coal_equiv = 0)",
                Integer.class, enterpriseId, auditYear
            );
            Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM de_energy_balance " +
                "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "AND energy_name IS NOT NULL AND energy_name <> ''",
                Integer.class, enterpriseId, auditYear
            );
            boolean useTce = totalCount != null && totalCount > 0 && (missingCount == null || missingCount == 0);
            String valueCol = useTce ? "standard_coal_equiv" : "consumption_amount";
            String unitLabel = useTce ? "'tce'" : "measurement_unit";
            rows = jdbcTemplate.queryForList(
                "SELECT energy_name AS name, " + valueCol + " AS value, " + unitLabel + " AS unit " +
                "FROM de_energy_balance WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "AND energy_name IS NOT NULL AND energy_name <> '' " +
                "AND " + valueCol + " IS NOT NULL AND " + valueCol + " > 0 " +
                "ORDER BY " + valueCol + " DESC",
                enterpriseId, auditYear
            );
        } catch (Exception e1) {
            // Attempt 2: wave4 schema — energy_value column
            try {
                rows = jdbcTemplate.queryForList(
                    "SELECT energy_name AS name, energy_value AS value, '' AS unit " +
                    "FROM de_energy_balance WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                    "AND energy_name IS NOT NULL AND energy_name <> '' " +
                    "AND energy_value IS NOT NULL AND energy_value > 0 " +
                    "ORDER BY energy_value DESC",
                    enterpriseId, auditYear
                );
            } catch (Exception e2) {
                rows = java.util.Collections.emptyList();
            }
        }
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
        // Three possible schema variants for de_product_unit_consumption:
        //   1. wave4: indicator_name, current_indicator, previous_indicator, ...
        //   2. 00-schema.sql: product_name, year_type, unit_consumption, ...
        //   3. schema.sql: product_id (no product_name), year_type, unit_consumption, ...
        List<Map<String, Object>> rows;
        try {
            // Attempt 1: wave4 columns
            rows = jdbcTemplate.queryForList(
                "SELECT indicator_name AS productName, " +
                "current_indicator AS unitConsumption, previous_indicator AS baseConsumption, " +
                "current_numerator AS energyConsumption, current_denominator AS output, " +
                "numerator_unit AS energyUnit, denominator_unit AS outputUnit " +
                "FROM de_product_unit_consumption " +
                "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "AND indicator_name IS NOT NULL AND indicator_name <> '' " +
                "ORDER BY indicator_name",
                enterpriseId, auditYear
            );
        } catch (Exception e1) {
            try {
                // Attempt 2: 00-schema.sql columns (has product_name)
                rows = jdbcTemplate.queryForList(
                    "SELECT product_name AS productName, " +
                    "unit_consumption AS unitConsumption, energy_consumption AS energyConsumption, " +
                    "output, measurement_unit AS energyUnit, year_type AS yearType " +
                    "FROM de_product_unit_consumption " +
                    "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                    "ORDER BY product_name, year_type",
                    enterpriseId, auditYear
                );
            } catch (Exception e2) {
                // Attempt 3: schema.sql columns (no product_name, use product_id)
                try {
                    rows = jdbcTemplate.queryForList(
                        "SELECT CAST(product_id AS CHAR) AS productName, " +
                        "unit_consumption AS unitConsumption, energy_consumption AS energyConsumption, " +
                        "output, measurement_unit AS energyUnit, year_type AS yearType " +
                        "FROM de_product_unit_consumption " +
                        "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                        "ORDER BY product_id, year_type",
                        enterpriseId, auditYear
                    );
                } catch (Exception e3) {
                    rows = java.util.Collections.emptyList();
                }
            }
        }
        return R.ok(rows);
    }

    @Operation(summary = "GHG emission composition (pie + bar chart)")
    @GetMapping("/ghg-emission")
    public R<List<Map<String, Object>>> ghgEmission(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();

        // Try de_ghg_emission first (schema.sql table with emission_type/annual_emission)
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList(
                "SELECT emission_type AS emissionType, " +
                "main_equipment AS energyName, annual_emission AS annualEmission " +
                "FROM de_ghg_emission " +
                "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "AND annual_emission IS NOT NULL AND annual_emission > 0 " +
                "ORDER BY emission_type, annual_emission DESC",
                enterpriseId, auditYear
            );
        } catch (Exception e) {
            // Fallback to de_carbon_emission (wave4 migration table)
            rows = jdbcTemplate.queryForList(
                "SELECT emission_category AS emissionType, source_name AS energyName, " +
                "co2_emission AS annualEmission " +
                "FROM de_carbon_emission " +
                "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 " +
                "AND co2_emission IS NOT NULL AND co2_emission > 0 " +
                "ORDER BY emission_category, co2_emission DESC",
                enterpriseId, auditYear
            );
        }
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
                "SELECT SUM(annual_emission) AS totalEmission, COUNT(*) AS sourceCount " +
                "FROM de_ghg_emission WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0",
                enterpriseId, auditYear
            );
            result.put("ghgTotal", ghgTotal);
        } catch (Exception e) {
            // Fallback to de_carbon_emission if de_ghg_emission doesn't exist
            try {
                Map<String, Object> ghgTotal = jdbcTemplate.queryForMap(
                    "SELECT SUM(co2_emission) AS totalEmission, COUNT(*) AS sourceCount " +
                    "FROM de_carbon_emission WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0",
                    enterpriseId, auditYear
                );
                result.put("ghgTotal", ghgTotal);
            } catch (Exception ex) {
                result.put("ghgTotal", null);
            }
        }

        return R.ok(result);
    }
}
