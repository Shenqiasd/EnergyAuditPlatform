-- Migration: Add review_comment column to tpl_submission
-- Supports audit review workflow: status 2=approved, 3=rejected

ALTER TABLE `tpl_submission`
    ADD COLUMN `review_comment` VARCHAR(512) DEFAULT NULL COMMENT '审核意见(退回时填写)' AFTER `status`;

-- Update status comment for documentation
ALTER TABLE `tpl_submission`
    MODIFY COLUMN `status` TINYINT DEFAULT 0 COMMENT '状态(0草稿 1已提交 2已通过 3已退回)';
