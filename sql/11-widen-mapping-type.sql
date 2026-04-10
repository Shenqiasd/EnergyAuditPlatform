-- ============================================================
-- 11-widen-mapping-type.sql
-- Widen tpl_tag_mapping.mapping_type from VARCHAR(16) to VARCHAR(32)
-- to accommodate EQUIPMENT_BENCHMARK (19 chars).
-- Already executed on production via direct DDL on 2026-04-10.
-- ============================================================

ALTER TABLE tpl_tag_mapping MODIFY COLUMN mapping_type VARCHAR(32) NOT NULL DEFAULT 'SCALAR';
