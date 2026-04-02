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
