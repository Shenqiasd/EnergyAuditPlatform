package com.energy.audit.dao.mapper.report;

import com.energy.audit.model.entity.report.ArReportTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArReportTemplateMapper {

    int insert(ArReportTemplate template);

    int update(ArReportTemplate template);

    ArReportTemplate selectById(@Param("id") Long id);

    List<ArReportTemplate> selectAll();

    /** Get the currently active template (status=1), most recent first */
    ArReportTemplate selectActive();

    /** Soft-delete a template by ID */
    int softDelete(@Param("id") Long id, @Param("updateBy") String updateBy);
}
