package com.energy.audit.service.template;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.model.entity.template.TplEditLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing template edit locks to prevent concurrent editing
 */
@Service
public class EditLockService {

    private static final Logger log = LoggerFactory.getLogger(EditLockService.class);

    /** Lock timeout in minutes */
    private static final int LOCK_TIMEOUT_MINUTES = 30;

    // TODO: Replace ConcurrentHashMap with database-backed storage via TplEditLockMapper
    // Key: "enterpriseId:templateId:auditYear"
    private final Map<String, TplEditLock> lockStore = new ConcurrentHashMap<>();

    private String lockKey(Long enterpriseId, Long templateId, Integer auditYear) {
        return enterpriseId + ":" + templateId + ":" + auditYear;
    }

    /**
     * Acquire an edit lock for a template within an enterprise and audit year
     */
    public TplEditLock acquireLock(Long enterpriseId, Long templateId, Integer auditYear) {
        String key = lockKey(enterpriseId, templateId, auditYear);
        TplEditLock existingLock = lockStore.get(key);
        Long currentUserId = SecurityUtils.getRequiredCurrentUserId();

        if (existingLock != null) {
            if (existingLock.getExpireTime().isAfter(LocalDateTime.now())) {
                if (!existingLock.getLockUserId().equals(currentUserId)) {
                    throw new BusinessException("当前文档正在被其他用户编辑中");
                }
                existingLock.setExpireTime(LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES));
                return existingLock;
            }
            lockStore.remove(key);
        }

        TplEditLock lock = new TplEditLock();
        lock.setEnterpriseId(enterpriseId);
        lock.setTemplateId(templateId);
        lock.setAuditYear(auditYear);
        lock.setLockUserId(currentUserId);
        lock.setLockTime(LocalDateTime.now());
        lock.setExpireTime(LocalDateTime.now().plusMinutes(LOCK_TIMEOUT_MINUTES));
        lockStore.put(key, lock);

        log.info("Lock acquired for enterprise:{} template:{} year:{} by user:{}",
                enterpriseId, templateId, auditYear, currentUserId);
        return lock;
    }

    /**
     * Release an edit lock
     */
    public void releaseLock(Long enterpriseId, Long templateId, Integer auditYear) {
        String key = lockKey(enterpriseId, templateId, auditYear);
        TplEditLock lock = lockStore.get(key);
        if (lock != null) {
            Long currentUserId = SecurityUtils.getRequiredCurrentUserId();
            if (lock.getLockUserId().equals(currentUserId)) {
                lockStore.remove(key);
                log.info("Lock released for enterprise:{} template:{} year:{}", enterpriseId, templateId, auditYear);
            } else {
                throw new BusinessException("无法释放其他用户持有的编辑锁");
            }
        }
    }

    /**
     * Check if a template is locked for editing
     */
    public TplEditLock checkLock(Long enterpriseId, Long templateId, Integer auditYear) {
        String key = lockKey(enterpriseId, templateId, auditYear);
        TplEditLock lock = lockStore.get(key);
        if (lock != null && lock.getExpireTime().isBefore(LocalDateTime.now())) {
            lockStore.remove(key);
            return null;
        }
        return lock;
    }
}
