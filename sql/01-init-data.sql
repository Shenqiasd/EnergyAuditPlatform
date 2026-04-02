-- ============================================================================
-- 初始数据：管理员账号 / 基础角色 / 菜单权限
-- ============================================================================

-- Admin user (password: admin123 BCrypt encoded)
INSERT INTO sys_user (username, password, real_name, user_type, status, password_changed, create_by)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 1, 1, 1, 'system');

-- Roles
INSERT INTO sys_role (role_name, role_key, sort_order, create_by) VALUES
('系统管理员', 'ROLE_ADMIN', 1, 'system'),
('审核员', 'ROLE_AUDITOR', 2, 'system'),
('企业用户', 'ROLE_ENTERPRISE', 3, 'system');

-- Admin user role
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
