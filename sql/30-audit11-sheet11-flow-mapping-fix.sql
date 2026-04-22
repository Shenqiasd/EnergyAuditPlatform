-- =============================================================================
-- Migration 30: 修正 sql/28 首次执行时的 Sheet 11 布局错误假设
--
-- 背景：sql/28 第一版按 9 列 A5:I34 布局写入（含 seq_no / flow_stage 两列），
-- 但生产 v31 模板的 Sheet 11 "11.能流图（二维表）" 实际为 6 列 A3:F12，无序号、
-- 无环节列。首版 sql/28 已在 Railway 等环境执行，插入了 5 条列位错误的 mapping
-- (sheet11_energy_flow_table / source_unit_dropdown / target_unit_dropdown /
-- energy_product_dropdown / flow_stage_dropdown)。直接跑修正后的 sql/28 因
-- WHERE NOT EXISTS 命中旧行会被跳过，必须先软删旧行。
--
-- 本迁移：
--   1. 软删旧的 5 个 sheet11_* mapping（以 cell_range 模式精确定位）
--   2. 软删 sheet11_flow_stage_dropdown（新布局不再需要）
--   3. 重新执行 sql/28 的 INSERT 逻辑，插入修正版 4 条 mapping
--      (TABLE + 3 个下拉；无 flow_stage_dropdown)
--
-- 幂等：软删带 cell_range 条件限定到旧布局；INSERT 用 WHERE NOT EXISTS 保障
-- 多次执行安全。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. 软删旧 5 条 mapping（按 cell_range 精确定位，避免误删修正后行）
-- ---------------------------------------------------------------------------
UPDATE tpl_tag_mapping
SET deleted = 1, update_by = 'migration-30', update_time = NOW()
WHERE deleted = 0
  AND tag_name IN (
      'sheet11_energy_flow_table',
      'sheet11_source_unit_dropdown',
      'sheet11_target_unit_dropdown',
      'sheet11_energy_product_dropdown',
      'sheet11_flow_stage_dropdown'
  )
  AND cell_range IN (
      'A5:I34',   -- TABLE 旧范围
      'B5:B34',   -- flow_stage_dropdown 旧范围
      'C5:C34',   -- source_unit_dropdown 旧范围（错列）
      'D5:D34',   -- target_unit_dropdown 旧范围（错列）
      'E5:E34'    -- energy_product_dropdown 旧范围（错列）
  );

-- ---------------------------------------------------------------------------
-- 2. 重新执行 sql/28 的 4 条 INSERT（幂等：旧行已软删，WHERE NOT EXISTS 通过）
-- ---------------------------------------------------------------------------

-- 2.1 sheet11_energy_flow_table → de_energy_flow (A3:F12)
INSERT INTO tpl_tag_mapping (
    template_version_id, tag_name, field_name, target_table,
    sheet_index, sheet_name, cell_range,
    mapping_type, source_type, header_row, row_key_column,
    column_mappings, data_type, required, create_by, create_time, deleted
)
SELECT
    v.id,
    'sheet11_energy_flow_table',
    'energy_flow_rows',
    'de_energy_flow',
    15,
    '11.能流图（二维表）',
    'A3:F12',
    'TABLE',
    'TABLE_RANGE',
    NULL,
    0,
    '[{"col":0,"field":"sourceUnit","type":"string"},{"col":1,"field":"targetUnit","type":"string"},{"col":2,"field":"energyProduct","type":"string"},{"col":3,"field":"physicalQuantity","type":"decimal"},{"col":4,"field":"standardQuantity","type":"decimal"},{"col":5,"field":"remark","type":"string"}]',
    'string',
    0,
    'migration-30',
    NOW(),
    0
FROM tpl_template_version v JOIN tpl_template tpl ON tpl.id = v.template_id AND tpl.deleted = 0
WHERE v.deleted = 0
  AND tpl.template_name LIKE '%能碳审计%'
  AND NOT EXISTS (
      SELECT 1 FROM tpl_tag_mapping t
      WHERE t.template_version_id = v.id
        AND t.tag_name = 'sheet11_energy_flow_table'
        AND t.deleted = 0
  );

