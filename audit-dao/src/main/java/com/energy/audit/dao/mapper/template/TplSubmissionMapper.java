package com.energy.audit.dao.mapper.template;

import com.energy.audit.model.entity.template.TplSubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper for tpl_submission
 */
@Mapper
public interface TplSubmissionMapper {

    int insert(TplSubmission submission);

    TplSubmission selectById(@Param("id") Long id);

    TplSubmission selectByIdAndEnterprise(@Param("id") Long id, @Param("enterpriseId") Long enterpriseId);

    /** Find existing submission by the unique business key */
    TplSubmission selectByEnterpriseTemplateYear(@Param("enterpriseId") Long enterpriseId,
                                                 @Param("templateId") Long templateId,
                                                 @Param("auditYear") Integer auditYear);

    /** List all submissions for an enterprise (latest first) */
    List<TplSubmission> selectListByEnterprise(@Param("enterpriseId") Long enterpriseId);

    int updateById(TplSubmission submission);
}
