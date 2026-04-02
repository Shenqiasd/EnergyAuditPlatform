package com.energy.audit.service.template;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.template.TplEditLockMapper;
import com.energy.audit.model.entity.template.TplEditLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing template edit locks (pessimistic locking).
 * Backed by tpl_edit_lock table — locks survive server restarts.
 *
 * Concurrency safety: acquireLock is @Transactional and uses SELECT ... FOR UPDATE
 * to atomically check and claim the lock row, eliminating the TOCTOU race condition.
 */
@Service
public class EditLockService {

    private static final Logger log = LoggerFactory.getLogger(EditLockService.class);

    private static final int LOCK_TIMEOUT_MINUTES = 30;

    private final TplEditLockMapper editLockMapper;

    public EditLockService(TplEditLockMapper editLockMapper) {
        this.editLockMapper = editLockMapper;
    }

    /**
     * Atomically acquire an edit lock for a template/enterprise/year triple.
     *
     * Uses SELECT ... FOR UPDATE inside a transaction to prevent the check-then-write
     * race condition. Scenarios handled:
     *   - No row → INSERT new lock
     *   - Row exists, expired or soft-deleted → UPDATE (reactivate) for current user
     *   - Row exists, active, same user → UPDATE (refresh expiry)
     *   - Row exists, active, different user → throw BusinessException 409
     */
    @Transactional
    public TplEditLock acquireLock(Long enterpriseId, Long templateId, Integer auditYear) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserId();
        String operator = SecurityUtils.getRequiredCurrentUsername();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(LOCK_TIMEOUT_MINUTES);

        TplEditLock existing = editLockMapper.selectByKeyForUpdate(enterpriseId, templateId, auditYear);

        if (existing != null && existing.getDeleted() == 0 && existing.getExpireTime().isAfter(now)) {
            if (!existing.getLockUserId().equals(currentUserId)) {
                throw new BusinessException("当前文档正在被其他用户编辑中");
            }
            editLockMapper.updateByKey(enterpriseId, templateId, auditYear,
                    currentUserId, now, expireTime, operator);
        } else if (existing != null) {
            editLockMapper.updateByKey(enterpriseId, templateId, auditYear,
                    currentUserId, now, expireTime, operator);
        } else {
            TplEditLock lock = new TplEditLock();
            lock.setEnterpriseId(enterpriseId);
            lock.setTemplateId(templateId);
            lock.setAuditYear(auditYear);
            lock.setLockUserId(currentUserId);
            lock.setLockTime(now);
            lock.setExpireTime(expireTime);
            lock.setCreateBy(operator);
            lock.setUpdateBy(operator);
            editLockMapper.insert(lock);
        }

        TplEditLock result = editLockMapper.selectByKey(enterpriseId, templateId, auditYear);
        log.info("Lock acquired: enterprise={} template={} year={} user={}",
                enterpriseId, templateId, auditYear, currentUserId);
        return result;
    }

    /**
     * Release a lock. Only the lock owner may release their own lock.
     */
    public void releaseLock(Long enterpriseId, Long templateId, Integer auditYear) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserId();
        String operator = SecurityUtils.getRequiredCurrentUsername();

        TplEditLock existing = editLockMapper.selectByKey(enterpriseId, templateId, auditYear);
        if (existing != null) {
            if (!existing.getLockUserId().equals(currentUserId)) {
                throw new BusinessException("无法释放其他用户持有的编辑锁");
            }
            editLockMapper.deleteByKey(enterpriseId, templateId, auditYear, operator);
            log.info("Lock released: enterprise={} template={} year={}",
                    enterpriseId, templateId, auditYear);
        }
    }

    /**
     * Renew (heartbeat) an existing lock held by the current user.
     * Extends the expiry by LOCK_TIMEOUT_MINUTES from now.
     * Throws BusinessException if no active lock exists or the caller does not own it.
     */
    @Transactional
    public TplEditLock renewLock(Long enterpriseId, Long templateId, Integer auditYear) {
        Long currentUserId = SecurityUtils.getRequiredCurrentUserId();
        String operator = SecurityUtils.getRequiredCurrentUsername();
        LocalDateTime now = LocalDateTime.now();

        TplEditLock existing = editLockMapper.selectByKey(enterpriseId, templateId, auditYear);
        if (existing == null || existing.getExpireTime().isBefore(now)) {
            throw new BusinessException("编辑锁已过期，请关闭后重新打开文档");
        }
        if (!existing.getLockUserId().equals(currentUserId)) {
            throw new BusinessException("无法续约其他用户持有的编辑锁");
        }
        LocalDateTime newExpiry = now.plusMinutes(LOCK_TIMEOUT_MINUTES);
        editLockMapper.updateByKey(enterpriseId, templateId, auditYear,
                currentUserId, existing.getLockTime(), newExpiry, operator);
        log.debug("Lock renewed: enterprise={} template={} year={} newExpiry={}",
                enterpriseId, templateId, auditYear, newExpiry);
        return editLockMapper.selectByKey(enterpriseId, templateId, auditYear);
    }

    /**
     * Check lock status. Returns null if no active (non-expired) lock exists.
     */
    public TplEditLock checkLock(Long enterpriseId, Long templateId, Integer auditYear) {
        TplEditLock lock = editLockMapper.selectByKey(enterpriseId, templateId, auditYear);
        if (lock != null && lock.getExpireTime().isBefore(LocalDateTime.now())) {
            editLockMapper.deleteByKey(enterpriseId, templateId, auditYear,
                    SecurityUtils.getRequiredCurrentUsername());
            return null;
        }
        return lock;
    }
}
