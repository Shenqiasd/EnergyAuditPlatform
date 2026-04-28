-- =============================================================================
-- Migration 33: 0427v2 新模板（template_version_id=33）Tag 映射与字段补齐
--
-- 目标：对标 AUDIT_FULL_0412，为 0427v2 / Energy_Audit_0428 草稿版本写入完整映射。
-- 说明：0427v2 实际 Excel sheet 名为短名（如 “1.十四五已实施节能技改项目”
--       在 Workbook 内显示为短名时，系统以 template_json 的 sheet_name 为准）。本迁移
--       采用已支持的 cell_range(TABLE) 兜底映射，不依赖 Named Range 是否已经在
--       SpreadJS Designer 内创建。
--
-- 幂等：先软删 version=33 的旧 active mapping，再插入本迁移维护的完整 mapping。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) 补齐 0427v2 映射会使用到、但历史独立表未覆盖的新列
-- -----------------------------------------------------------------------------


-- Common historical NOT NULL columns made nullable for template-driven rows without lookup IDs.
ALTER TABLE de_product_unit_consumption
    MODIFY COLUMN product_id BIGINT DEFAULT NULL COMMENT '关联产品 -> bs_product.id';
ALTER TABLE de_product_energy_cost
    MODIFY COLUMN product_id BIGINT DEFAULT NULL COMMENT '关联产品 -> bs_product.id';
ALTER TABLE de_saving_potential
    MODIFY COLUMN category VARCHAR(64) DEFAULT NULL COMMENT '分类/项目类型';
ALTER TABLE de_carbon_emission
    MODIFY COLUMN emission_category VARCHAR(32) DEFAULT NULL COMMENT '排放类别';
ALTER TABLE de_five_year_target
    MODIFY COLUMN section_type VARCHAR(32) DEFAULT NULL COMMENT '分区类型';

-- Energy purchase/consumption table fields used by 11.1.
CALL ensure_column('de_energy_consumption', 'transfer_out',
    'DECIMAL(18,4) NULL COMMENT ''转出量''');
CALL ensure_column('de_energy_consumption', 'gain_loss',
    'DECIMAL(18,4) NULL COMMENT ''盈亏量''');
CALL ensure_column('de_energy_consumption', 'unit_price',
    'DECIMAL(18,4) NULL COMMENT ''能源单价(元)''');

-- Product tables extract names from the template when no product_id is available.
CALL ensure_column('de_product_unit_consumption', 'product_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''产品名称(冗余)'' AFTER product_id');
CALL ensure_column('de_product_energy_cost', 'product_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''产品名称(冗余)'' AFTER product_id');
CALL ensure_column('de_product_energy_cost', 'cost_ratio',
    'DECIMAL(8,4) DEFAULT NULL COMMENT ''占该产品生产成本比例(%)'' AFTER production_cost');
CALL ensure_column('de_product_energy_cost', 'energy_total_ratio',
    'DECIMAL(8,4) DEFAULT NULL COMMENT ''占能源总成本比例(%)'' AFTER cost_ratio');

-- Sheet 1: 十四五已实施节能技改项目历史表字段。
CALL ensure_column('de_tech_reform_history', 'project_type',
    'VARCHAR(64) DEFAULT NULL COMMENT ''项目类型'' AFTER project_name');
