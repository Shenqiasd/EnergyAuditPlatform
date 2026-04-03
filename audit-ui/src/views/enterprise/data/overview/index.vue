<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getTableList, queryTableData, type TableMeta } from '@/api/extracted-data'

const COLUMN_LABELS: Record<string, Record<string, string>> = {
  de_company_overview: {
    energy_leader_name: '能源负责人', energy_leader_position: '职务',
    energy_dept_name: '能源管理部门', energy_dept_leader: '部门负责人',
    fulltime_staff_count: '专职人员数', parttime_staff_count: '兼职人员数',
    five_year_target_value: '目标值', five_year_target_name: '目标名称',
    five_year_target_dept: '目标部门',
  },
  de_tech_indicator: {
    indicator_year: '年度', gross_output: '工业总产值(万元)', sales_revenue: '销售收入(万元)',
    tax_paid: '上缴税金(万元)', energy_total_cost: '能源费用(万元)', production_cost: '生产成本(万元)',
    energy_cost_ratio: '能源费用占比', total_energy_equiv: '综合能耗(等价,tce)',
    total_energy_equal: '综合能耗(当量,tce)', total_energy_excl_material: '扣除原料能耗(tce)',
    unit_output_energy_equiv: '万元产值能耗(等价)', unit_output_energy_equal: '万元产值能耗(当量)',
    saving_project_count: '节能项目数', saving_invest_total: '节能投资(万元)',
    saving_capacity: '节能能力(tce)', saving_benefit: '节能效益(万元)',
    coal_target: '煤炭目标(tce)', coal_actual: '煤炭实际(tce)',
    employee_count: '从业人数', energy_manager_count: '能源管理人员数',
    total_energy_equiv_excl_green: '扣除绿电等价(tce)', total_energy_equal_excl_green: '扣除绿电当量(tce)',
  },
  de_energy_consumption: {
    energy_code: '能源代码', energy_name: '能源名称', measurement_unit: '计量单位',
    opening_stock: '期初库存', purchase_total: '购入量', purchase_from_province: '省内购入',
    purchase_amount: '购入金额', industrial_consumption: '工业消费',
    material_consumption: '原料消费', transport_consumption: '运输消费',
    closing_stock: '期末库存', external_supply: '外供', equiv_factor: '等价系数',
    equal_factor: '当量系数', standard_coal: '折标煤(tce)',
    non_industrial_consumption: '非工业消费', consumption_total: '消费合计',
    ref_factor: '参考系数', transfer_out: '调出', gain_loss: '盈亏', unit_price: '单价',
  },
  de_energy_conversion: {
    energy_name: '能源名称', measurement_unit: '计量单位',
    industrial_consumption: '工业消费', conversion_input_total: '加工转换投入合计',
    conv_power_gen: '火力发电', conv_heating: '供热', conv_coal_washing: '洗煤',
    conv_coking: '炼焦', conv_refining: '炼油', conv_gas_making: '制气',
    conv_lng: '液化天然气', conv_coal_product: '煤制品',
    conversion_output: '加工转换产出', conversion_output_std: '产出折标',
    recovery_utilization: '回收利用', equiv_factor: '等价系数', equal_factor: '当量系数',
  },
  de_product_unit_consumption: {
    indicator_name: '指标名称', indicator_unit: '指标单位',
    numerator_unit: '分子单位', denominator_unit: '分母单位',
    conversion_factor: '折标系数', current_indicator: '本期指标值',
    current_numerator: '本期分子', current_denominator: '本期分母',
    previous_indicator: '上期指标值', previous_numerator: '上期分子', previous_denominator: '上期分母',
  },
  de_equipment_detail: {
    equipment_type: '设备类型', equipment_name: '设备名称', model: '型号规格',
    quantity: '数量', capacity: '容量/功率', annual_runtime_hours: '年运行小时',
    annual_energy: '年用能量', energy_unit: '能源单位', energy_efficiency: '能效等级',
    install_location: '安装位置', equipment_overview: '设备概况', obsolete_status: '淘汰状态',
    remark: '备注',
  },
  de_carbon_emission: {
    emission_category: '排放类别', source_name: '排放源', measurement_unit: '计量单位',
    emission_factor: '排放因子', activity_data: '活动数据', co2_emission: 'CO₂排放(tCO₂)',
    low_heat_value: '低位热值', carbon_content: '含碳量', oxidation_rate: '氧化率',
    conversion_output: '加工转换产出', recovery_amount: '回收量',
    unit_output_emission: '单位产值排放', total_energy_consumption: '综合能耗',
    unit_output_energy: '单位产值能耗', remark: '备注',
  },
  de_energy_balance: {
    row_label: '行标签', row_category: '行类别', energy_name: '能源名称',
    energy_value: '数值', measurement_unit: '计量单位', row_seq: '序号',
  },
  de_energy_flow: {
    flow_stage: '流向阶段', seq_no: '序号', source_unit: '来源单元',
    target_unit: '目标单元', energy_product: '能源产品',
    physical_quantity: '实物量', standard_quantity: '折标量(tce)', remark: '备注',
  },
  de_five_year_target: {
    section_type: '板块', year_label: '年度', gross_output: '工业总产值(万元)',
    energy_equiv: '能耗(等价,tce)', energy_equal: '能耗(当量,tce)',
    unit_energy_equiv: '万元产值能耗(等价)', unit_energy_equal: '万元产值能耗(当量)',
    decline_rate: '下降率', product_name: '产品名称', indicator_name: '指标名称',
    indicator_value: '指标值', actual_value: '实际值', energy_control_total: '能耗控制总量(tce)',
    product_unit_consumption: '产品单耗', saving_amount: '节能量(tce)',
  },
  de_tech_reform_history: {
    seq_no: '序号', project_name: '项目名称', main_content: '主要内容',
    investment: '投资(万元)', designed_saving: '设计节能量(tce)', payback_period: '回收期(年)',
    completion_date: '完成日期', actual_saving: '实际节能(tce)', is_contract_energy: '合同能源管理',
    remark: '备注',
  },
  de_saving_project: {
    project_type: '项目类型', project_name: '项目名称', impl_status: '实施状态',
    impl_date: '实施日期', investment: '投资(万元)', saving_amount: '节能量(tce)',
    carbon_reduction: '碳减排(tCO₂)', is_contract_energy: '合同能源管理',
    approval_dept: '审批部门', main_content: '主要内容', remark: '备注',
  },
  de_product_output: {
    product_name: '产品名称', annual_capacity: '年产能', capacity_unit: '产能单位',
    annual_output: '年产量', output_unit: '产量单位', unit_consumption: '单位能耗',
    consumption_unit: '能耗单位',
  },
  de_meter_instrument: {
    management_no: '管理编号', model_spec: '型号规格', manufacturer: '制造厂家',
    serial_no: '出厂编号', meter_name: '仪表名称', multiplier: '倍率',
    accuracy_class: '准确度等级', energy_type: '能源类型', measurement_range: '测量范围',
    department: '使用部门', accuracy_grade: '检定等级', install_location: '安装位置',
    status: '状态', remark: '备注',
  },
  de_meter_config_rate: {
    energy_type: '能源类型', config_level: '配备层级', standard_rate: '标准配备率',
    required_count: '应配数量', actual_count: '实配数量', actual_rate: '实际配备率',
  },
  de_obsolete_equipment: {
    seq_no: '序号', equipment_name: '设备名称', model_spec: '型号规格',
    quantity: '数量', start_use_date: '启用日期', planned_retire_date: '计划淘汰日期',
    remark: '备注',
  },
  de_product_energy_cost: {
    seq_no: '序号', product_name: '产品名称', energy_cost: '能源费用(万元)',
    production_cost: '生产成本(万元)', cost_ratio: '费用占比',
    energy_total_ratio: '能源占比', remark: '备注',
  },
  de_saving_calculation: {
    energy_equal_current: '当期当量能耗', energy_equiv_current: '当期等价能耗',
    gross_output_current: '当期总产值', product_output_current: '当期产品产量',
    product_unit_current: '当期产品单位', energy_equal_base: '基期当量能耗',
    energy_equiv_base: '基期等价能耗', gross_output_base: '基期总产值',
    product_output_base: '基期产品产量', product_unit_base: '基期产品单位',
  },
  de_management_policy: {
    seq_no: '序号', policy_name: '制度名称', main_content: '主要内容',
    supervise_dept: '监督部门', publish_date: '发布日期', valid_period: '有效期',
    remark: '备注',
  },
  de_saving_potential: {
    seq_no: '序号', category: '类别', project_name: '项目名称',
    main_content: '主要内容', saving_potential: '节能潜力(tce)',
    calc_description: '计算说明', remark: '备注',
  },
  de_management_suggestion: {
    seq_no: '序号', project_name: '项目名称', main_content: '主要内容',
    investment: '投资(万元)', annual_saving: '年节能量(tce)', remark: '备注',
  },
  de_tech_reform_suggestion: {
    seq_no: '序号', project_name: '项目名称', main_content: '主要内容',
    investment: '投资(万元)', annual_saving: '年节能量(tce)', payback_period: '回收期(年)',
    remark: '备注',
  },
  de_rectification: {
    seq_no: '序号', project_name: '项目名称', measures: '整改措施',
    target_date: '完成期限', responsible_person: '责任人',
    estimated_cost: '预计费用(万元)', annual_saving: '年节能量(tce)',
    annual_benefit: '年效益(万元)',
  },
  de_report_text: {
    section_code: '章节编码', section_name: '章节名称', content: '内容',
  },
}

