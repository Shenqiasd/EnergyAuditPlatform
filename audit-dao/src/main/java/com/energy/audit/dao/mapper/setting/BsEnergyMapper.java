package com.energy.audit.dao.mapper.setting;

import com.energy.audit.model.entity.setting.BsEnergy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Energy setting mapper
 */
@Mapper
public interface BsEnergyMapper {

    BsEnergy selectById(@Param("id") Long id);

    /** Tenant-scoped lookup — returns null when id does not belong to given enterprise */
    BsEnergy selectByIdAndEnterprise(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);

    List<BsEnergy> selectList(BsEnergy query);

    int insert(BsEnergy energy);

    int updateById(BsEnergy energy);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
