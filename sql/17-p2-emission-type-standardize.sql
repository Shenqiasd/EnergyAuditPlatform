-- P2-10: Standardize emission_type values in de_ghg_emission
-- Migrate Chinese text labels to dictionary code values matching sys_dict_data
-- Dictionary codes: DIRECT_COMBUSTION, DIRECT_PROCESS, INDIRECT_ELECTRICITY, INDIRECT_HEAT, DIRECT_WASTE, FUGITIVE

-- Step 1: Map fully-qualified Chinese labels (unambiguous, safe to run first)
UPDATE de_ghg_emission SET emission_type = 'DIRECT_COMBUSTION'    WHERE emission_type = '化石燃料燃烧直接排放';
UPDATE de_ghg_emission SET emission_type = 'DIRECT_PROCESS'       WHERE emission_type = '工业生产过程直接排放';
UPDATE de_ghg_emission SET emission_type = 'INDIRECT_ELECTRICITY' WHERE emission_type = '净购入电力间接排放';
UPDATE de_ghg_emission SET emission_type = 'INDIRECT_HEAT'        WHERE emission_type = '净购入热力间接排放';
UPDATE de_ghg_emission SET emission_type = 'DIRECT_WASTE'         WHERE emission_type = '废弃物处理直接排放';
UPDATE de_ghg_emission SET emission_type = 'FUGITIVE'             WHERE emission_type = '逸散排放';

-- Step 2: Map ambiguous short-form '间接排放' using energy_name to disambiguate
-- Heat-related sources → INDIRECT_HEAT
UPDATE de_ghg_emission SET emission_type = 'INDIRECT_HEAT'
  WHERE emission_type = '间接排放'
    AND energy_name IN ('蒸汽', '热力', '热水', '蒸气');
-- Remaining '间接排放' rows (电力 and others) → INDIRECT_ELECTRICITY
UPDATE de_ghg_emission SET emission_type = 'INDIRECT_ELECTRICITY'
  WHERE emission_type = '间接排放';

-- Step 3: Map ambiguous short-form '直接排放' using energy_name to disambiguate
-- Default: fossil fuel combustion is the most common '直接排放' source
-- (If DIRECT_PROCESS or DIRECT_WASTE rows exist with short-form labels,
--  they would need manual review — flagged as a warning)
UPDATE de_ghg_emission SET emission_type = 'DIRECT_COMBUSTION'
  WHERE emission_type = '直接排放';

-- Ensure emission_type dictionary entries exist in sys_dict_data
-- (idempotent: only insert if not already present)
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, status, create_by)
SELECT 'emission_type', '化石燃料燃烧直接排放', 'DIRECT_COMBUSTION', 1, 1, 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'emission_type' AND dict_value = 'DIRECT_COMBUSTION');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, status, create_by)
SELECT 'emission_type', '工业生产过程直接排放', 'DIRECT_PROCESS', 2, 1, 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'emission_type' AND dict_value = 'DIRECT_PROCESS');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, status, create_by)
SELECT 'emission_type', '净购入电力间接排放', 'INDIRECT_ELECTRICITY', 3, 1, 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'emission_type' AND dict_value = 'INDIRECT_ELECTRICITY');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, status, create_by)
SELECT 'emission_type', '净购入热力间接排放', 'INDIRECT_HEAT', 4, 1, 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'emission_type' AND dict_value = 'INDIRECT_HEAT');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, status, create_by)
SELECT 'emission_type', '废弃物处理直接排放', 'DIRECT_WASTE', 5, 1, 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'emission_type' AND dict_value = 'DIRECT_WASTE');

INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, status, create_by)
SELECT 'emission_type', '逸散排放', 'FUGITIVE', 6, 1, 'system'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'emission_type' AND dict_value = 'FUGITIVE');
