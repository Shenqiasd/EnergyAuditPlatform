package com.energy.audit.dao.mapper.audit;

import com.energy.audit.model.entity.audit.AwAuditTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AwAuditTaskMapper {

    AwAuditTask selectById(@Param("id") Long id);

    List<AwAuditTask> selectList(AwAuditTask query);

    List<AwAuditTask> selectByAssignee(@Param("assigneeId") Long assigneeId, @Param("status") Integer status);

    AwAuditTask selectByEnterpriseAndYear(@Param("enterpriseId") Long enterpriseId, @Param("auditYear") Integer auditYear);

    int countByStatus(@Param("assigneeId") Long assigneeId, @Param("status") Integer status);

    int insert(AwAuditTask task);

    int updateById(AwAuditTask task);

    int clearResult(@Param("id") Long id);
}
