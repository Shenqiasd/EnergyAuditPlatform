-- =============================================================================
-- Migration 23: Fix CONFIG_PREFILL tags for Sheet 11.1 and Sheet 15
-- 
-- Sheet 11.1 (energy balance): B column should be linkedTo A (auto-fill unit from energy name)
-- Sheet 15 (GHG emission):     B/C/D columns should be linkedTo A (auto-fill lowHeatValue/carbonContent/oxidationRate)
--
-- Also ensures row count matches enterprise energy count (records.length, not maxRows)
-- and dropdown excludes values already used (linkedTo columns have no dropdown)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Sheet 11.1: Update columnMappings to add linkedTo on B column
-- Old: {"filter":{"isActive":1},"columns":[{"col":"A","field":"name"},{"col":"B","field":"measurementUnit"}]}
-- New: B column has linkedTo.masterCol=A, linkedTo.lookupField=name, dropdown=false
-- ---------------------------------------------------------------------------
UPDATE tpl_tag_mapping
SET column_mappings = JSON_OBJECT(
    'filter', JSON_OBJECT('isActive', 1),
    'columns', JSON_ARRAY(
        JSON_OBJECT('col', 'A', 'field', 'name'),
        JSON_OBJECT('col', 'B', 'field', 'measurementUnit', 'dropdown', false,
                    'linkedTo', JSON_OBJECT('masterCol', 'A', 'lookupField', 'name'))
    )
),
update_time = NOW()
WHERE mapping_type = 'CONFIG_PREFILL'
  AND target_table = 'bs_energy'
  AND tag_name LIKE '%11.1%'
  AND deleted = 0;

-- ---------------------------------------------------------------------------
-- Sheet 15: Update columnMappings to add linkedTo on B/C/D columns  
-- Old: {"filter":{"isActive":1},"columns":[{"col":"A","field":"name"},{"col":"B","field":"lowHeatValue","dropdown":false},{"col":"C","field":"carbonContent","dropdown":false},{"col":"D","field":"oxidationRate","dropdown":false}]}
-- New: B/C/D columns have linkedTo.masterCol=A, linkedTo.lookupField=name
-- ---------------------------------------------------------------------------
UPDATE tpl_tag_mapping
SET column_mappings = JSON_OBJECT(
    'filter', JSON_OBJECT('isActive', 1),
    'columns', JSON_ARRAY(
        JSON_OBJECT('col', 'A', 'field', 'name'),
        JSON_OBJECT('col', 'B', 'field', 'lowHeatValue', 'dropdown', false,
                    'linkedTo', JSON_OBJECT('masterCol', 'A', 'lookupField', 'name')),
        JSON_OBJECT('col', 'C', 'field', 'carbonContent', 'dropdown', false,
                    'linkedTo', JSON_OBJECT('masterCol', 'A', 'lookupField', 'name')),
        JSON_OBJECT('col', 'D', 'field', 'oxidationRate', 'dropdown', false,
                    'linkedTo', JSON_OBJECT('masterCol', 'A', 'lookupField', 'name'))
    )
),
update_time = NOW()
WHERE mapping_type = 'CONFIG_PREFILL'
  AND target_table = 'bs_energy'
  AND tag_name LIKE '%15%'
  AND deleted = 0;