-- 2.2 sheet11_source_unit_dropdown → bs_unit (A3:A12) + 外购 prepend
INSERT INTO tpl_tag_mapping (
    template_version_id, tag_name, field_name, target_table,
    sheet_index, sheet_name, cell_range,
    mapping_type, source_type,
    column_mappings, data_type, required, create_by, create_time, deleted
)
SELECT
    v.id,
    'sheet11_source_unit_dropdown',
    'source_unit_dropdown',
    'bs_unit',
    15,
    '11.能流图（二维表）',
    'A3:A12',
    'CONFIG_PREFILL',
    'CELL_TAG',
    '{"mode":"dropdown_only","filter":{"deleted":0},"columns":[{"col":"A","field":"name","extraValues":["外购"],"extraPosition":"prepend"}]}',
    'string',
    0,
    'migration-30',
    NOW(),
    0
FROM tpl_template_version v JOIN tpl_template tpl ON tpl.id = v.template_id AND tpl.deleted = 0
WHERE v.deleted = 0
  AND tpl.template_name LIKE '%能碳审计%'
  AND NOT EXISTS (
      SELECT 1 FROM tpl_tag_mapping t
      WHERE t.template_version_id = v.id
        AND t.tag_name = 'sheet11_source_unit_dropdown'
        AND t.deleted = 0
  );

-- 2.3 sheet11_target_unit_dropdown → bs_unit (B3:B12) + 产出 append
INSERT INTO tpl_tag_mapping (
    template_version_id, tag_name, field_name, target_table,
    sheet_index, sheet_name, cell_range,
    mapping_type, source_type,
    column_mappings, data_type, required, create_by, create_time, deleted
)
SELECT
    v.id,
    'sheet11_target_unit_dropdown',
    'target_unit_dropdown',
    'bs_unit',
    15,
    '11.能流图（二维表）',
    'B3:B12',
    'CONFIG_PREFILL',
    'CELL_TAG',
    '{"mode":"dropdown_only","filter":{"deleted":0},"columns":[{"col":"B","field":"name","extraValues":["产出"],"extraPosition":"append"}]}',
    'string',
    0,
    'migration-30',
    NOW(),
    0
FROM tpl_template_version v JOIN tpl_template tpl ON tpl.id = v.template_id AND tpl.deleted = 0
WHERE v.deleted = 0
  AND tpl.template_name LIKE '%能碳审计%'
  AND NOT EXISTS (
      SELECT 1 FROM tpl_tag_mapping t
      WHERE t.template_version_id = v.id
        AND t.tag_name = 'sheet11_target_unit_dropdown'
        AND t.deleted = 0
  );

-- 2.4 sheet11_energy_product_dropdown → bs_energy (C3:C12)
INSERT INTO tpl_tag_mapping (
    template_version_id, tag_name, field_name, target_table,
    sheet_index, sheet_name, cell_range,
    mapping_type, source_type,
    column_mappings, data_type, required, create_by, create_time, deleted
)
SELECT
    v.id,
    'sheet11_energy_product_dropdown',
    'energy_product_dropdown',
    'bs_energy',
    15,
    '11.能流图（二维表）',
    'C3:C12',
    'CONFIG_PREFILL',
    'CELL_TAG',
    '{"mode":"dropdown_only","filter":{"isActive":1},"columns":[{"col":"C","field":"name"}]}',
    'string',
    0,
    'migration-30',
    NOW(),
    0
FROM tpl_template_version v JOIN tpl_template tpl ON tpl.id = v.template_id AND tpl.deleted = 0
WHERE v.deleted = 0
  AND tpl.template_name LIKE '%能碳审计%'
  AND NOT EXISTS (
      SELECT 1 FROM tpl_tag_mapping t
      WHERE t.template_version_id = v.id
        AND t.tag_name = 'sheet11_energy_product_dropdown'
        AND t.deleted = 0
  );