const SYSTEM_FIELDS = new Set([
  'id', 'submission_id', 'enterprise_id', 'audit_year',
  'create_by', 'create_time', 'update_by', 'update_time', 'deleted',
])

const loading = ref(false)
const tables = ref<TableMeta[]>([])
const activeTab = ref('')
const auditYear = ref<number | undefined>(undefined)
const tableData = ref<Record<string, unknown>[]>([])
const dataLoading = ref(false)

const yearOptions = (() => {
  const currentYear = new Date().getFullYear()
  const years: number[] = []
  for (let y = currentYear; y >= currentYear - 5; y--) {
    years.push(y)
  }
  return years
})()

onMounted(async () => {
  loading.value = true
  try {
    tables.value = await getTableList()
    if (tables.value.length > 0) {
      activeTab.value = tables.value[0].tableName
    }
  } catch (e: any) {
    ElMessage.error('加载表列表失败')
  } finally {
    loading.value = false
  }
})

// activeTab 赋值会触发 watch → loadTableData()，无需在 onMounted 中额外调用
watch([activeTab, auditYear], () => {
  if (activeTab.value) loadTableData()
})

async function loadTableData() {
  if (!activeTab.value) return
  dataLoading.value = true
  try {
    tableData.value = await queryTableData(activeTab.value, auditYear.value)
  } catch (e: any) {
    ElMessage.error('Failed to load data')
    tableData.value = []
  } finally {
    dataLoading.value = false
  }
}

