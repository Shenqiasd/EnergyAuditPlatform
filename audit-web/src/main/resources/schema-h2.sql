-- H2 dev schema (MODE=MySQL compatible)
-- Columns and names MUST match the MyBatis mapper XMLs exactly.
-- Production uses MySQL with the full 55-table schema in /sql/

-- System user table  (matches SysUserMapper.xml)
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

-- System role table  (matches SysRoleMapper.xml)
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(128) NOT NULL,
    role_key    VARCHAR(64)  NOT NULL,
    order_num   INT          DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(255),
    create_by   VARCHAR(64),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_key (role_key)
);

-- User-role mapping
CREATE TABLE IF NOT EXISTS sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- Enterprise table  (matches EntEnterpriseMapper.xml)
CREATE TABLE IF NOT EXISTS ent_enterprise (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255) NOT NULL,
    credit_code     VARCHAR(64),
    industry_type   VARCHAR(32),
    province        VARCHAR(64),
    city            VARCHAR(64),
    district        VARCHAR(64),
    address         VARCHAR(512),
    contact_person  VARCHAR(64),
    contact_phone   VARCHAR(20),
    contact_email   VARCHAR(128),
    status          TINYINT      NOT NULL DEFAULT 1,
    remark          VARCHAR(512),
    create_by       VARCHAR(64),
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- Energy type base data  (matches BsEnergyMapper.xml)
CREATE TABLE IF NOT EXISTS bs_energy (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    energy_code       VARCHAR(32)    NOT NULL,
    energy_name       VARCHAR(128)   NOT NULL,
    category          VARCHAR(32),
    unit              VARCHAR(32),
    conversion_factor DECIMAL(18, 6) DEFAULT 1.0,
    carbon_factor     DECIMAL(18, 6) DEFAULT 0.0,
    order_num         INT            DEFAULT 0,
    status            TINYINT        NOT NULL DEFAULT 1,
    create_by         VARCHAR(64),
    create_time       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64),
    update_time       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_energy_code (energy_code)
);

-- Product base data  (matches BsProductMapper.xml)
CREATE TABLE IF NOT EXISTS bs_product (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    product_code VARCHAR(64)  NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    category     VARCHAR(32),
    unit         VARCHAR(32),
    order_num    INT          DEFAULT 0,
    status       TINYINT      NOT NULL DEFAULT 1,
    remark       VARCHAR(512),
    create_by    VARCHAR(64),
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    VARCHAR(64),
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code)
);

-- Unit base data  (matches BsUnitMapper.xml)
CREATE TABLE IF NOT EXISTS bs_unit (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    unit_code         VARCHAR(32)    NOT NULL,
    unit_name         VARCHAR(128)   NOT NULL,
    symbol            VARCHAR(32),
    category          VARCHAR(32),
    conversion_factor DECIMAL(18, 6) DEFAULT 1.0,
    order_num         INT            DEFAULT 0,
    status            TINYINT        NOT NULL DEFAULT 1,
    create_by         VARCHAR(64),
    create_time       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64),
    update_time       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_unit_code (unit_code)
);

-- Template table  (matches TplTemplateMapper.xml)
CREATE TABLE IF NOT EXISTS tpl_template (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    template_code    VARCHAR(64)  NOT NULL,
    template_name    VARCHAR(255) NOT NULL,
    category         VARCHAR(32),
    description      VARCHAR(512),
    current_version  VARCHAR(32),
    status           TINYINT      NOT NULL DEFAULT 1,
    remark           VARCHAR(512),
    create_by        VARCHAR(64),
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64),
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code)
);
