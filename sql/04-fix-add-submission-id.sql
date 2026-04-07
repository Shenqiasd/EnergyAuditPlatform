-- Fix: Add missing submission_id column to 12 pre-existing de_* tables
-- These tables were created by 00-schema.sql without submission_id,
-- and the Wave 4 migration (02-wave4-data-extraction.sql) used
-- CREATE TABLE IF NOT EXISTS which does NOT alter already-existing tables.
-- ==========================================================================

-- 1. de_company_overview
ALTER TABLE de_company_overview
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_company_overview
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 2. de_tech_indicator
ALTER TABLE de_tech_indicator
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_tech_indicator
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 3. de_product_unit_consumption
ALTER TABLE de_product_unit_consumption
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_product_unit_consumption
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 4. de_energy_balance
ALTER TABLE de_energy_balance
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_energy_balance
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 5. de_five_year_target
ALTER TABLE de_five_year_target
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_five_year_target
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 6. de_meter_instrument
ALTER TABLE de_meter_instrument
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_meter_instrument
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 7. de_meter_config_rate
ALTER TABLE de_meter_config_rate
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_meter_config_rate
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 8. de_obsolete_equipment
ALTER TABLE de_obsolete_equipment
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_obsolete_equipment
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 9. de_product_energy_cost
ALTER TABLE de_product_energy_cost
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_product_energy_cost
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 10. de_management_policy
ALTER TABLE de_management_policy
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_management_policy
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 11. de_saving_potential
ALTER TABLE de_saving_potential
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_saving_potential
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);

-- 12. de_rectification
ALTER TABLE de_rectification
    ADD COLUMN IF NOT EXISTS submission_id BIGINT NULL AFTER id;
ALTER TABLE de_rectification
    ADD INDEX IF NOT EXISTS idx_submission (submission_id);
