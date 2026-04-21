-- =============================================================================
-- Migration helpers (MySQL 8.0 compatible)
--
-- These stored procedures provide idempotent DDL primitives. MySQL 8.0 does
-- not support MariaDB's `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` / `ADD
-- INDEX IF NOT EXISTS` syntax. Any migration that wants to be re-runnable
-- against an already-partially-upgraded schema must go through these
-- procedures.
--
-- Sort order: runs after `00-schema.sql` / `01-init-data.sql` and before any
-- numbered migration (`02-*`, `03-*`, ...).
-- =============================================================================

DROP PROCEDURE IF EXISTS ensure_column;
DROP PROCEDURE IF EXISTS ensure_index;
DROP PROCEDURE IF EXISTS drop_column_if_exists;

DELIMITER $$

-- Add a column to `p_table` if and only if it does not already exist. The
-- `p_def` argument is the full column definition _minus_ the name, e.g.:
--   CALL ensure_column('my_table', 'new_col', 'VARCHAR(32) DEFAULT NULL COMMENT ''...'' AFTER other_col');
--
-- Note: MySQL's PREPARE/EXECUTE requires a session-local user variable for
-- the SQL text; `@sql_ec` is used here to avoid colliding with callers.
CREATE PROCEDURE ensure_column(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_def    TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND COLUMN_NAME  = p_column
    ) THEN
        SET @sql_ec = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_def);
        PREPARE stmt_ec FROM @sql_ec;
        EXECUTE stmt_ec;
        DEALLOCATE PREPARE stmt_ec;
    END IF;
END$$

-- Add a non-unique index on `p_table` if it does not already exist. Accepts
-- the index definition (column list + any options) as `p_cols`, e.g.:
--   CALL ensure_index('my_table', 'idx_foo', '(foo_id)');
--   CALL ensure_index('my_table', 'idx_foo_bar', '(foo_id, bar_id)');
CREATE PROCEDURE ensure_index(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND INDEX_NAME   = p_index
    ) THEN
        SET @sql_ei = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` ', p_cols);
        PREPARE stmt_ei FROM @sql_ei;
        EXECUTE stmt_ei;
        DEALLOCATE PREPARE stmt_ei;
    END IF;
END$$

-- Drop a column if it exists. Useful for schema-redesign migrations that
-- need to remove columns from an older normalized design without failing
-- on a fresh bootstrap where those columns have already been dropped.
CREATE PROCEDURE drop_column_if_exists(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND COLUMN_NAME  = p_column
    ) THEN
        SET @sql_dc = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt_dc FROM @sql_dc;
        EXECUTE stmt_dc;
        DEALLOCATE PREPARE stmt_dc;
    END IF;
END$$

DELIMITER ;
