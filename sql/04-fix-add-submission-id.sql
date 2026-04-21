-- Fix: Add missing submission_id column to 12 pre-existing de_* tables
-- These tables were created by 00-schema.sql without submission_id,
-- and the Wave 4 migration (02-wave4-data-extraction.sql) used
-- CREATE TABLE IF NOT EXISTS which does NOT alter already-existing tables.
-- ==========================================================================

-- 1. de_company_overview
CALL ensure_column('de_company_overview', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_company_overview', 'idx_submission',
    '(submission_id)');

-- 2. de_tech_indicator
CALL ensure_column('de_tech_indicator', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_tech_indicator', 'idx_submission',
    '(submission_id)');

-- 3. de_product_unit_consumption
CALL ensure_column('de_product_unit_consumption', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_product_unit_consumption', 'idx_submission',
    '(submission_id)');

-- 4. de_energy_balance
CALL ensure_column('de_energy_balance', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_energy_balance', 'idx_submission',
    '(submission_id)');

-- 5. de_five_year_target
CALL ensure_column('de_five_year_target', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_five_year_target', 'idx_submission',
    '(submission_id)');

-- 6. de_meter_instrument
CALL ensure_column('de_meter_instrument', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_meter_instrument', 'idx_submission',
    '(submission_id)');

-- 7. de_meter_config_rate
CALL ensure_column('de_meter_config_rate', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_meter_config_rate', 'idx_submission',
    '(submission_id)');

-- 8. de_obsolete_equipment
CALL ensure_column('de_obsolete_equipment', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_obsolete_equipment', 'idx_submission',
    '(submission_id)');

-- 9. de_product_energy_cost
CALL ensure_column('de_product_energy_cost', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_product_energy_cost', 'idx_submission',
    '(submission_id)');

-- 10. de_management_policy
CALL ensure_column('de_management_policy', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_management_policy', 'idx_submission',
    '(submission_id)');

-- 11. de_saving_potential
CALL ensure_column('de_saving_potential', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_saving_potential', 'idx_submission',
    '(submission_id)');

-- 12. de_rectification
CALL ensure_column('de_rectification', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_rectification', 'idx_submission',
    '(submission_id)');
