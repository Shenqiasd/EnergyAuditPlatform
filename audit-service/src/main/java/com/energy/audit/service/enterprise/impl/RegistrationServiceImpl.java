package com.energy.audit.service.enterprise.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import com.energy.audit.dao.mapper.enterprise.EntRegistrationMapper;
import com.energy.audit.dao.mapper.system.SysUserMapper;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.model.entity.enterprise.EntRegistration;
import com.energy.audit.model.entity.system.SysUser;
import com.energy.audit.service.enterprise.RegistrationService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Registration service implementation
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final EntRegistrationMapper registrationMapper;
    private final EntEnterpriseMapper enterpriseMapper;
    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(EntRegistrationMapper registrationMapper,
                                   EntEnterpriseMapper enterpriseMapper,
                                   SysUserMapper sysUserMapper,
                                   BCryptPasswordEncoder passwordEncoder) {
        this.registrationMapper = registrationMapper;
        this.enterpriseMapper = enterpriseMapper;
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public EntRegistration getById(Long id) {
        EntRegistration reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new BusinessException("Registration not found: " + id);
        }
        return reg;
    }

    @Override
    public List<EntRegistration> list(EntRegistration query) {
        return registrationMapper.selectList(query);
    }

    @Override
    public void submit(EntRegistration registration) {
        registration.setApplyNo("REG" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        registration.setApplyTime(LocalDateTime.now());
        registration.setAuditStatus(0);
        String operator = SecurityUtils.getCurrentUsername();
        registration.setCreateBy(operator);
        registration.setUpdateBy(operator);
        registrationMapper.insert(registration);
    }

    @Override
    @Transactional
    public void approve(Long id, String auditRemark) {
        EntRegistration reg = getById(id);
        if (reg.getAuditStatus() != 0) {
            throw new BusinessException("只能审核待审核状态的申请");
        }

        String operator = SecurityUtils.getCurrentUsername();
        Long operatorId = SecurityUtils.getCurrentUserId();

        // Update registration status
        reg.setAuditStatus(1);
        reg.setAuditUserId(operatorId);
        reg.setAuditTime(LocalDateTime.now());
        reg.setAuditRemark(auditRemark);
        reg.setUpdateBy(operator);
        registrationMapper.updateById(reg);

        // Create enterprise record
        EntEnterprise enterprise = new EntEnterprise();
        enterprise.setEnterpriseName(reg.getEnterpriseName());
        enterprise.setCreditCode(reg.getCreditCode());
        enterprise.setContactPerson(reg.getContactPerson());
        enterprise.setContactEmail(reg.getContactEmail());
        enterprise.setContactPhone(reg.getContactPhone());
        enterprise.setIsActive(1);
        enterprise.setIsLocked(0);
        enterprise.setSortOrder(0);
        enterprise.setCreateBy(operator);
        enterprise.setUpdateBy(operator);
        enterpriseMapper.insert(enterprise);

        // Create enterprise user account; initial password = last 6 digits of credit code
        String creditCode = reg.getCreditCode();
        String initialPassword = creditCode.length() >= 6
                ? creditCode.substring(creditCode.length() - 6)
                : creditCode;

        SysUser user = new SysUser();
        user.setUsername(reg.getCreditCode());
        user.setPassword(passwordEncoder.encode(initialPassword));
        user.setRealName(reg.getContactPerson());
        user.setPhone(reg.getContactPhone());
        user.setEmail(reg.getContactEmail());
        user.setUserType(3);
        user.setEnterpriseId(enterprise.getId());
        user.setStatus(1);
        user.setPasswordChanged(0);
        user.setCreateBy(operator);
        user.setUpdateBy(operator);
        sysUserMapper.insert(user);
    }

    @Override
    public void reject(Long id, String auditRemark) {
        EntRegistration reg = getById(id);
        if (reg.getAuditStatus() != 0) {
            throw new BusinessException("只能驳回待审核状态的申请");
        }

        String operator = SecurityUtils.getCurrentUsername();
        Long operatorId = SecurityUtils.getCurrentUserId();

        reg.setAuditStatus(2);
        reg.setAuditUserId(operatorId);
        reg.setAuditTime(LocalDateTime.now());
        reg.setAuditRemark(auditRemark);
        reg.setUpdateBy(operator);
        registrationMapper.updateById(reg);
    }
}
