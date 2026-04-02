package com.energy.audit.service.system.impl;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.dao.mapper.system.SysUserMapper;
import com.energy.audit.model.entity.system.SysUser;
import com.energy.audit.service.system.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * System user service implementation
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    private final SysUserMapper sysUserMapper;

    public SysUserServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
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
        // TODO: encrypt password, validate unique username
        sysUserMapper.insert(user);
    }

    @Override
    public void update(SysUser user) {
        // TODO: validate user exists, handle password update
        sysUserMapper.updateById(user);
    }

    @Override
    public void delete(Long id) {
        // TODO: validate user exists, check dependencies
        String operator = SecurityUtils.getCurrentUsername();
        sysUserMapper.deleteById(id, operator);
    }
}
