-- Audit05: Redesign de_meter_config_rate as wide table (方案D)
-- Each energy type = 1 row, 3 levels × 4 indicators = 12 numeric columns
-- Replaces the old normalized design (level_type TINYINT, 1 row per level)

-- Step 1: Clear old data
TRUNCATE TABLE de_meter_config_rate;

-- Step 2: Add submission_id (was missing in production)
ALTER TABLE de_meter_config_rate
  ADD COLUMN submission_id BIGINT DEFAULT NULL COMMENT '关联填报数据 -> tpl_submission.id' AFTER id;

-- Step 3: Add energy_sub_type for sub-categories (煤炭/焦炭/原油 etc.)
ALTER TABLE de_meter_config_rate
  ADD COLUMN energy_sub_type VARCHAR(64) DEFAULT NULL COMMENT '能源子类(煤炭/焦炭/原油等)' AFTER energy_type;

-- Step 4: Drop old normalized columns
ALTER TABLE de_meter_config_rate
  DROP COLUMN level_type,
  DROP COLUMN required_count,
  DROP COLUMN actual_count;

-- Step 5: Add wide-table columns (3 levels × 4 indicators)
ALTER TABLE de_meter_config_rate
  ADD COLUMN l1_standard_rate  DECIMAL(8,4) DEFAULT NULL COMMENT '进出用能单位-配备率标准%',
  ADD COLUMN l1_required_count INT          DEFAULT NULL COMMENT '进出用能单位-需要配置数',
  ADD COLUMN l1_actual_count   INT          DEFAULT NULL COMMENT '进出用能单位-实际配置数',
  ADD COLUMN l1_actual_rate    DECIMAL(8,4) DEFAULT NULL COMMENT '进出用能单位-配备率%',
  ADD COLUMN l2_standard_rate  DECIMAL(8,4) DEFAULT NULL COMMENT '进出主要次级用能单位-配备率标准%',
  ADD COLUMN l2_required_count INT          DEFAULT NULL COMMENT '进出主要次级用能单位-需要配置数',
  ADD COLUMN l2_actual_count   INT          DEFAULT NULL COMMENT '进出主要次级用能单位-实际配置数',
  ADD COLUMN l2_actual_rate    DECIMAL(8,4) DEFAULT NULL COMMENT '进出主要次级用能单位-配备率%',
  ADD COLUMN l3_standard_rate  DECIMAL(8,4) DEFAULT NULL COMMENT '主要用能设备-配备率标准%',
  ADD COLUMN l3_required_count INT          DEFAULT NULL COMMENT '主要用能设备-需要配置数',
  ADD COLUMN l3_actual_count   INT          DEFAULT NULL COMMENT '主要用能设备-实际配置数',
  ADD COLUMN l3_actual_rate    DECIMAL(8,4) DEFAULT NULL COMMENT '主要用能设备-配备率%';
