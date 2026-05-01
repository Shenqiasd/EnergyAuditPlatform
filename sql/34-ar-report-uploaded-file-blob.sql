-- Add columns to ar_report for storing the enterprise-uploaded final report (.docx).
-- Same dual-write pattern as ar_report_template (file system + DB BLOB) so the file
-- survives Railway container restarts while staying cheap on Tencent Cloud's
-- persistent-disk VPS where the BLOB acts as an off-host backup copy.

ALTER TABLE ar_report
    ADD COLUMN uploaded_file_data LONGBLOB     NULL COMMENT '上传报告的二进制副本（容器重启回源用）'        AFTER uploaded_file_path,
    ADD COLUMN uploaded_file_size BIGINT       NULL COMMENT '上传文件字节数'                                AFTER uploaded_file_data,
    ADD COLUMN uploaded_file_name VARCHAR(255) NULL COMMENT '上传时的原始文件名'                            AFTER uploaded_file_size,
    ADD COLUMN uploaded_at        DATETIME     NULL COMMENT '上传时间'                                      AFTER uploaded_file_name;
