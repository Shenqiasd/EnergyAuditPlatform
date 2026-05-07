-- =============================================================================
-- Migration 34: Fix Energy_Audit_0428 v1 enterprise prefill / scalar targets
--
-- Patch production environments where sql/33 has already been executed:
-- 1) Sheet 3 C5 should sync the energy leader title from enterprise settings.
-- 2) 0428 scalar mappings should use explicit CELL_RANGE coordinates instead
--    of duplicate CELL_TAG names that can resolve to the wrong sheet.
-- =============================================================================

SET @v_id = (
    SELECT v.id
    FROM tpl_template_version v
    JOIN tpl_template t ON t.id = v.template_id
    WHERE t.template_code = 'Energy_Audit_0428'
      AND v.version = 1
      AND v.deleted = 0
      AND t.deleted = 0
    ORDER BY v.id
    LIMIT 1
);

SELECT CASE
    WHEN @v_id IS NULL THEN 'WARN: Energy_Audit_0428 v1 not found — migration 34 skipped.'
    ELSE CONCAT('OK: Energy_Audit_0428 v1 resolved to template_version_id=', @v_id)
  END AS migration_34_preflight;

UPDATE tpl_tag_mapping
SET tag_name = 'ENERGY_LEADER_TITLE',
    field_name = 'energyLeaderTitle',
    remark = 'C5:单位主管节能领导职务',
    update_by = 'migration-34',
    update_time = NOW()
WHERE template_version_id = @v_id
  AND deleted = 0
  AND sheet_name = '3.主技指'
  AND cell_range = 'C5'
  AND target_table = 'ent_enterprise_setting';

UPDATE tpl_tag_mapping
SET source_type = 'CELL_RANGE',
    cell_range = CASE tag_name
        WHEN 'S14_ENERGY_EQUAL_CURRENT' THEN 'B3'
        WHEN 'S14_ENERGY_EQUAL_BASE' THEN 'C3'
        WHEN 'S14_ENERGY_EQUIV_CURRENT' THEN 'B4'
        WHEN 'S14_ENERGY_EQUIV_BASE' THEN 'C4'
        WHEN 'S14_GROSS_OUTPUT_CURRENT' THEN 'B5'
        WHEN 'S14_GROSS_OUTPUT_BASE' THEN 'C5'
        WHEN 'S14_PRODUCT_OUTPUT_CURRENT' THEN 'B6'
        WHEN 'S14_PRODUCT_OUTPUT_BASE' THEN 'C6'
        WHEN 'HEAT_EMISSION' THEN 'E39'
        WHEN 'ELEC_EMISSION' THEN 'E40'
        WHEN 'GREEN_ELEC_OFFSET' THEN 'E41'
        WHEN 'TOTAL_EMISSION' THEN 'B14'
        WHEN 'DIRECT_EMISSION' THEN 'C3'
        WHEN 'INDIRECT_EMISSION' THEN 'C7'
        WHEN 'PEAK_YEAR' THEN 'C3'
        WHEN 'PEAK_EMISSION' THEN 'C4'
        WHEN 'ACT_2025_OUTPUT' THEN 'A4'
        WHEN 'ACT_2025_ENERGY_EQUIV' THEN 'C4'
        WHEN 'ACT_2025_ENERGY_CURR' THEN 'C5'
        WHEN 'TGT_2030_OUTPUT' THEN 'E4'
        WHEN 'TGT_2030_ENERGY_EQUIV' THEN 'G4'
        WHEN 'TGT_2030_ENERGY_CURR' THEN 'G5'
        ELSE cell_range
    END,
    update_by = 'migration-34',
    update_time = NOW()
WHERE template_version_id = @v_id
  AND deleted = 0
  AND mapping_type = 'SCALAR'
  AND tag_name IN (
      'S14_ENERGY_EQUAL_CURRENT',
      'S14_ENERGY_EQUAL_BASE',
      'S14_ENERGY_EQUIV_CURRENT',
      'S14_ENERGY_EQUIV_BASE',
      'S14_GROSS_OUTPUT_CURRENT',
      'S14_GROSS_OUTPUT_BASE',
      'S14_PRODUCT_OUTPUT_CURRENT',
      'S14_PRODUCT_OUTPUT_BASE',
      'HEAT_EMISSION',
      'ELEC_EMISSION',
      'GREEN_ELEC_OFFSET',
      'TOTAL_EMISSION',
      'DIRECT_EMISSION',
      'INDIRECT_EMISSION',
      'PEAK_YEAR',
      'PEAK_EMISSION',
      'ACT_2025_OUTPUT',
      'ACT_2025_ENERGY_EQUIV',
      'ACT_2025_ENERGY_CURR',
      'TGT_2030_OUTPUT',
      'TGT_2030_ENERGY_EQUIV',
      'TGT_2030_ENERGY_CURR'
  );

SELECT tag_name, field_name, target_table, sheet_index, sheet_name, cell_range, source_type
FROM tpl_tag_mapping
WHERE template_version_id = @v_id
  AND deleted = 0
  AND sheet_name IN ('2.企概', '3.主技指')
  AND target_table = 'ent_enterprise_setting'
ORDER BY sheet_index, cell_range, tag_name;

SELECT COUNT(*) AS remaining_scalar_cell_tag_mappings
FROM tpl_tag_mapping
WHERE template_version_id = @v_id
  AND deleted = 0
  AND mapping_type = 'SCALAR'
  AND source_type = 'CELL_TAG';
