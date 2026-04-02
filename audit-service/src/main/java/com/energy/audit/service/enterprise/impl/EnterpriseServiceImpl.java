package com.energy.audit.service.enterprise.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.service.enterprise.EnterpriseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Enterprise service implementation
 */
@Service
public class EnterpriseServiceImpl implements EnterpriseService {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseServiceImpl.class);

    private final EntEnterpriseMapper enterpriseMapper;

    public EnterpriseServiceImpl(EntEnterpriseMapper enterpriseMapper) {
        this.enterpriseMapper = enterpriseMapper;
    }

    @Override
    public EntEnterprise getById(Long id) {
        EntEnterprise enterprise = enterpriseMapper.selectById(id);
        if (enterprise == null) {
            throw new BusinessException("Enterprise not found: " + id);
        }
        return enterprise;
    }

    @Override
    public List<EntEnterprise> list(EntEnterprise query) {
        return enterpriseMapper.selectList(query);
    }

    @Override
    public void create(EntEnterprise enterprise) {
        String operator = SecurityUtils.getCurrentUsername();
        enterprise.setCreateBy(operator);
        enterprise.setUpdateBy(operator);
        if (enterprise.getIsActive() == null) enterprise.setIsActive(1);
        if (enterprise.getIsLocked() == null) enterprise.setIsLocked(0);
        if (enterprise.getSortOrder() == null) enterprise.setSortOrder(0);
        enterpriseMapper.insert(enterprise);
    }

    @Override
    public void update(EntEnterprise enterprise) {
        getById(enterprise.getId());
        String operator = SecurityUtils.getCurrentUsername();
        enterprise.setUpdateBy(operator);
        enterpriseMapper.updateById(enterprise);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        String operator = SecurityUtils.getCurrentUsername();
        enterpriseMapper.deleteById(id, operator);
    }

    @Override
    public void lock(Long id) {
        getById(id);
        String operator = SecurityUtils.getCurrentUsername();
        EntEnterprise update = new EntEnterprise();
        update.setId(id);
        update.setIsLocked(1);
        update.setUpdateBy(operator);
        enterpriseMapper.updateById(update);
    }

    @Override
    public void unlock(Long id) {
        getById(id);
        String operator = SecurityUtils.getCurrentUsername();
        EntEnterprise update = new EntEnterprise();
        update.setId(id);
        update.setIsLocked(0);
        update.setUpdateBy(operator);
        enterpriseMapper.updateById(update);
    }

    @Override
    public void updateExpireDate(Long id, LocalDate expireDate) {
        getById(id);
        String operator = SecurityUtils.getCurrentUsername();
        EntEnterprise update = new EntEnterprise();
        update.setId(id);
        update.setExpireDate(expireDate);
        update.setUpdateBy(operator);
        enterpriseMapper.updateById(update);
    }
}
