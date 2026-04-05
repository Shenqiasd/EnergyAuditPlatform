INSERT INTO ent_enterprise (enterprise_name, credit_code, contact_person, contact_phone, contact_email, is_active, create_by, update_by)
VALUES ('测试企业', '91110000MA001TEST', '张三', '021-12345678', 'zhangsan@test.com', 1, 'system', 'system');

INSERT INTO ent_enterprise_setting (enterprise_id, enterprise_address, postal_code, fax, legal_representative, legal_phone, industry_category, industry_code, industry_name, compiler_name, compiler_contact, compiler_email, energy_cert, registered_capital, create_by, update_by)
VALUES (1, '上海市浦东新区张江高科技园区xxx路100号', '201203', '021-87654321', '李四', '13900001234', 'C35', '3511', '金属制品业', '上海节能技术服务有限公司', '王五', 'wangwu@compiler.com', 1, 5000.00, 'system', 'system');

INSERT INTO de_company_overview (enterprise_id, audit_year, energy_leader_name, energy_leader_position, energy_dept_name, energy_dept_leader, fulltime_staff_count, parttime_staff_count, five_year_target_name, five_year_target_value, five_year_target_dept, create_by, update_by)
VALUES (1, 2024, '赵六', '副总经理', '能源管理部', '钱七', 3, 5, '万元产值综合能耗下降目标', 15.0000, '上海市经信委', 'system', 'system');

INSERT INTO sys_user (username, password, real_name, user_type, enterprise_id, status, password_changed, create_by)
VALUES ('admin', '$2b$10$lAWXyYDn67AbLYAQwnwAKeUkEstnzSZX/q5sTtC5RjPtkdoZkzS6q', '系统管理员', 1, NULL, 1, 1, 'system');

INSERT INTO sys_user (username, password, real_name, user_type, enterprise_id, status, password_changed, create_by)
VALUES ('enterprise1', '$2b$10$lAWXyYDn67AbLYAQwnwAKeUkEstnzSZX/q5sTtC5RjPtkdoZkzS6q', '测试企业用户', 3, 1, 1, 1, 'system');

INSERT INTO de_energy_balance (enterprise_id, audit_year, energy_name, energy_category, measurement_unit, purchase_amount, consumption_amount, standard_coal_equiv, create_by, update_by)
VALUES (1, 2024, '电力', '二次能源', '万千瓦时', 5000.0, 4800.0, 5904.0, 'system', 'system');
INSERT INTO de_energy_balance (enterprise_id, audit_year, energy_name, energy_category, measurement_unit, purchase_amount, consumption_amount, standard_coal_equiv, create_by, update_by)
VALUES (1, 2024, '天然气', '一次能源', '万立方米', 200.0, 195.0, 2367.3, 'system', 'system');
INSERT INTO de_energy_balance (enterprise_id, audit_year, energy_name, energy_category, measurement_unit, purchase_amount, consumption_amount, standard_coal_equiv, create_by, update_by)
VALUES (1, 2024, '原煤', '一次能源', '吨', 10000.0, 9500.0, 6786.0, 'system', 'system');
INSERT INTO de_energy_balance (enterprise_id, audit_year, energy_name, energy_category, measurement_unit, purchase_amount, consumption_amount, standard_coal_equiv, create_by, update_by)
VALUES (1, 2024, '蒸汽', '二次能源', '吉焦', 15000.0, 14200.0, 4843.0, 'system', 'system');
INSERT INTO de_energy_balance (enterprise_id, audit_year, energy_name, energy_category, measurement_unit, purchase_amount, consumption_amount, standard_coal_equiv, create_by, update_by)
VALUES (1, 2024, '柴油', '一次能源', '吨', 50.0, 48.0, 70.1, 'system', 'system');

