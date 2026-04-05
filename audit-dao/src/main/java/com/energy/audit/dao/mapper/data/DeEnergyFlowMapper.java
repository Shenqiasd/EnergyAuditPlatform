package com.energy.audit.dao.mapper.data;

import com.energy.audit.model.entity.data.DeEnergyFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeEnergyFlowMapper {

    List<DeEnergyFlow> selectByEnterpriseAndYear(@Param("enterpriseId") Long enterpriseId,
                                                  @Param("auditYear") Integer auditYear);

    int insert(DeEnergyFlow record);

    int deleteByEnterpriseAndYear(@Param("enterpriseId") Long enterpriseId,
                                  @Param("auditYear") Integer auditYear,
                                  @Param("updateBy") String updateBy);
}
