-- H2 dev schema (MODE=MySQL compatible)
-- Column names MUST exactly match the production schema in /sql/00-schema.sql.
-- Production uses MySQL. This file is for local dev/CI only (H2 in-memory).

-- 1. sys_user  (matches SysUserMapper.xml + production sys_user)
CREATE TABLE IF NOT EXISTS sys_user (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    username         VARCHAR(64)  NOT NULL,
    password         VARCHAR(128) NOT NULL,
    real_name        VARCHAR(64),
    phone            VARCHAR(20),
    email            VARCHAR(128),
    user_type        TINYINT      NOT NULL DEFAULT 0,
    enterprise_id    BIGINT,
    status           TINYINT      NOT NULL DEFAULT 1,
    last_login_time  DATETIME,
    password_changed TINYINT      NOT NULL DEFAULT 0,
    create_by        VARCHAR(64),
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
);

-- 2. sys_role  (matches SysRoleMapper.xml + production sys_role)
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(64)  NOT NULL,
    role_key    VARCHAR(64)  NOT NULL,
    sort_order  INT          DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(256),
    create_by   VARCHAR(64),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
);

-- 3. sys_user_role  (with full audit columns matching production)
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    role_id     BIGINT      NOT NULL,
    create_by   VARCHAR(64),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- 4. ent_enterprise  (matches EntEnterpriseMapper.xml + production ent_enterprise)
CREATE TABLE IF NOT EXISTS ent_enterprise (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    enterprise_name VARCHAR(256) NOT NULL,
    credit_code     VARCHAR(64)  NOT NULL,
    contact_person  VARCHAR(64),
    contact_email   VARCHAR(128),
    contact_phone   VARCHAR(20),
    remark          VARCHAR(512),
    expire_date     DATE,
    is_locked       TINYINT      DEFAULT 0,
    is_active       TINYINT      DEFAULT 1,
    last_login_time DATETIME,
    sort_order      INT          DEFAULT 0,
    create_by       VARCHAR(64),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_credit_code (credit_code)
);

