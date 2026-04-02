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

-- 12. sys_operation_log  (matches SysOperationLogMapper.xml + production sys_operation_log)
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
