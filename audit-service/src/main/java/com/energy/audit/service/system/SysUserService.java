package com.energy.audit.service.system;

import com.energy.audit.model.entity.system.SysUser;

import java.util.List;

/**
 * System user service interface
 */
public interface SysUserService {

    SysUser getById(Long id);

    SysUser getByUsername(String username);

    List<SysUser> list(SysUser query);

    void create(SysUser user);

    void update(SysUser user);

    void delete(Long id);

    void resetPassword(Long id, String newPassword);

    void updateStatus(Long id, Integer status);
}
