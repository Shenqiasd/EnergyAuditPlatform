-- ============================================================================
-- 能源审计平台 (Energy Audit Platform) - 数据库架构
-- 数据库: MySQL 8.0+
-- 字符集: UTF-8 mb4
-- 创建日期: 2026-04-01
--
-- 模块说明:
--   1. sys_  - 系统管理模块 (用户/角色/菜单/字典/配置/日志)
--   2. ent_  - 企业管理模块 (注册/企业/设置)
--   3. bs_   - 基础设置模块 (能源品种/单元/产品)
--   4. tpl_  - 模板管理模块 (模板/版本/标签/填报/编辑锁)
--   5. de_   - 数据录入模块 (企业概况/技术指标/能源平衡/设备/节能等)
--   6. ar_   - 审计报告模块 (报告/章节/版本/附件)
--   7. aw_   - 审核流程模块 (任务/日志/整改跟踪)
--   8. cm_   - 碳排放管理模块 (排放因子)
--   9. ch_   - 图表配置模块 (图表配置)
--
-- 公共字段说明:
--   id          - 主键ID (BIGINT AUTO_INCREMENT)
--   create_by   - 创建人
--   create_time - 创建时间
--   update_by   - 更新人
--   update_time - 更新时间
--   deleted     - 逻辑删除标记 (0未删除 1已删除)
--
-- 注意: 外键关系以注释形式标注，不强制约束，以保证灵活性
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. 系统管理模块 (sys_)
-- ============================================================================

-- ----------------------------
-- 1.1 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`         VARCHAR(64)  NOT NULL                COMMENT '用户名',
    `password`         VARCHAR(128) NOT NULL                COMMENT '密码(BCrypt)',
    `real_name`        VARCHAR(64)  DEFAULT NULL            COMMENT '真实姓名',
    `phone`            VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `email`            VARCHAR(128) DEFAULT NULL            COMMENT '邮箱',
    `user_type`        TINYINT      NOT NULL                COMMENT '用户类型(1管理端 2审核端 3企业端)',
    `enterprise_id`    BIGINT       DEFAULT NULL            COMMENT '所属企业ID(企业用户关联) -> ent_enterprise.id',
    `status`           TINYINT      DEFAULT 1               COMMENT '状态(0禁用 1启用)',
    `last_login_time`  DATETIME     DEFAULT NULL            COMMENT '最后登录时间',
    `password_changed` TINYINT      DEFAULT 0               COMMENT '是否已修改初始密码(0未修改 1已修改)',
    `create_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ----------------------------
-- 1.2 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name`  VARCHAR(64)  NOT NULL                COMMENT '角色名称',
    `role_key`   VARCHAR(64)  NOT NULL                COMMENT '角色标识',
    `sort_order` INT          DEFAULT 0               COMMENT '排序',
    `status`     TINYINT      DEFAULT 1               COMMENT '状态(0禁用 1启用)',
    `remark`     VARCHAR(256) DEFAULT NULL            COMMENT '备注',
    `create_by`  VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`  VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`    TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- ----------------------------
-- 1.3 用户角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT      NOT NULL                COMMENT '用户ID -> sys_user.id',
    `role_id`     BIGINT      NOT NULL                COMMENT '角色ID -> sys_role.id',
    `create_by`   VARCHAR(64) DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64) DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- ----------------------------
-- 1.4 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id`   BIGINT       DEFAULT 0               COMMENT '父菜单ID',
    `menu_name`   VARCHAR(64)  NOT NULL                COMMENT '菜单名称',
    `menu_type`   TINYINT      DEFAULT NULL            COMMENT '类型(1目录 2菜单 3按钮)',
    `path`        VARCHAR(256) DEFAULT NULL            COMMENT '路由路径',
    `component`   VARCHAR(256) DEFAULT NULL            COMMENT '组件路径',
    `permission`  VARCHAR(128) DEFAULT NULL            COMMENT '权限标识',
    `icon`        VARCHAR(64)  DEFAULT NULL            COMMENT '图标',
    `sort_order`  INT          DEFAULT 0               COMMENT '排序',
    `visible`     TINYINT      DEFAULT 1               COMMENT '是否可见(0隐藏 1显示)',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态(0禁用 1启用)',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';

-- ----------------------------
-- 1.5 角色菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id`     BIGINT      NOT NULL                COMMENT '角色ID -> sys_role.id',
    `menu_id`     BIGINT      NOT NULL                COMMENT '菜单ID -> sys_menu.id',
    `create_by`   VARCHAR(64) DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64) DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- ----------------------------
-- 1.6 字典类型表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_name`   VARCHAR(128) NOT NULL                COMMENT '字典名称',
    `dict_type`   VARCHAR(128) NOT NULL                COMMENT '字典类型标识',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态(0禁用 1启用)',
    `remark`      VARCHAR(256) DEFAULT NULL            COMMENT '备注',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

-- ----------------------------
-- 1.7 字典数据表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_type`   VARCHAR(128) NOT NULL                COMMENT '字典类型 -> sys_dict_type.dict_type',
    `dict_label`  VARCHAR(256) NOT NULL                COMMENT '字典标签',
    `dict_value`  VARCHAR(256) NOT NULL                COMMENT '字典值',
    `dict_sort`   INT          DEFAULT 0               COMMENT '排序',
    `css_class`   VARCHAR(128) DEFAULT NULL            COMMENT '样式',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态(0禁用 1启用)',
    `remark`      VARCHAR(256) DEFAULT NULL            COMMENT '备注',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- ----------------------------
-- 1.8 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_name`  VARCHAR(128) NOT NULL                COMMENT '配置名称',
    `config_key`   VARCHAR(128) NOT NULL                COMMENT '配置键',
    `config_value` TEXT         DEFAULT NULL            COMMENT '配置值',
    `config_type`  TINYINT      DEFAULT 0               COMMENT '类型(0系统 1业务)',
    `remark`       VARCHAR(256) DEFAULT NULL            COMMENT '备注',
    `create_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- ----------------------------
-- 1.9 操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT       DEFAULT NULL            COMMENT '操作用户ID -> sys_user.id',
    `username`        VARCHAR(64)  DEFAULT NULL            COMMENT '操作用户名',
    `operation`       VARCHAR(256) DEFAULT NULL            COMMENT '操作描述',
    `method`          VARCHAR(256) DEFAULT NULL            COMMENT '请求方法',
    `request_url`     VARCHAR(512) DEFAULT NULL            COMMENT '请求URL',
    `request_params`  TEXT         DEFAULT NULL            COMMENT '请求参数',
    `response_result` TEXT         DEFAULT NULL            COMMENT '响应结果',
    `ip`              VARCHAR(64)  DEFAULT NULL            COMMENT 'IP地址',
    `status`          TINYINT      DEFAULT NULL            COMMENT '操作状态(0失败 1成功)',
    `error_msg`       TEXT         DEFAULT NULL            COMMENT '错误信息',
    `operation_time`  DATETIME     DEFAULT NULL            COMMENT '操作时间',
    `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ============================================================================
-- 2. 企业管理模块 (ent_)
-- ============================================================================

-- ----------------------------
-- 2.1 企业注册申请表
-- ----------------------------
DROP TABLE IF EXISTS `ent_registration`;
CREATE TABLE `ent_registration` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_name` VARCHAR(256) NOT NULL                COMMENT '企业名称',
    `credit_code`     VARCHAR(64)  NOT NULL                COMMENT '统一社会信用代码',
    `contact_person`  VARCHAR(64)  DEFAULT NULL            COMMENT '联系人',
    `contact_email`   VARCHAR(128) DEFAULT NULL            COMMENT '邮箱',
    `contact_phone`   VARCHAR(20)  DEFAULT NULL            COMMENT '电话',
    `apply_ip`        VARCHAR(64)  DEFAULT NULL            COMMENT '申请IP',
    `apply_no`        VARCHAR(64)  DEFAULT NULL            COMMENT '申请编号',
    `apply_time`      DATETIME     DEFAULT NULL            COMMENT '申请时间',
    `audit_status`    TINYINT      DEFAULT 0               COMMENT '审核状态(0待审核 1通过 2驳回)',
    `audit_user_id`   BIGINT       DEFAULT NULL            COMMENT '审核人ID -> sys_user.id',
    `audit_time`      DATETIME     DEFAULT NULL            COMMENT '审核时间',
    `audit_remark`    VARCHAR(512) DEFAULT NULL            COMMENT '审核备注',
    `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_credit_code` (`credit_code`),
    INDEX `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业注册申请表';

