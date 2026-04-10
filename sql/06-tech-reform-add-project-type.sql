-- ============================================================================
-- Wave 6 patch: add project_type column to de_tech_reform_history
-- Excel column C "项目类型" was missing from the original schema.
-- ============================================================================

ALTER TABLE de_tech_reform_history
    ADD COLUMN project_type VARCHAR(64) AFTER project_name;
