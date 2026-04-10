-- =============================================================================
-- 10-equipment-benchmark-expansion.sql
-- Audit06: 重点用能设备能效对标表 — Schema expansion + Dictionary data
-- =============================================================================

-- ─── 1. Expand de_equipment_benchmark table with semantic columns ────────────

ALTER TABLE `de_equipment_benchmark`
  -- Discriminator: which device type this row belongs to
  ADD COLUMN `equipment_type`         VARCHAR(32)   DEFAULT NULL COMMENT '设备类型(PUMP/COMPRESSOR/FAN/CENTRAL_AC/MOTOR/BOILER/TRANSFORMER)',

  -- ── Common columns (shared across most device types) ──
  ADD COLUMN `seq_no`                 INT           DEFAULT NULL COMMENT '序号',
  ADD COLUMN `device_purpose`         VARCHAR(256)  DEFAULT NULL COMMENT '设备用途',
  ADD COLUMN `unit_count`             INT           DEFAULT NULL COMMENT '台数',
  ADD COLUMN `start_date`             VARCHAR(64)   DEFAULT NULL COMMENT '启用日期',
  ADD COLUMN `is_obsolete`            VARCHAR(16)   DEFAULT NULL COMMENT '是否为淘汰设备',
  ADD COLUMN `annual_runtime_hours`   DECIMAL(10,2) DEFAULT NULL COMMENT '年运行时间(h)',
  ADD COLUMN `factory_mgmt_no`        VARCHAR(128)  DEFAULT NULL COMMENT '工厂管理编号',
  ADD COLUMN `install_location`       VARCHAR(256)  DEFAULT NULL COMMENT '安装地点',
  ADD COLUMN `manufacture_date`       VARCHAR(64)   DEFAULT NULL COMMENT '出厂日期',

  -- ── Motor parameters (shared by PUMP/COMPRESSOR/FAN/CENTRAL_AC/MOTOR) ──
  ADD COLUMN `motor_type`             VARCHAR(128)  DEFAULT NULL COMMENT '电机种类',
  ADD COLUMN `motor_model`            VARCHAR(128)  DEFAULT NULL COMMENT '电机型号',
  ADD COLUMN `motor_rated_power`      DECIMAL(12,2) DEFAULT NULL COMMENT '电机额定功率(kW)',
  ADD COLUMN `motor_poles`            VARCHAR(32)   DEFAULT NULL COMMENT '级数',
  ADD COLUMN `motor_rated_speed`      VARCHAR(64)   DEFAULT NULL COMMENT '额定转速(r/min)',
  ADD COLUMN `motor_rated_efficiency` DECIMAL(8,2)  DEFAULT NULL COMMENT '额定效率(%)',
  ADD COLUMN `motor_cooling_method`   VARCHAR(64)   DEFAULT NULL COMMENT '冷却方式',
  ADD COLUMN `motor_efficiency_grade` VARCHAR(32)   DEFAULT NULL COMMENT '能效等级(铭牌)',
  ADD COLUMN `rated_current`          DECIMAL(12,2) DEFAULT NULL COMMENT '额定电流(A)',
  ADD COLUMN `rated_voltage`          VARCHAR(32)   DEFAULT NULL COMMENT '额定电压(U)',
  ADD COLUMN `motor_power_factor`     DECIMAL(6,4)  DEFAULT NULL COMMENT '配套电机功率因数',
  ADD COLUMN `energy_grade`           VARCHAR(32)   DEFAULT NULL COMMENT '能效等级(铭牌)-设备侧',

  -- ── PUMP-specific ──
  ADD COLUMN `pump_type`              VARCHAR(128)  DEFAULT NULL COMMENT '水泵类型',
  ADD COLUMN `pump_sub_type`          VARCHAR(128)  DEFAULT NULL COMMENT '水泵子类型',
  ADD COLUMN `pump_rated_head`        DECIMAL(10,2) DEFAULT NULL COMMENT '额定扬程(m)',
  ADD COLUMN `pump_flow_rate`         DECIMAL(12,2) DEFAULT NULL COMMENT '流量(m3/h)',
  ADD COLUMN `pump_speed`             VARCHAR(64)   DEFAULT NULL COMMENT '转速(r/min)',
  ADD COLUMN `pump_shaft_power`       DECIMAL(12,2) DEFAULT NULL COMMENT '轴功率(kW)',
  ADD COLUMN `pump_stages`            VARCHAR(32)   DEFAULT NULL COMMENT '级数(多级泵)',
  ADD COLUMN `pump_efficiency`        DECIMAL(8,2)  DEFAULT NULL COMMENT '效率(%)',
  ADD COLUMN `pump_pressure`          DECIMAL(10,4) DEFAULT NULL COMMENT '压力(MPa)',
  ADD COLUMN `pump_work_temp`         VARCHAR(64)   DEFAULT NULL COMMENT '工作温度',

  -- ── COMPRESSOR-specific ──
  ADD COLUMN `compressor_type`        VARCHAR(128)  DEFAULT NULL COMMENT '空压机类型',
  ADD COLUMN `compressor_model`       VARCHAR(128)  DEFAULT NULL COMMENT '空压机型号',
  ADD COLUMN `compressor_rated_motor_power` DECIMAL(12,2) DEFAULT NULL COMMENT '额定电机功率(kW)',
  ADD COLUMN `compressor_discharge_pressure` DECIMAL(10,4) DEFAULT NULL COMMENT '排气压力(MPa)',
  ADD COLUMN `compressor_volume_flow` DECIMAL(12,4) DEFAULT NULL COMMENT '容积流量(m3/min)',
  ADD COLUMN `compressor_specific_power` DECIMAL(12,4) DEFAULT NULL COMMENT '机组比功率(kW/(m3/min))',
  ADD COLUMN `compressor_cooling`     VARCHAR(64)   DEFAULT NULL COMMENT '冷却方式',
  ADD COLUMN `compressor_heat_recovery` VARCHAR(16) DEFAULT NULL COMMENT '是否热回收',
  ADD COLUMN `compressor_stages`      VARCHAR(32)   DEFAULT NULL COMMENT '压缩级数',
  ADD COLUMN `compressor_name`        VARCHAR(128)  DEFAULT NULL COMMENT '空压机名称',

  -- ── FAN-specific ──
  ADD COLUMN `fan_type`               VARCHAR(128)  DEFAULT NULL COMMENT '风机类型',
  ADD COLUMN `fan_model`              VARCHAR(128)  DEFAULT NULL COMMENT '风机型号',
  ADD COLUMN `fan_total_pressure`     DECIMAL(12,2) DEFAULT NULL COMMENT '全压(Pa)',
  ADD COLUMN `fan_air_volume`         DECIMAL(12,2) DEFAULT NULL COMMENT '风量(m3/h)',
  ADD COLUMN `fan_pressure_coeff`     VARCHAR(64)   DEFAULT NULL COMMENT '压力系数',
  ADD COLUMN `fan_specific_speed`     VARCHAR(64)   DEFAULT NULL COMMENT '比转速',
  ADD COLUMN `fan_size_no`            VARCHAR(64)   DEFAULT NULL COMMENT '机号',
  ADD COLUMN `fan_hub_ratio`          VARCHAR(64)   DEFAULT NULL COMMENT '轮毂比',
  ADD COLUMN `fan_efficiency`         DECIMAL(8,2)  DEFAULT NULL COMMENT '效率(%)',
  ADD COLUMN `fan_pressure_pa`        DECIMAL(12,2) DEFAULT NULL COMMENT '压力(Pa)',

  -- ── CENTRAL_AC-specific ──
  ADD COLUMN `ac_type`                VARCHAR(128)  DEFAULT NULL COMMENT '空调系统类型',
  ADD COLUMN `ac_unit_type`           VARCHAR(256)  DEFAULT NULL COMMENT '机组种类',
  ADD COLUMN `ac_model`               VARCHAR(128)  DEFAULT NULL COMMENT '型号',
  ADD COLUMN `ac_cooling_capacity`    DECIMAL(12,2) DEFAULT NULL COMMENT '名义制冷量(kW)',
  ADD COLUMN `ac_cop`                 DECIMAL(8,4)  DEFAULT NULL COMMENT '性能系数COP(W/W)',
  ADD COLUMN `ac_iplv`                DECIMAL(8,4)  DEFAULT NULL COMMENT '综合部分负荷性能系数IPLV(W/W)',
  ADD COLUMN `ac_steam_consumption`   DECIMAL(12,4) DEFAULT NULL COMMENT '单位冷量蒸汽耗量(kg/(kWh))',
  ADD COLUMN `ac_steam_pressure`      VARCHAR(64)   DEFAULT NULL COMMENT '饱和蒸汽(MPa)',

  -- ── MOTOR-specific (其他拖动系统用电机) ──
  -- (Uses shared motor_* columns; no unique columns needed beyond device_purpose)

  -- ── BOILER-specific ──
  ADD COLUMN `boiler_type`            VARCHAR(128)  DEFAULT NULL COMMENT '锅炉类型',
  ADD COLUMN `boiler_capacity`        VARCHAR(128)  DEFAULT NULL COMMENT '蒸发量D(t/h)或热功率Q(MW)',
  ADD COLUMN `boiler_fuel_type`       VARCHAR(128)  DEFAULT NULL COMMENT '燃料品种',
  ADD COLUMN `boiler_thermal_eff`     DECIMAL(8,2)  DEFAULT NULL COMMENT '热效率(%)',
  ADD COLUMN `boiler_heat_recovery`   VARCHAR(16)   DEFAULT NULL COMMENT '是否进行烟气余热回收',
  ADD COLUMN `boiler_rated_pressure`  DECIMAL(10,4) DEFAULT NULL COMMENT '额定工作压力(MPa)',
  ADD COLUMN `boiler_rated_temp`      DECIMAL(8,2)  DEFAULT NULL COMMENT '额定工作温度(℃)',
  ADD COLUMN `boiler_inlet_temp`      DECIMAL(8,2)  DEFAULT NULL COMMENT '额定进水温度(℃)',
  ADD COLUMN `boiler_fuel_consumption` DECIMAL(12,4) DEFAULT NULL COMMENT '燃料消耗量(kg/h)',
  ADD COLUMN `boiler_fuel_heat_value` DECIMAL(12,4) DEFAULT NULL COMMENT '燃料低位发热量(kJ/kg)',
  ADD COLUMN `boiler_exhaust_temp`    DECIMAL(8,2)  DEFAULT NULL COMMENT '锅炉排烟温度(℃)',

  -- ── TRANSFORMER-specific ──
  ADD COLUMN `transformer_voltage`    VARCHAR(32)   DEFAULT NULL COMMENT '电压等级(kV)',
  ADD COLUMN `transformer_type`       VARCHAR(256)  DEFAULT NULL COMMENT '变压器类型',
  ADD COLUMN `transformer_capacity`   DECIMAL(12,2) DEFAULT NULL COMMENT '变压器容量(KVA)',
  ADD COLUMN `transformer_core_material` VARCHAR(64) DEFAULT NULL COMMENT '铁芯材质',
  ADD COLUMN `transformer_insulation` VARCHAR(32)   DEFAULT NULL COMMENT '绝缘等级',
  ADD COLUMN `transformer_connection` VARCHAR(64)   DEFAULT NULL COMMENT '联结组别',
  ADD COLUMN `transformer_no_load_loss` DECIMAL(12,2) DEFAULT NULL COMMENT '空载损耗(W)',
  ADD COLUMN `transformer_load_loss`  DECIMAL(12,2) DEFAULT NULL COMMENT '负载损耗(W)',
  ADD COLUMN `transformer_model`      VARCHAR(128)  DEFAULT NULL COMMENT '产品型号',
  ADD COLUMN `transformer_grade_ref`  VARCHAR(256)  DEFAULT NULL COMMENT '能效等级参考标准';


