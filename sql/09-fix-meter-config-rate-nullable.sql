-- Audit05: Allow NULL for energy_type in de_meter_config_rate
-- Merged cells in Excel mean the category (电力/固态能源/液态能源 etc.) only appears
-- in the first row of each group; subsequent rows have NULL for energy_type.
ALTER TABLE de_meter_config_rate MODIFY COLUMN energy_type VARCHAR(64) DEFAULT NULL COMMENT '能源种类';
