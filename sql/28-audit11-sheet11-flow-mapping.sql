-- =============================================================================
-- Migration 28: 能源流程图重构 v2 · PR #3 · 模板配置
--
-- 1) Sheet 11 "能源流程图" 表格新增 TABLE mapping → de_energy_flow
--    (此前仓库里没有 Sheet 11 的 tpl_tag_mapping，流程图数据之所以进不了
--     de_energy_flow 是因为抽取端没有挂接 — 本迁移修复该缺口。)
--
-- 2) Sheet 11 "源单元" / "目的单元" 两列加 CONFIG_PREFILL dropdown_only，
--    源列注入 bs_unit.name + 虚拟 "外购" 选项；
--    目的列注入 bs_unit.name + 虚拟 "产出" 选项。
--    (外购/产出 通过 column_mappings 的 extraValues 字段带入，见 SpreadSheet/index.vue。)
--
-- 3) Sheet 11 "能源品种" 列加 CONFIG_PREFILL dropdown_only 源自 bs_energy.name。
--
-- 4) Sheet 11.1 "能源购入消费储存" 原 TABLE → de_energy_balance 映射软删除；
--    v2 方案 X：de_energy_balance 由 EnergyFlowPostProcessor 从 de_energy_flow
--    聚合派生，不再由 11.1 直接填报。11.1 Sheet 的 CONFIG_PREFILL 保留不动，
--    模板层面如何处理该 Sheet 由管理员在模板编辑器决定（隐藏 / 只读均可）。
--
-- 幂等策略：全部 INSERT ... SELECT WHERE NOT EXISTS；UPDATE 使用精确条件。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Sheet 11 "能源流程图" TABLE mapping → de_energy_flow
--
-- 模板假定列布局（起始 A5，A 列 = 序号）:
--   A: 序号         → seq_no          (int)
--   B: 环节         → flow_stage      (string; 中文标签，EnergyFlowPostProcessor 转英文枚举)
--   C: 源单元       → source_unit     (string)
--   D: 目的单元     → target_unit     (string)
--   E: 能源品种     → energy_product  (string)
--   F: 实物量       → physical_quantity (decimal)
--   G: 单位         → (无对应列，抽取时忽略；单位信息随 bs_energy.measurementUnit 带出)
--   H: 折标量       → standard_quantity (decimal)
--   I: 备注         → remark          (string)
--
-- 适用模板版本：所有未删除、未发布的 Audit11 模板版本（以 sheet_name 匹配）。
-- 如果模板是已发布的（published=1），管理员需要创建新 draft 版本再挂接。
-- ---------------------------------------------------------------------------
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
    15,                      -- Sheet 11 在 Audit11 模板中通常索引为 15 (0-based, cover=0)
    '11.能源流程图',
    'A5:I34',                -- 30 行数据区，按需可扩大
    'TABLE',
    'TABLE_RANGE',
    NULL,
    0,                       -- row_key_column: A 列序号做行键
    '[{"col":0,"field":"seqNo","type":"int"},{"col":1,"field":"flowStage","type":"string"},{"col":2,"field":"sourceUnit","type":"string"},{"col":3,"field":"targetUnit","type":"string"},{"col":4,"field":"energyProduct","type":"string"},{"col":5,"field":"physicalQuantity","type":"decimal"},{"col":7,"field":"standardQuantity","type":"decimal"},{"col":8,"field":"remark","type":"string"}]',
    'string',
    0,
    'migration-28',
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

-- ---------------------------------------------------------------------------
-- 2a. Sheet 11 源单元下拉 (C 列) — bs_unit + "外购"
-- ---------------------------------------------------------------------------
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
    '11.能源流程图',
    'C5:C34',
    'CONFIG_PREFILL',
    'CELL_TAG',
    '{"mode":"dropdown_only","filter":{"deleted":0},"columns":[{"col":"C","field":"name","extraValues":["外购"],"extraPosition":"prepend"}]}',
    'string',
    0,
    'migration-28',
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

-- ---------------------------------------------------------------------------
-- 2b. Sheet 11 目的单元下拉 (D 列) — bs_unit + "产出"
-- ---------------------------------------------------------------------------
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
    '11.能源流程图',
    'D5:D34',
    'CONFIG_PREFILL',
    'CELL_TAG',
    '{"mode":"dropdown_only","filter":{"deleted":0},"columns":[{"col":"D","field":"name","extraValues":["产出"],"extraPosition":"append"}]}',
    'string',
    0,
    'migration-28',
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

-- ---------------------------------------------------------------------------
-- 2c. Sheet 11 能源品种下拉 (E 列) — bs_energy
-- ---------------------------------------------------------------------------
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
    '11.能源流程图',
    'E5:E34',
    'CONFIG_PREFILL',
    'CELL_TAG',
    '{"mode":"dropdown_only","filter":{"isActive":1},"columns":[{"col":"E","field":"name"}]}',
    'string',
    0,
    'migration-28',
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

-- ---------------------------------------------------------------------------
-- 2d. Sheet 11 环节下拉 (B 列) — 固定 4 选项
--     environment 直接用 extraValues 注入 4 个中文标签，不走任何表。
--     dropdown_only 模式 + 空 filter + bs_unit 作 source（实际不用），
--     仅利用 extraValues 注入。
-- ---------------------------------------------------------------------------
INSERT INTO tpl_tag_mapping (
    template_version_id, tag_name, field_name, target_table,
    sheet_index, sheet_name, cell_range,
    mapping_type, source_type,
    column_mappings, data_type, required, create_by, create_time, deleted
)
SELECT
    v.id,
    'sheet11_flow_stage_dropdown',
    'flow_stage_dropdown',
    'bs_unit',
    15,
    '11.能源流程图',
    'B5:B34',
    'CONFIG_PREFILL',
    'CELL_TAG',
    '{"mode":"dropdown_only","filter":{"id":-1},"columns":[{"col":"B","field":"name","extraValues":["购入储存","加工转换","分配输送","终端使用"],"extraPosition":"prepend"}]}',
    'string',
    0,
    'migration-28',
    NOW(),
    0
FROM tpl_template_version v JOIN tpl_template tpl ON tpl.id = v.template_id AND tpl.deleted = 0
WHERE v.deleted = 0
  AND tpl.template_name LIKE '%能碳审计%'
  AND NOT EXISTS (
      SELECT 1 FROM tpl_tag_mapping t
      WHERE t.template_version_id = v.id
        AND t.tag_name = 'sheet11_flow_stage_dropdown'
        AND t.deleted = 0
  );

-- ---------------------------------------------------------------------------
-- 3. 软删除 Sheet 11.1 → de_energy_balance 的 TABLE 映射
--    (v2 方案 X: de_energy_balance 由 EnergyFlowPostProcessor 派生，不再填报)
--
--    只处理 mapping_type = 'TABLE' 的那条，CONFIG_PREFILL 保留（模板 Sheet 还在，
--    下拉继续生效；模板 Sheet 可否隐藏由管理员在模板编辑器定）。
-- ---------------------------------------------------------------------------
UPDATE tpl_tag_mapping
SET deleted = 1,
    update_by = 'migration-28',
    update_time = NOW()
WHERE target_table = 'de_energy_balance'
  AND mapping_type = 'TABLE'
  AND deleted = 0;
