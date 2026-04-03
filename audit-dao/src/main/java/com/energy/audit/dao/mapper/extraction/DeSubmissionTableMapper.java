package com.energy.audit.dao.mapper.extraction;

import com.energy.audit.model.entity.extraction.DeSubmissionTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeSubmissionTableMapper {

    int batchInsert(@Param("list") List<DeSubmissionTable> list);

    List<DeSubmissionTable> selectBySubmissionId(@Param("submissionId") Long submissionId);

    int deleteBySubmissionId(@Param("submissionId") Long submissionId,
                            @Param("updateBy") String updateBy);
}
