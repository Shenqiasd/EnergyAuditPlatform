-- =============================================
-- Audit11.1 能源购入消费存储 — de_energy_balance 表扩展
-- 新增列: energy_name, measurement_unit, gain_loss
-- 放宽约束: energy_id NOT NULL → DEFAULT 0
-- =============================================

CALL ensure_column('de_energy_balance', 'energy_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''能源名称(冗余)'' AFTER energy_id');
CALL ensure_column('de_energy_balance', 'measurement_unit',
    'VARCHAR(32)  DEFAULT NULL COMMENT ''计量单位'' AFTER energy_name');
CALL ensure_column('de_energy_balance', 'gain_loss',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''盈亏量'' AFTER closing_stock');

-- MODIFY COLUMN is naturally idempotent — safe to re-run.
ALTER TABLE de_energy_balance MODIFY COLUMN energy_id BIGINT DEFAULT 0 COMMENT '关联能源 -> bs_energy.id';
