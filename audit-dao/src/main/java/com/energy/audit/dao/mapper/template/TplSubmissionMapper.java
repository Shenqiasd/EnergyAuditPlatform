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

    /** Reset a submitted record back to draft: status=0, submit_time=NULL, extracted_data=NULL */
    int resetToDraft(@Param("id") Long id, @Param("updateBy") String updateBy);

    /** Batch update status for all submissions of an enterprise in a given year */
    int batchUpdateStatusByEnterpriseAndYear(@Param("enterpriseId") Long enterpriseId,
                                              @Param("auditYear") Integer auditYear,
                                              @Param("oldStatus") Integer oldStatus,
                                              @Param("newStatus") Integer newStatus,
                                              @Param("reviewComment") String reviewComment,
                                              @Param("updateBy") String updateBy);
}
