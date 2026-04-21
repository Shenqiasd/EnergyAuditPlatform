-- Tech indicator schema expansion: add missing columns
-- MySQL 8.0 migration script (run against production DB)
-- ==========================================================================

-- Add 4 new columns to de_tech_indicator:
--   1. unit_output_energy_equal  — 单位产值综合能耗(等价值) (H2 already has it, MySQL missing)
--   2. raw_material_energy       — 原材料用能(吨标煤)
--   3. electrification_rate      — 电气化率(%)
--   4. total_energy_equal_excl_material — 综合能耗扣除原料-等价值(吨标煤)

CALL ensure_column('de_tech_indicator', 'unit_output_energy_equal',
    'DECIMAL(18,6) NULL AFTER unit_output_energy');
CALL ensure_column('de_tech_indicator', 'raw_material_energy',
    'DECIMAL(18,4) NULL AFTER total_energy_equal_excl_green');
CALL ensure_column('de_tech_indicator', 'electrification_rate',
    'DECIMAL(8,4)  NULL AFTER raw_material_energy');
CALL ensure_column('de_tech_indicator', 'total_energy_equal_excl_material',
    'DECIMAL(18,4) NULL AFTER electrification_rate');
