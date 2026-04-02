package com.energy.audit.dao.mapper.system;

import com.energy.audit.model.entity.system.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * System operation log mapper
 */
@Mapper
public interface SysOperationLogMapper {

    int insert(SysOperationLog log);
}
