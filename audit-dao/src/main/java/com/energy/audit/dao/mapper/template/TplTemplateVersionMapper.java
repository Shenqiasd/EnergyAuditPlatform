package com.energy.audit.dao.mapper.template;

import com.energy.audit.model.entity.template.TplTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TplTemplateVersionMapper {

    TplTemplateVersion selectById(@Param("id") Long id);

    List<TplTemplateVersion> selectByTemplateId(@Param("templateId") Long templateId);

    List<TplTemplateVersion> selectListByTemplateId(@Param("templateId") Long templateId);

    TplTemplateVersion selectLatestByTemplateId(@Param("templateId") Long templateId);

    int insert(TplTemplateVersion version);

    int updateById(TplTemplateVersion version);

    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);
}
