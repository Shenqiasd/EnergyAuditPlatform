-- Add `sheet_name` to tpl_tag_mapping. Idempotent: skipped if 00-schema.sql
-- (or an earlier run of this migration) already created the column.
CALL ensure_column('tpl_tag_mapping', 'sheet_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''所在Sheet名称(稳定标识,优先于sheet_index)'' AFTER `sheet_index`');
