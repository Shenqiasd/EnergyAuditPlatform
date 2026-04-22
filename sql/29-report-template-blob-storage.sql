-- Add columns to ar_report_template for persistent file storage.
-- Template files uploaded in Railway/Docker environments are lost on container restart.
-- Storing the file bytes as LONGBLOB in the database ensures templates survive redeployments.

ALTER TABLE ar_report_template
    ADD COLUMN template_file_data LONGBLOB COMMENT 'Template file content stored as binary (survives container restarts)' AFTER template_file_path;

ALTER TABLE ar_report_template
    ADD COLUMN original_file_name VARCHAR(255) COMMENT 'Original uploaded file name' AFTER template_file_data;

-- Relax NOT NULL on template_file_path — new uploads will have BLOB as primary storage
ALTER TABLE ar_report_template
    MODIFY COLUMN template_file_path VARCHAR(512) DEFAULT NULL;
