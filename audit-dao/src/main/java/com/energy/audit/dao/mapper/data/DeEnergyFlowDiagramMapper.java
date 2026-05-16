package com.energy.audit.dao.mapper.data;

import com.energy.audit.model.entity.extraction.DeEnergyFlowDiagram;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeEnergyFlowDiagramMapper {

    DeEnergyFlowDiagram selectByEnterpriseYearType(@Param("enterpriseId") Long enterpriseId,
                                                    @Param("auditYear") Integer auditYear,
                                                    @Param("diagramType") Integer diagramType);

    int insert(DeEnergyFlowDiagram record);

    int update(DeEnergyFlowDiagram record);

    int softDelete(@Param("id") Long id, @Param("updateBy") String updateBy);
}
