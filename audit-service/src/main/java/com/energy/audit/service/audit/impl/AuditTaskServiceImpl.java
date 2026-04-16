package com.energy.audit.service.audit.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.dao.mapper.audit.AwAuditLogMapper;
import com.energy.audit.dao.mapper.audit.AwAuditTaskMapper;
import com.energy.audit.dao.mapper.system.SysUserMapper;
import com.energy.audit.dao.mapper.template.TplSubmissionMapper;
import com.energy.audit.dao.mapper.template.TplTemplateMapper;
import com.energy.audit.model.entity.audit.AwAuditLog;
import com.energy.audit.model.entity.audit.AwAuditTask;
import com.energy.audit.model.entity.system.SysUser;
import com.energy.audit.model.entity.template.TplTemplate;
import com.energy.audit.model.entity.template.TplSubmission;
import com.energy.audit.service.audit.AuditTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditTaskServiceImpl implements AuditTaskService {

    private final AwAuditTaskMapper taskMapper;
    private final AwAuditLogMapper logMapper;
    private final SysUserMapper userMapper;
    private final TplTemplateMapper templateMapper;
    private final TplSubmissionMapper submissionMapper;

    public AuditTaskServiceImpl(AwAuditTaskMapper taskMapper, AwAuditLogMapper logMapper,
                                SysUserMapper userMapper, TplTemplateMapper templateMapper,
                                TplSubmissionMapper submissionMapper) {
        this.taskMapper = taskMapper;
        this.logMapper = logMapper;
        this.userMapper = userMapper;
        this.templateMapper = templateMapper;
        this.submissionMapper = submissionMapper;
    }

    @Override
    @Transactional
    public AwAuditTask submitForAudit(Long enterpriseId, Integer auditYear, String username) {
        validateAllTemplatesSubmitted(enterpriseId, auditYear);

        AwAuditTask existing = taskMapper.selectByEnterpriseAndYear(enterpriseId, auditYear);
        if (existing != null) {
            if (existing.getStatus() == 3) {
                existing.setStatus(1);
                existing.setUpdateBy(username);
                taskMapper.updateById(existing);
                taskMapper.clearResult(existing.getId());
                addLog(existing.getId(), getOperatorId(username), "RESUBMIT", "企业重新提交审核", username);
                return taskMapper.selectById(existing.getId());
            }
            throw new BusinessException(400, "该年度已存在审核任务，当前状态不允许重复提交");
        }

        AwAuditTask task = new AwAuditTask();
        task.setEnterpriseId(enterpriseId);
        task.setAuditYear(auditYear);
        task.setTaskType(1);
        task.setTaskTitle(auditYear + "年度能源审计审核");
        task.setStatus(0);
        task.setCreateBy(username);

        Long assigneeId = autoAssign();
        if (assigneeId != null) {
            task.setAssigneeId(assigneeId);
            task.setAssignTime(LocalDateTime.now());
            task.setStatus(1);
        }

        taskMapper.insert(task);
        addLog(task.getId(), getOperatorId(username), "SUBMIT", "企业提交审核", username);

        if (assigneeId != null) {
            addLog(task.getId(), getOperatorId(username), "AUTO_ASSIGN", "系统自动分配审核员", username);
        }

        return taskMapper.selectById(task.getId());
    }

    @Override
    public AwAuditTask getById(Long id) {
        AwAuditTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(404, "审核任务不存在");
        }
        return task;
    }

    @Override
    public AwAuditTask getByEnterpriseAndYear(Long enterpriseId, Integer auditYear) {
        return taskMapper.selectByEnterpriseAndYear(enterpriseId, auditYear);
    }

    @Override
    public List<AwAuditTask> list(AwAuditTask query) {
        return taskMapper.selectList(query);
    }

    @Override
    public List<AwAuditTask> listByAssignee(Long assigneeId, Integer status) {
        return taskMapper.selectByAssignee(assigneeId, status);
    }

    @Override
    public Map<String, Integer> countsByStatus(Long assigneeId) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("pending", taskMapper.countByStatus(assigneeId, 0));
        counts.put("reviewing", taskMapper.countByStatus(assigneeId, 1));
        counts.put("approved", taskMapper.countByStatus(assigneeId, 2));
        counts.put("rejected", taskMapper.countByStatus(assigneeId, 3));
        counts.put("completed", taskMapper.countByStatus(assigneeId, 4));
        return counts;
    }

    @Override
    @Transactional
    public void assignTask(Long taskId, Long assigneeId, String operatorName) {
        AwAuditTask task = getById(taskId);
        if (task.getStatus() != 0 && task.getStatus() != 1) {
            throw new BusinessException(400, "当前任务状态不允许分配审核员");
        }

        SysUser auditor = userMapper.selectById(assigneeId);
        if (auditor == null || auditor.getUserType() != 2) {
            throw new BusinessException(400, "指定的用户不是审核员");
        }

        task.setAssigneeId(assigneeId);
        task.setAssignTime(LocalDateTime.now());
        task.setStatus(1);
        task.setUpdateBy(operatorName);
        taskMapper.updateById(task);

        addLog(taskId, getOperatorId(operatorName), "ASSIGN",
                "分配审核员: " + auditor.getRealName(), operatorName);
    }

    @Override
    @Transactional
    public void approveTask(Long taskId, String comment, Long operatorId, String operatorName) {
        AwAuditTask task = getById(taskId);
        if (task.getStatus() != 1) {
            throw new BusinessException(400, "只有审核中的任务可以通过");
        }

        task.setStatus(2);
        task.setCompleteTime(LocalDateTime.now());
        task.setResult(comment);
        task.setUpdateBy(operatorName);
        taskMapper.updateById(task);

        // Sync: mark all submitted templates as approved
        submissionMapper.batchUpdateStatusByEnterpriseAndYear(
                task.getEnterpriseId(), task.getAuditYear(), 1, 2, null, operatorName);

        addLog(taskId, operatorId, "APPROVE", comment, operatorName);
    }

    @Override
    @Transactional
    public void rejectTask(Long taskId, String comment, Long operatorId, String operatorName) {
        AwAuditTask task = getById(taskId);
        if (task.getStatus() != 1) {
            throw new BusinessException(400, "只有审核中的任务可以退回");
        }
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(400, "退回时必须填写退回意见");
        }

        task.setStatus(3);
        task.setResult(comment);
        task.setUpdateBy(operatorName);
        taskMapper.updateById(task);

        // Sync: mark all submitted templates as rejected with the review comment
        submissionMapper.batchUpdateStatusByEnterpriseAndYear(
                task.getEnterpriseId(), task.getAuditYear(), 1, 3, comment, operatorName);

        addLog(taskId, operatorId, "REJECT", comment, operatorName);
    }

    @Override
    @Transactional
    public void addComment(Long taskId, String comment, Long operatorId, String operatorName) {
        getById(taskId);
        if (comment == null || comment.isBlank()) {
            throw new BusinessException(400, "评论内容不能为空");
        }
        addLog(taskId, operatorId, "COMMENT", comment, operatorName);
    }

    @Override
    public List<AwAuditLog> getTaskLogs(Long taskId) {
        return logMapper.selectByTaskId(taskId);
    }

    private void addLog(Long taskId, Long operatorId, String action, String comment, String createBy) {
        AwAuditLog log = new AwAuditLog();
        log.setTaskId(taskId);
        log.setOperatorId(operatorId != null ? operatorId : 0L);
        log.setAction(action);
        log.setComment(comment);
        log.setOperationTime(LocalDateTime.now());
        log.setCreateBy(createBy);
        logMapper.insert(log);
    }

    private Long getOperatorId(String username) {
        if (username == null) return null;
        SysUser user = userMapper.selectByUsername(username);
        return user != null ? user.getId() : null;
    }

    private void validateAllTemplatesSubmitted(Long enterpriseId, Integer auditYear) {
        TplTemplate tplQuery = new TplTemplate();
        tplQuery.setStatus(1);
        List<TplTemplate> publishedTemplates = templateMapper.selectList(tplQuery);
        if (publishedTemplates.isEmpty()) {
            return;
        }
        for (TplTemplate tpl : publishedTemplates) {
            TplSubmission sub = submissionMapper.selectByEnterpriseTemplateYear(
                    enterpriseId, tpl.getId(), auditYear);
            if (sub == null || sub.getStatus() != 1) {
                throw new BusinessException(400,
                        "模板「" + tpl.getTemplateName() + "」尚未提交，请先完成所有模板填报并提交数据");
            }
        }
    }

    private Long autoAssign() {
        SysUser query = new SysUser();
        query.setUserType(2);
        query.setStatus(1);
        List<SysUser> auditors = userMapper.selectList(query);
        if (auditors.isEmpty()) return null;

        int minCount = Integer.MAX_VALUE;
        Long bestId = null;
        for (SysUser auditor : auditors) {
            int count = taskMapper.countByStatus(auditor.getId(), 1);
            if (count < minCount) {
                minCount = count;
                bestId = auditor.getId();
            }
        }
        return bestId;
    }
}
