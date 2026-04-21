-- ============================================================================
-- Wave 6 patch: add project_type column to de_tech_reform_history
-- Excel column C "项目类型" was missing from the original schema.
-- ============================================================================

CALL ensure_column('de_tech_reform_history', 'project_type',
    'VARCHAR(64) AFTER project_name');