CALL ensure_column('de_tech_reform_history', 'designed_saving',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''设计年节能量(吨标煤)'' AFTER investment');
CALL ensure_column('de_tech_reform_history', 'completion_date',
    'VARCHAR(32) DEFAULT NULL COMMENT ''完成时间'' AFTER payback_period');
CALL ensure_column('de_tech_reform_history', 'actual_saving',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''实际节能量(吨标煤)'' AFTER completion_date');
CALL ensure_column('de_tech_reform_history', 'is_contract_energy',
    'VARCHAR(8) DEFAULT NULL COMMENT ''是否合同能源管理模式'' AFTER actual_saving');

-- Sheet 3: 主要技术指标下部表为行项目/今年/去年/增减结构。
CALL ensure_column('de_tech_indicator', 'project_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''项目名称'' AFTER indicator_year');
CALL ensure_column('de_tech_indicator', 'unit',
    'VARCHAR(64) DEFAULT NULL COMMENT ''计量单位'' AFTER project_name');
CALL ensure_column('de_tech_indicator', 'current_year',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''今年'' AFTER unit');
CALL ensure_column('de_tech_indicator', 'prev_year',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''去年'' AFTER current_year');
CALL ensure_column('de_tech_indicator', 'change_pct',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''增减百分比'' AFTER prev_year');
CALL ensure_column('de_tech_indicator', 'material_adjustment',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''扣原料'' AFTER change_pct');

-- Sheet 6: 能源管理制度历史 schema 兼容。
CALL ensure_column('de_management_policy', 'supervise_dept',
    'VARCHAR(128) DEFAULT NULL COMMENT ''主管部门'' AFTER main_content');

-- Sheet 16: 节能潜力明细
CALL ensure_column('de_saving_potential', 'project_type',
    'VARCHAR(64) DEFAULT NULL COMMENT ''项目类型'' AFTER seq_no');
CALL ensure_column('de_saving_potential', 'carbon_reduction',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''减碳量(tCO2/年)'' AFTER saving_potential');
CALL ensure_column('de_saving_potential', 'investment',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''投资(万元)'' AFTER carbon_reduction');

-- Fresh schemas used calculation_desc; Wave 6 used calc_description. Keep both compatible.
CALL ensure_column('de_saving_potential', 'main_content',
    'TEXT DEFAULT NULL COMMENT ''主要内容'' AFTER project_name');
CALL ensure_column('de_saving_potential', 'calc_description',
    'TEXT DEFAULT NULL COMMENT ''节能潜力计算说明'' AFTER investment');

-- Sheet 17/18: 建议表新增年减碳量；Sheet 18 新增项目类型。
CALL ensure_column('de_management_suggestion', 'annual_carbon_reduction',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''年减碳量(tCO2)'' AFTER annual_saving');
CALL ensure_column('de_tech_reform_suggestion', 'project_type',
    'VARCHAR(64) DEFAULT NULL COMMENT ''项目类型'' AFTER project_name');
CALL ensure_column('de_tech_reform_suggestion', 'annual_carbon_reduction',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''年减碳量(tCO2)'' AFTER annual_saving');

-- Sheet 19: 整改措施新增项目类型/减碳量，并兼容 measures/detail_content、annual_saving/saving_amount 两套历史列名。
CALL ensure_column('de_rectification', 'project_type',
    'VARCHAR(64) DEFAULT NULL COMMENT ''项目类型'' AFTER project_name');
CALL ensure_column('de_rectification', 'detail_content',
    'TEXT DEFAULT NULL COMMENT ''整改具体内容'' AFTER project_type');
CALL ensure_column('de_rectification', 'rectify_date',
    'DATE DEFAULT NULL COMMENT ''整改日期'' AFTER detail_content');
CALL ensure_column('de_rectification', 'responsible_person',
    'VARCHAR(64) DEFAULT NULL COMMENT ''责任人'' AFTER rectify_date');
CALL ensure_column('de_rectification', 'saving_amount',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''节能量(吨标准煤)'' AFTER estimated_cost');
CALL ensure_column('de_rectification', 'carbon_reduction',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''年减碳量(tCO2)'' AFTER saving_amount');
CALL ensure_column('de_rectification', 'economic_benefit',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''年经济效益(万元)'' AFTER carbon_reduction');

-- Sheet 20/21: 复杂目标表需要存宽表目标拆解、产品对比与年度目标。
CALL ensure_column('de_five_year_target', 'section_type',
    'VARCHAR(32) DEFAULT NULL COMMENT ''分区类型'' AFTER audit_year');
CALL ensure_column('de_five_year_target', 'year_label',
    'VARCHAR(32) DEFAULT NULL COMMENT ''年份/标签'' AFTER section_type');
CALL ensure_column('de_five_year_target', 'product_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''产品名称'' AFTER decline_rate');
CALL ensure_column('de_five_year_target', 'indicator_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''指标名称'' AFTER product_name');
CALL ensure_column('de_five_year_target', 'indicator_value',
    'DECIMAL(18,6) DEFAULT NULL COMMENT ''指标值'' AFTER indicator_name');
CALL ensure_column('de_five_year_target', 'actual_value',
    'DECIMAL(18,6) DEFAULT NULL COMMENT ''实际值'' AFTER indicator_value');
CALL ensure_column('de_five_year_target', 'target_name',
    'VARCHAR(128) DEFAULT NULL COMMENT ''目标名称'' AFTER actual_value');
CALL ensure_column('de_five_year_target', 'measurement_unit',
    'VARCHAR(64) DEFAULT NULL COMMENT ''计量单位'' AFTER target_name');
CALL ensure_column('de_five_year_target', 'y2025',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''2025年'' AFTER measurement_unit');
CALL ensure_column('de_five_year_target', 'y2026',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''2026年'' AFTER y2025');
CALL ensure_column('de_five_year_target', 'y2027',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''2027年'' AFTER y2026');
CALL ensure_column('de_five_year_target', 'y2028',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''2028年'' AFTER y2027');
CALL ensure_column('de_five_year_target', 'y2029',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''2029年'' AFTER y2028');
CALL ensure_column('de_five_year_target', 'y2030',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''2030年'' AFTER y2029');
CALL ensure_column('de_five_year_target', 'emission',
    'DECIMAL(18,4) DEFAULT NULL COMMENT ''排放量(tCO2)'' AFTER y2030');
CALL ensure_column('de_five_year_target', 'unit_strength',
    'DECIMAL(18,6) DEFAULT NULL COMMENT ''单位强度'' AFTER emission');
CALL ensure_column('de_five_year_target', 'intensity_drop',
    'DECIMAL(8,4) DEFAULT NULL COMMENT ''强度下降率(%)'' AFTER unit_strength');

-- 0427v2 行内没有 year_type 值；业务分区由 section_type 表达。
ALTER TABLE de_five_year_target
    MODIFY COLUMN year_type VARCHAR(32) DEFAULT NULL COMMENT '年份类型(旧字段，0427v2 允许为空)';

-- Sheet 20 的 carbon peak 基础信息需要按提交维度删除/重抽取。
CALL ensure_column('de_carbon_peak_info', 'submission_id',
    'BIGINT NULL AFTER id');
CALL ensure_index('de_carbon_peak_info', 'idx_submission', '(submission_id)');

-- -----------------------------------------------------------------------------
-- 2) 替换 template_version_id=33 的完整映射
-- -----------------------------------------------------------------------------

UPDATE tpl_tag_mapping
SET deleted = 1,
    deletion_source = 'USER',
    update_by = 'migration-33',
    update_time = NOW()
WHERE template_version_id = 33
  AND deleted = 0;

INSERT INTO tpl_tag_mapping (
    template_version_id, tag_name, field_name, target_table,
    data_type, dict_type, required, sheet_index, sheet_name, cell_range,
    mapping_type, source_type, row_key_column, column_mappings, header_row,
    remark, create_by, create_time, update_by, update_time, deleted
)
SELECT
    33, x.tag_name, x.field_name, x.target_table,
    x.data_type, NULL, 0, x.sheet_index, x.sheet_name, x.cell_range,
    x.mapping_type, x.source_type, x.row_key_column, x.column_mappings, x.header_row,
    x.remark, 'migration-33', NOW(), 'migration-33', NOW(), 0
FROM (
    -- Sheet 1: 十四五已实施节能技改项目（实际 11 列，存独立历史表）
    SELECT '表1_项目表' tag_name, 'de_tech_reform_history' field_name, 'de_tech_reform_history' target_table,
           'STRING' data_type, 1 sheet_index, '1.十四五已实施节能技改项目' sheet_name, 'A3:K202' cell_range,
           'TABLE' mapping_type, 'CELL_RANGE' source_type, 0 row_key_column, NULL header_row,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"project_name","label":"项目名称","type":"STRING"},{"col":2,"field":"project_type","label":"项目类型","type":"STRING"},{"col":3,"field":"main_content","label":"主要内容","type":"STRING"},{"col":4,"field":"investment","label":"投资（万）","type":"NUMBER"},{"col":5,"field":"designed_saving","label":"年节能量（吨标煤）","type":"NUMBER"},{"col":6,"field":"payback_period","label":"投资回收期（年）","type":"NUMBER"},{"col":7,"field":"completion_date","label":"完成时间","type":"STRING"},{"col":8,"field":"actual_saving","label":"实际节能量（吨标煤）","type":"NUMBER"},{"col":9,"field":"is_contract_energy","label":"是否合同能源管理模式","type":"STRING"},{"col":10,"field":"remark","label":"备注","type":"STRING"}]' column_mappings,
           'Sheet 1 实际模板包含方案外的实际节能量/合同能源管理/备注 3 列' remark
    UNION ALL
    -- Sheet 3: 企业基础信息 SCALAR（存企业设置，匹配现有双向同步）
    SELECT 'UNIT_NAME','enterpriseName','ent_enterprise_setting','STRING',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B3:单位名称（如模板含 cell tag 则抽取）'
    UNION ALL SELECT 'LEGAL_CODE','creditCode','ent_enterprise_setting','STRING',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C4:统一社会信用代码/法人代码'
    UNION ALL SELECT 'ENERGY_LEADER','energyLeaderName','ent_enterprise_setting','STRING',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B5:节能主管领导姓名/职务'
    UNION ALL SELECT 'ENERGY_DEPT','energyDeptName','ent_enterprise_setting','STRING',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B6:节能主管部门名称'
    UNION ALL SELECT 'DEPT_LEADER','energyManagerName','ent_enterprise_setting','STRING',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B7:节能管理部门负责人姓名'
    UNION ALL SELECT 'FULL_TIME_MGR','fulltimeStaffCount','de_company_overview','NUMBER',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B8:专职管理人数'
    UNION ALL SELECT 'PART_TIME_MGR','parttimeStaffCount','de_company_overview','NUMBER',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B9:兼职管理人数'
    UNION ALL SELECT 'TARGET_NAME','fiveYearTargetName','de_company_overview','STRING',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B10:十五五节能目标名称'
    UNION ALL SELECT 'TARGET_VALUE','fiveYearTargetValue','de_company_overview','NUMBER',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B11:十五五节能目标值'
    UNION ALL SELECT 'TARGET_DEPT','fiveYearTargetDept','de_company_overview','STRING',3,'3.主要技术指标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B12:目标下达部门'
    -- Sheet 3: 主要技术指标 TABLE（实际 A15:F42）
    UNION ALL SELECT '表3_技术指标','de_tech_indicator','de_tech_indicator','STRING',3,'3.主要技术指标','A15:F42','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"project_name","label":"项目名称","type":"STRING"},{"col":1,"field":"unit","label":"计量单位","type":"STRING"},{"col":2,"field":"current_year","label":"今年","type":"NUMBER"},{"col":3,"field":"prev_year","label":"去年","type":"NUMBER"},{"col":4,"field":"change_pct","label":"增减%","type":"NUMBER"},{"col":5,"field":"material_adjustment","label":"扣原料","type":"NUMBER"}]',
           '方案 header_row=12；实际数据区为 A15:F42'
    -- Sheet 4
    UNION ALL SELECT '表4_计量器具','de_meter_instrument','de_meter_instrument','STRING',4,'4.能源计量器具汇总','A3:O202','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"management_no","label":"管理编号","type":"STRING"},{"col":2,"field":"model_spec","label":"型号规格","type":"STRING"},{"col":3,"field":"manufacturer","label":"生产厂家","type":"STRING"},{"col":4,"field":"factory_no","label":"出厂编号","type":"STRING"},{"col":5,"field":"meter_name","label":"计量表名称","type":"STRING"},{"col":6,"field":"multiplier","label":"倍率","type":"NUMBER"},{"col":7,"field":"grade","label":"级别","type":"STRING"},{"col":8,"field":"energy_attribute","label":"能源类型","type":"STRING"},{"col":9,"field":"measure_range","label":"测量范围","type":"STRING"},{"col":10,"field":"department","label":"所属部门","type":"STRING"},{"col":11,"field":"accuracy_grade","label":"准确度等级","type":"STRING"},{"col":12,"field":"install_location","label":"安装位置/检定日期","type":"STRING"},{"col":13,"field":"status","label":"状态/下次检定","type":"STRING"},{"col":14,"field":"remark","label":"备注","type":"STRING"}]',NULL
    -- Sheet 5
    UNION ALL SELECT '表5_配备率','de_meter_config_rate','de_meter_config_rate','STRING',5,'5.能源计量器具配备率','A5:N17','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"energy_type","label":"能源种类","type":"STRING"},{"col":1,"field":"energy_sub_type","label":"能源子类","type":"STRING"},{"col":2,"field":"l1_standard_rate","label":"进出用能单位标准配备率","type":"NUMBER"},{"col":3,"field":"l1_required_count","label":"进出需配置数","type":"NUMBER"},{"col":4,"field":"l1_actual_count","label":"进出实际配置数","type":"NUMBER"},{"col":5,"field":"l1_actual_rate","label":"进出配备率","type":"NUMBER"},{"col":6,"field":"l2_standard_rate","label":"进出主要次级用能单位标准配备率","type":"NUMBER"},{"col":7,"field":"l2_required_count","label":"次级需配置数","type":"NUMBER"},{"col":8,"field":"l2_actual_count","label":"次级实际配置数","type":"NUMBER"},{"col":9,"field":"l2_actual_rate","label":"次级配备率","type":"NUMBER"},{"col":10,"field":"l3_standard_rate","label":"主要用能设备标准配备率","type":"NUMBER"},{"col":11,"field":"l3_required_count","label":"设备需配置数","type":"NUMBER"},{"col":12,"field":"l3_actual_count","label":"设备实际配置数","type":"NUMBER"},{"col":13,"field":"l3_actual_rate","label":"设备配备率","type":"NUMBER"}]',
           '实际模板含标准值/配备率公式列；一并映射到独立表，动态列不会遗漏'
    -- Sheet 6
    UNION ALL SELECT '表6_管理制度','de_management_policy','de_management_policy','STRING',6,'6.能源管理制度','A3:G52','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"policy_name","label":"制度名称","type":"STRING"},{"col":2,"field":"main_content","label":"主要内容","type":"STRING"},{"col":3,"field":"supervise_dept","label":"主管部门","type":"STRING"},{"col":4,"field":"publish_date","label":"颁布日期","type":"STRING"},{"col":5,"field":"valid_period","label":"有效期","type":"STRING"},{"col":6,"field":"remark","label":"备注","type":"STRING"}]',NULL
    -- Sheet 8
    UNION ALL SELECT '表8_设备汇总','de_equipment_summary','de_equipment_summary','STRING',7,'8.重点用能设备汇总管理','A3:L102','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"device_name","label":"设备名称","type":"STRING"},{"col":2,"field":"category","label":"分类","type":"STRING"},{"col":3,"field":"model","label":"型号","type":"STRING"},{"col":4,"field":"capacity","label":"容量","type":"STRING"},{"col":5,"field":"quantity","label":"数量","type":"NUMBER"},{"col":6,"field":"device_overview","label":"设备概况","type":"STRING"},{"col":7,"field":"obsolete_update_info","label":"淘汰更新情况","type":"STRING"},{"col":8,"field":"install_location","label":"安装使用场所","type":"STRING"},{"col":9,"field":"annual_runtime_hours","label":"年运行时间（小时）","type":"NUMBER"},{"col":10,"field":"energy_efficiency_level","label":"设备对标情况(能效等级)","type":"STRING"},{"col":11,"field":"remark","label":"备注","type":"STRING"}]',
           '方案缺少实际第11列设备对标情况；已补齐'
    -- Sheet 9
    UNION ALL SELECT '表9_设备测试','de_equipment_test','de_equipment_test','STRING',8,'9.重点设备测试数据','A4:K103','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"device_no","label":"设备编号","type":"STRING"},{"col":2,"field":"device_name","label":"设备名称","type":"STRING"},{"col":3,"field":"model_spec","label":"型号规格","type":"STRING"},{"col":4,"field":"test_indicator_name","label":"测试指标名称","type":"STRING"},{"col":5,"field":"measurement_unit","label":"计量单位","type":"STRING"},{"col":6,"field":"qualified_value","label":"合格值/限额","type":"STRING"},{"col":7,"field":"actual_value","label":"实测值","type":"NUMBER"},{"col":8,"field":"judgement","label":"判别","type":"STRING"},{"col":9,"field":"test_date","label":"测试日期","type":"STRING"},{"col":10,"field":"remark","label":"备注","type":"STRING"}]',NULL
    -- Sheet 10
    UNION ALL SELECT '表10_淘汰目录','de_obsolete_equipment','de_obsolete_equipment','STRING',9,'10.淘汰产品、设备、装置等目录','A3:G102','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"equipment_name","label":"淘汰设备名称","type":"STRING"},{"col":2,"field":"model_spec","label":"型号规格","type":"STRING"},{"col":3,"field":"quantity","label":"数量","type":"NUMBER"},{"col":4,"field":"start_use_date","label":"开始使用日期","type":"STRING"},{"col":5,"field":"planned_retire_date","label":"计划淘汰日期","type":"STRING"},{"col":6,"field":"remark","label":"备注","type":"STRING"}]',NULL
    -- Sheet 11
    UNION ALL SELECT '表11_能源流程图','de_energy_flow','de_energy_flow','STRING',10,'11.能源流程图（二维表）','A3:F12','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"sourceUnit","label":"源单元","type":"STRING"},{"col":1,"field":"targetUnit","label":"目的单元","type":"STRING"},{"col":2,"field":"energyProduct","label":"能源/产品","type":"STRING"},{"col":3,"field":"physicalQuantity","label":"实物量","type":"NUMBER"},{"col":4,"field":"standardQuantity","label":"折标量/价格（万元）","type":"NUMBER"},{"col":5,"field":"remark","label":"备注","type":"STRING"}]',NULL
    -- Sheet 11.1
    UNION ALL SELECT '表11_1_能源购消存','de_energy_consumption','de_energy_consumption','STRING',11,'11.1 能源购入、消费、存储','A4:I22','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"energy_name","label":"能源名称","type":"STRING"},{"col":1,"field":"measurement_unit","label":"计量单位","type":"STRING"},{"col":2,"field":"opening_stock","label":"期初库存量","type":"NUMBER"},{"col":3,"field":"purchase_total","label":"购入量","type":"NUMBER"},{"col":4,"field":"industrial_consumption","label":"消费量","type":"NUMBER"},{"col":5,"field":"transfer_out","label":"转出量","type":"NUMBER"},{"col":6,"field":"closing_stock","label":"期末库存量","type":"NUMBER"},{"col":7,"field":"gain_loss","label":"盈亏量","type":"NUMBER"},{"col":8,"field":"unit_price","label":"能源单价（元）","type":"NUMBER"}]','新增 unit_price 列'
    -- Sheet 12
    UNION ALL SELECT '表12_产品能耗','de_product_unit_consumption','de_product_unit_consumption','STRING',12,'12.单位产品能耗数据','A5:J20','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"indicator_name","label":"指标名称","type":"STRING"},{"col":1,"field":"indicator_unit","label":"计量单位/指标单位","type":"STRING"},{"col":2,"field":"numerator_unit","label":"本年分子项单位","type":"STRING"},{"col":3,"field":"denominator_unit","label":"本年分母项单位","type":"STRING"},{"col":4,"field":"unit_consumption","label":"本年指标值","type":"NUMBER"},{"col":5,"field":"energy_consumption","label":"本年分子项值","type":"NUMBER"},{"col":6,"field":"output","label":"本年分母项值","type":"NUMBER"},{"col":7,"field":"prev_unit_consumption","label":"上年指标值","type":"NUMBER"},{"col":8,"field":"prev_energy_consumption","label":"上年分子项值","type":"NUMBER"},{"col":9,"field":"prev_output","label":"上年分母项值","type":"NUMBER"}]',NULL
    -- Sheet 13
    UNION ALL SELECT '表13_能源成本','de_product_energy_cost','de_product_energy_cost','STRING',13,'13.企业产品能源成本表','A5:G102','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"product_name","label":"产品名称","type":"STRING"},{"col":2,"field":"energy_cost","label":"能源成本（万元）","type":"NUMBER"},{"col":3,"field":"production_cost","label":"生产成本（万元）","type":"NUMBER"},{"col":4,"field":"cost_ratio","label":"占该产品生产成本比例(%)","type":"NUMBER"},{"col":5,"field":"energy_total_ratio","label":"占能源总成本比例(%)","type":"NUMBER"},{"col":6,"field":"remark","label":"备注","type":"STRING"}]',NULL
    -- Sheet 14: 节能量计算数据（实际有审计年/基年两列）
    UNION ALL SELECT 'S14_ENERGY_EQUAL_CURRENT','energyEqualCurrent','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B3:审计年综合能耗等价值'
    UNION ALL SELECT 'S14_ENERGY_EQUAL_BASE','energyEqualBase','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C3:基年综合能耗等价值'
    UNION ALL SELECT 'S14_ENERGY_EQUIV_CURRENT','energyEquivCurrent','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B4:审计年综合能耗当量值'
    UNION ALL SELECT 'S14_ENERGY_EQUIV_BASE','energyEquivBase','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C4:基年综合能耗当量值'
    UNION ALL SELECT 'S14_GROSS_OUTPUT_CURRENT','grossOutputCurrent','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B5:审计年工业总产值'
    UNION ALL SELECT 'S14_GROSS_OUTPUT_BASE','grossOutputBase','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C5:基年工业总产值'
    UNION ALL SELECT 'S14_PRODUCT_OUTPUT_CURRENT','productOutputCurrent','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B6:审计年产品产量'
    UNION ALL SELECT 'S14_PRODUCT_OUTPUT_BASE','productOutputBase','de_saving_calculation','NUMBER',14,'14.节能量计算数据',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C6:基年产品产量'
    -- Sheet 15
    UNION ALL SELECT '表15_化石燃料排放','de_carbon_emission','de_carbon_emission','STRING',15,'15,温室气体排放排放汇总','A26:H33','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"source_name","label":"能源品种","type":"STRING"},{"col":1,"field":"low_heat_value","label":"收到基低位发热量","type":"NUMBER"},{"col":2,"field":"carbon_content","label":"单位热值含碳量","type":"NUMBER"},{"col":3,"field":"oxidation_rate","label":"碳氧化率","type":"NUMBER"},{"col":4,"field":"activity_data","label":"工业生产消费量","type":"NUMBER"},{"col":5,"field":"conversion_output","label":"能源加工转换产出","type":"NUMBER"},{"col":6,"field":"recovery_amount","label":"回收利用","type":"NUMBER"},{"col":7,"field":"co2_emission","label":"CO2排放量/t","type":"NUMBER"}]','emission_category 必填列未在模板中出现，当前会落通用存储或需后续默认值策略'
    UNION ALL SELECT 'HEAT_EMISSION','heatEmission','de_carbon_emission','NUMBER',15,'15,温室气体排放排放汇总',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'E39:热力排放量'
    UNION ALL SELECT 'ELEC_EMISSION','elecEmission','de_carbon_emission','NUMBER',15,'15,温室气体排放排放汇总',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'E40:电力排放量'
    UNION ALL SELECT 'GREEN_ELEC_OFFSET','greenElecOffset','de_carbon_emission','NUMBER',15,'15,温室气体排放排放汇总',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'E41:绿电抵消量'
    UNION ALL SELECT 'TOTAL_EMISSION','totalEmission','de_carbon_emission','NUMBER',15,'15,温室气体排放排放汇总',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'B14:碳排放量合计'
    UNION ALL SELECT 'DIRECT_EMISSION','directEmission','de_carbon_emission','NUMBER',15,'15,温室气体排放排放汇总',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C3:直接排放'
    UNION ALL SELECT 'INDIRECT_EMISSION','indirectEmission','de_carbon_emission','NUMBER',15,'15,温室气体排放排放汇总',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C7:C8 间接排放'
    -- Sheet 16-19
    UNION ALL SELECT '表16_节能潜力','de_saving_potential','de_saving_potential','STRING',16,'16,节能潜力明细','A3:I102','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"project_type","label":"项目类型","type":"STRING"},{"col":2,"field":"project_name","label":"项目名称","type":"STRING"},{"col":3,"field":"main_content","label":"主要内容","type":"STRING"},{"col":4,"field":"saving_potential","label":"节能潜力（吨标煤/年）","type":"NUMBER"},{"col":5,"field":"carbon_reduction","label":"减碳量（吨CO2/年）","type":"NUMBER"},{"col":6,"field":"investment","label":"投资（万元）","type":"NUMBER"},{"col":7,"field":"calc_description","label":"节能潜力计算说明","type":"STRING"},{"col":8,"field":"remark","label":"备注","type":"STRING"}]',NULL
    UNION ALL SELECT '表17_管理改进建议','de_management_suggestion','de_management_suggestion','STRING',17,'17,能源管理改进建议','A3:G101','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"project_name","label":"项目名称","type":"STRING"},{"col":2,"field":"main_content","label":"主要内容","type":"STRING"},{"col":3,"field":"investment","label":"投资（万元）","type":"NUMBER"},{"col":4,"field":"annual_saving","label":"年节能量（吨标煤）","type":"NUMBER"},{"col":5,"field":"annual_carbon_reduction","label":"年减碳量（吨CO2）","type":"NUMBER"},{"col":6,"field":"remark","label":"备注","type":"STRING"}]',NULL
    UNION ALL SELECT '表18_技改建议','de_tech_reform_suggestion','de_tech_reform_suggestion','STRING',18,'18.节能技术改造建议汇总','A3:I101','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"project_name","label":"项目名称","type":"STRING"},{"col":2,"field":"project_type","label":"项目类型","type":"STRING"},{"col":3,"field":"main_content","label":"主要内容","type":"STRING"},{"col":4,"field":"investment","label":"投资（万元）","type":"NUMBER"},{"col":5,"field":"annual_saving","label":"年节能量（吨标煤）","type":"NUMBER"},{"col":6,"field":"annual_carbon_reduction","label":"年减碳量（吨CO2）","type":"NUMBER"},{"col":7,"field":"payback_period","label":"投资回收期（年）","type":"NUMBER"},{"col":8,"field":"remark","label":"备注","type":"STRING"}]',NULL
    UNION ALL SELECT '表19_整改措施','de_rectification','de_rectification','STRING',19,'19.节能整改措施','A3:J102','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"seq_no","label":"序号","type":"NUMBER"},{"col":1,"field":"project_name","label":"整改项目名称","type":"STRING"},{"col":2,"field":"project_type","label":"项目类型","type":"STRING"},{"col":3,"field":"detail_content","label":"整改具体措施","type":"STRING"},{"col":4,"field":"rectify_date","label":"整改日期","type":"STRING"},{"col":5,"field":"responsible_person","label":"责任人","type":"STRING"},{"col":6,"field":"estimated_cost","label":"整改预计费用（万元）","type":"NUMBER"},{"col":7,"field":"saving_amount","label":"年节能量（吨标准煤）","type":"NUMBER"},{"col":8,"field":"carbon_reduction","label":"年减碳量（吨二氧化碳）","type":"NUMBER"},{"col":9,"field":"economic_benefit","label":"年经济效益（万元）","type":"NUMBER"}]',
           '方案缺少实际第9/10列年减碳量、年经济效益；已补齐'
    -- Sheet 20
    UNION ALL SELECT 'PEAK_YEAR','peakYear','de_carbon_peak_info','NUMBER',20,'20.碳达峰信息',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C3:企业碳排放达峰年'
    UNION ALL SELECT 'PEAK_EMISSION','peakEmission','de_carbon_peak_info','NUMBER',20,'20.碳达峰信息',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C4:达峰碳排放量'
    UNION ALL SELECT '表20_产品碳峰','de_five_year_target','de_five_year_target','STRING',20,'20.碳达峰信息','A18:F19','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"year_label","label":"产品类别","type":"STRING"},{"col":1,"field":"product_name","label":"产品产量","type":"NUMBER"},{"col":2,"field":"gross_output","label":"产品产值（万元）","type":"NUMBER"},{"col":3,"field":"emission","label":"排放量（吨CO2）","type":"NUMBER"},{"col":4,"field":"unit_strength","label":"单位产值碳排放强度","type":"NUMBER"},{"col":5,"field":"intensity_drop","label":"产品碳排放强度","type":"NUMBER"}]','产品表实际为 A18:F19；section_type 需后续默认值/业务处理'
    UNION ALL SELECT '表20_年度目标','de_five_year_target','de_five_year_target','STRING',20,'20.碳达峰信息','A11:H14','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"target_name","label":"目标名称","type":"STRING"},{"col":1,"field":"measurement_unit","label":"计量单位","type":"STRING"},{"col":2,"field":"y2025","label":"2025年","type":"NUMBER"},{"col":3,"field":"y2026","label":"2026年","type":"NUMBER"},{"col":4,"field":"y2027","label":"2027年","type":"NUMBER"},{"col":5,"field":"y2028","label":"2028年","type":"NUMBER"},{"col":6,"field":"y2029","label":"2029年","type":"NUMBER"},{"col":7,"field":"y2030","label":"2030年","type":"NUMBER"}]','实际含 2025 现状列，方案漏列'
    -- Sheet 21
    UNION ALL SELECT 'ACT_2025_OUTPUT','grossOutput','de_five_year_target','NUMBER',21,'21.“十五五”期间节能目标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'A4:2025年实际产值'
    UNION ALL SELECT 'ACT_2025_ENERGY_EQUIV','energyEqual','de_five_year_target','NUMBER',21,'21.“十五五”期间节能目标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C4:2025年综合能耗等价值'
    UNION ALL SELECT 'ACT_2025_ENERGY_CURR','energyEquiv','de_five_year_target','NUMBER',21,'21.“十五五”期间节能目标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'C5:2025年综合能耗当量值'
    UNION ALL SELECT 'TGT_2030_OUTPUT','grossOutput','de_five_year_target','NUMBER',21,'21.“十五五”期间节能目标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'E4:2030年目标产值'
    UNION ALL SELECT 'TGT_2030_ENERGY_EQUIV','energyEqual','de_five_year_target','NUMBER',21,'21.“十五五”期间节能目标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'G4:2030年目标综合能耗等价值'
    UNION ALL SELECT 'TGT_2030_ENERGY_CURR','energyEquiv','de_five_year_target','NUMBER',21,'21.“十五五”期间节能目标',NULL,'SCALAR','CELL_TAG',NULL,NULL,NULL,'G5:2030年目标综合能耗当量值'
    UNION ALL SELECT '表21_产品单耗','de_five_year_target','de_five_year_target','STRING',21,'21.“十五五”期间节能目标','A7:I9','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"product_name","label":"2025产品名称","type":"STRING"},{"col":1,"field":"indicator_name","label":"2025单耗指标名","type":"STRING"},{"col":2,"field":"actual_value","label":"2025单耗指标值","type":"NUMBER"},{"col":3,"field":"indicator_value","label":"2025单耗实际值","type":"NUMBER"},{"col":4,"field":"target_name","label":"2030产品名称","type":"STRING"},{"col":5,"field":"year_label","label":"2030单耗指标名","type":"STRING"},{"col":6,"field":"y2030","label":"2030单耗指标值","type":"NUMBER"},{"col":7,"field":"unit_energy_equal","label":"2030单耗实际值","type":"NUMBER"},{"col":8,"field":"decline_rate","label":"单耗指标下降率%","type":"NUMBER"}]',NULL
    UNION ALL SELECT '表21_年度节能','de_five_year_target','de_five_year_target','STRING',21,'21.“十五五”期间节能目标','A12:G16','TABLE','CELL_RANGE',0,NULL,
           '[{"col":0,"field":"target_name","label":"目标名称","type":"STRING"},{"col":1,"field":"measurement_unit","label":"计量单位","type":"STRING"},{"col":2,"field":"y2026","label":"2026年","type":"NUMBER"},{"col":3,"field":"y2027","label":"2027年","type":"NUMBER"},{"col":4,"field":"y2028","label":"2028年","type":"NUMBER"},{"col":5,"field":"y2029","label":"2029年","type":"NUMBER"},{"col":6,"field":"y2030","label":"2030年","type":"NUMBER"}]',NULL
    -- CONFIG_PREFILL / dropdowns
    UNION ALL SELECT 'PREFILL_11_1_ENERGY','config_prefill_energy_11_1','bs_energy','STRING',11,'11.1 能源购入、消费、存储','A4:B22','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"filter":{"isActive":1},"columns":[{"col":"A","field":"name"},{"col":"B","field":"measurementUnit","dropdown":false,"linkedTo":{"masterCol":"A","lookupField":"name"}}]}',NULL
    UNION ALL SELECT 'PREFILL_12_PRODUCT','config_prefill_product_12','bs_product','STRING',12,'12.单位产品能耗数据','A5:D20','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"source":"bs_product","columns":[{"col":"A","field":"name","format":"{name}单产综合能耗","locked":true},{"col":"B","field":"measurementUnit","format":"千克标准煤/{measurementUnit}","locked":true},{"col":"C","field":"name","format":"千克标准煤","locked":true,"dropdown":false},{"col":"D","field":"measurementUnit","locked":true,"dropdown":false}]}',NULL
    UNION ALL SELECT 'PREFILL_13_PRODUCT','config_prefill_product_13','bs_product','STRING',13,'13.企业产品能源成本表','B5:B20','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"columns":[{"col":"B","field":"name"}]}',NULL
    UNION ALL SELECT 'PREFILL_15_GHG','config_prefill_ghg_15','bs_energy','STRING',15,'15,温室气体排放排放汇总','A26:D33','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"filter":{"isActive":1,"attribution":"化石燃料"},"columns":[{"col":"A","field":"name"},{"col":"B","field":"lowHeatValue","dropdown":false,"linkedTo":{"masterCol":"A","lookupField":"name"}},{"col":"C","field":"carbonContent","dropdown":false,"linkedTo":{"masterCol":"A","lookupField":"name"}},{"col":"D","field":"oxidationRate","dropdown":false,"linkedTo":{"masterCol":"A","lookupField":"name"}}]}','按用户方案补 attribution=化石燃料 过滤'
    UNION ALL SELECT 'PREFILL_20_CARBON_PEAK','config_prefill_product_20','bs_product','STRING',20,'20.碳达峰信息','A18:A19','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"columns":[{"col":"A","field":"name","format":"{name}（{measurementUnit}）"}]}',NULL
    UNION ALL SELECT 'PREFILL_21_ENERGY_TARGET','config_prefill_product_21','bs_product','STRING',21,'21.“十五五”期间节能目标','A7:E9','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"columns":[{"col":"A","field":"name"},{"col":"E","field":"name"}]}',NULL
    UNION ALL SELECT 'DROPDOWN_4_ENERGY_ATTR','dropdown_energy_attr_4','bs_energy','STRING',4,'4.能源计量器具汇总','I3:I202','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"mode":"dropdown_only","filter":{"isActive":1},"columns":[{"col":"I","field":"name"}]}',NULL
    UNION ALL SELECT 'DROPDOWN_11_UNIT','dropdown_unit_11','bs_unit','STRING',10,'11.能源流程图（二维表）','A3:B12','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"mode":"dropdown_only","columns":[{"col":"A","field":"name"},{"col":"B","field":"name"}]}',NULL
    UNION ALL SELECT 'DROPDOWN_11_ENERGY_PRODUCT','dropdown_energy_product_11','bs_energy','STRING',10,'11.能源流程图（二维表）','C3:C12','CONFIG_PREFILL','CELL_TAG',NULL,NULL,
           '{"mode":"dropdown_only","filter":{"isActive":1},"columns":[{"col":"C","field":"name","extraSources":[{"table":"bs_product","field":"name"}]}]}',NULL
) x
WHERE EXISTS (SELECT 1 FROM tpl_template_version v WHERE v.id = 33 AND v.deleted = 0)
  AND NOT EXISTS (
      SELECT 1 FROM tpl_tag_mapping t
      WHERE t.template_version_id = 33
        AND t.tag_name = x.tag_name
        AND t.deleted = 0
  );