-- ─── 2. Insert new dictionary types ─────────────────────────────────────────

-- 2.1 水泵类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('水泵类型', 'pump_type', '0', 'admin', '重点用能设备-水泵类型下拉');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('pump_type', '单级单吸清水离心泵', 'single_stage_centrifugal', 1, '0', 'admin'),
('pump_type', '多级单吸清水离心泵', 'multi_stage_centrifugal', 2, '0', 'admin'),
('pump_type', '混流泵', 'mixed_flow', 3, '0', 'admin'),
('pump_type', '轴流泵', 'axial_flow', 4, '0', 'admin'),
('pump_type', '潜水泵', 'submersible', 5, '0', 'admin'),
('pump_type', '井用潜水泵', 'well_submersible', 6, '0', 'admin'),
('pump_type', '其他', 'other', 99, '0', 'admin');

-- 2.2 空压机类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('空压机类型', 'compressor_type', '0', 'admin', '重点用能设备-空压机类型下拉');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('compressor_type', '喷油回转空气压缩机', 'oil_injected_rotary', 1, '0', 'admin'),
('compressor_type', '变转速喷油回转空气压缩机', 'variable_speed_oil_rotary', 2, '0', 'admin'),
('compressor_type', '往复活塞空气压缩机', 'reciprocating_piston', 3, '0', 'admin'),
('compressor_type', '其他', 'other', 99, '0', 'admin');

