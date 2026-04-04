package com.energy.audit.dao.mapper.audit;

import com.energy.audit.model.entity.audit.AwAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AwAuditLogMapper {

    List<AwAuditLog> selectByTaskId(@Param("taskId") Long taskId);

    int insert(AwAuditLog log);
}