-- ----------------------------
-- 2.2 企业主表
-- ----------------------------
DROP TABLE IF EXISTS `ent_enterprise`;
CREATE TABLE `ent_enterprise` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_name` VARCHAR(256) NOT NULL                COMMENT '企业名称',
    `credit_code`     VARCHAR(64)  NOT NULL                COMMENT '统一社会信用代码',
    `contact_person`  VARCHAR(64)  DEFAULT NULL            COMMENT '联系人',
    `contact_email`   VARCHAR(128) DEFAULT NULL            COMMENT '邮箱',
    `contact_phone`   VARCHAR(20)  DEFAULT NULL            COMMENT '电话',
    `remark`          VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `expire_date`     DATE         DEFAULT NULL            COMMENT '过期日期',
    `is_locked`       TINYINT      DEFAULT 0               COMMENT '是否锁定(0未锁定 1已锁定)',
    `is_active`       TINYINT      DEFAULT 1               COMMENT '是否使用(0停用 1使用)',
    `last_login_time` DATETIME     DEFAULT NULL            COMMENT '最近登录时间',
    `sort_order`      INT          DEFAULT 0               COMMENT '排序标识',
    `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_credit_code` (`credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业主表';

-- ----------------------------
-- 2.3 企业设置表 (3.1)
-- ----------------------------
DROP TABLE IF EXISTS `ent_enterprise_setting`;
CREATE TABLE `ent_enterprise_setting` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`         BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `enterprise_address`    VARCHAR(512) DEFAULT NULL            COMMENT '企业地址',
    `unit_address`          VARCHAR(512) DEFAULT NULL            COMMENT '单位地址',
    `postal_code`           VARCHAR(10)  DEFAULT NULL            COMMENT '邮政编码',
    `fax`                   VARCHAR(32)  DEFAULT NULL            COMMENT '传真',
    `legal_representative`  VARCHAR(64)  DEFAULT NULL            COMMENT '法定代表人姓名',
    `legal_phone`           VARCHAR(20)  DEFAULT NULL            COMMENT '联系电话',
    `enterprise_contact`    VARCHAR(64)  DEFAULT NULL            COMMENT '企业联系人',
    `enterprise_mobile`     VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `enterprise_email`      VARCHAR(128) DEFAULT NULL            COMMENT '电子邮件',
    `compiler_contact`      VARCHAR(64)  DEFAULT NULL            COMMENT '编制单位联系人',
    `compiler_mobile`       VARCHAR(20)  DEFAULT NULL            COMMENT '编制单位手机号',
    `compiler_name`         VARCHAR(256) DEFAULT NULL            COMMENT '编制单位名称',
    `compiler_email`        VARCHAR(128) DEFAULT NULL            COMMENT '编制单位电子邮件',
    `energy_cert`           TINYINT      DEFAULT 0               COMMENT '是否通过能源管理体系认证(0否 1是)',
    `cert_authority`        VARCHAR(256) DEFAULT NULL            COMMENT '认证机构',
    `registered_capital`    DECIMAL(18,2) DEFAULT NULL           COMMENT '单位注册资本(万元)',
    `registered_date`       DATE         DEFAULT NULL            COMMENT '单位注册日期',
    `cert_pass_date`        DATE         DEFAULT NULL            COMMENT '通过日期',
    `industry_category`     VARCHAR(64)  DEFAULT NULL            COMMENT '行业分类(字典)',
    `industry_code`         VARCHAR(32)  DEFAULT NULL            COMMENT '行业代码(字典)',
    `industry_name`         VARCHAR(128) DEFAULT NULL            COMMENT '行业名称(字典)',
    `superior_department`   VARCHAR(256) DEFAULT NULL            COMMENT '上级主管部门(字典)',
    `unit_nature`           VARCHAR(64)  DEFAULT NULL            COMMENT '单位性质(字典)',
    `energy_enterprise_type` VARCHAR(64) DEFAULT NULL            COMMENT '用能企业类型(字典)',
    `remark`                VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `create_by`             VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`             VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`           DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业设置表';

-- ============================================================================
-- 3. 基础设置模块 (bs_)
-- ============================================================================

-- ----------------------------
-- 3.0 全局能源目录表 (管理端维护，企业可从此导入)
-- ----------------------------
DROP TABLE IF EXISTS `bs_energy_catalog`;
CREATE TABLE `bs_energy_catalog` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`             VARCHAR(128)  NOT NULL                COMMENT '名称',
    `category`         VARCHAR(64)   DEFAULT NULL            COMMENT '类别(字典: energy_category)',
    `measurement_unit` VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `equivalent_value` DECIMAL(18,6) DEFAULT NULL            COMMENT '当量值',
    `equal_value`      DECIMAL(18,6) DEFAULT NULL            COMMENT '等价值',
    `low_heat_value`   DECIMAL(18,6) DEFAULT NULL            COMMENT '低位热值',
    `carbon_content`   DECIMAL(18,6) DEFAULT NULL            COMMENT '碳含量',
    `oxidation_rate`   DECIMAL(18,6) DEFAULT NULL            COMMENT '氧化率',
    `color`            VARCHAR(16)   DEFAULT NULL            COMMENT '显示颜色',
    `is_active`        TINYINT       DEFAULT 1               COMMENT '是否启用(0否 1是)',
    `sort_order`       INT           DEFAULT 0               COMMENT '排序',
    `remark`           VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全局能源目录表(管理端维护)';

-- ----------------------------
-- 3.1 能源品种表 (3.2)
-- ----------------------------
DROP TABLE IF EXISTS `bs_energy`;
CREATE TABLE `bs_energy` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`    BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `name`             VARCHAR(128)  NOT NULL                COMMENT '名称',
    `category`         VARCHAR(64)   NOT NULL                COMMENT '类别(字典: 固体燃料/液体燃料/气体燃料/电力/热力等)',
    `measurement_unit` VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `equivalent_value` DECIMAL(18,6) DEFAULT NULL            COMMENT '当量值',
    `equal_value`      DECIMAL(18,6) DEFAULT NULL            COMMENT '等价值',
    `low_heat_value`   DECIMAL(18,6) DEFAULT NULL            COMMENT '低位热值',
    `carbon_content`   DECIMAL(18,6) DEFAULT NULL            COMMENT '单位热值含碳量',
    `oxidation_rate`   DECIMAL(18,6) DEFAULT NULL            COMMENT '氧化率参数',
    `color`            VARCHAR(16)   DEFAULT NULL            COMMENT '颜色',
    `is_active`        TINYINT       DEFAULT 1               COMMENT '是否使用(0停用 1使用)',
    `remark`           VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源品种表';

-- ----------------------------
-- 3.2 单元设置表 (3.3 统一存储三种类型)
-- ----------------------------
DROP TABLE IF EXISTS `bs_unit`;
CREATE TABLE `bs_unit` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id` BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `name`          VARCHAR(128) NOT NULL                COMMENT '名称',
    `unit_type`     TINYINT      NOT NULL                COMMENT '单元类型(1加工转换 2分配输送 3终端使用)',
    `sub_category`  VARCHAR(64)  DEFAULT NULL            COMMENT '分类(字典, 终端使用时使用)',
    `remark`        VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_unit_type` (`enterprise_id`, `unit_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='单元设置表';

-- ----------------------------
-- 3.3 单元能源关联表 (3.3.1和3.3.2关联能源)
-- ----------------------------
DROP TABLE IF EXISTS `bs_unit_energy`;
CREATE TABLE `bs_unit_energy` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `unit_id`     BIGINT      NOT NULL                COMMENT '单元ID -> bs_unit.id',
    `energy_id`   BIGINT      NOT NULL                COMMENT '能源ID -> bs_energy.id',
    `create_by`   VARCHAR(64) DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64) DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_unit_energy` (`unit_id`, `energy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='单元能源关联表';

