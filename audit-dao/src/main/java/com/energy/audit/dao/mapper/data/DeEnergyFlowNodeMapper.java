package com.energy.audit.dao.mapper.data;

import com.energy.audit.model.entity.extraction.DeEnergyFlowNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeEnergyFlowNodeMapper {

    List<DeEnergyFlowNode> selectByDiagramId(@Param("diagramId") Long diagramId);

    int insert(DeEnergyFlowNode record);

    int deleteByDiagramId(@Param("diagramId") Long diagramId, @Param("updateBy") String updateBy);
}
