-- Wave 6: Schema expansion — full 32-Sheet field coverage
-- MySQL 8.0 migration script (run against production DB)
-- ==========================================================================

-- =====================================================
-- Part 1: Extend existing tables with new columns
-- =====================================================

-- 1a. de_tech_indicator (+4 columns)
CALL ensure_column('de_tech_indicator', 'employee_count',
    'INT           NULL AFTER coal_actual');
CALL ensure_column('de_tech_indicator', 'energy_manager_count',
    'INT           NULL AFTER employee_count');
CALL ensure_column('de_tech_indicator', 'total_energy_equiv_excl_green',
    'DECIMAL(18,4) NULL AFTER energy_manager_count');
CALL ensure_column('de_tech_indicator', 'total_energy_equal_excl_green',
    'DECIMAL(18,4) NULL AFTER total_energy_equiv_excl_green');

-- 1b. de_energy_consumption (+6 columns)
CALL ensure_column('de_energy_consumption', 'non_industrial_consumption',
    'DECIMAL(18,4) NULL AFTER standard_coal');
CALL ensure_column('de_energy_consumption', 'consumption_total',
    'DECIMAL(18,4) NULL AFTER non_industrial_consumption');
CALL ensure_column('de_energy_consumption', 'ref_factor',
    'DECIMAL(18,6) NULL AFTER consumption_total');
CALL ensure_column('de_energy_consumption', 'transfer_out',
    'DECIMAL(18,4) NULL AFTER ref_factor');
CALL ensure_column('de_energy_consumption', 'gain_loss',
    'DECIMAL(18,4) NULL AFTER transfer_out');
CALL ensure_column('de_energy_consumption', 'unit_price',
    'DECIMAL(18,4) NULL AFTER gain_loss');

-- 1c. de_equipment_detail (+2 columns)
CALL ensure_column('de_equipment_detail', 'equipment_overview',
    'VARCHAR(512)  NULL AFTER detail_json');
CALL ensure_column('de_equipment_detail', 'obsolete_status',
    'VARCHAR(128)  NULL AFTER equipment_overview');

-- 1d. de_carbon_emission (+8 columns)
CALL ensure_column('de_carbon_emission', 'low_heat_value',
    'DECIMAL(18,6) NULL AFTER co2_emission');
CALL ensure_column('de_carbon_emission', 'carbon_content',
    'DECIMAL(18,6) NULL AFTER low_heat_value');
CALL ensure_column('de_carbon_emission', 'oxidation_rate',
    'DECIMAL(8,4)  NULL AFTER carbon_content');
CALL ensure_column('de_carbon_emission', 'conversion_output',
    'DECIMAL(18,4) NULL AFTER oxidation_rate');
CALL ensure_column('de_carbon_emission', 'recovery_amount',
    'DECIMAL(18,4) NULL AFTER conversion_output');
CALL ensure_column('de_carbon_emission', 'unit_output_emission',
    'DECIMAL(18,6) NULL AFTER recovery_amount');
CALL ensure_column('de_carbon_emission', 'total_energy_consumption',
    'DECIMAL(18,4) NULL AFTER unit_output_emission');
CALL ensure_column('de_carbon_emission', 'unit_output_energy',
    'DECIMAL(18,6) NULL AFTER total_energy_consumption');

-- 1e. de_energy_balance (+2 columns)
CALL ensure_column('de_energy_balance', 'measurement_unit',
    'VARCHAR(32) NULL AFTER energy_value');
CALL ensure_column('de_energy_balance', 'row_seq',
    'INT         NULL AFTER measurement_unit');

-- 1f. ent_enterprise_setting (+7 columns)
CALL ensure_column('ent_enterprise_setting', 'is_central_enterprise',
    'TINYINT      DEFAULT 0  AFTER energy_enterprise_type');
CALL ensure_column('ent_enterprise_setting', 'group_name',
    'VARCHAR(256) NULL       AFTER is_central_enterprise');
CALL ensure_column('ent_enterprise_setting', 'admin_division_code',
    'VARCHAR(16)  NULL       AFTER group_name');
CALL ensure_column('ent_enterprise_setting', 'energy_leader_phone',
    'VARCHAR(20)  NULL       AFTER admin_division_code');
CALL ensure_column('ent_enterprise_setting', 'energy_dept_leader_phone',
    'VARCHAR(20)  NULL       AFTER energy_leader_phone');
CALL ensure_column('ent_enterprise_setting', 'energy_manager_cert',
    'VARCHAR(128) NULL       AFTER energy_dept_leader_phone');
CALL ensure_column('ent_enterprise_setting', 'has_energy_center',
    'TINYINT      DEFAULT 0  AFTER energy_manager_cert');

-- =====================================================
-- Part 2: Create 14 new de_* tables
-- =====================================================