-- ----------------------------
-- 3.4 产品设置表 (3.4)
-- ----------------------------
DROP TABLE IF EXISTS `bs_product`;
CREATE TABLE `bs_product` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`    BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `name`             VARCHAR(128)  NOT NULL                COMMENT '名称',
    `measurement_unit` VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `unit_price`       DECIMAL(18,4) DEFAULT NULL            COMMENT '单价(万元)',
    `remark`           VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_id` (`enterprise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产品设置表';

-- ============================================================================
-- 4. 模板管理模块 (tpl_)
-- ============================================================================

-- ----------------------------
-- 4.1 模板主表
-- ----------------------------
DROP TABLE IF EXISTS `tpl_template`;
CREATE TABLE `tpl_template` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_name`   VARCHAR(128) NOT NULL                COMMENT '模板名称',
    `template_code`   VARCHAR(64)  NOT NULL                COMMENT '模板编码(对应业务模块)',
    `module_type`     VARCHAR(64)  DEFAULT NULL            COMMENT '所属模块',
    `description`     VARCHAR(512) DEFAULT NULL            COMMENT '描述',
    `status`          TINYINT      DEFAULT 1               COMMENT '状态(0停用 1启用)',
    `current_version` INT          DEFAULT 1               COMMENT '当前版本号',
    `create_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模板主表';

-- ----------------------------
-- 4.2 模板版本表
-- ----------------------------
DROP TABLE IF EXISTS `tpl_template_version`;
CREATE TABLE `tpl_template_version` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id`   BIGINT       NOT NULL                COMMENT '模板ID -> tpl_template.id',
    `version`       INT          NOT NULL                COMMENT '版本号',
    `template_json` LONGTEXT     NOT NULL                COMMENT 'SpreadJS模板JSON',
    `change_log`    VARCHAR(512) DEFAULT NULL            COMMENT '变更说明',
    `published`     TINYINT      DEFAULT 0               COMMENT '是否发布(0未发布 1已发布)',
    `publish_time`  DATETIME     DEFAULT NULL            COMMENT '发布时间',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_template_version` (`template_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模板版本表';

-- ----------------------------
-- 4.3 模板标签映射表
-- ----------------------------
DROP TABLE IF EXISTS `tpl_tag_mapping`;
CREATE TABLE `tpl_tag_mapping` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_version_id` BIGINT       NOT NULL                COMMENT '模板版本ID -> tpl_template_version.id',
    `tag_name`            VARCHAR(128) NOT NULL                COMMENT '标签名称(SpreadJS Tag/Named Range)',
    `field_name`          VARCHAR(128) NOT NULL                COMMENT '映射数据库字段名',
    `target_table`        VARCHAR(128) DEFAULT NULL            COMMENT '目标表名',
    `data_type`           VARCHAR(32)  DEFAULT NULL            COMMENT '数据类型(STRING/NUMBER/DATE/DICT)',
    `dict_type`           VARCHAR(128) DEFAULT NULL            COMMENT '字典类型(data_type=DICT时使用)',
    `required`            TINYINT      DEFAULT 0               COMMENT '是否必填(0非必填 1必填)',
    `sheet_index`         INT          DEFAULT 0               COMMENT '所在Sheet序号',
    `sheet_name`          VARCHAR(128) DEFAULT NULL            COMMENT '所在Sheet名称(稳定标识,优先于sheet_index)',
    `cell_range`          VARCHAR(32)  DEFAULT NULL            COMMENT '单元格范围(如A1:B10)',
    `remark`              VARCHAR(256) DEFAULT NULL            COMMENT '备注',
    `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_template_version_id` (`template_version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模板标签映射表';

-- ----------------------------
-- 4.4 模板填报数据表
-- ----------------------------
DROP TABLE IF EXISTS `tpl_submission`;
CREATE TABLE `tpl_submission` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`    BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `template_id`      BIGINT       NOT NULL                COMMENT '模板ID -> tpl_template.id',
    `template_version` INT          NOT NULL                COMMENT '填报时模板版本',
    `audit_year`       INT          NOT NULL                COMMENT '审计年度',
    `submission_json`  LONGTEXT     DEFAULT NULL            COMMENT '填报后的SpreadJS JSON',
    `extracted_data`   JSON         DEFAULT NULL            COMMENT '抽取的结构化数据(JSON)',
    `status`           TINYINT      DEFAULT 0               COMMENT '状态(0草稿 1已提交)',
    `submit_time`      DATETIME     DEFAULT NULL            COMMENT '提交时间',
    `create_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_template_year` (`enterprise_id`, `template_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模板填报数据表';

-- ----------------------------
-- 4.5 编辑锁表 (悲观锁)
-- ----------------------------
DROP TABLE IF EXISTS `tpl_edit_lock`;
CREATE TABLE `tpl_edit_lock` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id` BIGINT      NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `template_id`   BIGINT      NOT NULL                COMMENT '模板ID -> tpl_template.id',
    `audit_year`    INT         NOT NULL                COMMENT '审计年度',
    `lock_user_id`  BIGINT      NOT NULL                COMMENT '锁定用户ID -> sys_user.id',
    `lock_time`     DATETIME    NOT NULL                COMMENT '锁定时间',
    `expire_time`   DATETIME    NOT NULL                COMMENT '锁定过期时间(防死锁)',
    `create_by`     VARCHAR(64) DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64) DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT     DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_enterprise_template_year` (`enterprise_id`, `template_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='编辑锁表';

-- ============================================================================
-- 5. 数据录入模块 (de_)
-- ============================================================================

-- ----------------------------
-- 5.1 企业概况 (4.1)
-- ----------------------------
DROP TABLE IF EXISTS `de_company_overview`;
CREATE TABLE `de_company_overview` (
    `id`                     BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`          BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`             INT           NOT NULL                COMMENT '审计年度',
    `energy_leader_name`     VARCHAR(64)   DEFAULT NULL            COMMENT '节能主管领导姓名',
    `energy_leader_position` VARCHAR(64)   DEFAULT NULL            COMMENT '节能主管领导职务',
    `energy_dept_name`       VARCHAR(128)  DEFAULT NULL            COMMENT '节能主管部门名称',
    `energy_dept_leader`     VARCHAR(64)   DEFAULT NULL            COMMENT '部门负责人姓名',
    `fulltime_staff_count`   INT           DEFAULT NULL            COMMENT '专职管理人员数',
    `parttime_staff_count`   INT           DEFAULT NULL            COMMENT '兼职管理人员数',
    `five_year_target_value` DECIMAL(18,4) DEFAULT NULL            COMMENT '十四五节能目标值',
    `five_year_target_name`  VARCHAR(256)  DEFAULT NULL            COMMENT '十四五节能目标名称',
    `five_year_target_dept`  VARCHAR(256)  DEFAULT NULL            COMMENT '目标下达部门',
    `create_by`              VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`            DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`              VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业概况';

-- ----------------------------
-- 5.2 主要技术指标 (4.2)
-- ----------------------------
DROP TABLE IF EXISTS `de_tech_indicator`;
CREATE TABLE `de_tech_indicator` (
    `id`                        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`             BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`                INT           NOT NULL                COMMENT '审计年度',
    `indicator_year`            INT           NOT NULL                COMMENT '指标年份(审计年/上年度)',
    `gross_output`              DECIMAL(18,4) DEFAULT NULL            COMMENT '工业总产值(万元)',
    `sales_revenue`             DECIMAL(18,4) DEFAULT NULL            COMMENT '销售收入(万元)',
    `tax_paid`                  DECIMAL(18,4) DEFAULT NULL            COMMENT '上缴利税(万元)',
    `energy_total_cost`         DECIMAL(18,4) DEFAULT NULL            COMMENT '能源总成本(万元)',
    `production_cost`           DECIMAL(18,4) DEFAULT NULL            COMMENT '生产成本(万元)',
    `energy_cost_ratio`         DECIMAL(8,4)  DEFAULT NULL            COMMENT '能源成本占比(%)',
    `total_energy_equiv`        DECIMAL(18,4) DEFAULT NULL            COMMENT '综合能耗当量值(吨标煤)',
    `total_energy_equal`        DECIMAL(18,4) DEFAULT NULL            COMMENT '综合能耗等价值(吨标煤)',
    `total_energy_excl_material` DECIMAL(18,4) DEFAULT NULL           COMMENT '综合能耗剔除原料(吨标煤)',
    `unit_output_energy`        DECIMAL(18,6) DEFAULT NULL            COMMENT '单位产值综合能耗',
    `saving_project_count`      INT           DEFAULT NULL            COMMENT '节能项目数',
    `saving_invest_total`       DECIMAL(18,4) DEFAULT NULL            COMMENT '投资总额(万元)',
    `saving_capacity`           DECIMAL(18,4) DEFAULT NULL            COMMENT '节能能力(吨标煤)',
    `saving_benefit`            DECIMAL(18,4) DEFAULT NULL            COMMENT '经济效益(万元)',
    `coal_target`               DECIMAL(18,4) DEFAULT NULL            COMMENT '煤炭消费总量目标',
    `coal_actual`               DECIMAL(18,4) DEFAULT NULL            COMMENT '煤炭消费实际完成值',
    `create_by`                 VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`               DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`                 VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`               DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                   TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_enterprise_year_indicator` (`enterprise_id`, `audit_year`, `indicator_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='主要技术指标';

-- ----------------------------
-- 5.3 已实施节能技改项目 (4.3)
-- ----------------------------
DROP TABLE IF EXISTS `de_energy_saving_project`;
CREATE TABLE `de_energy_saving_project` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`      BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`         INT           NOT NULL                COMMENT '审计年度',
    `project_name`       VARCHAR(256)  NOT NULL                COMMENT '项目名称',
    `main_content`       TEXT          DEFAULT NULL            COMMENT '主要内容',
    `investment`         DECIMAL(18,4) DEFAULT NULL            COMMENT '投资(万元)',
    `annual_saving`      DECIMAL(18,4) DEFAULT NULL            COMMENT '年节能量(吨标煤)',
    `payback_period`     DECIMAL(8,2)  DEFAULT NULL            COMMENT '投资回收期(年)',
    `completion_time`    DATE          DEFAULT NULL            COMMENT '完成时间',
    `actual_saving`      DECIMAL(18,4) DEFAULT NULL            COMMENT '实际节能量(吨标煤)',
    `is_contract_energy` TINYINT       DEFAULT 0               COMMENT '是否合同能源管理模式(0否 1是)',
    `remark`             VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='已实施节能技改项目';

-- ----------------------------
-- 5.4 能源计量器具汇总 (4.4)
-- ----------------------------
DROP TABLE IF EXISTS `de_meter_instrument`;
CREATE TABLE `de_meter_instrument` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`    BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`       INT          NOT NULL                COMMENT '审计年度',
    `management_no`    VARCHAR(64)  DEFAULT NULL            COMMENT '管理编号',
    `meter_name`       VARCHAR(128) DEFAULT NULL            COMMENT '计量表名称',
    `install_location` VARCHAR(256) DEFAULT NULL            COMMENT '安装地点或计量区域',
    `model_spec`       VARCHAR(128) DEFAULT NULL            COMMENT '型号规格',
    `manufacturer`     VARCHAR(256) DEFAULT NULL            COMMENT '生产厂家',
    `factory_no`       VARCHAR(64)  DEFAULT NULL            COMMENT '出厂编号',
    `multiplier`       DECIMAL(18,4) DEFAULT NULL           COMMENT '倍率',
    `grade`            VARCHAR(32)  DEFAULT NULL            COMMENT '级别',
    `measure_range`    VARCHAR(128) DEFAULT NULL            COMMENT '测量范围',
    `department`       VARCHAR(128) DEFAULT NULL            COMMENT '所属部门',
    `accuracy_grade`   VARCHAR(32)  DEFAULT NULL            COMMENT '准确度等级',
    `status`           VARCHAR(32)  DEFAULT NULL            COMMENT '状态(字典)',
    `energy_id`        BIGINT       DEFAULT NULL            COMMENT '关联能源ID -> bs_energy.id',
    `remark`           VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源计量器具汇总';

-- ----------------------------
-- 5.5 能源计量器具配备率 (4.5)
-- ----------------------------
DROP TABLE IF EXISTS `de_meter_config_rate`;
CREATE TABLE `de_meter_config_rate` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`  BIGINT      NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`     INT         NOT NULL                COMMENT '审计年度',
    `energy_type`    VARCHAR(64) NOT NULL                COMMENT '能源种类(字典)',
    `level_type`     TINYINT     NOT NULL                COMMENT '层级(1进出用能单位 2进出主要次级用能单位 3主要用能设备)',
    `required_count` INT         DEFAULT NULL            COMMENT '需要配置数',
    `actual_count`   INT         DEFAULT NULL            COMMENT '实际配置数',
    `create_by`      VARCHAR(64) DEFAULT NULL            COMMENT '创建人',
    `create_time`    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64) DEFAULT NULL            COMMENT '更新人',
    `update_time`    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT     DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源计量器具配备率';

-- ----------------------------
-- 5.6 重点用能设备能效对标 (4.6) - SpreadJS模板驱动
-- ----------------------------
DROP TABLE IF EXISTS `de_equipment_benchmark`;
CREATE TABLE `de_equipment_benchmark` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id` BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`    INT          NOT NULL                COMMENT '审计年度',
    `submission_id` BIGINT       DEFAULT NULL            COMMENT '关联填报数据 -> tpl_submission.id',
    `remark`        VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='重点用能设备能效对标';

-- ----------------------------
-- 5.7 重点设备能耗和效率 (4.7) - 多维表格
-- ----------------------------
DROP TABLE IF EXISTS `de_equipment_energy`;
CREATE TABLE `de_equipment_energy` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`    BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`       INT           NOT NULL                COMMENT '审计年度',
    `location`         VARCHAR(256)  DEFAULT NULL            COMMENT '位置',
    `device_type`      VARCHAR(128)  DEFAULT NULL            COMMENT '设备类型',
    `indicator_name`   VARCHAR(128)  DEFAULT NULL            COMMENT '指标名称',
    `indicator_value`  DECIMAL(18,6) DEFAULT NULL            COMMENT '指标值',
    `measurement_unit` VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `remark`           VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='重点设备能耗和效率';

-- ----------------------------
-- 5.8 主要用能设备汇总 (4.8)
-- ----------------------------
DROP TABLE IF EXISTS `de_equipment_summary`;
CREATE TABLE `de_equipment_summary` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`        BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`           INT          NOT NULL                COMMENT '审计年度',
    `device_name`          VARCHAR(128) DEFAULT NULL            COMMENT '设备名称',
    `model`                VARCHAR(128) DEFAULT NULL            COMMENT '型号',
    `capacity`             VARCHAR(64)  DEFAULT NULL            COMMENT '容量',
    `quantity`             INT          DEFAULT NULL            COMMENT '数量',
    `annual_runtime_hours` DECIMAL(10,2) DEFAULT NULL           COMMENT '年运行时间(小时)',
    `category`             VARCHAR(64)  DEFAULT NULL            COMMENT '分类(字典)',
    `device_overview`      TEXT         DEFAULT NULL            COMMENT '设备概况',
    `obsolete_update_info` TEXT         DEFAULT NULL            COMMENT '淘汰更新情况',
    `install_location`     VARCHAR(256) DEFAULT NULL            COMMENT '安装使用场所',
    `remark`               VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `create_by`            VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='主要用能设备汇总';

-- ----------------------------
-- 5.9 重点设备测试数据 (4.9)
-- ----------------------------
DROP TABLE IF EXISTS `de_equipment_test`;
CREATE TABLE `de_equipment_test` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`       BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`          INT           NOT NULL                COMMENT '审计年度',
    `device_no`           VARCHAR(64)   DEFAULT NULL            COMMENT '设备编号',
    `device_name`         VARCHAR(128)  DEFAULT NULL            COMMENT '设备名称',
    `model_spec`          VARCHAR(128)  DEFAULT NULL            COMMENT '型号规格',
    `test_indicator_name` VARCHAR(128)  DEFAULT NULL            COMMENT '测试指标名称',
    `measurement_unit`    VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `qualified_value`     DECIMAL(18,6) DEFAULT NULL            COMMENT '合格值或限额',
    `actual_value`        DECIMAL(18,6) DEFAULT NULL            COMMENT '实测值',
    `test_date`           DATE          DEFAULT NULL            COMMENT '测试日期',
    `area`                VARCHAR(64)   DEFAULT NULL            COMMENT '所属区域(字典)',
    `judgement`           VARCHAR(32)   DEFAULT NULL            COMMENT '判别(字典)',
    `remark`              VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`           VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`           VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='重点设备测试数据';

-- ----------------------------
-- 5.10 淘汰产品设备装置目录 (4.10)
-- ----------------------------
DROP TABLE IF EXISTS `de_obsolete_equipment`;
CREATE TABLE `de_obsolete_equipment` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`      BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`         INT          NOT NULL                COMMENT '审计年度',
    `device_name`        VARCHAR(128) DEFAULT NULL            COMMENT '淘汰设备名称',
    `model_spec`         VARCHAR(128) DEFAULT NULL            COMMENT '型号规格',
    `quantity`           INT          DEFAULT NULL            COMMENT '数量',
    `start_use_date`     DATE         DEFAULT NULL            COMMENT '开始使用日期',
    `plan_complete_date` DATE         DEFAULT NULL            COMMENT '计划完成日期',
    `remark`             VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `create_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`          VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='淘汰产品设备装置目录';

-- ----------------------------
-- 5.11 能源平衡表 (4.11.1)
-- ----------------------------
DROP TABLE IF EXISTS `de_energy_balance`;
CREATE TABLE `de_energy_balance` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`       BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`          INT           NOT NULL                COMMENT '审计年度',
    `energy_id`           BIGINT        NOT NULL                COMMENT '关联能源 -> bs_energy.id',
    `opening_stock`       DECIMAL(18,4) DEFAULT NULL            COMMENT '期初库存量',
    `purchase_amount`     DECIMAL(18,4) DEFAULT NULL            COMMENT '购入量',
    `consumption_amount`  DECIMAL(18,4) DEFAULT NULL            COMMENT '消耗量',
    `transfer_out_amount` DECIMAL(18,4) DEFAULT NULL            COMMENT '转出量',
    `closing_stock`       DECIMAL(18,4) DEFAULT NULL            COMMENT '期末库存量',
    `energy_unit_price`   DECIMAL(18,4) DEFAULT NULL            COMMENT '能源单价(元)',
    `create_by`           VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`           VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`         DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源平衡表';

-- ----------------------------
-- 5.12 能源流程图主表 (4.11)
-- ----------------------------
DROP TABLE IF EXISTS `de_energy_flow_diagram`;
CREATE TABLE `de_energy_flow_diagram` (
    `id`            BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id` BIGINT  NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`    INT     NOT NULL                COMMENT '审计年度',
    `diagram_type`  TINYINT NOT NULL                COMMENT '图类型(1分层图式 2单元图式 3二维表式)',
    `diagram_data`  JSON    DEFAULT NULL            COMMENT '图布局数据(AntV X6 JSON)',
    `create_by`     VARCHAR(64) DEFAULT NULL        COMMENT '创建人',
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64) DEFAULT NULL        COMMENT '更新人',
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT  DEFAULT 0              COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_enterprise_year_type` (`enterprise_id`, `audit_year`, `diagram_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源流程图主表';

-- ----------------------------
-- 5.13 能流图节点表
-- ----------------------------
DROP TABLE IF EXISTS `de_energy_flow_node`;
CREATE TABLE `de_energy_flow_node` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `diagram_id`  BIGINT       NOT NULL                COMMENT '流程图ID -> de_energy_flow_diagram.id',
    `node_id`     VARCHAR(64)  NOT NULL                COMMENT '节点标识',
    `node_type`   VARCHAR(32)  NOT NULL                COMMENT '节点类型(PURCHASE/CONVERSION/DISTRIBUTION/TERMINAL/PRODUCT/FIXED)',
    `ref_type`    VARCHAR(32)  DEFAULT NULL            COMMENT '引用类型(UNIT/ENERGY/PRODUCT/FIXED)',
    `ref_id`      BIGINT       DEFAULT NULL            COMMENT '引用ID(对应bs_unit/bs_energy/bs_product的id)',
    `label`       VARCHAR(128) DEFAULT NULL            COMMENT '显示标签',
    `position_x`  DOUBLE       DEFAULT NULL            COMMENT 'X坐标',
    `position_y`  DOUBLE       DEFAULT NULL            COMMENT 'Y坐标',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_diagram_id` (`diagram_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能流图节点表';

-- ----------------------------
-- 5.14 能流图连线表
-- ----------------------------
DROP TABLE IF EXISTS `de_energy_flow_edge`;
CREATE TABLE `de_energy_flow_edge` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `diagram_id`      BIGINT        NOT NULL                COMMENT '流程图ID -> de_energy_flow_diagram.id',
    `edge_id`         VARCHAR(64)   NOT NULL                COMMENT '连线标识',
    `source_node_id`  VARCHAR(64)   NOT NULL                COMMENT '源节点ID',
    `target_node_id`  VARCHAR(64)   NOT NULL                COMMENT '目标节点ID',
    `energy_id`       BIGINT        DEFAULT NULL            COMMENT '关联能源 -> bs_energy.id',
    `product_id`      BIGINT        DEFAULT NULL            COMMENT '关联产品 -> bs_product.id',
    `physical_amount` DECIMAL(18,4) DEFAULT NULL            COMMENT '实物量',
    `remark`          VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_diagram_id` (`diagram_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能流图连线表';

-- ----------------------------
-- 5.15 单位产品能耗 (4.12)
-- ----------------------------
DROP TABLE IF EXISTS `de_product_unit_consumption`;
CREATE TABLE `de_product_unit_consumption` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`      BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`         INT           NOT NULL                COMMENT '审计年度',
    `product_id`         BIGINT        NOT NULL                COMMENT '关联产品 -> bs_product.id',
    `year_type`          VARCHAR(16)   NOT NULL                COMMENT '年份(字典: 审计年/上年度等)',
    `measurement_unit`   VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `output`             DECIMAL(18,4) DEFAULT NULL            COMMENT '产量',
    `energy_consumption` DECIMAL(18,4) DEFAULT NULL            COMMENT '能源消耗量(吨标煤)',
    `unit_consumption`   DECIMAL(18,6) DEFAULT NULL            COMMENT '单耗',
    `create_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='单位产品能耗';

-- ----------------------------
-- 5.16 企业产品能源成本 (4.13)
-- ----------------------------
DROP TABLE IF EXISTS `de_product_energy_cost`;
CREATE TABLE `de_product_energy_cost` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`   BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`      INT           NOT NULL                COMMENT '审计年度',
    `product_id`      BIGINT        NOT NULL                COMMENT '关联产品 -> bs_product.id',
    `energy_cost`     DECIMAL(18,4) DEFAULT NULL            COMMENT '能源成本(万元)',
    `production_cost` DECIMAL(18,4) DEFAULT NULL            COMMENT '生产成本(万元)',
    `remark`          VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业产品能源成本';

-- ----------------------------
-- 5.17 节能量计算 (4.14)
-- ----------------------------
DROP TABLE IF EXISTS `de_energy_saving_calc`;
CREATE TABLE `de_energy_saving_calc` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`  BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`     INT           NOT NULL                COMMENT '审计年度',
    `year_type`      VARCHAR(16)   NOT NULL                COMMENT '年份(字典: 2024/审计年)',
    `energy_equiv`   DECIMAL(18,4) DEFAULT NULL            COMMENT '综合能耗等价值(吨标煤)',
    `energy_equil`   DECIMAL(18,4) DEFAULT NULL            COMMENT '综合能耗当量值(吨标煤)',
    `gross_output`   DECIMAL(18,4) DEFAULT NULL            COMMENT '工业总产值(万元)',
    `product_output` DECIMAL(18,4) DEFAULT NULL            COMMENT '产品产量',
    `product_unit`   VARCHAR(32)   DEFAULT NULL            COMMENT '产品单位',
    `create_by`      VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='节能量计算';

-- ----------------------------
-- 5.18 温室气体排放 (4.15)
-- ----------------------------
DROP TABLE IF EXISTS `de_ghg_emission`;
CREATE TABLE `de_ghg_emission` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`    BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`       INT           NOT NULL                COMMENT '审计年度',
    `emission_type`    VARCHAR(64)   NOT NULL                COMMENT '排放类型(字典: 直接排放/间接排放等)',
    `energy_id`        BIGINT        DEFAULT NULL            COMMENT '关联能源 -> bs_energy.id',
    `main_equipment`   VARCHAR(256)  DEFAULT NULL            COMMENT '主要用能设备/生产部门',
    `measurement_unit` VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `activity_data`    DECIMAL(18,4) DEFAULT NULL            COMMENT '活动数据',
    `annual_emission`  DECIMAL(18,4) DEFAULT NULL            COMMENT '年度排放量(tCO2)',
    `total_emission`   DECIMAL(18,4) DEFAULT NULL            COMMENT '排放量(自动计算)',
    `remark`           VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='温室气体排放';

-- ----------------------------
-- 5.19 节能潜力明细 (4.17)
-- ----------------------------
DROP TABLE IF EXISTS `de_saving_potential`;
CREATE TABLE `de_saving_potential` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`    BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`       INT           NOT NULL                COMMENT '审计年度',
    `category`         VARCHAR(64)   NOT NULL                COMMENT '分类(字典)',
    `project_name`     VARCHAR(256)  DEFAULT NULL            COMMENT '项目名称',
    `saving_potential`  DECIMAL(18,4) DEFAULT NULL           COMMENT '节能潜力(吨标煤/年)',
    `calculation_desc` TEXT          DEFAULT NULL            COMMENT '节能潜力计算说明',
    `remark`           VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='节能潜力明细';

-- ----------------------------
-- 5.20 能源管理制度 (4.18)
-- ----------------------------
DROP TABLE IF EXISTS `de_management_policy`;
CREATE TABLE `de_management_policy` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id` BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`    INT          NOT NULL                COMMENT '审计年度',
    `policy_name`   VARCHAR(256) DEFAULT NULL            COMMENT '制度名称',
    `department`    VARCHAR(128) DEFAULT NULL            COMMENT '主管部门',
    `publish_date`  DATE         DEFAULT NULL            COMMENT '颁布日期',
    `valid_period`  VARCHAR(64)  DEFAULT NULL            COMMENT '有效期',
    `main_content`  TEXT         DEFAULT NULL            COMMENT '主要内容',
    `remark`        VARCHAR(512) DEFAULT NULL            COMMENT '备注',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源管理制度';

-- ----------------------------
-- 5.21 能源管理改进建议 (4.19)
-- ----------------------------
DROP TABLE IF EXISTS `de_improvement_suggestion`;
CREATE TABLE `de_improvement_suggestion` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id` BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`    INT           NOT NULL                COMMENT '审计年度',
    `project_name`  VARCHAR(256)  DEFAULT NULL            COMMENT '项目名称',
    `investment`    DECIMAL(18,4) DEFAULT NULL            COMMENT '投资(万元)',
    `annual_saving` DECIMAL(18,4) DEFAULT NULL            COMMENT '年节能量(吨标煤)',
    `remark`        VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`     VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源管理改进建议';

-- ----------------------------
-- 5.22 节能技术改造建议汇总 (4.20)
-- ----------------------------
DROP TABLE IF EXISTS `de_tech_reform`;
CREATE TABLE `de_tech_reform` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`   BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`      INT           NOT NULL                COMMENT '审计年度',
    `project_name`    VARCHAR(256)  DEFAULT NULL            COMMENT '项目名称',
    `investment`      DECIMAL(18,4) DEFAULT NULL            COMMENT '投资(万元)',
    `annual_saving`   DECIMAL(18,4) DEFAULT NULL            COMMENT '年节能量(吨标煤)',
    `payback_period`  DECIMAL(8,2)  DEFAULT NULL            COMMENT '投资回收期(年)',
    `remark`          VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='节能技术改造建议汇总';

-- ----------------------------
-- 5.23 节能整改措施 (4.21)
-- ----------------------------
DROP TABLE IF EXISTS `de_rectification`;
CREATE TABLE `de_rectification` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`      BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`         INT           NOT NULL                COMMENT '审计年度',
    `project_name`       VARCHAR(256)  DEFAULT NULL            COMMENT '整改项目名称',
    `detail_content`     TEXT          DEFAULT NULL            COMMENT '整改具体内容',
    `rectify_date`       DATE          DEFAULT NULL            COMMENT '整改日期',
    `responsible_person` VARCHAR(64)   DEFAULT NULL            COMMENT '责任人',
    `estimated_cost`     DECIMAL(18,4) DEFAULT NULL            COMMENT '整改预计费用(万元)',
    `saving_amount`      DECIMAL(18,4) DEFAULT NULL            COMMENT '节能量(吨标准煤)',
    `economic_benefit`   DECIMAL(18,4) DEFAULT NULL            COMMENT '经济效益(万元)',
    `create_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`          VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`            TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='节能整改措施';

-- ----------------------------
-- 5.24 十四五期间节能目标 (4.22)
-- ----------------------------
DROP TABLE IF EXISTS `de_five_year_target`;
CREATE TABLE `de_five_year_target` (
    `id`                        BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`             BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`                INT           NOT NULL                COMMENT '审计年度',
    `year_type`                 VARCHAR(32)   NOT NULL                COMMENT '年份类型(字典: 2020实际/2025目标/2020-2025)',
    `product_id`                BIGINT        DEFAULT NULL            COMMENT '关联产品 -> bs_product.id',
    `gross_output`              DECIMAL(18,4) DEFAULT NULL            COMMENT '产值(万元)',
    `energy_equiv`              DECIMAL(18,4) DEFAULT NULL            COMMENT '综合能耗当量值(吨标煤)',
    `energy_equal`              DECIMAL(18,4) DEFAULT NULL            COMMENT '综合能耗等价值(吨标煤)',
    `unit_energy_equiv`         DECIMAL(18,6) DEFAULT NULL            COMMENT '产值综合能耗当量值',
    `unit_energy_equal`         DECIMAL(18,6) DEFAULT NULL            COMMENT '产值综合能耗等价值',
    `decline_rate`              DECIMAL(8,4)  DEFAULT NULL            COMMENT '产值综合能耗下降率(%)',
    `target_indicator`          DECIMAL(18,4) DEFAULT NULL            COMMENT '单耗指标值',
    `actual_indicator`          DECIMAL(18,4) DEFAULT NULL            COMMENT '单耗实际值',
    `annual_target`             DECIMAL(18,4) DEFAULT NULL            COMMENT '目标值(按年度)',
    `energy_control_total`      DECIMAL(18,4) DEFAULT NULL            COMMENT '能耗控制总量等价值',
    `product_unit_consumption`  DECIMAL(18,6) DEFAULT NULL            COMMENT '产品单耗',
    `saving_amount`             DECIMAL(18,4) DEFAULT NULL            COMMENT '节能量(吨标煤)',
    `create_by`                 VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`               DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`                 VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`               DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                   TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='十四五期间节能目标';

-- ----------------------------
-- 5.25 能源数据和温室气体排放源 (4.23)
-- ----------------------------
DROP TABLE IF EXISTS `de_energy_ghg_source`;
CREATE TABLE `de_energy_ghg_source` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`        BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`           INT           NOT NULL                COMMENT '审计年度',
    `energy_id`            BIGINT        DEFAULT NULL            COMMENT '关联能源 -> bs_energy.id',
    `measurement_unit`     VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `start_time`           DATE          DEFAULT NULL            COMMENT '起始时间',
    `end_time`             DATE          DEFAULT NULL            COMMENT '结束时间',
    `period_consumption`   DECIMAL(18,4) DEFAULT NULL            COMMENT '期间消耗',
    `closing_stock`        DECIMAL(18,4) DEFAULT NULL            COMMENT '期末库存',
    `create_by`            VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='能源数据和温室气体排放源';

-- ============================================================================
-- 6. 审计报告模块 (ar_)
-- ============================================================================

-- ----------------------------
-- 6.1 审计报告主表
-- ----------------------------
DROP TABLE IF EXISTS `ar_report`;
CREATE TABLE `ar_report` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id`       BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`          INT          NOT NULL                COMMENT '审计年度',
    `report_name`         VARCHAR(256) DEFAULT NULL            COMMENT '报告名称',
    `report_type`         TINYINT      DEFAULT 1               COMMENT '报告类型(1初始报告 2最终报告)',
    `status`              TINYINT      DEFAULT 0               COMMENT '状态(0草稿 1生成中 2已生成 3已上传 4已提交)',
    `generated_file_path` VARCHAR(512) DEFAULT NULL            COMMENT '生成的文件路径',
    `uploaded_file_path`  VARCHAR(512) DEFAULT NULL            COMMENT '上传的文件路径',
    `onlyoffice_doc_key`  VARCHAR(128) DEFAULT NULL            COMMENT 'OnlyOffice文档Key',
    `generate_time`       DATETIME     DEFAULT NULL            COMMENT '生成时间',
    `submit_time`         DATETIME     DEFAULT NULL            COMMENT '提交时间',
    `create_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`           VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审计报告主表';

-- ----------------------------
-- 6.2 报告章节内容
-- ----------------------------
DROP TABLE IF EXISTS `ar_report_section`;
CREATE TABLE `ar_report_section` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`     BIGINT       NOT NULL                COMMENT '报告ID -> ar_report.id',
    `section_code`  VARCHAR(32)  DEFAULT NULL            COMMENT '章节编码',
    `section_name`  VARCHAR(128) DEFAULT NULL            COMMENT '章节名称',
    `section_order` INT          DEFAULT NULL            COMMENT '章节序号',
    `content`       LONGTEXT     DEFAULT NULL            COMMENT '章节内容(HTML/Rich Text)',
    `data_source`   VARCHAR(128) DEFAULT NULL            COMMENT '数据来源(关联哪个数据录入模块)',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报告章节内容';

-- ----------------------------
-- 6.3 报告版本历史
-- ----------------------------
DROP TABLE IF EXISTS `ar_report_version`;
CREATE TABLE `ar_report_version` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`   BIGINT       NOT NULL                COMMENT '报告ID -> ar_report.id',
    `version`     INT          NOT NULL                COMMENT '版本号',
    `file_path`   VARCHAR(512) DEFAULT NULL            COMMENT '文件路径',
    `change_desc` VARCHAR(512) DEFAULT NULL            COMMENT '变更描述',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报告版本历史';

-- ----------------------------
-- 6.4 报告附件表
-- ----------------------------
DROP TABLE IF EXISTS `ar_attachment`;
CREATE TABLE `ar_attachment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`   BIGINT       NOT NULL                COMMENT '报告ID -> ar_report.id',
    `file_name`   VARCHAR(256) DEFAULT NULL            COMMENT '文件名',
    `file_path`   VARCHAR(512) DEFAULT NULL            COMMENT '文件路径',
    `file_size`   BIGINT       DEFAULT NULL            COMMENT '文件大小(bytes)',
    `file_type`   VARCHAR(32)  DEFAULT NULL            COMMENT '文件类型',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_report_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报告附件表';

-- ============================================================================
-- 7. 审核流程模块 (aw_)
-- ============================================================================

-- ----------------------------
-- 7.1 审核任务表
-- ----------------------------
DROP TABLE IF EXISTS `aw_audit_task`;
CREATE TABLE `aw_audit_task` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enterprise_id` BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`    INT          NOT NULL                COMMENT '审计年度',
    `task_type`     TINYINT      DEFAULT NULL            COMMENT '任务类型(1报告审核 2整改跟踪 3项目验收)',
    `task_title`    VARCHAR(256) DEFAULT NULL            COMMENT '任务标题',
    `status`        TINYINT      DEFAULT 0               COMMENT '状态(0待审核 1审核中 2已通过 3已退回 4已完成)',
    `assignee_id`   BIGINT       DEFAULT NULL            COMMENT '审核人ID -> sys_user.id',
    `assign_time`   DATETIME     DEFAULT NULL            COMMENT '分配时间',
    `deadline`      DATETIME     DEFAULT NULL            COMMENT '截止时间',
    `complete_time` DATETIME     DEFAULT NULL            COMMENT '完成时间',
    `result`        VARCHAR(512) DEFAULT NULL            COMMENT '审核结果',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`),
    INDEX `idx_assignee_status` (`assignee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审核任务表';

-- ----------------------------
-- 7.2 审核日志表
-- ----------------------------
DROP TABLE IF EXISTS `aw_audit_log`;
CREATE TABLE `aw_audit_log` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id`        BIGINT      NOT NULL                COMMENT '任务ID -> aw_audit_task.id',
    `operator_id`    BIGINT      NOT NULL                COMMENT '操作人ID -> sys_user.id',
    `action`         VARCHAR(32) NOT NULL                COMMENT '操作(SUBMIT/APPROVE/REJECT/COMMENT)',
    `comment`        TEXT        DEFAULT NULL            COMMENT '审核意见',
    `operation_time` DATETIME    DEFAULT NULL            COMMENT '操作时间',
    `create_by`      VARCHAR(64) DEFAULT NULL            COMMENT '创建人',
    `create_time`    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64) DEFAULT NULL            COMMENT '更新人',
    `update_time`    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT     DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审核日志表';

-- ----------------------------
-- 7.3 整改跟踪表
-- ----------------------------
DROP TABLE IF EXISTS `aw_rectification_track`;
CREATE TABLE `aw_rectification_track` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id`       BIGINT       NOT NULL                COMMENT '关联审核任务 -> aw_audit_task.id',
    `enterprise_id` BIGINT       NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`    INT          NOT NULL                COMMENT '审计年度',
    `item_name`     VARCHAR(256) DEFAULT NULL            COMMENT '整改项名称',
    `requirement`   TEXT         DEFAULT NULL            COMMENT '整改要求',
    `status`        TINYINT      DEFAULT 0               COMMENT '状态(0未启动 1进行中 2已完成 3超期)',
    `deadline`      DATETIME     DEFAULT NULL            COMMENT '完成时限',
    `complete_time` DATETIME     DEFAULT NULL            COMMENT '实际完成时间',
    `result`        TEXT         DEFAULT NULL            COMMENT '整改结果',
    `create_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`),
    INDEX `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='整改跟踪表';

-- ============================================================================
-- 8. 碳排放管理模块 (cm_)
-- ============================================================================

-- ----------------------------
-- 8.1 碳排放因子表
-- ----------------------------
DROP TABLE IF EXISTS `cm_emission_factor`;
CREATE TABLE `cm_emission_factor` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `factor_name`      VARCHAR(128)  NOT NULL                COMMENT '因子名称',
    `energy_type`      VARCHAR(64)   DEFAULT NULL            COMMENT '能源类型',
    `factor_value`     DECIMAL(18,8) DEFAULT NULL            COMMENT '排放因子值',
    `measurement_unit` VARCHAR(32)   DEFAULT NULL            COMMENT '计量单位',
    `source`           VARCHAR(256)  DEFAULT NULL            COMMENT '数据来源(标准/文件名)',
    `effective_year`   INT           DEFAULT NULL            COMMENT '生效年份',
    `status`           TINYINT       DEFAULT 1               COMMENT '状态(0禁用 1启用)',
    `remark`           VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`        VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_energy_type` (`energy_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='碳排放因子表';

-- ============================================================================
-- 9. 图表配置模块 (ch_)
-- ============================================================================

-- ----------------------------
-- 9.1 图表配置表
-- ----------------------------
DROP TABLE IF EXISTS `ch_chart_config`;
CREATE TABLE `ch_chart_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `chart_name`  VARCHAR(128) NOT NULL                COMMENT '图表名称',
    `chart_code`  VARCHAR(64)  NOT NULL                COMMENT '图表编码',
    `chart_type`  VARCHAR(32)  DEFAULT NULL            COMMENT '图表类型(BAR/LINE/PIE/SANKEY等)',
    `data_source` TEXT         DEFAULT NULL            COMMENT '数据源配置(JSON: 关联哪些表/字段)',
    `config_json` TEXT         DEFAULT NULL            COMMENT 'ECharts配置JSON',
    `module_type` VARCHAR(64)  DEFAULT NULL            COMMENT '所属模块(5.1规定图表/5.2报告辅助)',
    `status`      TINYINT      DEFAULT 1               COMMENT '状态(0禁用 1启用)',
    `create_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '创建人',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)  DEFAULT NULL            COMMENT '更新人',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_chart_code` (`chart_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='图表配置表';

-- ============================================================================
-- 初始字典数据
-- ============================================================================

-- ----------------------------
-- 字典类型初始数据
-- ----------------------------
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `remark`, `create_by`) VALUES
('能源类别', 'energy_category', 1, '能源品种分类', 'system'),
('单元类型', 'unit_type', 1, '用能单元类型', 'system'),
('排放类型', 'emission_type', 1, '温室气体排放类型', 'system'),
('设备分类', 'equipment_category', 1, '主要用能设备分类', 'system'),
('区域类型', 'area_type', 1, '设备所属区域分类', 'system'),
('判别类型', 'judgement_type', 1, '设备测试判别结果', 'system'),
('年份类型', 'year_type', 1, '数据录入年份类型', 'system'),
('行业分类', 'industry_category', 1, '企业行业分类', 'system'),
('单位性质', 'unit_nature', 1, '企业单位性质', 'system'),
('用能企业类型', 'energy_enterprise_type', 1, '用能企业类型分类', 'system'),
('计量器具状态', 'meter_status', 1, '能源计量器具状态', 'system'),
('节能潜力分类', 'saving_potential_category', 1, '节能潜力项目分类', 'system'),
('五年目标年份类型', 'five_year_type', 1, '十四五期间年份类型', 'system'),
('终端使用分类', 'terminal_sub_category', 1, '终端使用单元子分类', 'system'),
('用户类型', 'user_type', 1, '系统用户类型', 'system'),
('审核状态', 'audit_status', 1, '审核流程状态', 'system'),
('报告状态', 'report_status', 1, '审计报告状态', 'system'),
('图表类型', 'chart_type', 1, '图表展示类型', 'system');

-- ----------------------------
-- 字典数据初始数据
-- ----------------------------

-- 能源类别
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('energy_category', '固体燃料', 'SOLID_FUEL', 1, 1, 'system'),
('energy_category', '液体燃料', 'LIQUID_FUEL', 2, 1, 'system'),
('energy_category', '气体燃料', 'GAS_FUEL', 3, 1, 'system'),
('energy_category', '电力', 'ELECTRICITY', 4, 1, 'system'),
('energy_category', '热力', 'HEAT', 5, 1, 'system'),
('energy_category', '工质', 'WORKING_MEDIUM', 6, 1, 'system'),
('energy_category', '耗能工质', 'ENERGY_CONSUMING_MEDIUM', 7, 1, 'system'),
('energy_category', '可再生能源', 'RENEWABLE', 8, 1, 'system');

-- 单元类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('unit_type', '加工转换', '1', 1, 1, 'system'),
('unit_type', '分配输送', '2', 2, 1, 'system'),
('unit_type', '终端使用', '3', 3, 1, 'system');

-- 排放类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('emission_type', '化石燃料燃烧直接排放', 'DIRECT_COMBUSTION', 1, 1, 'system'),
('emission_type', '工业生产过程直接排放', 'DIRECT_PROCESS', 2, 1, 'system'),
('emission_type', '净购入电力间接排放', 'INDIRECT_ELECTRICITY', 3, 1, 'system'),
('emission_type', '净购入热力间接排放', 'INDIRECT_HEAT', 4, 1, 'system'),
('emission_type', '废弃物处理直接排放', 'DIRECT_WASTE', 5, 1, 'system'),
('emission_type', '逸散排放', 'FUGITIVE', 6, 1, 'system');

-- 设备分类
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('equipment_category', '锅炉', 'BOILER', 1, 1, 'system'),
('equipment_category', '电动机', 'MOTOR', 2, 1, 'system'),
('equipment_category', '变压器', 'TRANSFORMER', 3, 1, 'system'),
('equipment_category', '风机', 'FAN', 4, 1, 'system'),
('equipment_category', '水泵', 'PUMP', 5, 1, 'system'),
('equipment_category', '空压机', 'COMPRESSOR', 6, 1, 'system'),
('equipment_category', '中央空调', 'CENTRAL_AC', 7, 1, 'system'),
('equipment_category', '工业窑炉', 'KILN', 8, 1, 'system'),
('equipment_category', '制冷设备', 'REFRIGERATION', 9, 1, 'system'),
('equipment_category', '照明设备', 'LIGHTING', 10, 1, 'system'),
('equipment_category', '其他', 'OTHER', 99, 1, 'system');

-- 区域类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('area_type', '生产区', 'PRODUCTION', 1, 1, 'system'),
('area_type', '办公区', 'OFFICE', 2, 1, 'system'),
('area_type', '仓储区', 'WAREHOUSE', 3, 1, 'system'),
('area_type', '辅助区', 'AUXILIARY', 4, 1, 'system'),
('area_type', '公用区', 'PUBLIC', 5, 1, 'system');

-- 判别类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('judgement_type', '合格', 'QUALIFIED', 1, 1, 'system'),
('judgement_type', '不合格', 'UNQUALIFIED', 2, 1, 'system'),
('judgement_type', '待复测', 'PENDING_RETEST', 3, 1, 'system');

-- 年份类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('year_type', '审计年', 'AUDIT_YEAR', 1, 1, 'system'),
('year_type', '上年度', 'PREVIOUS_YEAR', 2, 1, 'system'),
('year_type', '前两年', 'TWO_YEARS_AGO', 3, 1, 'system');

-- 行业分类
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('industry_category', '制造业', 'MANUFACTURING', 1, 1, 'system'),
('industry_category', '采矿业', 'MINING', 2, 1, 'system'),
('industry_category', '电力热力燃气及水生产和供应业', 'UTILITIES', 3, 1, 'system'),
('industry_category', '建筑业', 'CONSTRUCTION', 4, 1, 'system'),
('industry_category', '交通运输仓储和邮政业', 'TRANSPORT', 5, 1, 'system'),
('industry_category', '其他', 'OTHER', 99, 1, 'system');

-- 单位性质
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('unit_nature', '国有企业', 'STATE_OWNED', 1, 1, 'system'),
('unit_nature', '集体企业', 'COLLECTIVE', 2, 1, 'system'),
('unit_nature', '私营企业', 'PRIVATE', 3, 1, 'system'),
('unit_nature', '外商投资企业', 'FOREIGN_INVESTED', 4, 1, 'system'),
('unit_nature', '港澳台投资企业', 'HK_MACAO_TW', 5, 1, 'system'),
('unit_nature', '股份有限公司', 'JOINT_STOCK', 6, 1, 'system'),
('unit_nature', '有限责任公司', 'LIMITED_LIABILITY', 7, 1, 'system'),
('unit_nature', '其他', 'OTHER', 99, 1, 'system');

-- 用能企业类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('energy_enterprise_type', '重点用能单位', 'KEY_ENERGY_USER', 1, 1, 'system'),
('energy_enterprise_type', '一般用能单位', 'GENERAL_ENERGY_USER', 2, 1, 'system'),
('energy_enterprise_type', '万吨标煤以上', 'ABOVE_10K_TCE', 3, 1, 'system'),
('energy_enterprise_type', '五千至一万吨标煤', 'BETWEEN_5K_10K_TCE', 4, 1, 'system');

-- 计量器具状态
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('meter_status', '在用', 'IN_USE', 1, 1, 'system'),
('meter_status', '停用', 'OUT_OF_USE', 2, 1, 'system'),
('meter_status', '报废', 'SCRAPPED', 3, 1, 'system'),
('meter_status', '待检', 'PENDING_CHECK', 4, 1, 'system');

-- 节能潜力分类
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('saving_potential_category', '管理节能', 'MANAGEMENT', 1, 1, 'system'),
('saving_potential_category', '技术节能', 'TECHNICAL', 2, 1, 'system'),
('saving_potential_category', '结构节能', 'STRUCTURAL', 3, 1, 'system'),
('saving_potential_category', '设备更新', 'EQUIPMENT_UPDATE', 4, 1, 'system');

-- 五年目标年份类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('five_year_type', '2020年实际', '2020_ACTUAL', 1, 1, 'system'),
('five_year_type', '2021年实际', '2021_ACTUAL', 2, 1, 'system'),
('five_year_type', '2022年实际', '2022_ACTUAL', 3, 1, 'system'),
('five_year_type', '2023年实际', '2023_ACTUAL', 4, 1, 'system'),
('five_year_type', '2024年实际', '2024_ACTUAL', 5, 1, 'system'),
('five_year_type', '2025年目标', '2025_TARGET', 6, 1, 'system'),
('five_year_type', '2020-2025累计', '2020_2025_TOTAL', 7, 1, 'system');

-- 终端使用分类
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('terminal_sub_category', '生产工艺', 'PRODUCTION_PROCESS', 1, 1, 'system'),
('terminal_sub_category', '辅助生产', 'AUXILIARY_PRODUCTION', 2, 1, 'system'),
('terminal_sub_category', '附属生产', 'AFFILIATED_PRODUCTION', 3, 1, 'system'),
('terminal_sub_category', '非工业生产', 'NON_INDUSTRIAL', 4, 1, 'system');

-- 用户类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('user_type', '管理端', '1', 1, 1, 'system'),
('user_type', '审核端', '2', 2, 1, 'system'),
('user_type', '企业端', '3', 3, 1, 'system');

-- 审核状态
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('audit_status', '待审核', '0', 1, 1, 'system'),
('audit_status', '审核中', '1', 2, 1, 'system'),
('audit_status', '已通过', '2', 3, 1, 'system'),
('audit_status', '已退回', '3', 4, 1, 'system'),
('audit_status', '已完成', '4', 5, 1, 'system');

-- 报告状态
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('report_status', '草稿', '0', 1, 1, 'system'),
('report_status', '生成中', '1', 2, 1, 'system'),
('report_status', '已生成', '2', 3, 1, 'system'),
('report_status', '已上传', '3', 4, 1, 'system'),
('report_status', '已提交', '4', 5, 1, 'system');

-- 图表类型
INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('chart_type', '柱状图', 'BAR', 1, 1, 'system'),
('chart_type', '折线图', 'LINE', 2, 1, 'system'),
('chart_type', '饼图', 'PIE', 3, 1, 'system'),
('chart_type', '桑基图', 'SANKEY', 4, 1, 'system'),
('chart_type', '雷达图', 'RADAR', 5, 1, 'system'),
('chart_type', '堆叠图', 'STACKED', 6, 1, 'system');

SET FOREIGN_KEY_CHECKS = 1;
