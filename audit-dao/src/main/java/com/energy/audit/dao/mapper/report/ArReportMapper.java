package com.energy.audit.dao.mapper.report;

import com.energy.audit.model.entity.report.ArReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArReportMapper {

    int insert(ArReport report);

    int update(ArReport report);

    ArReport selectById(@Param("id") Long id);

    List<ArReport> selectByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                      @Param("auditYear") Integer auditYear);

    ArReport selectByEnterpriseAndYear(@Param("enterpriseId") Long enterpriseId,
                                       @Param("auditYear") Integer auditYear,
                                       @Param("reportType") Integer reportType);
}
