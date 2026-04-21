-- Migration: Add missing columns to tpl_tag_mapping
-- Root cause: mapper XML references deletion_source, mapping_type, source_type,
-- row_key_column, column_mappings, header_row — but production CREATE TABLE
-- never included them. This causes submit 500 on selectListByVersionId.

-- 1. mapping_type (added in wave-4 but never migrated)
CALL ensure_column('tpl_tag_mapping', 'mapping_type',
    'VARCHAR(32) DEFAULT ''SCALAR'' COMMENT ''映射类型(SCALAR/TABLE/EQUIPMENT_BENCHMARK)'' AFTER `cell_range`');

-- 2. source_type
CALL ensure_column('tpl_tag_mapping', 'source_type',
    'VARCHAR(16) DEFAULT ''CELL_TAG'' COMMENT ''来源类型(CELL_TAG/TABLE_RANGE)'' AFTER `mapping_type`');

-- 3. row_key_column
CALL ensure_column('tpl_tag_mapping', 'row_key_column',
    'INT DEFAULT NULL COMMENT ''行键列索引(TABLE类型)'' AFTER `source_type`');

-- 4. column_mappings (JSON text storing per-column field mappings for TABLE extraction)
CALL ensure_column('tpl_tag_mapping', 'column_mappings',
    'TEXT DEFAULT NULL COMMENT ''列映射JSON(TABLE类型)'' AFTER `row_key_column`');

-- 5. header_row
CALL ensure_column('tpl_tag_mapping', 'header_row',
    'INT DEFAULT NULL COMMENT ''表头行号(TABLE类型)'' AFTER `column_mappings`');

-- 6. deletion_source — tracks whether a soft-delete was triggered by USER action or SYNC
CALL ensure_column('tpl_tag_mapping', 'deletion_source',
    'VARCHAR(16) DEFAULT NULL COMMENT ''删除来源(USER/SYNC)'' AFTER `remark`');
