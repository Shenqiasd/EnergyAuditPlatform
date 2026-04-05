INSERT INTO ent_enterprise (enterprise_name, credit_code, contact_person, is_active, create_by, update_by)
VALUES ('测试企业', '91110000MA001TEST', '张三', 1, 'system', 'system');

INSERT INTO sys_user (username, password, real_name, user_type, enterprise_id, status, password_changed, create_by)
VALUES ('admin', '$2b$10$lAWXyYDn67AbLYAQwnwAKeUkEstnzSZX/q5sTtC5RjPtkdoZkzS6q', '系统管理员', 1, NULL, 1, 1, 'system');

INSERT INTO sys_user (username, password, real_name, user_type, enterprise_id, status, password_changed, create_by)
VALUES ('enterprise1', '$2b$10$lAWXyYDn67AbLYAQwnwAKeUkEstnzSZX/q5sTtC5RjPtkdoZkzS6q', '测试企业用户', 3, 1, 1, 1, 'system');

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
