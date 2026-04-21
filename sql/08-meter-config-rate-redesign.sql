-- Audit05: Redesign de_meter_config_rate as wide table (方案D)
-- Each energy type = 1 row, 3 levels × 4 indicators = 12 numeric columns
-- Replaces the old normalized design (level_type TINYINT, 1 row per level)
--
-- Idempotent: ensure_column/DROP ... IF EXISTS so reruns are safe.

-- Step 1: Clear old data (always safe)
TRUNCATE TABLE de_meter_config_rate;

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
