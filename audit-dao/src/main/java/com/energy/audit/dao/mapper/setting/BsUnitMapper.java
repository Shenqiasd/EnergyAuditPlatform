package com.energy.audit.dao.mapper.setting;

import com.energy.audit.model.entity.setting.BsUnit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Unit setting mapper
 */
@Mapper
public interface BsUnitMapper {

    BsUnit selectById(@Param("id") Long id);

    /** Tenant-scoped lookup — returns null when id does not belong to given enterprise */
    BsUnit selectByIdAndEnterprise(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);

    List<BsUnit> selectList(BsUnit query);

    int insert(BsUnit unit);

    int updateById(BsUnit unit);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