-- 5. bs_energy  (matches BsEnergyMapper.xml + production bs_energy)
CREATE TABLE IF NOT EXISTS bs_energy (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    enterprise_id    BIGINT         NOT NULL,
    name             VARCHAR(128)   NOT NULL,
    category         VARCHAR(64),
    measurement_unit VARCHAR(32),
    equivalent_value DECIMAL(18, 6),
    equal_value      DECIMAL(18, 6),
    low_heat_value   DECIMAL(18, 6),
    carbon_content   DECIMAL(18, 6),
    oxidation_rate   DECIMAL(18, 6),
    color            VARCHAR(16),
    is_active        TINYINT        DEFAULT 1,
    remark           VARCHAR(512),
    create_by        VARCHAR(64),
    create_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 6. bs_product  (matches BsProductMapper.xml + production bs_product)
CREATE TABLE IF NOT EXISTS bs_product (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    enterprise_id    BIGINT        NOT NULL,
    name             VARCHAR(128)  NOT NULL,
    measurement_unit VARCHAR(32),
    unit_price       DECIMAL(18, 4),
    remark           VARCHAR(512),
    create_by        VARCHAR(64),
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 7. bs_unit  (matches BsUnitMapper.xml + production bs_unit — process units, NOT measurement units)
CREATE TABLE IF NOT EXISTS bs_unit (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    enterprise_id BIGINT       NOT NULL,
    name          VARCHAR(128) NOT NULL,
    unit_type     TINYINT      NOT NULL,
    sub_category  VARCHAR(64),
    remark        VARCHAR(512),
    create_by     VARCHAR(64),
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64),
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 8. tpl_template  (matches TplTemplateMapper.xml + production tpl_template)
CREATE TABLE IF NOT EXISTS tpl_template (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    template_name    VARCHAR(128) NOT NULL,
    template_code    VARCHAR(64)  NOT NULL,
    module_type      VARCHAR(64),
    description      VARCHAR(512),
    status           TINYINT      DEFAULT 1,
    current_version  INT          DEFAULT 1,
    create_by        VARCHAR(64),
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code)
);

-- 9. ent_registration  (matches EntRegistrationMapper.xml + production ent_registration)
CREATE TABLE IF NOT EXISTS ent_registration (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    enterprise_name VARCHAR(256) NOT NULL,
    credit_code     VARCHAR(64)  NOT NULL,
    contact_person  VARCHAR(64),
    contact_email   VARCHAR(128),
    contact_phone   VARCHAR(20),
    apply_ip        VARCHAR(64),
    apply_no        VARCHAR(64),
    apply_time      DATETIME,
    audit_status    TINYINT      DEFAULT 0,
    audit_user_id   BIGINT,
    audit_time      DATETIME,
    audit_remark    VARCHAR(512),
    create_by       VARCHAR(64),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 10. sys_dict_type  (matches SysDictTypeMapper.xml + production sys_dict_type)
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    dict_name   VARCHAR(128) NOT NULL,
    dict_type   VARCHAR(128) NOT NULL,
    status      TINYINT      DEFAULT 1,
    remark      VARCHAR(256),
    create_by   VARCHAR(64),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
);

-- 11. sys_dict_data  (matches SysDictDataMapper.xml + production sys_dict_data)
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    dict_type   VARCHAR(128) NOT NULL,
    dict_label  VARCHAR(256) NOT NULL,
    dict_value  VARCHAR(256) NOT NULL,
    dict_sort   INT          DEFAULT 0,
    css_class   VARCHAR(128),
    status      TINYINT      DEFAULT 1,
    remark      VARCHAR(256),
    create_by   VARCHAR(64),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 12. ent_enterprise_setting  (matches EntEnterpriseSettingMapper.xml + production ent_enterprise_setting)
--     Fields align with the "1.企业概况" spreadsheet template for bidirectional sync.
CREATE TABLE IF NOT EXISTS ent_enterprise_setting (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    enterprise_id            BIGINT        NOT NULL,
    region                   VARCHAR(64),
    industry_field           VARCHAR(64),
    industry_name            VARCHAR(128),
    unit_nature              VARCHAR(64),
    registered_date          DATE,
    registered_capital       DECIMAL(18,2),
    legal_representative     VARCHAR(64),
    legal_phone              VARCHAR(20),
    is_central_enterprise    TINYINT       DEFAULT 0,
    group_name               VARCHAR(256),
    enterprise_address       VARCHAR(512),
    unit_address             VARCHAR(512),
    postal_code              VARCHAR(10),
    admin_division_code      VARCHAR(16),
    enterprise_email         VARCHAR(128),
    fax                      VARCHAR(32),
    energy_mgmt_org          VARCHAR(128),
    energy_leader_name       VARCHAR(64),
    energy_leader_phone      VARCHAR(20),
    energy_manager_name      VARCHAR(64),
    energy_manager_mobile    VARCHAR(20),
    energy_manager_cert      VARCHAR(128),
    energy_dept_leader_phone VARCHAR(20),
    energy_cert              TINYINT       DEFAULT 0,
    cert_pass_date           DATE,
    cert_authority           VARCHAR(256),
    has_energy_center        TINYINT       DEFAULT 0,
    enterprise_contact       VARCHAR(64),
    enterprise_mobile        VARCHAR(20),
    compiler_contact         VARCHAR(64),
    compiler_name            VARCHAR(256),
    compiler_mobile          VARCHAR(20),
    compiler_email           VARCHAR(128),
    industry_category        VARCHAR(64),
    industry_code            VARCHAR(32),
    superior_department      VARCHAR(256),
    energy_enterprise_type   VARCHAR(64),
    remark                   VARCHAR(512),
    create_by                VARCHAR(64),
    create_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by                VARCHAR(64),
    update_time              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enterprise_id (enterprise_id)
);

-- 13. bs_energy_catalog  (matches BsEnergyCatalogMapper.xml — global admin-managed energy reference)
CREATE TABLE IF NOT EXISTS bs_energy_catalog (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    name             VARCHAR(128)   NOT NULL,
    category         VARCHAR(64),
    measurement_unit VARCHAR(32),
    equivalent_value DECIMAL(18,6),
    equal_value      DECIMAL(18,6),
    low_heat_value   DECIMAL(18,6),
    carbon_content   DECIMAL(18,6),
    oxidation_rate   DECIMAL(18,6),
    color            VARCHAR(16),
    is_active        TINYINT        DEFAULT 1,
    sort_order       INT            DEFAULT 0,
    remark           VARCHAR(512),
    create_by        VARCHAR(64),
    create_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 14. cm_emission_factor  (matches CmEmissionFactorMapper.xml + production cm_emission_factor)
CREATE TABLE IF NOT EXISTS cm_emission_factor (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    factor_name      VARCHAR(128)   NOT NULL,
    energy_type      VARCHAR(64),
    factor_value     DECIMAL(18,8),
    measurement_unit VARCHAR(32),
    source           VARCHAR(256),
    effective_year   INT,
    status           TINYINT        DEFAULT 1,
    remark           VARCHAR(512),
    create_by        VARCHAR(64),
    create_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 15. bs_unit_energy  (matches BsUnitEnergyMapper.xml + production bs_unit_energy)
CREATE TABLE IF NOT EXISTS bs_unit_energy (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    unit_id     BIGINT      NOT NULL,
    energy_id   BIGINT      NOT NULL,
    create_by   VARCHAR(64),
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unit_energy (unit_id, energy_id)
);

-- 17. tpl_template_version  (matches TplTemplateVersionMapper.xml + production tpl_template_version)
CREATE TABLE IF NOT EXISTS tpl_template_version (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    template_id   BIGINT       NOT NULL,
    version       INT          NOT NULL,
    template_json CLOB         NOT NULL DEFAULT '{}',
    change_log    VARCHAR(512),
    published     TINYINT      DEFAULT 0,
    publish_time  DATETIME,
    create_by     VARCHAR(64),
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64),
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_version (template_id, version)
);

-- 18. tpl_tag_mapping  (matches TplTagMappingMapper.xml + production tpl_tag_mapping)
CREATE TABLE IF NOT EXISTS tpl_tag_mapping (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    template_version_id BIGINT       NOT NULL,
    tag_name            VARCHAR(128) NOT NULL,
    field_name          VARCHAR(128) NOT NULL,
    target_table        VARCHAR(128),
    data_type           VARCHAR(32),
    dict_type           VARCHAR(128),
    required            TINYINT      DEFAULT 0,
    sheet_index         INT          DEFAULT 0,
    sheet_name          VARCHAR(128),
    cell_range          VARCHAR(32),
    mapping_type        VARCHAR(16)  DEFAULT 'SCALAR',
    source_type         VARCHAR(16)  DEFAULT 'CELL_TAG',
    row_key_column      INT,
    column_mappings     CLOB,
    header_row          INT,
    remark              VARCHAR(256),
    create_by           VARCHAR(64),
    create_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64),
    update_time         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 19. tpl_submission  (matches TplSubmissionMapper.xml + production tpl_submission)
CREATE TABLE IF NOT EXISTS tpl_submission (
    id               BIGINT  NOT NULL AUTO_INCREMENT,
    enterprise_id    BIGINT  NOT NULL,
    template_id      BIGINT  NOT NULL,
    template_version INT     NOT NULL,
    audit_year       INT     NOT NULL,
    submission_json  CLOB,
    extracted_data   CLOB,
    status           TINYINT DEFAULT 0,
    submit_time      DATETIME,
    create_by        VARCHAR(64),
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 20. tpl_edit_lock  (matches TplEditLockMapper.xml + production tpl_edit_lock)
CREATE TABLE IF NOT EXISTS tpl_edit_lock (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    enterprise_id BIGINT      NOT NULL,
    template_id   BIGINT      NOT NULL,
    audit_year    INT         NOT NULL,
    lock_user_id  BIGINT      NOT NULL,
    lock_time     DATETIME    NOT NULL,
    expire_time   DATETIME    NOT NULL,
    create_by     VARCHAR(64),
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64),
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_enterprise_template_year (enterprise_id, template_id, audit_year)
);

-- 16. sys_operation_log  (matches SysOperationLogMapper.xml + production sys_operation_log)
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT,
    username         VARCHAR(64),
    operation        VARCHAR(256),
    method           VARCHAR(256),
    request_url      VARCHAR(512),
    request_params   TEXT,
    response_result  TEXT,
    ip               VARCHAR(64),
    status           TINYINT,
    error_msg        TEXT,
    operation_time   DATETIME,
    create_by        VARCHAR(64),
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ============================================================================
-- Data Extraction Tables (de_*)
-- 24 key business tables + 2 generic storage tables
-- ============================================================================

-- 21. de_company_overview (Sheet 4/12 scalar fields)
CREATE TABLE IF NOT EXISTS de_company_overview (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 22. de_tech_indicator (Sheet 4/12 economic & energy indicators)
CREATE TABLE IF NOT EXISTS de_tech_indicator (
    id                           BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id                BIGINT        NOT NULL DEFAULT 0,
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
    unit_output_energy           DECIMAL(18,6),
    unit_output_energy_equal     DECIMAL(18,6),
    saving_project_count         INT,
    saving_invest_total          DECIMAL(18,4),
    saving_capacity              DECIMAL(18,4),
    saving_benefit               DECIMAL(18,4),
    coal_target                  DECIMAL(18,4),
    coal_actual                  DECIMAL(18,4),
    employee_count               INT,
    energy_manager_count         INT,
    total_energy_equiv_excl_green DECIMAL(18,4),
    total_energy_equal_excl_green DECIMAL(18,4),
    raw_material_energy          DECIMAL(18,4),
    electrification_rate         DECIMAL(8,4),
    total_energy_equal_excl_material DECIMAL(18,4),
    create_by                    VARCHAR(64),
    create_time                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by                    VARCHAR(64),
    update_time                  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 23. de_energy_consumption (Sheet 5/8 energy purchase/consumption/stock by energy type)
CREATE TABLE IF NOT EXISTS de_energy_consumption (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    non_industrial_consumption DECIMAL(18,4),
    consumption_total      DECIMAL(18,4),
    ref_factor             DECIMAL(18,6),
    transfer_out           DECIMAL(18,4),
    gain_loss              DECIMAL(18,4),
    unit_price             DECIMAL(18,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 24. de_energy_conversion (Sheet 6 energy processing/conversion)
CREATE TABLE IF NOT EXISTS de_energy_conversion (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 25. de_product_unit_consumption (Sheet 7 product unit energy consumption)
CREATE TABLE IF NOT EXISTS de_product_unit_consumption (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    product_id             BIGINT        NOT NULL,
    year_type              VARCHAR(16)   NOT NULL,
    measurement_unit       VARCHAR(32),
    output                 DECIMAL(18,4),
    energy_consumption     DECIMAL(18,4),
    unit_consumption       DECIMAL(18,6),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 26. de_equipment_detail (Sheet 13/14 equipment with type discriminator + JSON details)
CREATE TABLE IF NOT EXISTS de_equipment_detail (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    detail_json            CLOB,
    equipment_overview     VARCHAR(512),
    obsolete_status        VARCHAR(128),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 27. de_carbon_emission (Sheet 16-20 carbon emission summary + details)
CREATE TABLE IF NOT EXISTS de_carbon_emission (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    emission_category      VARCHAR(32)   NOT NULL,
    source_name            VARCHAR(128),
    measurement_unit       VARCHAR(32),
    emission_factor        DECIMAL(18,6),
    activity_data          DECIMAL(18,4),
    co2_emission           DECIMAL(18,4),
    low_heat_value         DECIMAL(18,6),
    carbon_content         DECIMAL(18,6),
    oxidation_rate         DECIMAL(8,4),
    conversion_output      DECIMAL(18,4),
    recovery_amount        DECIMAL(18,4),
    unit_output_emission   DECIMAL(18,6),
    total_energy_consumption DECIMAL(18,4),
    unit_output_energy     DECIMAL(18,6),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 27b. de_ghg_emission (GHG emission by energy source for chart C5)
CREATE TABLE IF NOT EXISTS de_ghg_emission (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    emission_type          VARCHAR(64)   NOT NULL,
    energy_id              BIGINT,
    energy_name            VARCHAR(128),
    main_equipment         VARCHAR(256),
    measurement_unit       VARCHAR(32),
    activity_data          DECIMAL(18,4),
    annual_emission        DECIMAL(18,4),
    total_emission         DECIMAL(18,4),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 28. de_energy_balance (Sheet 11/31 energy consumption balance matrix)
CREATE TABLE IF NOT EXISTS de_energy_balance (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    energy_id              BIGINT        NOT NULL,
    opening_stock          DECIMAL(18,4),
    purchase_amount        DECIMAL(18,4),
    consumption_amount     DECIMAL(18,4),
    transfer_out_amount    DECIMAL(18,4),
    closing_stock          DECIMAL(18,4),
    measurement_unit       VARCHAR(32),
    energy_unit_price      DECIMAL(18,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 29. de_energy_flow (Sheet 21 energy flow diagram 2D table)
CREATE TABLE IF NOT EXISTS de_energy_flow (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 30. de_five_year_target (Sheet 30 five-year energy saving targets)
CREATE TABLE IF NOT EXISTS de_five_year_target (
    id                          BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id               BIGINT        NOT NULL DEFAULT 0,
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
    update_time                 DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 31. de_submission_field (generic scalar storage for tags not mapped to key tables)
CREATE TABLE IF NOT EXISTS de_submission_field (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 32. de_submission_table (generic table/array storage for tags not mapped to key tables)
CREATE TABLE IF NOT EXISTS de_submission_table (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    tag_name               VARCHAR(128)  NOT NULL,
    row_index              INT           NOT NULL,
    row_key                VARCHAR(256),
    column_values          CLOB,
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ============================================================================
-- Wave 6: Additional Data Extraction Tables (14 new de_* tables)
-- ============================================================================

-- 33. de_tech_reform_history (Sheet 2 — prior energy-saving retrofit projects)
CREATE TABLE IF NOT EXISTS de_tech_reform_history (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    project_type           VARCHAR(64),
    main_content           CLOB,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 34. de_saving_project (Sheet 3 — energy-saving projects)
CREATE TABLE IF NOT EXISTS de_saving_project (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    main_content           CLOB,
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 35. de_product_output (Sheet 4.5 — product output by year)
CREATE TABLE IF NOT EXISTS de_product_output (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 36. de_meter_instrument (Sheet 9 — energy metering instruments)
CREATE TABLE IF NOT EXISTS de_meter_instrument (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    management_no          VARCHAR(64),
    model_spec             VARCHAR(128),
    manufacturer           VARCHAR(256),
    factory_no             VARCHAR(64),
    meter_name             VARCHAR(128),
    multiplier             DECIMAL(18,4),
    grade                  VARCHAR(32),
    energy_attribute       VARCHAR(128),
    energy_id              BIGINT,
    measure_range          VARCHAR(128),
    department             VARCHAR(128),
    accuracy_grade         VARCHAR(32),
    install_location       VARCHAR(256),
    status                 VARCHAR(32),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 37. de_meter_config_rate (Sheet 10 — metering instrument configuration rate, wide-table design)
CREATE TABLE IF NOT EXISTS de_meter_config_rate (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    energy_type            VARCHAR(64),
    energy_sub_type        VARCHAR(64),
    l1_standard_rate       DECIMAL(8,4),
    l1_required_count      INT,
    l1_actual_count        INT,
    l1_actual_rate         DECIMAL(8,4),
    l2_standard_rate       DECIMAL(8,4),
    l2_required_count      INT,
    l2_actual_count        INT,
    l2_actual_rate         DECIMAL(8,4),
    l3_standard_rate       DECIMAL(8,4),
    l3_required_count      INT,
    l3_actual_count        INT,
    l3_actual_rate         DECIMAL(8,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 38. de_obsolete_equipment (Sheet 15 — obsolete equipment catalog)
CREATE TABLE IF NOT EXISTS de_obsolete_equipment (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 39. de_product_energy_cost (Sheet 23 — product energy cost)
CREATE TABLE IF NOT EXISTS de_product_energy_cost (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 40. de_saving_calculation (Sheet 24 — energy saving calculation data)
CREATE TABLE IF NOT EXISTS de_saving_calculation (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
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
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 41. de_management_policy (Sheet 25 — energy management policies)
CREATE TABLE IF NOT EXISTS de_management_policy (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    policy_name            VARCHAR(256),
    main_content           CLOB,
    supervise_dept         VARCHAR(128),
    publish_date           VARCHAR(32),
    valid_period           VARCHAR(64),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 42. de_saving_potential (Sheet 26 — energy saving potential details)
CREATE TABLE IF NOT EXISTS de_saving_potential (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    category               VARCHAR(64),
    project_name           VARCHAR(256),
    main_content           CLOB,
    saving_potential       DECIMAL(18,4),
    calc_description       CLOB,
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 43. de_management_suggestion (Sheet 27 — energy management improvement suggestions)
CREATE TABLE IF NOT EXISTS de_management_suggestion (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    main_content           CLOB,
    investment             DECIMAL(18,4),
    annual_saving          DECIMAL(18,4),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 44. de_tech_reform_suggestion (Sheet 28 — energy-saving retrofit suggestions)
CREATE TABLE IF NOT EXISTS de_tech_reform_suggestion (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    main_content           CLOB,
    investment             DECIMAL(18,4),
    annual_saving          DECIMAL(18,4),
    payback_period         DECIMAL(8,2),
    remark                 VARCHAR(512),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 45. de_rectification (Sheet 29 — energy-saving rectification measures)
CREATE TABLE IF NOT EXISTS de_rectification (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    seq_no                 INT,
    project_name           VARCHAR(256),
    measures               CLOB,
    target_date            VARCHAR(32),
    responsible_person     VARCHAR(64),
    estimated_cost         DECIMAL(18,4),
    annual_saving          DECIMAL(18,4),
    annual_benefit         DECIMAL(18,4),
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 46. de_report_text (Sheet 32 — supplementary report text)
CREATE TABLE IF NOT EXISTS de_report_text (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id          BIGINT        NOT NULL DEFAULT 0,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    section_code           VARCHAR(16),
    section_name           VARCHAR(128),
    content                CLOB,
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ============================================================================
-- Audit Workflow Tables (aw_*)
-- ============================================================================

-- 47. aw_audit_task (审核任务表)
CREATE TABLE IF NOT EXISTS aw_audit_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    enterprise_id   BIGINT       NOT NULL,
    audit_year      INT          NOT NULL,
    task_type       TINYINT      DEFAULT NULL,
    task_title      VARCHAR(256) DEFAULT NULL,
    status          TINYINT      DEFAULT 0,
    assignee_id     BIGINT       DEFAULT NULL,
    assign_time     DATETIME     DEFAULT NULL,
    deadline        DATETIME     DEFAULT NULL,
    complete_time   DATETIME     DEFAULT NULL,
    result          VARCHAR(512) DEFAULT NULL,
    create_by       VARCHAR(64),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 48a. aw_rectification_track (整改跟踪表)
CREATE TABLE IF NOT EXISTS aw_rectification_track (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    enterprise_id   BIGINT       NOT NULL,
    audit_year      INT          NOT NULL,
    item_name       VARCHAR(256) DEFAULT NULL,
    requirement     CLOB         DEFAULT NULL,
    status          TINYINT      DEFAULT 0,
    deadline        DATETIME     DEFAULT NULL,
    complete_time   DATETIME     DEFAULT NULL,
    result          CLOB         DEFAULT NULL,
    create_by       VARCHAR(64),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- 48b. aw_audit_log (审核日志表)
CREATE TABLE IF NOT EXISTS aw_audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    task_id         BIGINT       NOT NULL,
    operator_id     BIGINT       NOT NULL,
    action          VARCHAR(32)  NOT NULL,
    comment         CLOB         DEFAULT NULL,
    operation_time  DATETIME     DEFAULT NULL,
    create_by       VARCHAR(64),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ar_report (audit report records)
CREATE TABLE IF NOT EXISTS ar_report (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    enterprise_id          BIGINT        NOT NULL,
    audit_year             INT           NOT NULL,
    report_name            VARCHAR(256),
    report_type            TINYINT       NOT NULL DEFAULT 1,
    status                 TINYINT       NOT NULL DEFAULT 0,
    generated_file_path    VARCHAR(512),
    uploaded_file_path     VARCHAR(512),
    onlyoffice_doc_key     VARCHAR(128),
    generate_time          DATETIME,
    submit_time            DATETIME,
    create_by              VARCHAR(64),
    create_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by              VARCHAR(64),
    update_time            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
