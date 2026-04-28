-- GRA-38: 企业信息字段扩展
-- Uses ensure_column() from 01a-migration-helpers.sql for MySQL 8.0 compatibility.

CALL ensure_column('ent_enterprise_setting', 'superior_department',       'VARCHAR(100) COMMENT ''上级主管部门''');
CALL ensure_column('ent_enterprise_setting', 'energy_usage_type',         'VARCHAR(50)  COMMENT ''用能企业类型''');
CALL ensure_column('ent_enterprise_setting', 'energy_leader_title',       'VARCHAR(50)  COMMENT ''单位主管节能领导职务''');
CALL ensure_column('ent_enterprise_setting', 'energy_dept_name',          'VARCHAR(100) COMMENT ''节能主管部门名称''');
CALL ensure_column('ent_enterprise_setting', 'energy_audit_contact_name', 'VARCHAR(50)  COMMENT ''能源审计联系人姓名''');
CALL ensure_column('ent_enterprise_setting', 'energy_audit_contact_phone','VARCHAR(30)  COMMENT ''能源审计联系人电话''');
