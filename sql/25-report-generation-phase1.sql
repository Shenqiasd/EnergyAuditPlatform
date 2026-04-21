-- Phase 1: Report Generation Engine — Schema Changes
-- This migration:
--   1. Extends ar_report with new columns for template-based generation
--   2. Creates ar_report_template table for Word template management

-- 1. Extend ar_report table
ALTER TABLE ar_report ADD COLUMN report_html LONGTEXT DEFAULT NULL COMMENT 'HTML content for TinyMCE online editing' AFTER submit_time;
ALTER TABLE ar_report ADD COLUMN template_id BIGINT DEFAULT NULL COMMENT 'Report template ID -> ar_report_template.id' AFTER report_html;
ALTER TABLE ar_report ADD COLUMN submission_id BIGINT DEFAULT NULL COMMENT 'Submission ID -> tpl_submission.id' AFTER template_id;
ALTER TABLE ar_report ADD COLUMN flow_chart_path VARCHAR(512) DEFAULT NULL COMMENT 'Path to energy flow chart image (PNG)' AFTER submission_id;
ALTER TABLE ar_report ADD COLUMN review_comment VARCHAR(512) DEFAULT NULL COMMENT 'Review comment from auditor' AFTER flow_chart_path;
ALTER TABLE ar_report ADD COLUMN reviewer_id BIGINT DEFAULT NULL COMMENT 'Reviewer user ID' AFTER review_comment;

-- 2. Create ar_report_template table
CREATE TABLE IF NOT EXISTS `ar_report_template` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_name`      VARCHAR(128) NOT NULL                COMMENT '模板名称',
    `template_file_path` VARCHAR(512) NOT NULL                COMMENT '模板文件路径(.docx)',
    `version`            INT          DEFAULT 1               COMMENT '模板版本号',
    `status`             TINYINT      DEFAULT 0               COMMENT '状态(0=草稿 1=启用)',
    `create_by`          VARCHAR(64)  DEFAULT NULL             COMMENT '创建人',
    `create_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`          VARCHAR(64)  DEFAULT NULL             COMMENT '更新人',
    `update_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审计报告模板';
