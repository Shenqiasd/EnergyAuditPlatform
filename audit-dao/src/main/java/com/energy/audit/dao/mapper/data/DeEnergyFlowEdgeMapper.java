package com.energy.audit.dao.mapper.data;

import com.energy.audit.model.entity.extraction.DeEnergyFlowEdge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeEnergyFlowEdgeMapper {

    List<DeEnergyFlowEdge> selectByDiagramId(@Param("diagramId") Long diagramId);

    int insert(DeEnergyFlowEdge record);

    int deleteByDiagramId(@Param("diagramId") Long diagramId, @Param("updateBy") String updateBy);
}
