package com.energy.audit.web.controller.audit;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.audit.AwAuditLog;
import com.energy.audit.model.entity.audit.AwAuditTask;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.model.entity.enterprise.EntEnterpriseSetting;
import com.energy.audit.service.audit.AuditTaskService;
import com.energy.audit.service.enterprise.EnterpriseSettingService;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AuditTask", description = "审核任务管理")
@RestController
@RequestMapping("/audit/task")
public class AuditTaskController {

    private final AuditTaskService auditTaskService;
    private final EnterpriseSettingService settingService;
    private final EntEnterpriseMapper enterpriseMapper;

    public AuditTaskController(AuditTaskService auditTaskService,
                               EnterpriseSettingService settingService,
                               EntEnterpriseMapper enterpriseMapper) {
        this.auditTaskService = auditTaskService;
        this.settingService = settingService;
        this.enterpriseMapper = enterpriseMapper;
    }

    @Operation(summary = "企业提交审核")
    @PostMapping("/submit")
    public R<AwAuditTask> submit(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        String username = SecurityUtils.getCurrentUsername();
        AwAuditTask task = auditTaskService.submitForAudit(enterpriseId, auditYear, username);
        return R.ok(task);
    }

    @Operation(summary = "企业查询自己的审核状态")
    @GetMapping("/my-status")
    public R<AwAuditTask> myStatus(@RequestParam Integer auditYear) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(auditTaskService.getByEnterpriseAndYear(enterpriseId, auditYear));
    }

    @Operation(summary = "查询审核任务列表")
    @GetMapping("/list")
    public R<List<AwAuditTask>> list(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) Integer auditYear,
            @RequestParam(required = false) Integer status) {
        Integer userType = requireAuthenticatedUserType();
        AwAuditTask query = new AwAuditTask();
        query.setAuditYear(auditYear);
        query.setStatus(status);

        if (userType == 3) {
            query.setEnterpriseId(SecurityUtils.getRequiredCurrentEnterpriseId());
        } else if (userType == 2) {
            query.setAssigneeId(SecurityUtils.getRequiredCurrentUserId());
            query.setEnterpriseId(enterpriseId);
        } else {
            query.setEnterpriseId(enterpriseId);
        }

        return R.ok(auditTaskService.list(query));
    }

    @Operation(summary = "查询审核任务详情")
    @GetMapping("/{id}")
    public R<AwAuditTask> getById(@PathVariable Long id) {
        AwAuditTask task = auditTaskService.getById(id);
        checkTaskVisibility(task);
        return R.ok(task);
    }

    @Operation(summary = "审核统计")
    @GetMapping("/counts")
    public R<Map<String, Integer>> counts() {
        Integer userType = requireAuthenticatedUserType();
        Long assigneeId = null;
        if (userType == 2) {
            assigneeId = SecurityUtils.getRequiredCurrentUserId();
        } else if (userType == 3) {
            throw new BusinessException(403, "企业用户无权访问统计");
        }
        return R.ok(auditTaskService.countsByStatus(assigneeId));
    }

    @Operation(summary = "分配审核员（管理端）")
    @PostMapping("/{id}/assign")
    public R<Void> assign(@PathVariable Long id, @RequestParam Long assigneeId) {
        requireAdmin();
        String username = SecurityUtils.getCurrentUsername();
        auditTaskService.assignTask(id, assigneeId, username);
        return R.ok();
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        requireAuditor();
        Long operatorId = SecurityUtils.getRequiredCurrentUserId();
        requireTaskAssignee(id, operatorId);
        String username = SecurityUtils.getCurrentUsername();
        String comment = body != null ? body.get("comment") : null;
        auditTaskService.approveTask(id, comment, operatorId, username);
        return R.ok();
    }

    @Operation(summary = "审核退回")
    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        requireAuditor();
        Long operatorId = SecurityUtils.getRequiredCurrentUserId();
        requireTaskAssignee(id, operatorId);
        String username = SecurityUtils.getCurrentUsername();
        String comment = body != null ? body.get("comment") : null;
        auditTaskService.rejectTask(id, comment, operatorId, username);
        return R.ok();
    }

    @Operation(summary = "添加评论")
    @PostMapping("/{id}/comment")
    public R<Void> comment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Integer userType = requireAuthenticatedUserType();
        if (userType == 3) {
            throw new BusinessException(403, "企业用户请通过提交审核操作添加信息");
        }
        AwAuditTask task = auditTaskService.getById(id);
        checkTaskVisibility(task);
        Long operatorId = SecurityUtils.getRequiredCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        String comment = body != null ? body.get("comment") : null;
        auditTaskService.addComment(id, comment, operatorId, username);
        return R.ok();
    }

    @Operation(summary = "查询审核日志")
    @GetMapping("/{id}/logs")
    public R<List<AwAuditLog>> logs(@PathVariable Long id) {
        AwAuditTask task = auditTaskService.getById(id);
        checkTaskVisibility(task);
        return R.ok(auditTaskService.getTaskLogs(id));
    }

    @Operation(summary = "查询任务关联企业信息")
    @GetMapping("/{id}/enterprise-info")
    public R<Map<String, Object>> enterpriseInfo(@PathVariable Long id) {
        AwAuditTask task = auditTaskService.getById(id);
        checkTaskVisibility(task);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        EntEnterprise enterprise = enterpriseMapper.selectById(task.getEnterpriseId());
        if (enterprise != null) {
            result.put("enterpriseName", enterprise.getEnterpriseName());
            result.put("creditCode", enterprise.getCreditCode());
            result.put("contactPerson", enterprise.getContactPerson());
            result.put("contactPhone", enterprise.getContactPhone());
            result.put("contactEmail", enterprise.getContactEmail());
        }
        EntEnterpriseSetting setting = settingService.get(task.getEnterpriseId());
        if (setting != null) {
            result.put("enterpriseAddress", setting.getEnterpriseAddress());
            result.put("legalRepresentative", setting.getLegalRepresentative());
            result.put("industryCode", setting.getIndustryCode());
            result.put("industryCategory", setting.getIndustryCategory());
            result.put("industryName", setting.getIndustryName());
            result.put("unitNature", setting.getUnitNature());
            result.put("energyEnterpriseType", setting.getEnergyEnterpriseType());
            result.put("registeredCapital", setting.getRegisteredCapital());
        }
        return R.ok(result);
    }

    private void checkTaskVisibility(AwAuditTask task) {
        Integer userType = requireAuthenticatedUserType();
        if (userType == 1) {
            return;
        }
        if (userType == 2) {
            Long currentUserId = SecurityUtils.getRequiredCurrentUserId();
            if (!currentUserId.equals(task.getAssigneeId())) {
                throw new BusinessException(403, "无权查看该任务");
            }
            return;
        }
        if (userType == 3) {
            Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
            if (!enterpriseId.equals(task.getEnterpriseId())) {
                throw new BusinessException(403, "无权查看该任务");
            }
            return;
        }
        throw new BusinessException(403, "无权访问");
    }

    private void requireTaskAssignee(Long taskId, Long operatorId) {
        AwAuditTask task = auditTaskService.getById(taskId);
        if (!operatorId.equals(task.getAssigneeId())) {
            throw new BusinessException(403, "您不是该任务的指定审核员");
        }
    }

    private Integer requireAuthenticatedUserType() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null) {
            throw new BusinessException(401, "未认证");
        }
        return userType;
    }

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3) {
            throw new BusinessException(403, "该操作仅企业用户可执行");
        }
    }

    private void requireAdmin() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 1) {
            throw new BusinessException(403, "该操作仅管理员可执行");
        }
    }

    private void requireAuditor() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 2) {
            throw new BusinessException(403, "该操作仅审核员可执行");
        }
    }
}
