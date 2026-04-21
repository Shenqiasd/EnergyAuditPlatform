-- 19: Add protection_enabled column to tpl_template_version
-- Allows admins to toggle cell protection per template version (default ON)
CALL ensure_column('tpl_template_version', 'protection_enabled',
    'TINYINT DEFAULT 1 COMMENT ''是否启用单元格保护(0关闭 1开启)'' AFTER `publish_time`');
