package com.energy.audit.dao.mapper.system;

import com.energy.audit.model.entity.system.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * System dictionary type mapper
 */
@Mapper
public interface SysDictTypeMapper {

    SysDictType selectById(@Param("id") Long id);

    SysDictType selectByDictType(@Param("dictType") String dictType);

    List<SysDictType> selectList(SysDictType query);

    int insert(SysDictType dictType);

    int updateById(SysDictType dictType);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
