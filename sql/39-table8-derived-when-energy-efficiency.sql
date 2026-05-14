-- EA-CUST-043: When table 8 column C (分类) = '工艺设备', column K (设备对标情况/能效等级)
-- must be auto-set to '-'. Add derivedWhen rule to the column_mappings JSON.
--
-- Locates table 8 tags by stable condition: target_table = 'de_equipment_summary'.
-- Handles all three live JSON formats for the energy_efficiency_level column entry:
--
--   Latest (0506_表8, A3:L102, no spaces, col 10):
--     {"col":10,"field":"energy_efficiency_level","type":"STRING"}
--     sourceCol = category at col 2
--
--   Mid-gen (A3:M102, spaces, col 11, has process_device_type at col 3):
--     {"col": 11, "field": "energy_efficiency_level", "type": "STRING"}
--     sourceCol = category at col 2
--
--   Oldest (A2:K200, spaces, col 9, no seq_no):
--     {"col": 9, "field": "energy_efficiency_level", "type": "STRING"}
--     sourceCol = category at col 1
--
-- Idempotent: each statement skips rows that already contain derivedWhen.

-- Format 1: latest compact JSON (col 10, category at col 2)
UPDATE tpl_tag_mapping
SET column_mappings = REPLACE(
      column_mappings,
      '{"col":10,"field":"energy_efficiency_level","type":"STRING"}',
      '{"col":10,"field":"energy_efficiency_level","type":"STRING","derivedWhen":[{"sourceCol":2,"equals":"工艺设备","value":"-"}]}'
    )
WHERE target_table = 'de_equipment_summary'
  AND mapping_type = 'TABLE'
  AND column_mappings LIKE '%"energy_efficiency_level"%'
  AND column_mappings NOT LIKE '%derivedWhen%'
  AND column_mappings LIKE '%{"col":10,"field":"energy_efficiency_level","type":"STRING"}%';

-- Format 2: mid-gen spaced JSON (col 11, category at col 2)
UPDATE tpl_tag_mapping
SET column_mappings = REPLACE(
      column_mappings,
      '{"col": 11, "field": "energy_efficiency_level", "type": "STRING"}',
      '{"col": 11, "field": "energy_efficiency_level", "type": "STRING", "derivedWhen": [{"sourceCol": 2, "equals": "工艺设备", "value": "-"}]}'
    )
WHERE target_table = 'de_equipment_summary'
  AND mapping_type = 'TABLE'
  AND column_mappings LIKE '%"energy_efficiency_level"%'
  AND column_mappings NOT LIKE '%derivedWhen%'
  AND column_mappings LIKE '%{"col": 11, "field": "energy_efficiency_level", "type": "STRING"}%';

-- Format 3: oldest spaced JSON (col 9, category at col 1)
UPDATE tpl_tag_mapping
SET column_mappings = REPLACE(
      column_mappings,
      '{"col": 9, "field": "energy_efficiency_level", "type": "STRING"}',
      '{"col": 9, "field": "energy_efficiency_level", "type": "STRING", "derivedWhen": [{"sourceCol": 1, "equals": "工艺设备", "value": "-"}]}'
    )
WHERE target_table = 'de_equipment_summary'
  AND mapping_type = 'TABLE'
  AND column_mappings LIKE '%"energy_efficiency_level"%'
  AND column_mappings NOT LIKE '%derivedWhen%'
  AND column_mappings LIKE '%{"col": 9, "field": "energy_efficiency_level", "type": "STRING"}%';

-- Format 4: seed SQL with label (col 10, category at col 2)
UPDATE tpl_tag_mapping
SET column_mappings = REPLACE(
      column_mappings,
      '{"col":10,"field":"energy_efficiency_level","label":"设备对标情况(能效等级)","type":"STRING"}',
      '{"col":10,"field":"energy_efficiency_level","label":"设备对标情况(能效等级)","type":"STRING","derivedWhen":[{"sourceCol":2,"equals":"工艺设备","value":"-"}]}'
    )
WHERE target_table = 'de_equipment_summary'
  AND mapping_type = 'TABLE'
  AND column_mappings LIKE '%"energy_efficiency_level"%'
  AND column_mappings NOT LIKE '%derivedWhen%'
  AND column_mappings LIKE '%"label":"设备对标情况(能效等级)"%';
