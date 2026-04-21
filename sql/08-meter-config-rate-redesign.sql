-- Audit05: Redesign de_meter_config_rate as wide table (方案D)
-- Each energy type = 1 row, 3 levels × 4 indicators = 12 numeric columns
-- Replaces the old normalized design (level_type TINYINT, 1 row per level)
--
-- Idempotent: safe to re-run. TRUNCATE only fires when the old normalized
-- schema is still present (i.e. the redesign has not yet been applied), so
-- a second run against a DB that has already been migrated preserves data.

-- Step 1: Clear stale rows only when the old schema is still in place. The
-- old normalized rows (keyed by level_type) are meaningless under the new
-- wide-table layout, so wiping them on the first run is intentional. On
-- subsequent runs, `level_type` no longer exists (dropped in Step 4 below),
-- so this block is a no-op and real production data is preserved.
SET @has_level_type := (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'de_meter_config_rate'
      AND COLUMN_NAME  = 'level_type'
);
SET @trunc_sql := IF(@has_level_type = 1,
    'TRUNCATE TABLE de_meter_config_rate',
    'DO 0'); -- no-op when redesign has already been applied
PREPARE stmt_trunc FROM @trunc_sql;
EXECUTE stmt_trunc;
DEALLOCATE PREPARE stmt_trunc;

-- Step 2: Add submission_id (may already exist from 04-fix-add-submission-id.sql)
CALL ensure_column('de_meter_config_rate', 'submission_id',
    'BIGINT DEFAULT NULL COMMENT ''关联填报数据 -> tpl_submission.id'' AFTER id');

-- Step 3: Add energy_sub_type for sub-categories (煤炭/焦炭/原油 etc.)
CALL ensure_column('de_meter_config_rate', 'energy_sub_type',
    'VARCHAR(64) DEFAULT NULL COMMENT ''能源子类(煤炭/焦炭/原油等)'' AFTER energy_type');

-- Step 4: Drop old normalized columns
CALL drop_column_if_exists('de_meter_config_rate', 'level_type');
CALL drop_column_if_exists('de_meter_config_rate', 'required_count');
CALL drop_column_if_exists('de_meter_config_rate', 'actual_count');

-- Step 5: Add wide-table columns (3 levels × 4 indicators) — each idempotent
CALL ensure_column('de_meter_config_rate', 'l1_standard_rate',  'DECIMAL(8,4) DEFAULT NULL COMMENT ''进出用能单位-配备率标准%''');
CALL ensure_column('de_meter_config_rate', 'l1_required_count', 'INT          DEFAULT NULL COMMENT ''进出用能单位-需要配置数''');
CALL ensure_column('de_meter_config_rate', 'l1_actual_count',   'INT          DEFAULT NULL COMMENT ''进出用能单位-实际配置数''');
CALL ensure_column('de_meter_config_rate', 'l1_actual_rate',    'DECIMAL(8,4) DEFAULT NULL COMMENT ''进出用能单位-配备率%''');
CALL ensure_column('de_meter_config_rate', 'l2_standard_rate',  'DECIMAL(8,4) DEFAULT NULL COMMENT ''进出主要次级用能单位-配备率标准%''');
CALL ensure_column('de_meter_config_rate', 'l2_required_count', 'INT          DEFAULT NULL COMMENT ''进出主要次级用能单位-需要配置数''');
CALL ensure_column('de_meter_config_rate', 'l2_actual_count',   'INT          DEFAULT NULL COMMENT ''进出主要次级用能单位-实际配置数''');
CALL ensure_column('de_meter_config_rate', 'l2_actual_rate',    'DECIMAL(8,4) DEFAULT NULL COMMENT ''进出主要次级用能单位-配备率%''');
CALL ensure_column('de_meter_config_rate', 'l3_standard_rate',  'DECIMAL(8,4) DEFAULT NULL COMMENT ''主要用能设备-配备率标准%''');
CALL ensure_column('de_meter_config_rate', 'l3_required_count', 'INT          DEFAULT NULL COMMENT ''主要用能设备-需要配置数''');
CALL ensure_column('de_meter_config_rate', 'l3_actual_count',   'INT          DEFAULT NULL COMMENT ''主要用能设备-实际配置数''');
CALL ensure_column('de_meter_config_rate', 'l3_actual_rate',    'DECIMAL(8,4) DEFAULT NULL COMMENT ''主要用能设备-配备率%''');
