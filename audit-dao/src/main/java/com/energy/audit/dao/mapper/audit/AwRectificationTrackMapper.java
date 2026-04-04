package com.energy.audit.dao.mapper.audit;

import com.energy.audit.model.entity.audit.AwRectificationTrack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AwRectificationTrackMapper {

    AwRectificationTrack selectById(@Param("id") Long id);

    List<AwRectificationTrack> selectList(AwRectificationTrack query);

    List<AwRectificationTrack> selectByTaskId(@Param("taskId") Long taskId);

    List<AwRectificationTrack> selectByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    int countOverdueByTaskId(@Param("taskId") Long taskId);

    List<AwRectificationTrack> selectOverdueCandidates();

    int insert(AwRectificationTrack track);

    int updateById(AwRectificationTrack track);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status, @Param("updateBy") String updateBy);
}
