package com.energy.audit.service.audit;

import com.energy.audit.model.entity.audit.AwAuditLog;
import com.energy.audit.model.entity.audit.AwAuditTask;

import java.util.List;
import java.util.Map;

public interface AuditTaskService {

    AwAuditTask submitForAudit(Long enterpriseId, Integer auditYear, String username);

    AwAuditTask getById(Long id);

    AwAuditTask getByEnterpriseAndYear(Long enterpriseId, Integer auditYear);

    List<AwAuditTask> list(AwAuditTask query);

    List<AwAuditTask> listByAssignee(Long assigneeId, Integer status);

    Map<String, Integer> countsByStatus(Long assigneeId);

    void assignTask(Long taskId, Long assigneeId, String operatorName);

    void approveTask(Long taskId, String comment, Long operatorId, String operatorName);

    void rejectTask(Long taskId, String comment, Long operatorId, String operatorName);

    void addComment(Long taskId, String comment, Long operatorId, String operatorName);

    List<AwAuditLog> getTaskLogs(Long taskId);
}
