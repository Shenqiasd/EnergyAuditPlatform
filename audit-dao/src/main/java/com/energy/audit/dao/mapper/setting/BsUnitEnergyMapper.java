package com.energy.audit.dao.mapper.setting;

import com.energy.audit.model.entity.setting.BsUnitEnergy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Unit-energy association mapper
 */
@Mapper
public interface BsUnitEnergyMapper {

    List<BsUnitEnergy> selectByUnitId(@Param("unitId") Long unitId);

    BsUnitEnergy selectByUnitIdAndEnergyId(@Param("unitId") Long unitId, @Param("energyId") Long energyId);

    int insert(BsUnitEnergy unitEnergy);

    int deleteByUnitIdAndEnergyId(@Param("unitId") Long unitId, @Param("energyId") Long energyId,
                                   @Param("updateBy") String updateBy);

    int deleteAllByUnitId(@Param("unitId") Long unitId, @Param("updateBy") String updateBy);
}
