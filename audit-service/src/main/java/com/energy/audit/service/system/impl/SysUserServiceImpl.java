package com.energy.audit.service.system.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.system.SysUserMapper;
import com.energy.audit.model.entity.system.SysUser;
import com.energy.audit.service.system.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * System user service implementation
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserMapper sysUserMapper, BCryptPasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("User not found: " + id);
        }
        return user;
    }

    @Override
    public SysUser getByUsername(String username) {
        return sysUserMapper.selectByUsername(username);
    }

    @Override
    public List<SysUser> list(SysUser query) {
        return sysUserMapper.selectList(query);
    }

    @Override
    public void create(SysUser user) {
        if (sysUserMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在: " + user.getUsername());
        }
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setPasswordChanged(0);
        if (user.getStatus() == null) user.setStatus(1);
        String operator = SecurityUtils.getCurrentUsername();
        user.setCreateBy(operator);
        user.setUpdateBy(operator);
        sysUserMapper.insert(user);
    }

    @Override
    public void update(SysUser user) {
        getById(user.getId());
        // Never update password through this method
        user.setPassword(null);
        String operator = SecurityUtils.getCurrentUsername();
        user.setUpdateBy(operator);
        sysUserMapper.updateById(user);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        String operator = SecurityUtils.getCurrentUsername();
        sysUserMapper.deleteById(id, operator);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        getById(id);
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }
        String operator = SecurityUtils.getCurrentUsername();
        // Use dedicated mapper — sets password + password_changed=0 (force user to re-set on next login)
        sysUserMapper.resetPasswordByAdmin(id, passwordEncoder.encode(newPassword), operator);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        getById(id);
        String operator = SecurityUtils.getCurrentUsername();
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        update.setUpdateBy(operator);
        sysUserMapper.updateById(update);
    }
}
