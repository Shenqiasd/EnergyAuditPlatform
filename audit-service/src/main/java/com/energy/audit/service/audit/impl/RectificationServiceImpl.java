package com.energy.audit.service.audit.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.dao.mapper.audit.AwAuditLogMapper;
import com.energy.audit.dao.mapper.audit.AwAuditTaskMapper;
import com.energy.audit.dao.mapper.audit.AwRectificationTrackMapper;
import com.energy.audit.model.entity.audit.AwAuditLog;
import com.energy.audit.model.entity.audit.AwAuditTask;
import com.energy.audit.model.entity.audit.AwRectificationTrack;
import com.energy.audit.service.audit.RectificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RectificationServiceImpl implements RectificationService {

    private static final Logger log = LoggerFactory.getLogger(RectificationServiceImpl.class);

    private final AwRectificationTrackMapper rectificationMapper;
    private final AwAuditTaskMapper taskMapper;
    private final AwAuditLogMapper logMapper;

    public RectificationServiceImpl(AwRectificationTrackMapper rectificationMapper,
                                     AwAuditTaskMapper taskMapper,
                                     AwAuditLogMapper logMapper) {
        this.rectificationMapper = rectificationMapper;
        this.taskMapper = taskMapper;
        this.logMapper = logMapper;
    }

    @Override
    @Transactional
    public void createItems(Long taskId, List<AwRectificationTrack> items, Long operatorId, String username) {
        AwAuditTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "审核任务不存在");
        }

        for (AwRectificationTrack item : items) {
            if (item.getItemName() == null || item.getItemName().isBlank()) {
                throw new BusinessException(400, "整改项名称不能为空");
            }
            item.setId(null);
            item.setTaskId(taskId);
            item.setEnterpriseId(task.getEnterpriseId());
            item.setAuditYear(task.getAuditYear());
            item.setStatus(0);
            item.setCreateBy(username);
            rectificationMapper.insert(item);
        }

        addAuditLog(taskId, operatorId, username, "ADD_RECTIFICATION",
                "添加 " + items.size() + " 项整改要求");
    }

    @Override
    public List<AwRectificationTrack> listByTaskId(Long taskId) {
        return rectificationMapper.selectByTaskId(taskId);
    }

    @Override
    public List<AwRectificationTrack> listByEnterpriseId(Long enterpriseId) {
        return rectificationMapper.selectByEnterpriseId(enterpriseId);
    }

    @Override
    public int countOverdueByTaskId(Long taskId) {
        return rectificationMapper.countOverdueByTaskId(taskId);
    }

    @Override
    @Transactional
    public void updateProgress(Long id, Integer status, String result, Long operatorId, String username) {
        AwRectificationTrack track = rectificationMapper.selectById(id);
        if (track == null) {
            throw new BusinessException(404, "整改项不存在");
        }
        if (track.getStatus() == 2) {
            throw new BusinessException(400, "该整改项已验收完成，不可修改");
        }
        if (track.getStatus() == 3) {
            throw new BusinessException(400, "该整改项已超期，请联系审核员处理");
        }

        if (status != 1 && status != 2) {
            throw new BusinessException(400, "企业只能将状态更新为进行中(1)或已完成(2)");
        }

        AwRectificationTrack update = new AwRectificationTrack();
        update.setId(id);
        update.setStatus(status);
        update.setResult(result);
        update.setUpdateBy(username);
        if (status == 2) {
            update.setCompleteTime(LocalDateTime.now());
        }
        rectificationMapper.updateById(update);

        String statusLabel = status == 1 ? "进行中" : "已完成";
        addAuditLog(track.getTaskId(), operatorId, username, "UPDATE_RECTIFICATION",
                "更新整改项「" + track.getItemName() + "」状态为" + statusLabel);
    }

    @Override
    @Transactional
    public void acceptItem(Long id, Long operatorId, String username) {
        AwRectificationTrack track = rectificationMapper.selectById(id);
        if (track == null) {
            throw new BusinessException(404, "整改项不存在");
        }
        if (track.getStatus() != 1 && track.getStatus() != 2) {
            throw new BusinessException(400, "只有进行中或已完成的整改项可以验收");
        }

        AwRectificationTrack update = new AwRectificationTrack();
        update.setId(id);
        update.setStatus(2);
        update.setCompleteTime(LocalDateTime.now());
        update.setUpdateBy(username);
        rectificationMapper.updateById(update);

        addAuditLog(track.getTaskId(), operatorId, username, "ACCEPT_RECTIFICATION",
                "验收通过整改项「" + track.getItemName() + "」");
    }

    @Override
    public AwRectificationTrack getById(Long id) {
        AwRectificationTrack track = rectificationMapper.selectById(id);
        if (track == null) {
            throw new BusinessException(404, "整改项不存在");
        }
        return track;
    }

    @Override
    @Transactional
    public void markOverdueItems() {
        List<AwRectificationTrack> candidates = rectificationMapper.selectOverdueCandidates();
        if (candidates.isEmpty()) {
            return;
        }

        List<Long> ids = candidates.stream()
                .map(AwRectificationTrack::getId)
                .collect(Collectors.toList());
        rectificationMapper.batchUpdateStatus(ids, 3, "SYSTEM");

        log.info("Marked {} rectification items as overdue", ids.size());

        candidates.stream()
                .map(AwRectificationTrack::getTaskId)
                .distinct()
                .forEach(taskId -> addAuditLog(taskId, 0L, "SYSTEM", "OVERDUE_DETECTED",
                        "系统检测到超期整改项"));
    }

    private void addAuditLog(Long taskId, Long operatorId, String username, String action, String comment) {
        AwAuditLog auditLog = new AwAuditLog();
        auditLog.setTaskId(taskId);
        auditLog.setOperatorId(operatorId != null ? operatorId : 0L);
        auditLog.setAction(action);
        auditLog.setComment(comment);
        auditLog.setOperationTime(LocalDateTime.now());
        auditLog.setCreateBy(username);
        logMapper.insert(auditLog);
    }
}
