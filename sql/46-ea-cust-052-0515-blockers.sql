-- EA-CUST-052: 0515 template blocker cleanup
-- - Extend Sheet 7 重点设备能耗和效率 persistence to official A:Q columns.
-- - Add product-level Sheet12-driven saving calculation detail persistence.
-- - Update SJS/Railway 0515 template tag mapping for Sheet 7.

SELECT id INTO @ea052_template_version_id
FROM tpl_template_version
WHERE template_id = (SELECT id FROM tpl_template WHERE template_code = 'Energy_Audit_0515' AND deleted = 0 LIMIT 1)
  AND version = 1
  AND deleted = 0
LIMIT 1;

SET @ea052_template_version_id = COALESCE(@ea052_template_version_id, 43);

CALL ensure_column('de_equipment_energy', 'nameplate_output', 'VARCHAR(128) NULL COMMENT ''铭牌出力'' AFTER model_spec');
CALL ensure_column('de_equipment_energy', 'main_energy_name', 'VARCHAR(128) NULL COMMENT ''主要能源名称'' AFTER nameplate_output');
CALL ensure_column('de_equipment_energy', 'main_energy_consumption', 'DECIMAL(18,4) NULL COMMENT ''主要能源消费量'' AFTER main_energy_name');
CALL ensure_column('de_equipment_energy', 'avg_operating_efficiency', 'DECIMAL(18,6) NULL COMMENT ''平均运行效率'' AFTER main_energy_consumption');
CALL ensure_column('de_equipment_energy', 'residual_heat_energy', 'DECIMAL(18,4) NULL COMMENT ''余热余能量'' AFTER avg_operating_efficiency');
CALL ensure_column('de_equipment_energy', 'available_residual_heat_energy', 'DECIMAL(18,4) NULL COMMENT ''可回收余热余能量'' AFTER residual_heat_energy');
CALL ensure_column('de_equipment_energy', 'utilized_residual_heat_energy', 'DECIMAL(18,4) NULL COMMENT ''已利用余热余能量'' AFTER available_residual_heat_energy');
CALL ensure_column('de_equipment_energy', 'recovery_utilization_rate', 'DECIMAL(18,6) NULL COMMENT ''回收利用率'' AFTER utilized_residual_heat_energy');
CALL ensure_column('de_equipment_energy', 'statistical_load_rate', 'DECIMAL(18,6) NULL COMMENT ''统计负荷率'' AFTER recovery_utilization_rate');
CALL ensure_column('de_equipment_energy', 'test_efficiency', 'DECIMAL(18,6) NULL COMMENT ''测试效率'' AFTER statistical_load_rate');
CALL ensure_column('de_equipment_energy', 'flue_gas_loss_rate', 'DECIMAL(18,6) NULL COMMENT ''排烟热损失率'' AFTER test_efficiency');
CALL ensure_column('de_equipment_energy', 'heat_loss_rate', 'DECIMAL(18,6) NULL COMMENT ''散热损失率'' AFTER flue_gas_loss_rate');
CALL ensure_column('de_equipment_energy', 'other_loss', 'DECIMAL(18,6) NULL COMMENT ''其他损失'' AFTER heat_loss_rate');
CALL ensure_column('de_equipment_energy', 'test_date', 'DATE NULL COMMENT ''测试日期'' AFTER other_loss');

CREATE TABLE IF NOT EXISTS de_saving_calculation_detail (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    submission_id     BIGINT        NOT NULL,
    enterprise_id     BIGINT        NOT NULL,
    audit_year        INT           NOT NULL,
    product_seq       INT           NULL COMMENT '产品序号',
    row_type          VARCHAR(32)   NULL COMMENT 'OUTPUT/UNIT_CONSUMPTION',
    row_label         VARCHAR(256)  NULL COMMENT '节能量计算行名称',
    product_name      VARCHAR(128)  NULL COMMENT '产品名称',
    measurement_unit  VARCHAR(64)   NULL COMMENT '单位',
    current_value     DECIMAL(18,6) NULL COMMENT '审计期值',
    base_value        DECIMAL(18,6) NULL COMMENT '基准期值',
    create_by         VARCHAR(64)   NULL,
    create_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64)   NULL,
    update_time       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_submission (submission_id),
    KEY idx_enterprise_year (enterprise_id, audit_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节能量计算产品级明细';

UPDATE tpl_tag_mapping
SET cell_range = 'A3:Q52',
    column_mappings = '[{"col":0,"field":"seq_no","type":"NUMBER"},{"col":1,"field":"device_name","type":"STRING"},{"col":2,"field":"model_spec","type":"STRING"},{"col":3,"field":"nameplate_output","type":"STRING"},{"col":4,"field":"main_energy_name","type":"STRING"},{"col":5,"field":"main_energy_consumption","type":"NUMBER"},{"col":6,"field":"avg_operating_efficiency","type":"NUMBER"},{"col":7,"field":"residual_heat_energy","type":"NUMBER"},{"col":8,"field":"available_residual_heat_energy","type":"NUMBER"},{"col":9,"field":"utilized_residual_heat_energy","type":"NUMBER"},{"col":10,"field":"recovery_utilization_rate","type":"NUMBER"},{"col":11,"field":"statistical_load_rate","type":"NUMBER"},{"col":12,"field":"test_efficiency","type":"NUMBER"},{"col":13,"field":"flue_gas_loss_rate","type":"NUMBER"},{"col":14,"field":"heat_loss_rate","type":"NUMBER"},{"col":15,"field":"other_loss","type":"NUMBER"},{"col":16,"field":"test_date","type":"DATE"}]',
    update_by = 'EA-CUST-052',
    update_time = NOW()
WHERE template_version_id = @ea052_template_version_id
  AND tag_name = '0511_表7_重点设备能耗和效率'
  AND deleted = 0;

UPDATE tpl_tag_mapping
SET deleted = 1,
    deletion_source = 'EA052',
    update_by = 'EA-CUST-052',
    update_time = NOW()
WHERE template_version_id = @ea052_template_version_id
  AND tag_name = '0511_表27_重点设备终端用户'
  AND field_name = 'de_equipment_energy'
  AND deleted = 0;