-- 2a. de_tech_reform_history (Sheet 2)
CREATE TABLE IF NOT EXISTS de_tech_reform_history (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    main_content           TEXT,
    investment             DECIMAL(18,4),
    designed_saving        DECIMAL(18,4),
    payback_period         DECIMAL(8,2),
    completion_date        VARCHAR(32),
    actual_saving          DECIMAL(18,4),
    is_contract_energy     VARCHAR(8),
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

-- 2b. de_saving_project (Sheet 3)
CREATE TABLE IF NOT EXISTS de_saving_project (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    project_type           VARCHAR(64),
    project_name           VARCHAR(256),
    impl_status            VARCHAR(32),
    impl_date              VARCHAR(32),
    investment             DECIMAL(18,4),
    saving_amount          DECIMAL(18,4),
    carbon_reduction       DECIMAL(18,4),
    is_contract_energy     VARCHAR(8),
    approval_dept          VARCHAR(128),
    main_content           TEXT,
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

-- 2c. de_product_output (Sheet 4.5)
CREATE TABLE IF NOT EXISTS de_product_output (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    product_name           VARCHAR(128),
    annual_capacity        DECIMAL(18,4),
    capacity_unit          VARCHAR(32),
    annual_output          DECIMAL(18,4),
    output_unit            VARCHAR(32),
    unit_consumption       DECIMAL(18,6),
    consumption_unit       VARCHAR(32),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2d. de_meter_instrument (Sheet 9)
CREATE TABLE IF NOT EXISTS de_meter_instrument (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    management_no          VARCHAR(64),
    model_spec             VARCHAR(128),
    manufacturer           VARCHAR(128),
    serial_no              VARCHAR(64),
    meter_name             VARCHAR(128),
    multiplier             DECIMAL(10,4),
    accuracy_class         VARCHAR(32),
    energy_type            VARCHAR(64),
    measurement_range      VARCHAR(128),
    department             VARCHAR(128),
    accuracy_grade         VARCHAR(32),
    install_location       VARCHAR(256),
    status                 VARCHAR(32),
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

-- 2e. de_meter_config_rate (Sheet 10)
CREATE TABLE IF NOT EXISTS de_meter_config_rate (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    energy_type            VARCHAR(64),
    config_level           VARCHAR(32),
    standard_rate          DECIMAL(8,4),
    required_count         INT,
    actual_count           INT,
    actual_rate            DECIMAL(8,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2f. de_obsolete_equipment (Sheet 15)
CREATE TABLE IF NOT EXISTS de_obsolete_equipment (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    equipment_name         VARCHAR(128),
    model_spec             VARCHAR(128),
    quantity               INT,
    start_use_date         VARCHAR(32),
    planned_retire_date    VARCHAR(32),
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

-- 2g. de_product_energy_cost (Sheet 23)
CREATE TABLE IF NOT EXISTS de_product_energy_cost (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    product_name           VARCHAR(128),
    energy_cost            DECIMAL(18,4),
    production_cost        DECIMAL(18,4),
    cost_ratio             DECIMAL(8,4),
    energy_total_ratio     DECIMAL(8,4),
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

-- 2h. de_saving_calculation (Sheet 24)
CREATE TABLE IF NOT EXISTS de_saving_calculation (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    energy_equal_current   DECIMAL(18,4),
    energy_equiv_current   DECIMAL(18,4),
    gross_output_current   DECIMAL(18,4),
    product_output_current DECIMAL(18,4),
    product_unit_current   VARCHAR(32),
    energy_equal_base      DECIMAL(18,4),
    energy_equiv_base      DECIMAL(18,4),
    gross_output_base      DECIMAL(18,4),
    product_output_base    DECIMAL(18,4),
    product_unit_base      VARCHAR(32),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2i. de_management_policy (Sheet 25)
CREATE TABLE IF NOT EXISTS de_management_policy (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    policy_name            VARCHAR(256),
    main_content           TEXT,
    supervise_dept         VARCHAR(128),
    publish_date           VARCHAR(32),
    valid_period           VARCHAR(64),
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

-- 2j. de_saving_potential (Sheet 26)
CREATE TABLE IF NOT EXISTS de_saving_potential (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    category               VARCHAR(64),
    project_name           VARCHAR(256),
    main_content           TEXT,
    saving_potential       DECIMAL(18,4),
    calc_description       TEXT,
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

-- 2k. de_management_suggestion (Sheet 27)
CREATE TABLE IF NOT EXISTS de_management_suggestion (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    main_content           TEXT,
    investment             DECIMAL(18,4),
    annual_saving          DECIMAL(18,4),
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

-- 2l. de_tech_reform_suggestion (Sheet 28)
CREATE TABLE IF NOT EXISTS de_tech_reform_suggestion (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    main_content           TEXT,
    investment             DECIMAL(18,4),
    annual_saving          DECIMAL(18,4),
    payback_period         DECIMAL(8,2),
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

-- 2m. de_rectification (Sheet 29)
CREATE TABLE IF NOT EXISTS de_rectification (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    measures               TEXT,
    target_date            VARCHAR(32),
    responsible_person     VARCHAR(64),
    estimated_cost         DECIMAL(18,4),
    annual_saving          DECIMAL(18,4),
    annual_benefit         DECIMAL(18,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2n. de_report_text (Sheet 32)
CREATE TABLE IF NOT EXISTS de_report_text (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    section_code           VARCHAR(16),
    section_name           VARCHAR(128),
    content                TEXT,
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
