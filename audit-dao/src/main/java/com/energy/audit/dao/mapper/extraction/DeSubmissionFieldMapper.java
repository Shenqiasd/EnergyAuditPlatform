package com.energy.audit.dao.mapper.extraction;

import com.energy.audit.model.entity.extraction.DeSubmissionField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeSubmissionFieldMapper {

    int batchInsert(@Param("list") List<DeSubmissionField> list);

    List<DeSubmissionField> selectBySubmissionId(@Param("submissionId") Long submissionId);

    int deleteBySubmissionId(@Param("submissionId") Long submissionId,
                            @Param("updateBy") String updateBy);
}
