-- =============================================================================
-- Migration 27: 能源流程图重构 v2 · PR #2 后端骨架
--
-- 1) de_energy_flow: 加入源/目的单元外键列（可空，保持与旧字面值列冗余共存，
--    便于渐进迁移；后续查询和聚合逐步改用 *_unit_id）。
-- 2) de_energy_balance: 放宽 NOT NULL，允许 Sheet 11.1 被废弃后由后端从
--    de_energy_flow 自动派生 purchase_amount / consumption_amount 两列，
--    其余列（opening_stock / closing_stock / 盈亏 / 单价等）在 v2 方案 X
--    下不再维护，保留结构不删列但允许 NULL。
--
-- 设计要点:
--   - 所有变更通过 ensure_column / ensure_index 幂等包装，可重复执行。
--   - 新列均允许 NULL，便于旧数据无缝兼容。
--   - 不建立 FOREIGN KEY 物理约束，避免跨 shard / 跨环境 bs_unit 主键漂移导致的
--     写入失败；仅通过索引加速 JOIN/UPDATE。
-- =============================================================================

-- 1. de_energy_flow 增加 source_unit_id / target_unit_id
CALL ensure_column('de_energy_flow', 'source_unit_id',
    'BIGINT NULL COMMENT ''源单元ID -> bs_unit.id (可空; 字符串 source_unit 仍保留做回退)'' AFTER source_unit');
CALL ensure_column('de_energy_flow', 'target_unit_id',
    'BIGINT NULL COMMENT ''目的单元ID -> bs_unit.id (可空; 字符串 target_unit 仍保留做回退)'' AFTER target_unit');

CALL ensure_index('de_energy_flow', 'idx_source_unit_id', '(source_unit_id)');
CALL ensure_index('de_energy_flow', 'idx_target_unit_id', '(target_unit_id)');

-- 2. de_energy_balance 兼容 v2 方案 X (彻底舍弃 11.1)
--    老部署 de_energy_balance 的某些列可能是 NOT NULL，这里统一放宽为 NULL,
--    让"仅派生 purchase_amount / consumption_amount"这种写入路径能成功。
--    MODIFY COLUMN 本身幂等。
ALTER TABLE de_energy_balance MODIFY COLUMN row_label          VARCHAR(128)  DEFAULT NULL COMMENT '行标签(旧字段;v2 已弃用)';
ALTER TABLE de_energy_balance MODIFY COLUMN row_category       VARCHAR(64)   DEFAULT NULL COMMENT '行分类(旧字段;v2 已弃用)';
ALTER TABLE de_energy_balance MODIFY COLUMN energy_value       DECIMAL(18,4) DEFAULT NULL COMMENT '能源值(旧字段;v2 已弃用)';

-- 确保 v2 派生所需列存在（幂等）：不同环境历史迁移路径不一，部分线上库可能缺
-- 其中一两个；这里全部显式 ensure。
CALL ensure_column('de_energy_balance', 'energy_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''能源名称'' AFTER audit_year');
CALL ensure_column('de_energy_balance', 'measurement_unit',
    'VARCHAR(32)  DEFAULT NULL COMMENT ''计量单位'' AFTER energy_name');
CALL ensure_column('de_energy_balance', 'purchase_amount',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''购入量(v2: 从 de_energy_flow 聚合派生)'' AFTER measurement_unit');
CALL ensure_column('de_energy_balance', 'consumption_amount',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''消耗量(v2: 从 de_energy_flow 聚合派生)'' AFTER purchase_amount');
