package com.energy.audit.web.controller.audit;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.result.R;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.audit.AwAuditTask;
import com.energy.audit.model.entity.audit.AwRectificationTrack;
import com.energy.audit.service.audit.AuditTaskService;
import com.energy.audit.service.audit.RectificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Rectification", description = "整改跟踪管理")
@RestController
@RequestMapping("/audit/rectification")
public class RectificationController {

    private final RectificationService rectificationService;
    private final AuditTaskService auditTaskService;

    public RectificationController(RectificationService rectificationService,
                                    AuditTaskService auditTaskService) {
        this.rectificationService = rectificationService;
        this.auditTaskService = auditTaskService;
    }

    @Operation(summary = "创建整改项（审核员）")
    @PostMapping("/create")
    public R<Void> create(@RequestParam Long taskId,
                           @RequestBody List<AwRectificationTrack> items) {
        requireAuditor();
        Long operatorId = SecurityUtils.getRequiredCurrentUserId();
        AwAuditTask task = auditTaskService.getById(taskId);
        if (!operatorId.equals(task.getAssigneeId())) {
            throw new BusinessException(403, "您不是该任务的指定审核员");
        }
        String username = SecurityUtils.getCurrentUsername();
        rectificationService.createItems(taskId, items, operatorId, username);
        return R.ok();
    }

    @Operation(summary = "查询任务整改项列表")
    @GetMapping("/list")
    public R<List<AwRectificationTrack>> list(@RequestParam Long taskId) {
        Integer userType = requireAuthenticatedUserType();
        AwAuditTask task = auditTaskService.getById(taskId);
        checkTaskAccess(task, userType);
        return R.ok(rectificationService.listByTaskId(taskId));
    }

    @Operation(summary = "查询企业整改项列表")
    @GetMapping("/my-list")
    public R<List<AwRectificationTrack>> myList() {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        return R.ok(rectificationService.listByEnterpriseId(enterpriseId));
    }

    @Operation(summary = "查询任务超期整改数")
    @GetMapping("/overdue-count")
    public R<Map<String, Integer>> overdueCount(@RequestParam Long taskId) {
        Integer userType = requireAuthenticatedUserType();
        AwAuditTask task = auditTaskService.getById(taskId);
        checkTaskAccess(task, userType);
        return R.ok(Map.of("count", rectificationService.countOverdueByTaskId(taskId)));
    }

    @Operation(summary = "批量查询多个任务的超期整改数（管理员/审核员）")
    @PostMapping("/overdue-counts")
    public R<Map<Long, Integer>> overdueCounts(@RequestBody List<Long> taskIds) {
        Integer userType = requireAuthenticatedUserType();
        if (userType == 3) {
            throw new BusinessException(403, "企业用户无权查询超期统计");
        }
        Long currentUserId = SecurityUtils.getRequiredCurrentUserId();
        Map<Long, Integer> result = new java.util.LinkedHashMap<>();
        for (Long taskId : taskIds) {
            AwAuditTask task = auditTaskService.getById(taskId);
            if (userType == 2 && !currentUserId.equals(task.getAssigneeId())) {
                continue;
            }
            result.put(taskId, rectificationService.countOverdueByTaskId(taskId));
        }
        return R.ok(result);
    }

    @Operation(summary = "企业更新整改进度")
    @PostMapping("/{id}/update-progress")
    public R<Void> updateProgress(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body) {
        requireEnterprise();
        Long enterpriseId = SecurityUtils.getRequiredCurrentEnterpriseId();
        getRectificationAndCheckEnterprise(id, enterpriseId);
        Object statusObj = body.get("status");
        if (statusObj == null) {
            throw new BusinessException(400, "status 参数不能为空");
        }
        Integer status;
        try {
            status = statusObj instanceof Integer ? (Integer) statusObj : Integer.valueOf(statusObj.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "status 参数格式错误");
        }
        String result = body.get("result") != null ? body.get("result").toString() : null;
        Long operatorId = SecurityUtils.getRequiredCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();
        rectificationService.updateProgress(id, status, result, operatorId, username);
        return R.ok();
    }

    @Operation(summary = "审核员验收整改项")
    @PostMapping("/{id}/accept")
    public R<Void> accept(@PathVariable Long id) {
        requireAuditor();
        Long operatorId = SecurityUtils.getRequiredCurrentUserId();
        AwRectificationTrack track = rectificationService.getById(id);
        AwAuditTask task = auditTaskService.getById(track.getTaskId());
        if (!operatorId.equals(task.getAssigneeId())) {
            throw new BusinessException(403, "您不是该任务的指定审核员");
        }
        String username = SecurityUtils.getCurrentUsername();
        rectificationService.acceptItem(id, operatorId, username);
        return R.ok();
    }

    private AwRectificationTrack getRectificationAndCheckEnterprise(Long id, Long enterpriseId) {
        AwRectificationTrack track = rectificationService.getById(id);
        if (!enterpriseId.equals(track.getEnterpriseId())) {
            throw new BusinessException(403, "无权操作该整改项");
        }
        return track;
    }

    private void checkTaskAccess(AwAuditTask task, Integer userType) {
        if (userType == 1) return;
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

    private Integer requireAuthenticatedUserType() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null) throw new BusinessException(401, "未认证");
        return userType;
    }

    private void requireEnterprise() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 3)
            throw new BusinessException(403, "该操作仅企业用户可执行");
    }

    private void requireAuditor() {
        Integer userType = SecurityUtils.getCurrentUserType();
        if (userType == null || userType != 2)
            throw new BusinessException(403, "该操作仅审核员可执行");
    }

}
