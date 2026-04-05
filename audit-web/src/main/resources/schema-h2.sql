CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(100),
    user_type INT DEFAULT 3,
    enterprise_id BIGINT,
    status INT DEFAULT 1,
    last_login_time TIMESTAMP,
    password_changed INT DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(64),
    role_key VARCHAR(64),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    remark VARCHAR(512),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ent_enterprise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_name VARCHAR(128),
    credit_code VARCHAR(64),
    contact_person VARCHAR(64),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(20),
    remark VARCHAR(512),
    expire_date DATE,
    is_locked INT DEFAULT 0,
    is_active INT DEFAULT 1,
    last_login_time TIMESTAMP,
    sort_order INT DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tpl_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(64),
    template_name VARCHAR(128),
    module_type VARCHAR(32),
    description VARCHAR(512),
    current_version INT DEFAULT 1,
    status INT DEFAULT 0,
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tpl_template_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT,
    version INT DEFAULT 1,
    template_json CLOB,
    change_log VARCHAR(512),
    published INT DEFAULT 0,
    publish_time TIMESTAMP,
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tpl_edit_lock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT,
    template_id BIGINT,
    audit_year INT,
    lock_user_id BIGINT,
    lock_time TIMESTAMP,
    expire_time TIMESTAMP,
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS bs_energy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT,
    name VARCHAR(128),
    category VARCHAR(64),
    measurement_unit VARCHAR(32),
    equivalent_value DECIMAL(18,6),
    equal_value DECIMAL(18,6),
    low_heat_value DECIMAL(18,6),
    carbon_content DECIMAL(18,6),
    oxidation_rate DECIMAL(18,6),
    color VARCHAR(16),
    is_active INT DEFAULT 1,
    remark VARCHAR(512),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS bs_energy_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    category VARCHAR(64),
    measurement_unit VARCHAR(32),
    equivalent_value DECIMAL(18,6),
    equal_value DECIMAL(18,6),
    low_heat_value DECIMAL(18,6),
    carbon_content DECIMAL(18,6),
    oxidation_rate DECIMAL(18,6),
    color VARCHAR(16),
    is_active INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    remark VARCHAR(512),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS bs_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT,
    name VARCHAR(128),
    unit_type INT,
    sub_category VARCHAR(64),
    remark VARCHAR(512),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS bs_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_id BIGINT,
    name VARCHAR(128),
    measurement_unit VARCHAR(32),
    unit_price DECIMAL(18,4),
    remark VARCHAR(512),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS de_energy_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT,
    enterprise_id BIGINT NOT NULL,
    audit_year INT NOT NULL,
    flow_stage VARCHAR(32),
    seq_no INT DEFAULT 0,
    source_unit VARCHAR(128),
    target_unit VARCHAR(128),
    energy_product VARCHAR(128),
    physical_quantity DECIMAL(18,4) DEFAULT 0,
    standard_quantity DECIMAL(18,4) DEFAULT 0,
    remark VARCHAR(512),
    create_by VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);
