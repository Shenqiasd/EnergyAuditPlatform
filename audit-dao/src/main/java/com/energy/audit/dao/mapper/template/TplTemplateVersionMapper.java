package com.energy.audit.dao.mapper.template;

import com.energy.audit.model.entity.template.TplTemplateVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper for tpl_template_version
 */
@Mapper
public interface TplTemplateVersionMapper {

    int insert(TplTemplateVersion version);

    TplTemplateVersion selectById(@Param("id") Long id);

    /** All versions of a template ordered by version DESC */
    List<TplTemplateVersion> selectListByTemplateId(@Param("templateId") Long templateId);

    /** Latest published version for a template */
    TplTemplateVersion selectPublishedByTemplateId(@Param("templateId") Long templateId);

    /** Return current max version number for a template (null when no versions exist) */
    Integer selectMaxVersion(@Param("templateId") Long templateId);

    int updateById(TplTemplateVersion version);

    /** Soft-delete */
    int deleteById(@Param("id") Long id, @Param("updateBy") String updateBy);

    /** Set published=1 and publish_time for a specific version row */
    int updatePublishStatus(@Param("id") Long id,
                            @Param("updateBy") String updateBy);
}
