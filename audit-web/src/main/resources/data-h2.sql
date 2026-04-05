-- H2 dev seed data
-- Seed admin account: username=admin, password=admin123
-- BCrypt hash generated with Spring Security BCryptPasswordEncoder (prefix $2a$, cost=10).
-- $2a$ is the canonical prefix emitted by Java's BCryptPasswordEncoder.
--   $2a$10$0T0mZTyDLfErwhn/qSjIG.MlhJOt9BdDpKQ58iXLRMNlwqI2pmbpK

MERGE INTO sys_user (
    id, username, password, real_name, phone, email,
    user_type, enterprise_id, status, password_changed,
    create_by, create_time, update_by, update_time, deleted
) KEY (username) VALUES (
    1,
    'admin',
    '$2a$10$0T0mZTyDLfErwhn/qSjIG.MlhJOt9BdDpKQ58iXLRMNlwqI2pmbpK',
    '系统管理员',
    '13800000000',
    'admin@energy-audit.com',
    1,
    NULL,
    1,
    0,
    'system',
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    0
);

MERGE INTO sys_role (
    id, role_name, role_key, sort_order, status, remark,
    create_by, create_time, update_by, update_time, deleted
) KEY (role_key) VALUES
    (1, '系统管理员', 'ROLE_ADMIN',      1, 1, '平台超级管理员', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
    (2, '企业用户',   'ROLE_ENTERPRISE', 2, 1, '企业端普通用户', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
    (3, '审核员',     'ROLE_AUDITOR',    3, 1, '能源审计员',     'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0);

MERGE INTO sys_user_role (id, user_id, role_id, create_by, update_by, deleted)
KEY (user_id, role_id)
VALUES (1, 1, 1, 'system', 'system', 0);

-- Seed enterprise record (id=1)
MERGE INTO ent_enterprise (
    id, enterprise_name, credit_code, contact_person, contact_email, contact_phone,
    is_active, create_by, create_time, update_by, update_time, deleted
) KEY (id) VALUES (
    1, '示例能源科技有限公司', '91310000DEMO00001X', '张三', 'enterprise@example.com', '13900000001',
    1, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0
);

-- Seed enterprise user (username=enterprise, password=admin123)
-- BCrypt hash reuses admin123 hash; Spring Security verifies against the stored salt
MERGE INTO sys_user (
    id, username, password, real_name, phone, email,
    user_type, enterprise_id, status, password_changed,
    create_by, create_time, update_by, update_time, deleted
) KEY (username) VALUES (
    2,
    'enterprise',
    '$2a$10$0T0mZTyDLfErwhn/qSjIG.MlhJOt9BdDpKQ58iXLRMNlwqI2pmbpK',
    '企业联络人',
    '13900000001',
    'enterprise@example.com',
    3,
    1,
    1,
    0,
    'system',
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    0
);

-- Bind enterprise user to ROLE_ENTERPRISE
MERGE INTO sys_user_role (id, user_id, role_id, create_by, update_by, deleted)
KEY (user_id, role_id)
VALUES (2, 2, 2, 'system', 'system', 0);

-- Seed auditor user (username=auditor, password=admin123)
MERGE INTO sys_user (
    id, username, password, real_name, phone, email,
    user_type, enterprise_id, status, password_changed,
    create_by, create_time, update_by, update_time, deleted
) KEY (username) VALUES (
    3,
    'auditor',
    '$2a$10$0T0mZTyDLfErwhn/qSjIG.MlhJOt9BdDpKQ58iXLRMNlwqI2pmbpK',
    '审核员张工',
    '13800000003',
    'auditor@energy-audit.com',
    2,
    NULL,
    1,
    0,
    'system',
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    0
);

-- Bind auditor user to ROLE_AUDITOR
MERGE INTO sys_user_role (id, user_id, role_id, create_by, update_by, deleted)
KEY (user_id, role_id)
VALUES (3, 3, 3, 'system', 'system', 0);

-- Seed published template (for enterprise report-input testing in H2 dev env)
MERGE INTO tpl_template (
    id, template_name, template_code, module_type, description,
    status, current_version,
    create_by, create_time, update_by, update_time, deleted
) KEY (id) VALUES (
    1, '2024年度能源审计报告', 'ENERGY_AUDIT_2024', 'energy_audit', '能源审计年度报告填报模板（开发测试用）',
    1, 1,
    'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0
);

-- Seed published template version v1
MERGE INTO tpl_template_version (
    id, template_id, version, template_json, change_log,
    published, publish_time,
    create_by, create_time, update_by, update_time, deleted
) KEY (id) VALUES (
    1, 1, 1, '{}', '初始发布版本',
    1, CURRENT_TIMESTAMP,
    'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0
);

-- Energy flow seed data (enterprise 1, year 2024)
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'purchased', 1, '电力公司', '变电站', '电力', 50000.0000, 6145.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'purchased', 2, '天然气公司', '锅炉房', '天然气', 20000.0000, 2428.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'purchased', 3, '煤炭供应商', '锅炉房', '原煤', 10000.0000, 7143.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'conversion', 4, '锅炉房', '蒸汽管网', '蒸汽', 15000.0000, 5357.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'distribution', 5, '变电站', '生产车间A', '电力', 30000.0000, 3687.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'distribution', 6, '变电站', '办公区', '电力', 10000.0000, 1229.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'distribution', 7, '蒸汽管网', '生产车间A', '蒸汽', 10000.0000, 3571.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'terminal', 8, '生产车间A', '产品A', '综合能源', 0.0000, 7258.0000, 'system', 'system');
INSERT INTO de_energy_flow (enterprise_id, audit_year, flow_stage, seq_no, source_unit, target_unit, energy_product, physical_quantity, standard_quantity, create_by, update_by)
VALUES (1, 2024, 'terminal', 9, '办公区', '照明暖通', '电力', 10000.0000, 1229.0000, 'system', 'system');
