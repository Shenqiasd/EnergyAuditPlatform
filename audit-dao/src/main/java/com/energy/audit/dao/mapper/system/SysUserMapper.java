package com.energy.audit.dao.mapper.system;

import com.energy.audit.model.entity.system.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SysUserMapper {

    SysUser selectByUsername(@Param("username") String username);

    SysUser selectById(@Param("id") Long id);

    List<SysUser> selectList(SysUser query);

    int insert(SysUser user);

    int updateById(SysUser user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int resetPasswordByAdmin(@Param("id") Long id, @Param("password") String password, @Param("updateBy") String updateBy);

    int updateLastLoginTime(@Param("id") Long id);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
