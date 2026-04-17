-- ============================================================================
-- 22-fix-enterprise-overview-missing-tags.sql
-- Fix: 企业概况 sheet 8个单元格缺少 CELL_TAG 导致保护后无法编辑
--
-- 缺失的单元格:
--   D2  (R2C3) 所属领域              → industry_field
--   B3  (R3C1) 单位详细名称          → enterprise_name
--   F3  (R3C5) 统一社会信用代码      → credit_code
--   F6  (R6C5) 所属集团名称          → group_name
--   B9  (R9C1) 能源管理机构名称      → energy_mgmt_org
--   F9  (R9C5) 是否建设能碳管理中心  → has_energy_center
--   F10 (R10C5) 单位主管节能领导姓名 → energy_leader_name
--   H10 (R10C7) 联系电话(节能领导)   → energy_leader_phone
--
-- 方案:
--   1. 向 tpl_tag_mapping 插入缺失的 tag 映射
--   2. 通过 JSON_SET 在 template_json 中为对应单元格添加 tag 属性
--
-- 影响版本: 31 (已发布 v6), 32 (草稿 v7)
-- ============================================================================

-- ── Step 1: 插入缺失的 tag 映射 ──────────────────────────────────────────
-- sheet_index=3 对应 "1.企业概况" (0-indexed)

-- industry_field (所属领域) — D2
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'industry_field', 'industryField', 'ent_enterprise_setting',
    'STRING', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'industry_field' AND deleted = 0);

-- enterprise_name (单位详细名称) — B3 (merged B3:D3)
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'enterprise_name', 'enterpriseName', 'ent_enterprise_setting',
    'STRING', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'enterprise_name' AND deleted = 0);

-- credit_code (统一社会信用代码) — F3 (merged F3:H3)
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'credit_code', 'creditCode', 'ent_enterprise_setting',
    'STRING', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'credit_code' AND deleted = 0);

-- group_name (所属集团名称) — F6 (merged F6:H6)
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'group_name', 'groupName', 'ent_enterprise_setting',
    'STRING', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'group_name' AND deleted = 0);

-- energy_mgmt_org (能源管理机构名称) — B9 (merged B9:D9)
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'energy_mgmt_org', 'energyMgmtOrg', 'ent_enterprise_setting',
    'STRING', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'energy_mgmt_org' AND deleted = 0);

-- has_energy_center (是否建设能碳管理中心) — F9 (merged F9:H9)
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'has_energy_center', 'hasEnergyCenter', 'ent_enterprise_setting',
    'NUMBER', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'has_energy_center' AND deleted = 0);

-- energy_leader_name (单位主管节能领导姓名) — F10
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'energy_leader_name', 'energyLeaderName', 'ent_enterprise_setting',
    'STRING', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'energy_leader_name' AND deleted = 0);

-- energy_leader_phone (节能领导联系电话) — H10
INSERT INTO tpl_tag_mapping (template_version_id, tag_name, field_name, target_table,
    data_type, required, sheet_index, sheet_name, mapping_type, source_type, create_by, update_by)
SELECT v.id, 'energy_leader_phone', 'energyLeaderPhone', 'ent_enterprise_setting',
    'STRING', 0, 3, '1.企业概况', 'SCALAR', 'CELL_TAG', 'migration', 'migration'
FROM tpl_template_version v WHERE v.id IN (31, 32)
AND NOT EXISTS (SELECT 1 FROM tpl_tag_mapping WHERE template_version_id = v.id AND tag_name = 'energy_leader_phone' AND deleted = 0);

-- ── Step 2: 在 template_json 中为这些单元格添加 cell tag ──────────────────
-- 使用 MySQL JSON_SET 注入 tag 值 (0-indexed row/col)

-- v31 (已发布)
UPDATE tpl_template_version
SET template_json = JSON_SET(
    template_json,
    '$.sheets."1.企业概况".data.dataTable."1"."3".tag',  'industry_field',
    '$.sheets."1.企业概况".data.dataTable."2"."1".tag',  'enterprise_name',
    '$.sheets."1.企业概况".data.dataTable."2"."5".tag',  'credit_code',
    '$.sheets."1.企业概况".data.dataTable."5"."5".tag',  'group_name',
    '$.sheets."1.企业概况".data.dataTable."8"."1".tag',  'energy_mgmt_org',
    '$.sheets."1.企业概况".data.dataTable."8"."5".tag',  'has_energy_center',
    '$.sheets."1.企业概况".data.dataTable."9"."5".tag',  'energy_leader_name',
    '$.sheets."1.企业概况".data.dataTable."9"."7".tag',  'energy_leader_phone'
)
WHERE id = 31;

-- v32 (草稿)
UPDATE tpl_template_version
SET template_json = JSON_SET(
    template_json,
    '$.sheets."1.企业概况".data.dataTable."1"."3".tag',  'industry_field',
    '$.sheets."1.企业概况".data.dataTable."2"."1".tag',  'enterprise_name',
    '$.sheets."1.企业概况".data.dataTable."2"."5".tag',  'credit_code',
    '$.sheets."1.企业概况".data.dataTable."5"."5".tag',  'group_name',
    '$.sheets."1.企业概况".data.dataTable."8"."1".tag',  'energy_mgmt_org',
    '$.sheets."1.企业概况".data.dataTable."8"."5".tag',  'has_energy_center',
    '$.sheets."1.企业概况".data.dataTable."9"."5".tag',  'energy_leader_name',
    '$.sheets."1.企业概况".data.dataTable."9"."7".tag',  'energy_leader_phone'
)
WHERE id = 32;
