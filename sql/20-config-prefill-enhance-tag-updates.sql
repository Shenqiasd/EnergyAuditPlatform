-- =============================================================
-- CONFIG_PREFILL 预配置增强 — 更新 tag mappings
-- 1. 修正 cellRange 为精确数据区域
-- 2. Sheet 15 增加 B=lowHeatValue 列 + C/D 设 dropdown:false
-- 3. Sheet 11.1 修正 cellRange 从 Row 4 开始
-- =============================================================

-- Sheet 11.1 能源购入、消费、存储 (sheetIndex=16)
-- 修正 cellRange: 数据从 Row 4 开始 (确认)，缩小到 A4:B15
UPDATE tpl_tag_mapping
SET cell_range = 'A4:B15',
    column_mappings = '{"filter":{"isActive":1},"columns":[{"col":"A","field":"name"},{"col":"B","field":"measurementUnit"}]}'
WHERE version_id = 31
  AND mapping_type = 'CONFIG_PREFILL'
  AND sheet_index = 16
  AND target_table = 'bs_energy';

-- Sheet 13 企业产品能源成本表 (sheetIndex=18)
-- 缩小 cellRange 到合理范围 B5:B20
UPDATE tpl_tag_mapping
SET cell_range = 'B5:B20'
WHERE version_id = 31
  AND mapping_type = 'CONFIG_PREFILL'
  AND sheet_index = 18
  AND target_table = 'bs_product';

-- Sheet 15 温室气体排放汇总 — 化石燃料排放量 (sheetIndex=20)
-- 修正 cellRange 到 A26:D33 (合计行 Row 34 之前)
-- 增加 B=lowHeatValue 列, C/D 设 dropdown:false
UPDATE tpl_tag_mapping
SET cell_range = 'A26:D33',
    column_mappings = '{"filter":{"isActive":1},"columns":[{"col":"A","field":"name"},{"col":"B","field":"lowHeatValue","dropdown":false},{"col":"C","field":"carbonContent","dropdown":false},{"col":"D","field":"oxidationRate","dropdown":false}]}'
WHERE version_id = 31
  AND mapping_type = 'CONFIG_PREFILL'
  AND sheet_index = 20
  AND target_table = 'bs_energy';

-- Sheet 20 碳达峰信息 (sheetIndex=25)
-- 缩小 cellRange 到 A18:A21 (产品类别区域)
UPDATE tpl_tag_mapping
SET cell_range = 'A18:A21'
WHERE version_id = 31
  AND mapping_type = 'CONFIG_PREFILL'
  AND sheet_index = 25
  AND target_table = 'bs_product';

-- Sheet 21 "十五五"期间节能目标 (sheetIndex=26)
-- 缩小 cellRange 到 A7:E9 (产品数据区，Row 10 开始是分解表)
UPDATE tpl_tag_mapping
SET cell_range = 'A7:E9'
WHERE version_id = 31
  AND mapping_type = 'CONFIG_PREFILL'
  AND sheet_index = 26
  AND target_table = 'bs_product';
