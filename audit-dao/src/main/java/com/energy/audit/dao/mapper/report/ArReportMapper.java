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

    /**
     * Fetch only the {@code uploaded_file_data} BLOB column for a report.
     * Used for the upload-download path so callers don't pull the BLOB on every selectById.
     * Returns {@code null} if the row exists but has no uploaded data, or if no row matches.
     */
    byte[] selectUploadedFileBytesById(@Param("id") Long id);

    /**
     * Targeted update for the self-heal path in {@code downloadUploadedReportBytes}.
     * Only touches {@code uploaded_file_path} so it can't accidentally clobber other columns
     * (notably {@code review_comment}, which the general-purpose {@code update} sets unconditionally).
     */
    int updateUploadedFilePathById(@Param("id") Long id,
                                   @Param("path") String path,
                                   @Param("updateBy") String updateBy);

    List<ArReport> selectByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                      @Param("auditYear") Integer auditYear);

    ArReport selectByEnterpriseAndYear(@Param("enterpriseId") Long enterpriseId,
                                       @Param("auditYear") Integer auditYear,
                                       @Param("reportType") Integer reportType);

    /**
     * List reports pending review (status=4) or all reports for auditor/admin.
     * If status is null, returns all non-deleted reports.
     */
    List<ArReport> selectByStatus(@Param("status") Integer status,
                                   @Param("auditYear") Integer auditYear);
}
