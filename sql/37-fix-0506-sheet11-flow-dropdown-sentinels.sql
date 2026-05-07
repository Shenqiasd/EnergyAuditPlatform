-- Migration 37: Add virtual energy-flow endpoints to 0506 Sheet 11 dropdowns
--
-- Sheet "11.能源流程图（二维表）" stores source/target unit names in
-- de_energy_flow. The downstream EnergyFlowPostProcessor treats the literal
-- values "外购" and "产出" as virtual nodes, so the data-entry dropdowns must
-- expose them even though they are not rows in bs_unit.

SET @v_id := (
    SELECT tv.id
    FROM tpl_template_version tv
    JOIN tpl_template t ON t.id = tv.template_id
    WHERE t.template_code = 'Energy_Audit_0506'
      AND tv.published = 1
    ORDER BY tv.publish_time DESC, tv.id DESC
    LIMIT 1
);

SELECT CASE
    WHEN @v_id IS NULL THEN 'WARN: Energy_Audit_0506 published version not found - migration 37 skipped.'
    ELSE CONCAT('OK: Energy_Audit_0506 published version resolved to template_version_id=', @v_id)
END AS migration_37_status;

UPDATE tpl_tag_mapping
SET column_mappings = JSON_OBJECT(
        'mode', 'dropdown_only',
        'columns', JSON_ARRAY(
            JSON_OBJECT(
                'col', 'A',
                'field', 'name',
                'extraValues', JSON_ARRAY('外购'),
                'extraPosition', 'prepend'
            ),
            JSON_OBJECT(
                'col', 'B',
                'field', 'name',
                'extraValues', JSON_ARRAY('产出'),
                'extraPosition', 'append'
            )
        )
    ),
    remark = 'Sheet 11 source dropdown prepends 外购; target dropdown appends 产出 for energy-flow virtual nodes',
    update_time = NOW()
WHERE @v_id IS NOT NULL
  AND template_version_id = @v_id
  AND tag_name = '0506_DROPDOWN_11_UNIT'
  AND mapping_type = 'CONFIG_PREFILL';

SELECT ROW_COUNT() AS migration_37_updated_rows;
