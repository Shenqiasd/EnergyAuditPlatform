package com.energy.audit.dao.mapper.system;

import com.energy.audit.model.entity.system.SysDictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * System dictionary data mapper
 */
@Mapper
public interface SysDictDataMapper {

    SysDictData selectById(@Param("id") Long id);

    List<SysDictData> selectByDictType(@Param("dictType") String dictType);

    List<SysDictData> selectList(SysDictData query);

    int insert(SysDictData dictData);

    int updateById(SysDictData dictData);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);

    int deleteByDictType(@Param("dictType") String dictType, @Param("updateBy") String updateBy);
}