-- 2.3 风机类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('风机类型', 'fan_type', '0', 'admin', '重点用能设备-风机类型下拉');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('fan_type', '离心通风机', 'centrifugal', 1, '0', 'admin'),
('fan_type', '轴流通风机', 'axial', 2, '0', 'admin'),
('fan_type', '外转子电动机直联传动型式的前向多翼离心通风机', 'forward_multi_wing', 3, '0', 'admin');

-- 2.4 风机机号 (per fan_type)
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('离心通风机机号', 'fan_size_centrifugal', '0', 'admin', '离心通风机机号选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('fan_size_centrifugal', 'No2＜机号≤No2.5', 'No2_to_No2.5', 1, '0', 'admin'),
('fan_size_centrifugal', 'No2.5＜机号≤No3.5', 'No2.5_to_No3.5', 2, '0', 'admin'),
('fan_size_centrifugal', 'No3.5＜机号≤No4.5', 'No3.5_to_No4.5', 3, '0', 'admin'),
('fan_size_centrifugal', 'No4.5＜机号≤No7', 'No4.5_to_No7', 4, '0', 'admin'),
('fan_size_centrifugal', 'No7＜机号≤No10', 'No7_to_No10', 5, '0', 'admin'),
('fan_size_centrifugal', '机号＞No10', 'gt_No10', 6, '0', 'admin'),
('fan_size_centrifugal', 'No2＜机号＜ No5(0.25≤压力系数＜0.95)', 'No2_to_No5_low', 7, '0', 'admin'),
('fan_size_centrifugal', 'No5≤机号＜No10(0.25≤压力系数＜0.95)', 'No5_to_No10_low', 8, '0', 'admin'),
('fan_size_centrifugal', '机号≥No10(0.25≤压力系数＜0.95)', 'ge_No10_low', 9, '0', 'admin');

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('前向多翼离心通风机机号', 'fan_size_forward', '0', 'admin', '前向多翼离心通风机机号选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('fan_size_forward', '机号≤ No2', 'le_No2', 1, '0', 'admin'),
('fan_size_forward', 'No2＜机号≤ No2.5', 'No2_to_No2.5', 2, '0', 'admin'),
('fan_size_forward', 'No2.5＜机号≤ No3.5', 'No2.5_to_No3.5', 3, '0', 'admin'),
('fan_size_forward', 'No3.5＜机号≤ No4.5', 'No3.5_to_No4.5', 4, '0', 'admin'),
('fan_size_forward', '机号＞No4.5', 'gt_No4.5', 5, '0', 'admin');

INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('轴流通风机机号', 'fan_size_axial', '0', 'admin', '轴流通风机机号选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('fan_size_axial', 'No2.5≤机号＜ No5', 'No2.5_to_No5', 1, '0', 'admin'),
('fan_size_axial', 'No5≤机号＜No10', 'No5_to_No10', 2, '0', 'admin'),
('fan_size_axial', '机号≥No10', 'ge_No10', 3, '0', 'admin');

-- 2.5 离心通风机压力系数
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('离心通风机压力系数', 'fan_pressure_coeff', '0', 'admin', '离心通风机压力系数选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('fan_pressure_coeff', '1.35≤ψ＜1.55', '1.35_to_1.55', 1, '0', 'admin'),
('fan_pressure_coeff', '1.05≤ψ＜1.35', '1.05_to_1.35', 2, '0', 'admin'),
('fan_pressure_coeff', '0.95≤ψ＜1.05', '0.95_to_1.05', 3, '0', 'admin'),
('fan_pressure_coeff', '0.85≤ψ＜0.95', '0.85_to_0.95', 4, '0', 'admin'),
('fan_pressure_coeff', '0.75≤ψ＜0.85', '0.75_to_0.85', 5, '0', 'admin'),
('fan_pressure_coeff', '0.65≤ψ＜0.75', '0.65_to_0.75', 6, '0', 'admin'),
('fan_pressure_coeff', '0.55≤ψ＜0.65', '0.55_to_0.65', 7, '0', 'admin'),
('fan_pressure_coeff', '0.45≤ψ＜0.55', '0.45_to_0.55', 8, '0', 'admin'),
('fan_pressure_coeff', '0.35≤ψ＜0.45', '0.35_to_0.45', 9, '0', 'admin'),
('fan_pressure_coeff', '0.25≤ψ＜0.35', '0.25_to_0.35', 10, '0', 'admin');

-- 2.6 前向多翼离心通风机压力系数
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('前向多翼离心通风机压力系数', 'fan_pressure_coeff_forward', '0', 'admin', '前向多翼离心通风机压力系数选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('fan_pressure_coeff_forward', '1.0≤ψ＜1.1', '1.0_to_1.1', 1, '0', 'admin'),
('fan_pressure_coeff_forward', '1.1≤ψ＜1.2', '1.1_to_1.2', 2, '0', 'admin'),
('fan_pressure_coeff_forward', '1.2≤ψ＜1.3', '1.2_to_1.3', 3, '0', 'admin'),
('fan_pressure_coeff_forward', '1.3≤ψ＜1.4', '1.3_to_1.4', 4, '0', 'admin'),
('fan_pressure_coeff_forward', '1.4≤ψ', 'ge_1.4', 5, '0', 'admin');

-- 2.7 轮毂比
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('轴流通风机轮毂比', 'fan_hub_ratio', '0', 'admin', '轴流通风机轮毂比选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('fan_hub_ratio', 'γ＜ 0.3', 'lt_0.3', 1, '0', 'admin'),
('fan_hub_ratio', '0.3≤γ＜ 0.4', '0.3_to_0.4', 2, '0', 'admin'),
('fan_hub_ratio', '0.4≤γ＜ 0.55', '0.4_to_0.55', 3, '0', 'admin'),
('fan_hub_ratio', '0.55≤γ＜ 0.75', '0.55_to_0.75', 4, '0', 'admin');

-- 2.8 空调系统类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('空调系统类型', 'ac_system_type', '0', 'admin', '重点用能设备-空调系统类型下拉');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('ac_system_type', '风冷式或蒸发冷却式', 'air_cooled', 1, '0', 'admin'),
('ac_system_type', '水冷式', 'water_cooled', 2, '0', 'admin'),
('ac_system_type', '多联热泵', 'multi_heat_pump', 3, '0', 'admin');

-- 2.9 空调机组种类
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('空调机组种类', 'ac_unit_type', '0', 'admin', '重点用能设备-空调机组种类下拉');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('ac_unit_type', '溴化锂吸收式冷水机组', 'libr_absorption', 1, '0', 'admin'),
('ac_unit_type', '溴化锂蒸汽型', 'libr_steam', 2, '0', 'admin'),
('ac_unit_type', '溴化锂直燃式', 'libr_direct_fired', 3, '0', 'admin'),
('ac_unit_type', 'GB2024蒸气压缩循环冷水(热泵)机组', 'gb2024_vapor_compression', 4, '0', 'admin'),
('ac_unit_type', '舒适型水冷式', 'comfort_water_cooled', 5, '0', 'admin'),
('ac_unit_type', '舒适型风冷式', 'comfort_air_cooled', 6, '0', 'admin'),
('ac_unit_type', '舒适型蒸发冷却式', 'comfort_evaporative', 7, '0', 'admin'),
('ac_unit_type', '数据中心专用型水冷式', 'datacenter_water', 8, '0', 'admin'),
('ac_unit_type', '数据中心专用型风冷式', 'datacenter_air', 9, '0', 'admin'),
('ac_unit_type', 'GB2024低环境温度空气源热泵(冷水)机组', 'gb2024_low_temp_air_hp', 10, '0', 'admin'),
('ac_unit_type', '地板采暖型', 'floor_heating', 11, '0', 'admin'),
('ac_unit_type', '风机盘管型', 'fan_coil', 12, '0', 'admin'),
('ac_unit_type', '散热器型', 'radiator', 13, '0', 'admin'),
('ac_unit_type', 'GB2024水(地)源热泵机组', 'gb2024_water_ground_hp', 14, '0', 'admin'),
('ac_unit_type', '冷热风型-热泵型-水环式', 'cold_hot_air_hp_water_loop', 15, '0', 'admin'),
('ac_unit_type', '冷热风型-热泵型-地下水式', 'cold_hot_air_hp_groundwater', 16, '0', 'admin'),
('ac_unit_type', '冷热风型-热泵型-地埋管式、地表水式', 'cold_hot_air_hp_ground_surface', 17, '0', 'admin'),
('ac_unit_type', '冷热水型-单热型-水环式', 'cold_hot_water_single_water_loop', 18, '0', 'admin'),
('ac_unit_type', '冷热水型-单热型-地下水式', 'cold_hot_water_single_groundwater', 19, '0', 'admin'),
('ac_unit_type', '冷热水型-单热型-地埋管式、地表水式', 'cold_hot_water_single_ground_surface', 20, '0', 'admin'),
('ac_unit_type', '冷热水型-热泵型-水环式', 'cold_hot_water_hp_water_loop', 21, '0', 'admin'),
('ac_unit_type', '冷热水型-热泵型-地下水式', 'cold_hot_water_hp_groundwater', 22, '0', 'admin'),
('ac_unit_type', '冷热水型-地埋管式、地表水式', 'cold_hot_water_ground_surface', 23, '0', 'admin'),
('ac_unit_type', 'GB2024溴化锂吸收式冷(温)水机组', 'gb2024_libr', 24, '0', 'admin'),
('ac_unit_type', '饱和蒸汽压力0.4MPa', 'libr_steam_0.4', 25, '0', 'admin'),
('ac_unit_type', '饱和蒸汽压力0.6MPa', 'libr_steam_0.6', 26, '0', 'admin'),
('ac_unit_type', '饱和蒸汽压力0.8MPa', 'libr_steam_0.8', 27, '0', 'admin'),
('ac_unit_type', '直燃型机组', 'direct_fired', 28, '0', 'admin'),
('ac_unit_type', 'GB2024蒸气压缩循环高温热泵机组', 'gb2024_high_temp_hp', 29, '0', 'admin'),
('ac_unit_type', 'H1a', 'H1a', 30, '0', 'admin'),
('ac_unit_type', 'H2a', 'H2a', 31, '0', 'admin'),
('ac_unit_type', 'H3a', 'H3a', 32, '0', 'admin'),
('ac_unit_type', 'H4a', 'H4a', 33, '0', 'admin'),
('ac_unit_type', 'H5a', 'H5a', 34, '0', 'admin'),
('ac_unit_type', 'H1b', 'H1b', 35, '0', 'admin'),
('ac_unit_type', 'H2b', 'H2b', 36, '0', 'admin'),
('ac_unit_type', 'H3b', 'H3b', 37, '0', 'admin'),
('ac_unit_type', 'H4b', 'H4b', 38, '0', 'admin'),
('ac_unit_type', 'H5b', 'H5b', 39, '0', 'admin'),
('ac_unit_type', '循环供水式热泵高温热水机组', 'circulating_hot_water_hp', 40, '0', 'admin'),
('ac_unit_type', 'GB2024间接蒸发冷却冷水机组', 'gb2024_indirect_evaporative', 41, '0', 'admin'),
('ac_unit_type', '数据中心等类似场所用-标准机型', 'datacenter_standard', 42, '0', 'admin'),
('ac_unit_type', '数据中心等类似场所用-大温差型', 'datacenter_large_diff', 43, '0', 'admin'),
('ac_unit_type', '外冷式', 'external_cooling', 44, '0', 'admin'),
('ac_unit_type', '内冷式', 'internal_cooling', 45, '0', 'admin'),
('ac_unit_type', '内外冷串联式', 'internal_external_series', 46, '0', 'admin'),
('ac_unit_type', 'GB2024一体式冷水(热泵)机组', 'gb2024_integrated', 47, '0', 'admin'),
('ac_unit_type', '蒸发冷却式冷却塔式', 'evaporative_cooling_tower', 48, '0', 'admin'),
('ac_unit_type', '风冷式', 'air_cooled_unit', 49, '0', 'admin');

-- 2.10 锅炉类型
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('锅炉类型', 'boiler_type', '0', 'admin', '重点用能设备-锅炉类型下拉');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('boiler_type', 'D≤10 t/h（或 Q≤7MW)', 'small', 1, '0', 'admin'),
('boiler_type', '20≥D>10 t/h(或 14≥Q>7 MW)', 'medium', 2, '0', 'admin'),
('boiler_type', 'D>20t/h（或 Q>14 MW)', 'large', 3, '0', 'admin');

-- 2.11 变压器电压等级
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('变压器电压等级', 'transformer_voltage', '0', 'admin', '重点用能设备-变压器电压等级下拉');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('transformer_voltage', '6kV', '6kV', 1, '0', 'admin'),
('transformer_voltage', '10kV', '10kV', 2, '0', 'admin'),
('transformer_voltage', '35kV', '35kV', 3, '0', 'admin'),
('transformer_voltage', '66kV', '66kV', 4, '0', 'admin'),
('transformer_voltage', '110kV', '110kV', 5, '0', 'admin'),
('transformer_voltage', '220kV', '220kV', 6, '0', 'admin'),
('transformer_voltage', '330kV', '330kV', 7, '0', 'admin'),
('transformer_voltage', '500kV', '500kV', 8, '0', 'admin');

-- 2.12 变压器类型 (per voltage level — uses config table from user)
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('变压器类型', 'transformer_type', '0', 'admin', '重点用能设备-变压器类型下拉(全部)');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('transformer_type', '油浸式三相双绕组无励磁调压新能源发电侧光伏用、风电用、储能用变压器', 'renewable_oil_3p2w', 1, '0', 'admin'),
('transformer_type', '干式三相双绕组无励磁调压新能源发电侧光伏用、风电用、储能用变压器', 'renewable_dry_3p2w', 2, '0', 'admin'),
('transformer_type', '油浸式三相双绕组无励磁调压配电变压器', 'oil_3p2w_distribution', 3, '0', 'admin'),
('transformer_type', '干式三相双绕组无励磁调压配电变压器', 'dry_3p2w_distribution', 4, '0', 'admin'),
('transformer_type', '油浸式三相双绕组无励磁调压电力变压器', 'oil_3p2w_power_noload', 5, '0', 'admin'),
('transformer_type', '油浸式三相双绕组有载调压电力变压器', 'oil_3p2w_power_ontap', 6, '0', 'admin'),
('transformer_type', '油浸式三相三绕组无励磁调压电力变压器', 'oil_3p3w_power_noload', 7, '0', 'admin'),
('transformer_type', '油浸式三相三绕组有载调压电力变压器', 'oil_3p3w_power_ontap', 8, '0', 'admin'),
('transformer_type', '油浸式三相双绕组低压为35kV无励磁调压电力变压器', 'oil_3p2w_35kv_noload', 9, '0', 'admin'),
('transformer_type', '油浸式三相双绕组低压为66kV无励磁调压电力变压器', 'oil_3p2w_66kv_noload', 10, '0', 'admin'),
('transformer_type', '油浸式三相三绕组无励磁调压变压器', 'oil_3p3w_noload', 11, '0', 'admin'),
('transformer_type', '油浸式三相三绕组有载调压自耦电力变压器', 'oil_3p3w_auto_ontap', 12, '0', 'admin'),
('transformer_type', '油浸式三相三绕组无励磁调压自耦电力变压器(串联绕组末端调压，中压110kV)', 'oil_3p3w_auto_noload_110', 13, '0', 'admin'),
('transformer_type', '油浸式三相三绕组有载调压自耦电力变压器(串联绕组末端调压，中压110kV)', 'oil_3p3w_auto_ontap_110', 14, '0', 'admin'),
('transformer_type', '油浸式三相三绕组有载调压自耦电力变压器(中压110kV线端调压)', 'oil_3p3w_auto_ontap_110_line', 15, '0', 'admin'),
('transformer_type', '油浸式三相三绕组无励磁调压自耦电力变压器能效等级(中压220kV线端调压)', 'oil_3p3w_auto_noload_220', 16, '0', 'admin'),
('transformer_type', '油浸式三相三绕组有载调压自耦电力变压器能效等级(中压220kV线端调压)', 'oil_3p3w_auto_ontap_220', 17, '0', 'admin'),
('transformer_type', '油浸式单相双绕组无励磁调压电力变压器', 'oil_1p2w_power_noload', 18, '0', 'admin'),
('transformer_type', '油浸式三相双绕组无励磁调压电力变压器(500kV)', 'oil_3p2w_500kv_noload', 19, '0', 'admin'),
('transformer_type', '油浸式单相三绕组无励磁调压自耦电力变压器(中压线端调压)', 'oil_1p3w_auto_noload', 20, '0', 'admin'),
('transformer_type', '油浸式单相三绕组有载调压自耦电力变压器(中压线端调压)', 'oil_1p3w_auto_ontap', 21, '0', 'admin');

-- 2.13 变压器铁芯材质
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('变压器铁芯材质', 'transformer_core_material', '0', 'admin', '变压器铁芯材质(仅10kV)');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('transformer_core_material', '电工钢带', 'silicon_steel', 1, '0', 'admin'),
('transformer_core_material', '非晶合金', 'amorphous_alloy', 2, '0', 'admin');

-- 2.14 变压器联结组别
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('变压器联结组别', 'transformer_connection', '0', 'admin', '变压器联结组别(仅10kV油浸式)');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('transformer_connection', 'Dyn11/Yzn11', 'Dyn11_Yzn11', 1, '0', 'admin'),
('transformer_connection', 'Yyn0', 'Yyn0', 2, '0', 'admin');

-- 2.15 变压器绝缘等级
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('变压器绝缘等级', 'transformer_insulation', '0', 'admin', '变压器绝缘等级(仅10kV干式)');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('transformer_insulation', 'B', 'B', 1, '0', 'admin'),
('transformer_insulation', 'F', 'F', 2, '0', 'admin'),
('transformer_insulation', 'H', 'H', 3, '0', 'admin');

-- 2.16 是/否选项
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('是否选项', 'yes_no', '0', 'admin', '通用是/否下拉选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('yes_no', '是', 'yes', 1, '0', 'admin'),
('yes_no', '否', 'no', 2, '0', 'admin');

-- 2.17 能效等级
INSERT INTO `sys_dict_type` (`dict_name`, `dict_type`, `status`, `create_by`, `remark`)
VALUES ('能效等级', 'energy_efficiency_grade', '0', 'admin', '通用能效等级下拉选项');

INSERT INTO `sys_dict_data` (`dict_type`, `dict_label`, `dict_value`, `dict_sort`, `status`, `create_by`) VALUES
('energy_efficiency_grade', '1级', '1', 1, '0', 'admin'),
('energy_efficiency_grade', '2级', '2', 2, '0', 'admin'),
('energy_efficiency_grade', '3级', '3', 3, '0', 'admin'),
('energy_efficiency_grade', '4级', '4', 4, '0', 'admin'),
('energy_efficiency_grade', '5级', '5', 5, '0', 'admin');
