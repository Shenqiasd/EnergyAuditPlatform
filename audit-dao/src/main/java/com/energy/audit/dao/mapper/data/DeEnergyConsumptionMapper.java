package com.energy.audit.dao.mapper.data;

import com.energy.audit.model.entity.extraction.DeEnergyConsumption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeEnergyConsumptionMapper {

    List<DeEnergyConsumption> selectByEnterpriseAndYear(@Param("enterpriseId") Long enterpriseId,
                                                         @Param("auditYear") Integer auditYear);
}
