package com.energy.audit.web.controller.enterprise;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enterprise dashboard controller — provides aggregated stats and progress
 * for the enterprise overview page.
 */
@Tag(name = "Enterprise Dashboard", description = "Enterprise dashboard stats & progress")
@RestController
@RequestMapping("/enterprise/dashboard")
public class EnterpriseDashboardController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new BusinessException("仅企业用户可访问工作台数据");
        }
    }

    @Operation(summary = "Dashboard stats — 4 stat cards data")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        Map<String, Object> result = new HashMap<>();

        // Card 1: total energy equiv (current year)
        result.put("totalEnergyEquiv", queryDecimal(
            "SELECT total_energy_equiv FROM de_tech_indicator " +
            "WHERE enterprise_id = ? AND audit_year = ? AND indicator_year = ? AND deleted = 0",
            enterpriseId, auditYear, auditYear));

        // Card 1: total energy equiv (previous year)
        result.put("totalEnergyEquivPrev", queryDecimal(
            "SELECT total_energy_equiv FROM de_tech_indicator " +
            "WHERE enterprise_id = ? AND audit_year = ? AND indicator_year = ? AND deleted = 0",
            enterpriseId, auditYear, auditYear - 1));

        // Card 2: carbon emission total (SUM of all rows for current year)
        result.put("totalCarbonEmission", queryDecimal(
            "SELECT SUM(annual_emission) FROM de_ghg_emission " +
            "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0",
            enterpriseId, auditYear));

        // Card 2: carbon emission total (previous year)
        result.put("totalCarbonEmissionPrev", queryDecimal(
            "SELECT SUM(annual_emission) FROM de_ghg_emission " +
            "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0",
            enterpriseId, auditYear - 1));

        // Card 3: unit output energy (current year)
        result.put("unitOutputEnergy", queryDecimal(
            "SELECT unit_output_energy FROM de_tech_indicator " +
            "WHERE enterprise_id = ? AND audit_year = ? AND indicator_year = ? AND deleted = 0",
            enterpriseId, auditYear, auditYear));

        // Card 3: unit output energy (previous year)
        result.put("unitOutputEnergyPrev", queryDecimal(
            "SELECT unit_output_energy FROM de_tech_indicator " +
            "WHERE enterprise_id = ? AND audit_year = ? AND indicator_year = ? AND deleted = 0",
            enterpriseId, auditYear, auditYear - 1));

        // Card 4: submission completeness
        Integer submittedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tpl_submission " +
            "WHERE enterprise_id = ? AND audit_year = ? AND status = 1 AND deleted = 0",
            Integer.class, enterpriseId, auditYear);
        Integer totalTemplateCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tpl_template WHERE status = 1 AND deleted = 0",
            Integer.class);

        result.put("submittedCount", submittedCount != null ? submittedCount : 0);
        result.put("totalTemplateCount", totalTemplateCount != null ? totalTemplateCount : 0);

        return R.ok(result);
    }

    @Operation(summary = "Dashboard progress — 4-stage progress items")
    @GetMapping("/progress")
    public R<List<Map<String, Object>>> progress(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        List<Map<String, Object>> items = new ArrayList<>();

        // Stage 1: Basic settings (enterprise info + energy + unit + product)
        int settingParts = 0;
        int settingTotal = 4;

        boolean hasEnterpriseSetting = queryExists(
            "SELECT 1 FROM ent_enterprise_setting WHERE enterprise_id = ? AND deleted = 0",
            enterpriseId);
        if (hasEnterpriseSetting) settingParts++;

        boolean hasEnergy = queryExists(
            "SELECT 1 FROM bs_energy_setting WHERE enterprise_id = ? AND deleted = 0",
            enterpriseId);
        if (hasEnergy) settingParts++;

        boolean hasUnit = queryExists(
            "SELECT 1 FROM bs_unit_setting WHERE enterprise_id = ? AND deleted = 0",
            enterpriseId);
        if (hasUnit) settingParts++;

        boolean hasProduct = queryExists(
            "SELECT 1 FROM bs_product_setting WHERE enterprise_id = ? AND deleted = 0",
            enterpriseId);
        if (hasProduct) settingParts++;

        int settingPct = settingTotal > 0 ? (settingParts * 100 / settingTotal) : 0;
        items.add(progressItem("基础设置（企业/能源/单元/产品）", settingPct,
            settingParts + "/" + settingTotal + " 已配置"));

        // Stage 2: Template submission
        Integer totalTpls = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tpl_template WHERE status = 1 AND deleted = 0",
            Integer.class);
        int total = totalTpls != null ? totalTpls : 0;

        Integer submittedTpls = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tpl_submission " +
            "WHERE enterprise_id = ? AND audit_year = ? AND status = 1 AND deleted = 0",
            Integer.class, enterpriseId, auditYear);
        int submitted = submittedTpls != null ? submittedTpls : 0;

        Integer draftTpls = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tpl_submission " +
            "WHERE enterprise_id = ? AND audit_year = ? AND status = 0 AND deleted = 0",
            Integer.class, enterpriseId, auditYear);
        int drafts = draftTpls != null ? draftTpls : 0;

        int tplPct = 0;
        String tplDetail;
        if (total == 0) {
            tplDetail = "暂无已发布模板";
        } else if (submitted == total) {
            tplPct = 100;
            tplDetail = "全部提交完成";
        } else {
            // weight: submitted=100%, draft=50%, not started=0%
            tplPct = (submitted * 100 + drafts * 50) / total;
            tplDetail = submitted + "/" + total + " 已提交" + (drafts > 0 ? "，" + drafts + " 草稿" : "");
        }
        items.add(progressItem("模板填报", tplPct, tplDetail));

        // Stage 3: Audit submission
        int auditPct = 0;
        String auditDetail;
        try {
            Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT status FROM aw_audit_task " +
                "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 ORDER BY id DESC LIMIT 1",
                enterpriseId, auditYear);
            int status = ((Number) task.get("status")).intValue();
            if (status == 2 || status == 4) {
                auditPct = 100;
                auditDetail = "审核已通过";
            } else if (status == 3) {
                auditPct = 50;
                auditDetail = "已退回，需修改后重新提交";
            } else {
                auditPct = 70;
                auditDetail = "已提交，等待审核";
            }
        } catch (EmptyResultDataAccessException e) {
            auditDetail = submitted == total && total > 0 ? "可提交审核" : "等待所有模板提交";
        }
        items.add(progressItem("提交审核", auditPct, auditDetail));

        // Stage 4: Audit report
        int reportPct = 0;
        String reportDetail;
        try {
            Map<String, Object> report = jdbcTemplate.queryForMap(
                "SELECT status FROM ar_report " +
                "WHERE enterprise_id = ? AND audit_year = ? AND deleted = 0 ORDER BY id DESC LIMIT 1",
                enterpriseId, auditYear);
            int rStatus = ((Number) report.get("status")).intValue();
            if (rStatus >= 2) {
                reportPct = 100;
                reportDetail = "报告已生成";
            } else {
                reportPct = 50;
                reportDetail = "报告生成中";
            }
        } catch (EmptyResultDataAccessException e) {
            reportDetail = "未生成";
        }
        items.add(progressItem("审计报告", reportPct, reportDetail));

        return R.ok(items);
    }

    private Map<String, Object> progressItem(String name, int pct, String detail) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("pct", pct);
        item.put("detail", detail);
        return item;
    }

    private BigDecimal queryDecimal(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private boolean queryExists(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql + " LIMIT 1", args);
        return !rows.isEmpty();
    }
}
