export interface FieldSchema {
  label: string
  type: 'STRING' | 'NUMBER' | 'DATE' | 'DICT' | 'TEXT' | 'DECIMAL'
}

export interface TableSchema {
  label: string
  fields: Record<string, FieldSchema>
}

export const SCHEMA_REGISTRY: Record<string, TableSchema> = {
  ent_enterprise_setting: {
    label: '企业概况（企业设置表）',
    fields: {
      region:                 { label: '所属地区', type: 'STRING' },
      industryField:          { label: '所属领域', type: 'STRING' },
      industryName:           { label: '行业分类名称', type: 'STRING' },
      unitNature:             { label: '单位类型', type: 'STRING' },
      registeredDate:         { label: '单位注册日期', type: 'DATE' },
      registeredCapital:      { label: '注册资本（万元）', type: 'DECIMAL' },
      legalRepresentative:    { label: '法定代表人姓名', type: 'STRING' },
      legalPhone:             { label: '法定代表人联系电话', type: 'STRING' },
      isCentralEnterprise:    { label: '是否央企', type: 'NUMBER' },
      groupName:              { label: '所属集团名称', type: 'STRING' },
      enterpriseAddress:      { label: '单位地址', type: 'STRING' },
      postalCode:             { label: '邮政编码', type: 'STRING' },
      adminDivisionCode:      { label: '行政区划代码', type: 'STRING' },
      enterpriseEmail:        { label: '电子邮箱', type: 'STRING' },
      fax:                    { label: '传真（区号）', type: 'STRING' },
      energyMgmtOrg:          { label: '能源管理机构名称', type: 'STRING' },
      energyLeaderName:       { label: '节能领导姓名', type: 'STRING' },
      energyLeaderPhone:      { label: '节能领导联系电话', type: 'STRING' },
      energyManagerName:      { label: '能源管理负责人', type: 'STRING' },
      energyManagerMobile:    { label: '负责人手机', type: 'STRING' },
      energyManagerCert:      { label: '能源管理师证号', type: 'STRING' },
      energyDeptLeaderPhone:  { label: '能源部门负责人电话', type: 'STRING' },
      energyCert:             { label: '是否通过能源管理体系认证', type: 'NUMBER' },
      certPassDate:           { label: '认证通过日期', type: 'DATE' },
      certAuthority:          { label: '认证机构', type: 'STRING' },
      hasEnergyCenter:        { label: '是否建设能源管理中心', type: 'NUMBER' },
      enterpriseContact:      { label: '企业联系人', type: 'STRING' },
      enterpriseMobile:       { label: '企业联系手机', type: 'STRING' },
      compilerContact:        { label: '编制人联系人', type: 'STRING' },
      compilerName:           { label: '编制人姓名', type: 'STRING' },
      compilerMobile:         { label: '编制人手机', type: 'STRING' },
      compilerEmail:          { label: '编制人邮箱', type: 'STRING' },
      industryCategory:       { label: '行业大类', type: 'STRING' },
      industryCode:           { label: '行业代码', type: 'STRING' },
      superiorDepartment:     { label: '上级主管部门', type: 'STRING' },
      energyEnterpriseType:   { label: '用能企业类型', type: 'STRING' },
      remark:                 { label: '备注', type: 'TEXT' },
    },
  },

  de_tech_indicator: {
    label: '主要技术指标',
    fields: {
      indicator_year:             { label: '指标年份', type: 'NUMBER' },
      gross_output:               { label: '工业总产值(万元)', type: 'DECIMAL' },
      sales_revenue:              { label: '销售收入(万元)', type: 'DECIMAL' },
      tax_paid:                   { label: '上缴利税(万元)', type: 'DECIMAL' },
      energy_total_cost:          { label: '能源总成本(万元)', type: 'DECIMAL' },
      production_cost:            { label: '生产成本(万元)', type: 'DECIMAL' },
      energy_cost_ratio:          { label: '能源成本占比(%)', type: 'DECIMAL' },
      total_energy_equiv:         { label: '综合能耗当量值(吨标煤)', type: 'DECIMAL' },
      total_energy_equal:         { label: '综合能耗等价值(吨标煤)', type: 'DECIMAL' },
      total_energy_excl_material: { label: '综合能耗剔除原料(吨标煤)', type: 'DECIMAL' },
      unit_output_energy:         { label: '单位产值综合能耗(当量值)', type: 'DECIMAL' },
      unit_output_energy_equal:   { label: '单位产值综合能耗(等价值)', type: 'DECIMAL' },
      saving_project_count:       { label: '节能项目数', type: 'NUMBER' },
      saving_invest_total:        { label: '投资总额(万元)', type: 'DECIMAL' },
      saving_capacity:            { label: '节能能力(吨标煤)', type: 'DECIMAL' },
      saving_benefit:             { label: '经济效益(万元)', type: 'DECIMAL' },
      coal_target:                { label: '本期考核指标(等价值)', type: 'DECIMAL' },
      coal_actual:                { label: '本期考核指标(强度等价值)', type: 'DECIMAL' },
      employee_count:             { label: '从业人员(人)', type: 'NUMBER' },
      energy_manager_count:       { label: '能源管理师人数(人)', type: 'NUMBER' },
      total_energy_equiv_excl_green: { label: '综合能耗扣除绿电(当量值)(吨标煤)', type: 'DECIMAL' },
      total_energy_equal_excl_green: { label: '综合能耗扣除绿电(等价值)(吨标煤)', type: 'DECIMAL' },
      raw_material_energy:        { label: '原材料用能(吨标煤)', type: 'DECIMAL' },
      electrification_rate:       { label: '电气化率(%)', type: 'DECIMAL' },
      total_energy_equal_excl_material: { label: '综合能耗扣除原料(等价值)(吨标煤)', type: 'DECIMAL' },
    },
  },

  de_energy_saving_project: {
    label: '已实施节能技改项目',
    fields: {
      project_name:       { label: '项目名称', type: 'STRING' },
      main_content:       { label: '主要内容', type: 'TEXT' },
      investment:         { label: '投资(万元)', type: 'DECIMAL' },
      annual_saving:      { label: '年节能量(吨标煤)', type: 'DECIMAL' },
      payback_period:     { label: '投资回收期(年)', type: 'DECIMAL' },
      completion_time:    { label: '完成时间', type: 'DATE' },
      actual_saving:      { label: '实际节能量(吨标煤)', type: 'DECIMAL' },
      is_contract_energy: { label: '是否合同能源管理模式', type: 'NUMBER' },
      remark:             { label: '备注', type: 'STRING' },
    },
  },

  de_tech_reform_history: {
    label: '十四五已实施节能技改项目',
    fields: {
      seq_no:              { label: '序号', type: 'NUMBER' },
      project_name:        { label: '项目名称', type: 'STRING' },
      project_type:        { label: '项目类型', type: 'STRING' },
      main_content:        { label: '主要内容', type: 'TEXT' },
      investment:          { label: '投资(万元)', type: 'DECIMAL' },
      designed_saving:     { label: '年节能量(吨标煤)', type: 'DECIMAL' },
      payback_period:      { label: '投资回收期(年)', type: 'DECIMAL' },
      completion_date:     { label: '完成时间', type: 'STRING' },
      actual_saving:       { label: '实际节能量(吨标煤)', type: 'DECIMAL' },
      is_contract_energy:  { label: '是否合同能源管理模式', type: 'STRING' },
      remark:              { label: '备注', type: 'STRING' },
    },
  },

  de_meter_instrument: {
    label: '能源计量器具汇总',
    fields: {
      management_no:    { label: '管理编号', type: 'STRING' },
      meter_name:       { label: '计量表名称', type: 'STRING' },
      install_location: { label: '安装地点或计量区域', type: 'STRING' },
      model_spec:       { label: '型号规格', type: 'STRING' },
      manufacturer:     { label: '生产厂家', type: 'STRING' },
      factory_no:       { label: '出厂编号', type: 'STRING' },
      multiplier:       { label: '倍率', type: 'DECIMAL' },
      grade:            { label: '级别', type: 'STRING' },
      energy_attribute: { label: '能源属性', type: 'STRING' },
      measure_range:    { label: '测量范围', type: 'STRING' },
      department:       { label: '所属部门', type: 'STRING' },
      accuracy_grade:   { label: '准确度等级', type: 'STRING' },
      status:           { label: '状态', type: 'DICT' },
      energy_id:        { label: '关联能源ID', type: 'NUMBER' },
      remark:           { label: '备注', type: 'STRING' },
    },
  },

  de_meter_config_rate: {
    label: '能源计量器具配备率',
    fields: {
      energy_type:       { label: '能源种类', type: 'STRING' },
      energy_sub_type:   { label: '能源子类', type: 'STRING' },
      l1_standard_rate:  { label: '进出用能单位-配备率标准%', type: 'DECIMAL' },
      l1_required_count: { label: '进出用能单位-需要配置数', type: 'NUMBER' },
      l1_actual_count:   { label: '进出用能单位-实际配置数', type: 'NUMBER' },
      l1_actual_rate:    { label: '进出用能单位-配备率%', type: 'DECIMAL' },
      l2_standard_rate:  { label: '次级用能单位-配备率标准%', type: 'DECIMAL' },
      l2_required_count: { label: '次级用能单位-需要配置数', type: 'NUMBER' },
      l2_actual_count:   { label: '次级用能单位-实际配置数', type: 'NUMBER' },
      l2_actual_rate:    { label: '次级用能单位-配备率%', type: 'DECIMAL' },
      l3_standard_rate:  { label: '主要用能设备-配备率标准%', type: 'DECIMAL' },
      l3_required_count: { label: '主要用能设备-需要配置数', type: 'NUMBER' },
      l3_actual_count:   { label: '主要用能设备-实际配置数', type: 'NUMBER' },
      l3_actual_rate:    { label: '主要用能设备-配备率%', type: 'DECIMAL' },
    },
  },

  de_equipment_benchmark: {
    label: '重点用能设备能效对标',
    fields: {
      // ── 鉴别列 ──
      equipment_type:              { label: '设备类型', type: 'DICT' },
      // ── 公共列 ──
      seq_no:                      { label: '序号', type: 'NUMBER' },
      device_purpose:              { label: '设备用途', type: 'STRING' },
      unit_count:                  { label: '台数', type: 'NUMBER' },
      start_date:                  { label: '启用日期', type: 'STRING' },
      is_obsolete:                 { label: '是否为淘汰设备', type: 'DICT' },
      annual_runtime_hours:        { label: '年运行时间(h)', type: 'DECIMAL' },
      factory_mgmt_no:             { label: '工厂管理编号', type: 'STRING' },
      install_location:            { label: '安装地点', type: 'STRING' },
      manufacture_date:            { label: '出厂日期', type: 'STRING' },
      // ── 电机参数(水泵/空压机/风机/空调/电机共用) ──
      motor_type:                  { label: '电机种类', type: 'DICT' },
      motor_model:                 { label: '电机型号', type: 'STRING' },
      motor_rated_power:           { label: '电机额定功率(kW)', type: 'DECIMAL' },
      motor_poles:                 { label: '级数', type: 'STRING' },
      motor_rated_speed:           { label: '额定转速(r/min)', type: 'STRING' },
      motor_rated_efficiency:      { label: '额定效率(%)', type: 'DECIMAL' },
      motor_cooling_method:        { label: '冷却方式', type: 'DICT' },
      motor_efficiency_grade:      { label: '能效等级(铭牌)', type: 'DICT' },
      rated_current:               { label: '额定电流(A)', type: 'DECIMAL' },
      rated_voltage:               { label: '额定电压(U)', type: 'STRING' },
      motor_power_factor:          { label: '配套电机功率因数', type: 'DECIMAL' },
      energy_grade:                { label: '能效等级(设备侧)', type: 'DICT' },
      // ── 水泵专属 ──
      pump_type:                   { label: '水泵类型', type: 'DICT' },
      pump_sub_type:               { label: '水泵子类型', type: 'STRING' },
      pump_rated_head:             { label: '额定扬程(m)', type: 'DECIMAL' },
      pump_flow_rate:              { label: '流量(m³/h)', type: 'DECIMAL' },
      pump_speed:                  { label: '转速(r/min)', type: 'STRING' },
      pump_shaft_power:            { label: '轴功率(kW)', type: 'DECIMAL' },
      pump_stages:                 { label: '级数(多级泵)', type: 'STRING' },
      pump_efficiency:             { label: '效率(%)', type: 'DECIMAL' },
      pump_pressure:               { label: '压力(MPa)', type: 'DECIMAL' },
      pump_work_temp:              { label: '工作温度', type: 'STRING' },
      // ── 空压机专属 ──
      compressor_type:             { label: '空压机类型', type: 'DICT' },
      compressor_model:            { label: '空压机型号', type: 'STRING' },
      compressor_rated_motor_power: { label: '额定电机功率(kW)', type: 'DECIMAL' },
      compressor_discharge_pressure: { label: '排气压力(MPa)', type: 'DECIMAL' },
      compressor_volume_flow:      { label: '容积流量(m³/min)', type: 'DECIMAL' },
      compressor_specific_power:   { label: '机组比功率(kW/(m³/min))', type: 'DECIMAL' },
      compressor_cooling:          { label: '冷却方式', type: 'DICT' },
      compressor_heat_recovery:    { label: '是否热回收', type: 'DICT' },
      compressor_stages:           { label: '压缩级数', type: 'STRING' },
      compressor_name:             { label: '空压机名称', type: 'STRING' },
      // ── 风机专属 ──
      fan_type:                    { label: '风机类型', type: 'DICT' },
      fan_model:                   { label: '风机型号', type: 'STRING' },
      fan_total_pressure:          { label: '全压(Pa)', type: 'DECIMAL' },
      fan_air_volume:              { label: '风量(m³/h)', type: 'DECIMAL' },
      fan_pressure_coeff:          { label: '压力系数', type: 'DICT' },
      fan_specific_speed:          { label: '比转速', type: 'STRING' },
      fan_size_no:                 { label: '机号', type: 'DICT' },
      fan_hub_ratio:               { label: '轮毂比', type: 'DICT' },
      fan_efficiency:              { label: '效率(%)', type: 'DECIMAL' },
      fan_pressure_pa:             { label: '压力(Pa)', type: 'DECIMAL' },
      // ── 空调系统专属 ──
      ac_type:                     { label: '空调系统类型', type: 'DICT' },
      ac_unit_type:                { label: '机组种类', type: 'DICT' },
      ac_model:                    { label: '型号', type: 'STRING' },
      ac_cooling_capacity:         { label: '名义制冷量(kW)', type: 'DECIMAL' },
      ac_cop:                      { label: '性能系数COP(W/W)', type: 'DECIMAL' },
      ac_iplv:                     { label: '综合部分负荷性能系数IPLV(W/W)', type: 'DECIMAL' },
      ac_steam_consumption:        { label: '单位冷量蒸汽耗量(kg/(kWh))', type: 'DECIMAL' },
      ac_steam_pressure:           { label: '饱和蒸汽(MPa)', type: 'STRING' },
      // ── 锅炉专属 ──
      boiler_type:                 { label: '锅炉类型', type: 'DICT' },
      boiler_capacity:             { label: '蒸发量/热功率', type: 'STRING' },
      boiler_fuel_type:            { label: '燃料品种', type: 'STRING' },
      boiler_thermal_eff:          { label: '热效率(%)', type: 'DECIMAL' },
      boiler_heat_recovery:        { label: '是否进行烟气余热回收', type: 'DICT' },
      boiler_rated_pressure:       { label: '额定工作压力(MPa)', type: 'DECIMAL' },
      boiler_rated_temp:           { label: '额定工作温度(℃)', type: 'DECIMAL' },
      boiler_inlet_temp:           { label: '额定进水温度(℃)', type: 'DECIMAL' },
      boiler_fuel_consumption:     { label: '燃料消耗量(kg/h)', type: 'DECIMAL' },
      boiler_fuel_heat_value:      { label: '燃料低位发热量(kJ/kg)', type: 'DECIMAL' },
      boiler_exhaust_temp:         { label: '锅炉排烟温度(℃)', type: 'DECIMAL' },
      // ── 变压器专属 ──
      transformer_voltage:         { label: '电压等级(kV)', type: 'DICT' },
      transformer_type:            { label: '变压器类型', type: 'DICT' },
      transformer_capacity:        { label: '变压器容量(KVA)', type: 'DECIMAL' },
      transformer_core_material:   { label: '铁芯材质', type: 'DICT' },
      transformer_insulation:      { label: '绝缘等级', type: 'DICT' },
      transformer_connection:      { label: '联结组别', type: 'DICT' },
      transformer_no_load_loss:    { label: '空载损耗(W)', type: 'DECIMAL' },
      transformer_load_loss:       { label: '负载损耗(W)', type: 'DECIMAL' },
      transformer_model:           { label: '产品型号', type: 'STRING' },
      transformer_grade_ref:       { label: '能效等级参考标准', type: 'STRING' },
      remark:                      { label: '备注', type: 'STRING' },
    },
  },

  de_equipment_energy: {
    label: '重点设备能耗和效率',
    fields: {
      location:         { label: '位置', type: 'STRING' },
      device_type:      { label: '设备类型', type: 'STRING' },
      indicator_name:   { label: '指标名称', type: 'STRING' },
      indicator_value:  { label: '指标值', type: 'DECIMAL' },
      measurement_unit: { label: '计量单位', type: 'STRING' },
      remark:           { label: '备注', type: 'STRING' },
    },
  },

  de_equipment_summary: {
    label: '主要用能设备汇总',
    fields: {
      device_name:              { label: '设备名称', type: 'STRING' },
      category:                 { label: '分类', type: 'STRING' },
      model:                    { label: '型号', type: 'STRING' },
      capacity:                 { label: '容量', type: 'STRING' },
      quantity:                 { label: '数量', type: 'NUMBER' },
      device_overview:          { label: '设备概况', type: 'TEXT' },
      obsolete_update_info:     { label: '淘汰更新情况', type: 'TEXT' },
      install_location:         { label: '安装使用场所', type: 'STRING' },
      energy_efficiency_level:  { label: '设备对标情况(能效级别)', type: 'STRING' },
      annual_runtime_hours:     { label: '年运行时间(小时)', type: 'DECIMAL' },
      remark:                   { label: '备注', type: 'STRING' },
    },
  },

  de_equipment_test: {
    label: '重点设备测试数据',
    fields: {
      device_no:           { label: '设备编号', type: 'STRING' },
      device_name:         { label: '设备名称', type: 'STRING' },
      model_spec:          { label: '型号规格', type: 'STRING' },
      test_indicator_name: { label: '测试指标名称', type: 'STRING' },
      measurement_unit:    { label: '计量单位', type: 'STRING' },
      qualified_value:     { label: '合格值或限额', type: 'DECIMAL' },
      actual_value:        { label: '实测值', type: 'DECIMAL' },
      test_date:           { label: '测试日期', type: 'DATE' },
      area:                { label: '所属区域', type: 'DICT' },
      judgement:           { label: '判别', type: 'DICT' },
      remark:              { label: '备注', type: 'STRING' },
    },
  },

  de_obsolete_equipment: {
    label: '淘汰产品设备装置目录',
    fields: {
      device_name:        { label: '淘汰设备名称', type: 'STRING' },
      model_spec:         { label: '型号规格', type: 'STRING' },
      quantity:           { label: '数量', type: 'NUMBER' },
      start_use_date:     { label: '开始使用日期', type: 'DATE' },
      plan_complete_date: { label: '计划完成日期', type: 'DATE' },
      remark:             { label: '备注', type: 'STRING' },
    },
  },

  de_energy_balance: {
    label: '能源购入消费存储',
    fields: {
      energy_id:           { label: '关联能源', type: 'NUMBER' },
      energy_name:         { label: '能源名称', type: 'STRING' },
      measurement_unit:    { label: '计量单位', type: 'STRING' },
      opening_stock:       { label: '期初库存量', type: 'DECIMAL' },
      purchase_amount:     { label: '购入量', type: 'DECIMAL' },
      consumption_amount:  { label: '消费量', type: 'DECIMAL' },
      transfer_out_amount: { label: '转出量', type: 'DECIMAL' },
      closing_stock:       { label: '期末库存量', type: 'DECIMAL' },
      gain_loss:           { label: '盈亏量', type: 'DECIMAL' },
      energy_unit_price:   { label: '能源单价(元)', type: 'DECIMAL' },
    },
  },

  de_product_unit_consumption: {
    label: '单位产品能耗',
    fields: {
      product_id:         { label: '关联产品', type: 'NUMBER' },
      year_type:          { label: '年份', type: 'DICT' },
      measurement_unit:   { label: '计量单位', type: 'STRING' },
      output:             { label: '产量', type: 'DECIMAL' },
      energy_consumption: { label: '能源消耗量(吨标煤)', type: 'DECIMAL' },
      unit_consumption:   { label: '单耗', type: 'DECIMAL' },
    },
  },

  de_product_energy_cost: {
    label: '企业产品能源成本',
    fields: {
      product_id:      { label: '关联产品', type: 'NUMBER' },
      energy_cost:     { label: '能源成本(万元)', type: 'DECIMAL' },
      production_cost: { label: '生产成本(万元)', type: 'DECIMAL' },
      remark:          { label: '备注', type: 'STRING' },
    },
  },

  de_energy_saving_calc: {
    label: '节能量计算',
    fields: {
      year_type:      { label: '年份', type: 'DICT' },
      energy_equiv:   { label: '综合能耗等价值(吨标煤)', type: 'DECIMAL' },
      energy_equil:   { label: '综合能耗当量值(吨标煤)', type: 'DECIMAL' },
      gross_output:   { label: '工业总产值(万元)', type: 'DECIMAL' },
      product_output: { label: '产品产量', type: 'DECIMAL' },
      product_unit:   { label: '产品单位', type: 'STRING' },
    },
  },

  de_ghg_emission: {
    label: '温室气体排放',
    fields: {
      emission_type:    { label: '排放类型', type: 'DICT' },
      energy_id:        { label: '关联能源', type: 'NUMBER' },
      main_equipment:   { label: '主要用能设备/生产部门', type: 'STRING' },
      measurement_unit: { label: '计量单位', type: 'STRING' },
      activity_data:    { label: '活动数据', type: 'DECIMAL' },
      annual_emission:  { label: '年度排放量(tCO2)', type: 'DECIMAL' },
      total_emission:   { label: '排放量(自动计算)', type: 'DECIMAL' },
      remark:           { label: '备注', type: 'STRING' },
    },
  },

  de_saving_potential: {
    label: '节能潜力明细',
    fields: {
      category:         { label: '分类', type: 'DICT' },
      project_name:     { label: '项目名称', type: 'STRING' },
      saving_potential:  { label: '节能潜力(吨标煤/年)', type: 'DECIMAL' },
      calculation_desc: { label: '节能潜力计算说明', type: 'TEXT' },
      remark:           { label: '备注', type: 'STRING' },
    },
  },

  de_management_policy: {
    label: '能源管理制度',
    fields: {
      policy_name:  { label: '制度名称', type: 'STRING' },
      department:   { label: '主管部门', type: 'STRING' },
      publish_date: { label: '颁布日期', type: 'DATE' },
      valid_period: { label: '有效期', type: 'STRING' },
      main_content: { label: '主要内容', type: 'TEXT' },
      remark:       { label: '备注', type: 'STRING' },
    },
  },

  de_improvement_suggestion: {
    label: '能源管理改进建议',
    fields: {
      project_name:  { label: '项目名称', type: 'STRING' },
      investment:    { label: '投资(万元)', type: 'DECIMAL' },
      annual_saving: { label: '年节能量(吨标煤)', type: 'DECIMAL' },
      remark:        { label: '备注', type: 'STRING' },
    },
  },

  de_tech_reform: {
    label: '节能技术改造建议汇总',
    fields: {
      project_name:   { label: '项目名称', type: 'STRING' },
      investment:     { label: '投资(万元)', type: 'DECIMAL' },
      annual_saving:  { label: '年节能量(吨标煤)', type: 'DECIMAL' },
      payback_period: { label: '投资回收期(年)', type: 'DECIMAL' },
      remark:         { label: '备注', type: 'STRING' },
    },
  },

  de_rectification: {
    label: '节能整改措施',
    fields: {
      project_name:       { label: '整改项目名称', type: 'STRING' },
      detail_content:     { label: '整改具体内容', type: 'TEXT' },
      rectify_date:       { label: '整改日期', type: 'DATE' },
      responsible_person: { label: '责任人', type: 'STRING' },
      estimated_cost:     { label: '整改预计费用(万元)', type: 'DECIMAL' },
      saving_amount:      { label: '节能量(吨标准煤)', type: 'DECIMAL' },
      economic_benefit:   { label: '经济效益(万元)', type: 'DECIMAL' },
    },
  },

  de_five_year_target: {
    label: '十四五期间节能目标',
    fields: {
      year_type:                 { label: '年份类型', type: 'DICT' },
      product_id:                { label: '关联产品', type: 'NUMBER' },
      gross_output:              { label: '产值(万元)', type: 'DECIMAL' },
      energy_equiv:              { label: '综合能耗当量值(吨标煤)', type: 'DECIMAL' },
      energy_equal:              { label: '综合能耗等价值(吨标煤)', type: 'DECIMAL' },
      unit_energy_equiv:         { label: '产值综合能耗当量值', type: 'DECIMAL' },
      unit_energy_equal:         { label: '产值综合能耗等价值', type: 'DECIMAL' },
      decline_rate:              { label: '产值综合能耗下降率(%)', type: 'DECIMAL' },
      target_indicator:          { label: '单耗指标值', type: 'DECIMAL' },
      actual_indicator:          { label: '单耗实际值', type: 'DECIMAL' },
      annual_target:             { label: '目标值(按年度)', type: 'DECIMAL' },
      energy_control_total:      { label: '能耗控制总量等价值', type: 'DECIMAL' },
      product_unit_consumption:  { label: '产品单耗', type: 'DECIMAL' },
      saving_amount:             { label: '节能量(吨标煤)', type: 'DECIMAL' },
    },
  },

  de_energy_ghg_source: {
    label: '能源数据和温室气体排放源',
    fields: {
      energy_id:          { label: '关联能源', type: 'NUMBER' },
      measurement_unit:   { label: '计量单位', type: 'STRING' },
      start_time:         { label: '起始时间', type: 'DATE' },
      end_time:           { label: '结束时间', type: 'DATE' },
      period_consumption: { label: '期间消耗', type: 'DECIMAL' },
      closing_stock:      { label: '期末库存', type: 'DECIMAL' },
    },
  },

  de_energy_flow_diagram: {
    label: '能源流程图',
    fields: {
      diagram_type: { label: '图类型(1分层/2单元/3二维表)', type: 'NUMBER' },
      diagram_data: { label: '图布局数据(JSON)', type: 'TEXT' },
    },
  },

  de_energy_flow_node: {
    label: '能流图节点',
    fields: {
      node_id:    { label: '节点标识', type: 'STRING' },
      node_type:  { label: '节点类型', type: 'STRING' },
      ref_type:   { label: '引用类型', type: 'STRING' },
      ref_id:     { label: '引用ID', type: 'NUMBER' },
      label:      { label: '显示标签', type: 'STRING' },
      position_x: { label: 'X坐标', type: 'DECIMAL' },
      position_y: { label: 'Y坐标', type: 'DECIMAL' },
    },
  },

  de_energy_flow_edge: {
    label: '能流图连线',
    fields: {
      edge_id:         { label: '连线标识', type: 'STRING' },
      source_node_id:  { label: '源节点ID', type: 'STRING' },
      target_node_id:  { label: '目标节点ID', type: 'STRING' },
      energy_id:       { label: '关联能源', type: 'NUMBER' },
      product_id:      { label: '关联产品', type: 'NUMBER' },
      physical_amount: { label: '实物量', type: 'DECIMAL' },
      remark:          { label: '备注', type: 'STRING' },
    },
  },

  ent_enterprise: {
    label: '企业主表',
    fields: {
      enterprise_name: { label: '企业名称', type: 'STRING' },
      credit_code:     { label: '统一社会信用代码', type: 'STRING' },
      contact_person:  { label: '联系人', type: 'STRING' },
      contact_email:   { label: '邮箱', type: 'STRING' },
      contact_phone:   { label: '电话', type: 'STRING' },
      remark:          { label: '备注', type: 'STRING' },
    },
  },

  ar_report: {
    label: '审计报告',
    fields: {
      report_name:         { label: '报告名称', type: 'STRING' },
      report_type:         { label: '报告类型', type: 'NUMBER' },
      status:              { label: '状态', type: 'NUMBER' },
      generated_file_path: { label: '生成的文件路径', type: 'STRING' },
      uploaded_file_path:  { label: '上传的文件路径', type: 'STRING' },
      onlyoffice_doc_key:  { label: 'OnlyOffice文档Key', type: 'STRING' },
      generate_time:       { label: '生成时间', type: 'DATE' },
      submit_time:         { label: '提交时间', type: 'DATE' },
    },
  },
}

export function getTableOptions() {
  return Object.entries(SCHEMA_REGISTRY).map(([key, schema]) => ({
    label: `${schema.label} (${key})`,
    value: key,
  }))
}

export function getFieldOptions(tableName: string) {
  const table = SCHEMA_REGISTRY[tableName]
  if (!table) return []
  return Object.entries(table.fields).map(([key, field]) => ({
    label: `${field.label} (${key})`,
    value: key,
  }))
}