function getVisibleColumns(tableName: string): string[] {
  if (tableData.value.length === 0) {
    const labels = COLUMN_LABELS[tableName]
    return labels ? Object.keys(labels) : []
  }
  const allKeys = Object.keys(tableData.value[0])
  return allKeys.filter(k => !SYSTEM_FIELDS.has(k))
}

function getColumnLabel(tableName: string, column: string): string {
  const labels = COLUMN_LABELS[tableName]
  if (labels && labels[column]) return labels[column]
  return column
}
</script>

<template>
  <div class="page-container" v-loading="loading">
    <el-card class="filter-card">
      <div class="filter-bar">
        <span class="page-title">抽取数据总览</span>
        <el-select
          v-model="auditYear"
          placeholder="全部年度"
          clearable
          size="default"
          style="width: 140px"
        >
          <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
        </el-select>
      </div>
    </el-card>

    <el-card>
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane
          v-for="t in tables"
          :key="t.tableName"
          :label="t.count > 0 ? `${t.label} (${t.count})` : t.label"
          :name="t.tableName"
        >
          <el-table
            :data="tableData"
            v-loading="dataLoading"
            border
            stripe
            max-height="600"
            size="small"
            empty-text="暂无数据"
            style="width: 100%"
          >
            <el-table-column
              v-for="col in getVisibleColumns(t.tableName)"
              :key="col"
              :prop="col"
              :label="getColumnLabel(t.tableName, col)"
              min-width="120"
              show-overflow-tooltip
            />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-card {
  :deep(.el-card__body) {
    padding: 12px 20px;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
