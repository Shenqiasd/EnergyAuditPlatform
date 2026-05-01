-- Add columns to ar_report for storing the enterprise-uploaded final report (.docx).
-- Same dual-write pattern as ar_report_template (file system + DB BLOB) so the file
-- survives Railway container restarts while staying cheap on Tencent Cloud's
-- persistent-disk VPS where the BLOB acts as an off-host backup copy.

ALTER TABLE ar_report
    ADD COLUMN uploaded_file_data LONGBLOB     NULL COMMENT '上传报告的二进制副本（容器重启回源用）'        AFTER uploaded_file_path,
    ADD COLUMN uploaded_file_size BIGINT       NULL COMMENT '上传文件字节数'                                AFTER uploaded_file_data,
    ADD COLUMN uploaded_file_name VARCHAR(255) NULL COMMENT '上传时的原始文件名'                            AFTER uploaded_file_size,
    ADD COLUMN uploaded_at        DATETIME     NULL COMMENT '上传时间'                                      AFTER uploaded_file_name;

-- Enforce only-one-row-per-(enterprise, year, type, deleted-cohort) at the DB level so
-- two concurrent uploads from the same enterprise can't both pass the application-level
-- "select then insert" check and create duplicate rows. Including `deleted` in the key
-- lets soft-deleted rows coexist with a live row for the same triple.
-- NOTE: if production already has duplicates, clean them up before this DDL or the
-- statement will fail.
ALTER TABLE ar_report
    ADD UNIQUE KEY uk_ar_report_ent_year_type
        (enterprise_id, audit_year, report_type, deleted);
