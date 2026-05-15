import type { RegColumn } from './components/RegulationTable.vue'

export interface StandardChartDefinition {
  label: string
  routeName: string
  exportSheetName: string
  tableName?: string
  columns?: RegColumn[]
}

export const equipmentEnergyColumns: RegColumn[] = [
  { prop: 'seq_no', label: '序号', width: 60 },
  { prop: 'device_name', label: '设备名称', minWidth: 120 },
  { prop: 'model_spec', label: '型号规格', minWidth: 120 },
  { prop: 'nameplate_output', label: '铭牌出力', minWidth: 100 },
  { prop: 'main_energy_name', label: '主要能源名称', minWidth: 120 },
  { prop: 'main_energy_consumption', label: '主要能源消费量', minWidth: 140 },
  { prop: 'avg_operating_efficiency', label: '平均运行效率', minWidth: 120 },
  { prop: 'residual_heat_energy', label: '余热余能量', minWidth: 120 },
  { prop: 'available_residual_heat_energy', label: '可回收余热余能量', minWidth: 150 },
  { prop: 'utilized_residual_heat_energy', label: '已利用余热余能量', minWidth: 150 },
  { prop: 'recovery_utilization_rate', label: '回收利用率', minWidth: 120 },
  { prop: 'statistical_load_rate', label: '统计负荷率', minWidth: 120 },
  { prop: 'test_efficiency', label: '测试效率', minWidth: 120 },
  { prop: 'flue_gas_loss_rate', label: '排烟热损失率', minWidth: 130 },
  { prop: 'heat_loss_rate', label: '散热损失率', minWidth: 120 },
  { prop: 'other_loss', label: '其他损失', minWidth: 100 },
  { prop: 'test_date', label: '测试日期', minWidth: 110 },
]

export const savingCalculationColumns: RegColumn[] = [
  { prop: 'product_seq', label: '产品序号', width: 90 },
  { prop: 'row_label', label: '项目', minWidth: 220 },
  { prop: 'current_value', label: '审计期', minWidth: 120 },
  { prop: 'base_value', label: '基准期', minWidth: 120 },
  { prop: 'measurement_unit', label: '单位', minWidth: 120 },
]

export const standardChartDefinitions: StandardChartDefinition[] = [
  { label: '1. 企业基本信息表', routeName: 'StandardBasicInfo', exportSheetName: '企业基本信息表' },
  { label: '2. 上一轮已实施的节能技改项目表', routeName: 'StandardRetrofitProjects', exportSheetName: '上一轮已实施的节能技改项目表', tableName: 'de_tech_reform_history' },
  { label: '3. 企业概况及主要技术指标一览表', routeName: 'StandardEnterpriseOverview', exportSheetName: '企业概况及主要技术指标一览表', tableName: 'de_tech_indicator' },
  { label: '4. 重点用能设备汇总表', routeName: 'StandardMajorEquipment', exportSheetName: '重点用能设备汇总表', tableName: 'de_equipment_summary' },
  { label: '5. 能源流程图', routeName: 'StandardEnergyFlow', exportSheetName: '能源流程图', tableName: 'de_energy_flow' },
  { label: '6. 重点设备能耗和效率', routeName: 'StandardEquipmentEnergy', exportSheetName: '重点设备能耗和效率', tableName: 'de_equipment_energy', columns: equipmentEnergyColumns },
  { label: '7. 能碳计量器具汇总表', routeName: 'StandardMeterSummary', exportSheetName: '能碳计量器具汇总表', tableName: 'de_meter_instrument' },
  { label: '8. 能碳计量器具配备率表', routeName: 'StandardMeterRate', exportSheetName: '能碳计量器具配备率表', tableName: 'de_meter_config_rate' },
  { label: '9. 温室气体排放表', routeName: 'StandardGhgEmission', exportSheetName: '温室气体排放表', tableName: 'de_ghg_emission' },
  { label: '10. 能源消费平衡综合表', routeName: 'StandardEnergyBalance', exportSheetName: '能源消费平衡综合表', tableName: 'de_energy_balance' },
  { label: '11. 淘汰产品、设备、装置、工艺和生产能力目录表', routeName: 'StandardObsoleteEquipment', exportSheetName: '淘汰产品、设备、装置、工艺和生产能力目录表', tableName: 'de_obsolete_equipment' },
  { label: '12. 企业产品能源成本表', routeName: 'StandardProductEnergyCost', exportSheetName: '企业产品能源成本表', tableName: 'de_product_energy_cost' },
  { label: '13. 设备测试报告主要指标汇总表', routeName: 'StandardTestIndicators', exportSheetName: '设备测试报告主要指标汇总表', tableName: 'de_equipment_test' },
  { label: '14. 节能量计算', routeName: 'StandardSavingCalculation', exportSheetName: '节能量计算', tableName: 'de_saving_calculation_detail', columns: savingCalculationColumns },
  { label: '15. 节能降碳潜力明细表', routeName: 'StandardSavingPotential', exportSheetName: '节能降碳潜力明细表', tableName: 'de_saving_potential' },
  { label: '16. 能碳管理改进建议表', routeName: 'StandardMgmtSuggestions', exportSheetName: '能碳管理改进建议表', tableName: 'de_management_suggestion' },
  { label: '17. 节能降碳技术改造建议汇总表', routeName: 'StandardRetrofitSuggestions', exportSheetName: '节能降碳技术改造建议汇总表', tableName: 'de_tech_reform_suggestion' },
  { label: '18. 节能降碳整改措施表', routeName: 'StandardRectification', exportSheetName: '节能降碳整改措施表', tableName: 'de_rectification' },
  { label: '19. 整改-“十五五”期间节能降碳目标', routeName: 'StandardFiveYearTarget', exportSheetName: '整改-“十五五”期间节能降碳目标', tableName: 'de_five_year_target' },
]
