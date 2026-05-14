-- EA-CUST-043: When table 8 column C (分类) = '工艺设备', column K (设备对标情况/能效等级)
-- must be auto-set to '-'. Add derivedWhen rule to the column_mappings JSON.
--
-- This updates the '表8_设备汇总' tag mapping to include a derivedWhen rule on
-- the energy_efficiency_level column (col index 10), keyed by category (col index 2).
-- Uses string REPLACE for safety since column_mappings is TEXT, not JSON type.

UPDATE tpl_tag_mapping
SET column_mappings = REPLACE(
      column_mappings,
      '{"col":10,"field":"energy_efficiency_level","label":"设备对标情况(能效等级)","type":"STRING"}',
      '{"col":10,"field":"energy_efficiency_level","label":"设备对标情况(能效等级)","type":"STRING","derivedWhen":[{"sourceCol":2,"equals":"工艺设备","value":"-"}]}'
    )
WHERE tag_name = '表8_设备汇总'
  AND mapping_type = 'TABLE'
  AND column_mappings LIKE '%"energy_efficiency_level"%'
  AND column_mappings NOT LIKE '%derivedWhen%';