INSERT INTO de_tech_indicator (enterprise_id, audit_year, indicator_year, gross_output, sales_revenue, tax_paid, energy_total_cost, production_cost, energy_cost_ratio, total_energy_equiv, total_energy_equal, total_energy_excl_material, unit_output_energy, saving_project_count, saving_invest_total, saving_capacity, saving_benefit, coal_target, coal_actual, create_by, update_by)
VALUES (1, 2024, 2024, 85000.0, 78000.0, 3200.0, 4200.0, 62000.0, 6.77, 19970.4, 22150.0, 18500.0, 0.2349, 3, 580.0, 520.0, 310.0, 20000.0, 19970.4, 'system', 'system');
INSERT INTO de_tech_indicator (enterprise_id, audit_year, indicator_year, gross_output, sales_revenue, tax_paid, energy_total_cost, production_cost, energy_cost_ratio, total_energy_equiv, total_energy_equal, total_energy_excl_material, unit_output_energy, saving_project_count, saving_invest_total, saving_capacity, saving_benefit, coal_target, coal_actual, create_by, update_by)
VALUES (1, 2024, 2023, 82000.0, 75000.0, 3000.0, 4500.0, 60000.0, 7.50, 20800.0, 23100.0, 19200.0, 0.2537, 2, 420.0, 380.0, 220.0, 21000.0, 20800.0, 'system', 'system');
INSERT INTO de_tech_indicator (enterprise_id, audit_year, indicator_year, gross_output, sales_revenue, tax_paid, energy_total_cost, production_cost, energy_cost_ratio, total_energy_equiv, total_energy_equal, total_energy_excl_material, unit_output_energy, saving_project_count, saving_invest_total, saving_capacity, saving_benefit, coal_target, coal_actual, create_by, update_by)
VALUES (1, 2024, 2022, 79000.0, 72000.0, 2800.0, 4100.0, 58000.0, 7.07, 21500.0, 23800.0, 19800.0, 0.2722, 1, 200.0, 180.0, 100.0, 22000.0, 21500.0, 'system', 'system');

INSERT INTO de_product_unit_consumption (enterprise_id, audit_year, product_name, year_type, measurement_unit, output, energy_consumption, unit_consumption, create_by, update_by)
VALUES (1, 2024, '产品A', '审计年', '吨', 12000.0, 8500.0, 0.7083, 'system', 'system');
INSERT INTO de_product_unit_consumption (enterprise_id, audit_year, product_name, year_type, measurement_unit, output, energy_consumption, unit_consumption, create_by, update_by)
VALUES (1, 2024, '产品A', '上年度', '吨', 11500.0, 8800.0, 0.7652, 'system', 'system');
INSERT INTO de_product_unit_consumption (enterprise_id, audit_year, product_name, year_type, measurement_unit, output, energy_consumption, unit_consumption, create_by, update_by)
VALUES (1, 2024, '产品B', '审计年', '吨', 8000.0, 5200.0, 0.6500, 'system', 'system');
INSERT INTO de_product_unit_consumption (enterprise_id, audit_year, product_name, year_type, measurement_unit, output, energy_consumption, unit_consumption, create_by, update_by)
VALUES (1, 2024, '产品B', '上年度', '吨', 7500.0, 5100.0, 0.6800, 'system', 'system');

INSERT INTO de_ghg_emission (enterprise_id, audit_year, emission_type, energy_name, main_equipment, activity_data, annual_emission, create_by, update_by)
VALUES (1, 2024, '直接排放', '原煤', '锅炉', 9500.0, 18240.0, 'system', 'system');
INSERT INTO de_ghg_emission (enterprise_id, audit_year, emission_type, energy_name, main_equipment, activity_data, annual_emission, create_by, update_by)
VALUES (1, 2024, '直接排放', '天然气', '锅炉', 195.0, 4380.0, 'system', 'system');
INSERT INTO de_ghg_emission (enterprise_id, audit_year, emission_type, energy_name, main_equipment, activity_data, annual_emission, create_by, update_by)
VALUES (1, 2024, '直接排放', '柴油', '运输车辆', 48.0, 151.0, 'system', 'system');
INSERT INTO de_ghg_emission (enterprise_id, audit_year, emission_type, energy_name, main_equipment, activity_data, annual_emission, create_by, update_by)
VALUES (1, 2024, '间接排放', '电力', '全厂', 4800.0, 27360.0, 'system', 'system');
INSERT INTO de_ghg_emission (enterprise_id, audit_year, emission_type, energy_name, main_equipment, activity_data, annual_emission, create_by, update_by)
VALUES (1, 2024, '间接排放', '蒸汽', '全厂', 14200.0, 1207.0, 'system', 'system');

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
