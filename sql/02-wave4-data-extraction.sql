-- Wave 4: Template-driven data extraction tables
-- MySQL 8.0 migration script (run against production DB)
-- ==========================================================================

-- 1. Extend tpl_tag_mapping with new columns for SCALAR/TABLE dual mapping
CALL ensure_column('tpl_tag_mapping', 'mapping_type',
    'VARCHAR(16) DEFAULT ''SCALAR'' AFTER cell_range');
CALL ensure_column('tpl_tag_mapping', 'source_type',
    'VARCHAR(16) DEFAULT ''CELL_TAG'' AFTER mapping_type');
CALL ensure_column('tpl_tag_mapping', 'row_key_column',
    'INT NULL AFTER source_type');
CALL ensure_column('tpl_tag_mapping', 'column_mappings',
    'TEXT NULL AFTER row_key_column');
CALL ensure_column('tpl_tag_mapping', 'header_row',
    'INT NULL AFTER column_mappings');

-- 2. de_company_overview
CREATE TABLE IF NOT EXISTS de_company_overview (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    energy_leader_name     VARCHAR(64),
    energy_leader_position VARCHAR(64),
    energy_dept_name       VARCHAR(128),
    energy_dept_leader     VARCHAR(64),
    fulltime_staff_count   INT,
    parttime_staff_count   INT,
    five_year_target_value DECIMAL(18,4),
    five_year_target_name  VARCHAR(256),
    five_year_target_dept  VARCHAR(256),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. de_tech_indicator
CREATE TABLE IF NOT EXISTS de_tech_indicator (
    id                           BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id                BIGINT        NOT NULL,
    enterprise_id                BIGINT        NOT NULL,
    audit_year                   INT           NOT NULL,
    indicator_year               INT           NOT NULL,
    gross_output                 DECIMAL(18,4),
    sales_revenue                DECIMAL(18,4),
    tax_paid                     DECIMAL(18,4),
    energy_total_cost            DECIMAL(18,4),
    production_cost              DECIMAL(18,4),
    energy_cost_ratio            DECIMAL(8,4),
    total_energy_equiv           DECIMAL(18,4),
    total_energy_equal           DECIMAL(18,4),
    total_energy_excl_material   DECIMAL(18,4),
    unit_output_energy_equiv     DECIMAL(18,6),
    unit_output_energy_equal     DECIMAL(18,6),
    saving_project_count         INT,
    saving_invest_total          DECIMAL(18,4),
    saving_capacity              DECIMAL(18,4),
    saving_benefit               DECIMAL(18,4),
    coal_target                  DECIMAL(18,4),
    coal_actual                  DECIMAL(18,4),
    create_by                    VARCHAR(64),
    create_time                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by                    VARCHAR(64),
    update_time                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. de_energy_consumption
CREATE TABLE IF NOT EXISTS de_energy_consumption (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    energy_code            VARCHAR(16),
    energy_name            VARCHAR(128),
    measurement_unit       VARCHAR(32),
    opening_stock          DECIMAL(18,4),
    purchase_total         DECIMAL(18,4),
    purchase_from_province DECIMAL(18,4),
    purchase_amount        DECIMAL(18,4),
    industrial_consumption DECIMAL(18,4),
    material_consumption   DECIMAL(18,4),
    transport_consumption  DECIMAL(18,4),
    closing_stock          DECIMAL(18,4),
    external_supply        DECIMAL(18,4),
    equiv_factor           DECIMAL(18,6),
    equal_factor           DECIMAL(18,6),
    standard_coal          DECIMAL(18,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. de_energy_conversion
CREATE TABLE IF NOT EXISTS de_energy_conversion (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    energy_name            VARCHAR(128),
    measurement_unit       VARCHAR(32),
    industrial_consumption DECIMAL(18,4),
    conversion_input_total DECIMAL(18,4),
    conv_power_gen         DECIMAL(18,4),
    conv_heating           DECIMAL(18,4),
    conv_coal_washing      DECIMAL(18,4),
    conv_coking            DECIMAL(18,4),
    conv_refining          DECIMAL(18,4),
    conv_gas_making        DECIMAL(18,4),
    conv_lng               DECIMAL(18,4),
    conv_coal_product      DECIMAL(18,4),
    conversion_output      DECIMAL(18,4),
    conversion_output_std  DECIMAL(18,4),
    recovery_utilization   DECIMAL(18,4),
    equiv_factor           DECIMAL(18,6),
    equal_factor           DECIMAL(18,6),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. de_product_unit_consumption
CREATE TABLE IF NOT EXISTS de_product_unit_consumption (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    indicator_name         VARCHAR(256),
    indicator_unit         VARCHAR(64),
    numerator_unit         VARCHAR(64),
    denominator_unit       VARCHAR(64),
    conversion_factor      DECIMAL(18,6),
    current_indicator      DECIMAL(18,6),
    current_numerator      DECIMAL(18,4),
    current_denominator    DECIMAL(18,4),
    previous_indicator     DECIMAL(18,6),
    previous_numerator     DECIMAL(18,4),
    previous_denominator   DECIMAL(18,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. de_equipment_detail
CREATE TABLE IF NOT EXISTS de_equipment_detail (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    equipment_type         VARCHAR(32)   NOT NULL,
    equipment_name         VARCHAR(128),
    model                  VARCHAR(128),
    quantity               INT,
    capacity               VARCHAR(64),
    annual_runtime_hours   DECIMAL(10,2),
    annual_energy          DECIMAL(18,4),
    energy_unit            VARCHAR(32),
    energy_efficiency      VARCHAR(32),
    install_location       VARCHAR(256),
    detail_json            TEXT,
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. de_carbon_emission
CREATE TABLE IF NOT EXISTS de_carbon_emission (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    emission_category      VARCHAR(32)   NOT NULL,
    source_name            VARCHAR(128),
    measurement_unit       VARCHAR(32),
    emission_factor        DECIMAL(18,6),
    activity_data          DECIMAL(18,4),
    co2_emission           DECIMAL(18,4),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. de_energy_balance
CREATE TABLE IF NOT EXISTS de_energy_balance (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    row_label              VARCHAR(128),
    row_category           VARCHAR(64),
    energy_name            VARCHAR(128),
    energy_value           DECIMAL(18,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. de_energy_flow
CREATE TABLE IF NOT EXISTS de_energy_flow (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    flow_stage             VARCHAR(32),
    seq_no                 INT,
    source_unit            VARCHAR(128),
    target_unit            VARCHAR(128),
    energy_product         VARCHAR(128),
    physical_quantity      DECIMAL(18,4),
    standard_quantity      DECIMAL(18,4),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. de_five_year_target
CREATE TABLE IF NOT EXISTS de_five_year_target (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id               BIGINT        NOT NULL,
    enterprise_id               BIGINT        NOT NULL,
    audit_year                  INT           NOT NULL,
    section_type                VARCHAR(32)   NOT NULL,
    year_label                  VARCHAR(32),
    gross_output                DECIMAL(18,4),
    energy_equiv                DECIMAL(18,4),
    energy_equal                DECIMAL(18,4),
    unit_energy_equiv           DECIMAL(18,6),
    unit_energy_equal           DECIMAL(18,6),
    decline_rate                DECIMAL(8,4),
    product_name                VARCHAR(128),
    indicator_name              VARCHAR(128),
    indicator_value             DECIMAL(18,6),
    actual_value                DECIMAL(18,6),
    energy_control_total        DECIMAL(18,4),
    product_unit_consumption    DECIMAL(18,6),
    saving_amount               DECIMAL(18,4),
    create_by                   VARCHAR(64),
    create_time                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by                   VARCHAR(64),
    update_time                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. de_submission_field (generic scalar storage)
CREATE TABLE IF NOT EXISTS de_submission_field (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    tag_name               VARCHAR(128)  NOT NULL,
    field_name             VARCHAR(128),
    value_text             VARCHAR(4000),
    value_number           DECIMAL(18,6),
    value_date             VARCHAR(32),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. de_submission_table (generic table/array storage)
CREATE TABLE IF NOT EXISTS de_submission_table (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    tag_name               VARCHAR(128)  NOT NULL,
    row_index              INT           NOT NULL,
    row_key                VARCHAR(256),
    column_values          TEXT,
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year_tag (enterprise_id, audit_year, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
