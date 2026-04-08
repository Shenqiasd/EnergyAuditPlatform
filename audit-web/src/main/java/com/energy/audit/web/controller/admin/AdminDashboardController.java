package com.energy.audit.web.controller.admin;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin dashboard controller — provides aggregated platform-wide stats,
 * audit overview by year, and recent activity for the admin overview page.
 */
@Tag(name = "Admin Dashboard", description = "Admin dashboard stats, overview & activity")
@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void requireAdmin() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 1) {
            throw new BusinessException(403, "仅管理员可访问管理首页数据");
        }
    }

    /**
     * 4 stat cards: enterprise count, pending registrations, active audit tasks, published templates.
     */
    @Operation(summary = "Dashboard stats — 4 stat cards data")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        requireAdmin();
        Map<String, Object> result = new HashMap<>();

        // Card 1: Total enterprises
        int enterpriseTotal = queryCount(
            "SELECT COUNT(*) FROM ent_enterprise WHERE deleted = 0");
        // New enterprises this year
        int enterpriseNewThisYear = queryCount(
            "SELECT COUNT(*) FROM ent_enterprise WHERE deleted = 0 AND YEAR(create_time) = YEAR(CURDATE())");
        // New enterprises last year
        int enterpriseNewLastYear = queryCount(
            "SELECT COUNT(*) FROM ent_enterprise WHERE deleted = 0 AND YEAR(create_time) = YEAR(CURDATE()) - 1");

        result.put("enterpriseTotal", enterpriseTotal);
        result.put("enterpriseNewThisYear", enterpriseNewThisYear);
        result.put("enterpriseNewLastYear", enterpriseNewLastYear);

        // Card 2: Pending registrations
        int pendingRegistrations = queryCount(
            "SELECT COUNT(*) FROM ent_registration WHERE audit_status = 0 AND deleted = 0");
        result.put("pendingRegistrations", pendingRegistrations);

        // Card 3: Active audit tasks (status 0=pending, 1=in progress)
        int activeAuditTasks = queryCount(
            "SELECT COUNT(*) FROM aw_audit_task WHERE status IN (0, 1) AND deleted = 0");
        // Overdue rectification items
        int overdueRectifications = queryCount(
            "SELECT COUNT(*) FROM aw_rectification_track WHERE status = 3 AND deleted = 0");
        result.put("activeAuditTasks", activeAuditTasks);
        result.put("overdueRectifications", overdueRectifications);

        // Card 4: Published templates
        int publishedTemplates = queryCount(
            "SELECT COUNT(*) FROM tpl_template WHERE status = 1 AND deleted = 0");
        result.put("publishedTemplates", publishedTemplates);

        return R.ok(result);
    }

    /**
     * Audit overview by year — enterprise-level audit completion for a given year.
     */
    @Operation(summary = "Audit overview by year")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview(@RequestParam Integer auditYear) {
        requireAdmin();
        Map<String, Object> result = new HashMap<>();

        // Total enterprises
        int totalEnterprises = queryCount(
            "SELECT COUNT(*) FROM ent_enterprise WHERE deleted = 0");
        result.put("totalEnterprises", totalEnterprises);

        // Enterprises that have submitted at least one template this year
        int submittedEnterprises = queryCount(
            "SELECT COUNT(DISTINCT enterprise_id) FROM tpl_submission " +
            "WHERE audit_year = ? AND status = 1 AND deleted = 0", auditYear);
        result.put("submittedEnterprises", submittedEnterprises);

        // Enterprises with audit tasks this year
        int auditingEnterprises = queryCount(
            "SELECT COUNT(DISTINCT enterprise_id) FROM aw_audit_task " +
            "WHERE audit_year = ? AND status IN (0, 1) AND deleted = 0", auditYear);
        result.put("auditingEnterprises", auditingEnterprises);

        // Enterprises with completed audits this year (status 2=approved, 4=completed)
        int completedEnterprises = queryCount(
            "SELECT COUNT(DISTINCT enterprise_id) FROM aw_audit_task " +
            "WHERE audit_year = ? AND status IN (2, 4) AND deleted = 0", auditYear);
        result.put("completedEnterprises", completedEnterprises);

        // Enterprises with generated reports this year
        int reportedEnterprises = queryCount(
            "SELECT COUNT(DISTINCT enterprise_id) FROM ar_report " +
            "WHERE audit_year = ? AND deleted = 0", auditYear);
        result.put("reportedEnterprises", reportedEnterprises);

        return R.ok(result);
    }

    /**
     * Recent activity — latest registration applications and audit log entries.
     */
    @Operation(summary = "Recent activity")
    @GetMapping("/activity")
    public R<Map<String, Object>> activity() {
        requireAdmin();
        Map<String, Object> result = new HashMap<>();

        // Recent 5 registration applications
        List<Map<String, Object>> recentRegistrations = queryList(
            "SELECT id, enterprise_name AS enterpriseName, credit_code AS creditCode, " +
            "audit_status AS auditStatus, apply_time AS applyTime " +
            "FROM ent_registration WHERE deleted = 0 ORDER BY apply_time DESC LIMIT 5");
        result.put("recentRegistrations", recentRegistrations);

        // Recent 5 audit log entries (with enterprise name)
        List<Map<String, Object>> recentAuditLogs = queryList(
            "SELECT al.id, al.action, al.comment, al.operation_time AS operationTime, " +
            "u.real_name AS operatorName, e.enterprise_name AS enterpriseName " +
            "FROM aw_audit_log al " +
            "LEFT JOIN sys_user u ON al.operator_id = u.id " +
            "LEFT JOIN aw_audit_task at2 ON al.task_id = at2.id " +
            "LEFT JOIN ent_enterprise e ON at2.enterprise_id = e.id " +
            "WHERE al.deleted = 0 ORDER BY al.operation_time DESC LIMIT 5");
        result.put("recentAuditLogs", recentAuditLogs);

        return R.ok(result);
    }

    private int queryCount(String sql, Object... args) {
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            log.warn("AdminDashboard: queryCount failed [{}]", sql, e);
            return 0;
        }
    }

    private List<Map<String, Object>> queryList(String sql, Object... args) {
        try {
            return jdbcTemplate.queryForList(sql, args);
        } catch (DataAccessException e) {
            log.warn("AdminDashboard: queryList failed [{}]", sql, e);
            return new ArrayList<>();
        }
    }
}
