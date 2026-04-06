ALTER TABLE `tpl_tag_mapping`
    ADD COLUMN `sheet_name` VARCHAR(128) DEFAULT NULL COMMENT '所在Sheet名称(稳定标识,优先于sheet_index)'
    AFTER `sheet_index`;
