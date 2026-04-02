package com.energy.audit.service.enterprise.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.service.enterprise.EnterpriseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
        // TODO: validate unique credit code
        enterpriseMapper.insert(enterprise);
    }

    @Override
    public void update(EntEnterprise enterprise) {
        // TODO: validate enterprise exists
        enterpriseMapper.updateById(enterprise);
    }

    @Override
    public void delete(Long id) {
        // TODO: validate enterprise exists, check dependencies
        String operator = SecurityUtils.getCurrentUsername();
        enterpriseMapper.deleteById(id, operator);
    }
}
