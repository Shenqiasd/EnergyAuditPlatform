package com.energy.audit.dao.mapper.system;

import com.energy.audit.model.entity.system.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * System role mapper
 */
@Mapper
public interface SysRoleMapper {

    SysRole selectById(@Param("id") Long id);

    List<SysRole> selectList(SysRole query);

    int insert(SysRole role);

    int updateById(SysRole role);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
