<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getExtractedTables,
  queryExtractedTable,
  type TableSummary,
} from '@/api/extracted-data'

const selectedYear = ref<number>(new Date().getFullYear())
const yearOptions = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)

const loadingSummary = ref(false)
const tables = ref<TableSummary[]>([])
const activeTab = ref('')

const loadingData = ref(false)
const currentRows = ref<Record<string, unknown>[]>([])
const currentColumns = ref<string[]>([])
const currentTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const SYSTEM_COLS = new Set([
  'id', 'submission_id', 'enterprise_id', 'audit_year',
  'create_by', 'create_time', 'update_by', 'update_time', 'deleted',
])

const COL_LABELS: Record<string, string> = {
  // ent_enterprise_setting (企业概况)
  region: '所属地区',
  industry_field: '所属领域',
  industry_name: '行业分类名称',
  unit_nature: '单位类型',
  registered_date: '单位注册日期',
  registered_capital: '注册资本(万元)',
  legal_representative: '法定代表人',
  legal_phone: '法定代表人电话',
  is_central_enterprise: '是否央企',
  group_name: '所属集团名称',
  enterprise_address: '单位地址',
  unit_address: '单位地址(备用)',
  postal_code: '邮政编码',
  admin_division_code: '行政区划代码',
  enterprise_email: '电子邮箱',
  fax: '传真(区号)',
  energy_mgmt_org: '能源管理机构名称',
  energy_leader_name: '节能领导姓名',
  energy_leader_phone: '节能领导电话',
  energy_manager_name: '能源管理负责人',
  energy_manager_mobile: '负责人手机',
  energy_manager_cert: '能源管理师证号',
  energy_dept_leader_phone: '能源部门负责人电话',
  energy_cert: '是否通过能源认证',
  cert_pass_date: '认证通过日期',
  cert_authority: '认证机构',
  has_energy_center: '是否建设能源管理中心',
  enterprise_contact: '企业联系人',
  enterprise_mobile: '企业联系手机',
  compiler_contact: '编制人联系人',
  compiler_name: '编制人姓名',
  compiler_mobile: '编制人手机',
  compiler_email: '编制人邮箱',
  industry_category: '行业大类',
  industry_code: '行业代码',
  superior_department: '上级主管部门',
  energy_enterprise_type: '用能企业类型',
  // de_company_overview (legacy)
  energy_leader_position: '能源负责人职务',
  energy_dept_name: '能源管理部门',
  energy_dept_leader: '部门负责人',
  fulltime_staff_count: '专职人数',
  parttime_staff_count: '兼职人数',
  five_year_target_value: '五年目标值',
  five_year_target_name: '五年目标名称',
  five_year_target_dept: '五年目标部门',
  indicator_year: '指标年度',
  gross_output: '总产值',
  sales_revenue: '销售收入',
  tax_paid: '缴纳税金',
  energy_total_cost: '能源总成本',
  production_cost: '生产成本',
  energy_cost_ratio: '能源费用比',
  total_energy_equiv: '综合能耗(当量)',
  total_energy_equal: '综合能耗(等价)',
  total_energy_excl_material: '扣除原料能耗',
  unit_output_energy_equiv: '万元产值能耗(当量)',
  unit_output_energy_equal: '万元产值能耗(等价)',
  saving_project_count: '节能项目数',
  saving_invest_total: '节能投资总额',
  saving_capacity: '节能量',
  saving_benefit: '节能效益',
  coal_target: '本期考核指标(等价值)',
  coal_actual: '本期考核指标(强度等价值)',
  employee_count: '从业人员(人)',
  energy_manager_count: '能源管理师人数(人)',
  total_energy_equiv_excl_green: '综合能耗扣除绿电(当量)',
  total_energy_equal_excl_green: '综合能耗扣除绿电(等价)',
  raw_material_energy: '原材料用能(吨标煤)',
  electrification_rate: '电气化率(%)',
  total_energy_equal_excl_material: '综合能耗扣除原料(等价)',
  energy_code: '能源编码',
  energy_name: '能源名称',
  measurement_unit: '计量单位',
  opening_stock: '期初库存',
  purchase_total: '购入总量',
  purchase_from_province: '省内购入',
  purchase_amount: '购入金额',
  industrial_consumption: '工业消费',
  material_consumption: '原料消费',
  transport_consumption: '运输消费',
  closing_stock: '期末库存',
  external_supply: '外供',
  equiv_factor: '当量系数',
  equal_factor: '等价系数',
  standard_coal: '标准煤',
  conversion_input_total: '加工转换投入',
  conv_power_gen: '发电',
  conv_heating: '供热',
  conv_coal_washing: '洗煤',
  conv_coking: '炼焦',
  conv_refining: '炼油',
  conv_gas_making: '制气',
  conv_lng: '液化天然气',
  conv_coal_product: '煤制品',
  conversion_output: '产出量',
  conversion_output_std: '产出标准煤',
  recovery_utilization: '回收利用',
  indicator_name: '指标名称',
  indicator_unit: '指标单位',
  numerator_unit: '分子单位',
  denominator_unit: '分母单位',
  conversion_factor: '转换系数',
  current_indicator: '本期指标',
  current_numerator: '本期分子',
  current_denominator: '本期分母',
  previous_indicator: '上期指标',
  previous_numerator: '上期分子',
  previous_denominator: '上期分母',
  equipment_type: '设备类型',
  equipment_name: '设备名称',
  model: '型号',
  quantity: '数量',
  capacity: '容量',
  annual_runtime_hours: '年运行小时',
  annual_energy: '年耗能',
  energy_unit: '能源单位',
  energy_efficiency: '能效等级',
  install_location: '安装位置',
  detail_json: '详细信息',
  remark: '备注',
  emission_category: '排放类别',
  source_name: '排放源',
  emission_factor: '排放因子',
  activity_data: '活动数据',
  co2_emission: 'CO₂排放量',
  row_label: '行标签',
  row_category: '行类别',
  energy_value: '能源值',
  flow_stage: '流程阶段',
  seq_no: '序号',
  project_name: '项目名称',
  project_type: '项目类型',
  main_content: '主要内容',
  investment: '投资(万元)',
  designed_saving: '年节能量(吨标煤)',
  payback_period: '投资回收期(年)',
  completion_date: '完成时间',
  actual_saving: '实际节能量(吨标煤)',
  is_contract_energy: '是否合同能源管理模式',
  // de_meter_instrument (能源计量器具汇总)
  management_no: '管理编号',
  meter_name: '计量表名称',
  model_spec: '型号规格',
  manufacturer: '生产厂家',
  factory_no: '出厂编号',
  multiplier: '倍率',
  grade: '级别',
  energy_attribute: '能源属性',
  measure_range: '测量范围',
  department: '所属部门',
  accuracy_grade: '准确度等级',
  energy_sub_type: '能源子类',
  l1_standard_rate: '进出用能单位-配备率标准%',
  l1_required_count: '进出用能单位-需要配置数',
  l1_actual_count: '进出用能单位-实际配置数',
  l1_actual_rate: '进出用能单位-配备率%',
  l2_standard_rate: '次级用能单位-配备率标准%',
  l2_required_count: '次级用能单位-需要配置数',
  l2_actual_count: '次级用能单位-实际配置数',
  l2_actual_rate: '次级用能单位-配备率%',
  l3_standard_rate: '主要用能设备-配备率标准%',
  l3_required_count: '主要用能设备-需要配置数',
  l3_actual_count: '主要用能设备-实际配置数',
  l3_actual_rate: '主要用能设备-配备率%',
  // de_equipment_summary (主要用能设备汇总)
  device_name: '设备名称',
  category: '分类',
  device_overview: '设备概况',
  obsolete_update_info: '淘汰更新情况',
  energy_efficiency_level: '设备对标情况(能效级别)',
  // de_equipment_test (重点设备测试数据)
  device_no: '设备编号',
  test_indicator_name: '测试指标名称',
  qualified_value: '合格值或限额',
  judgement: '判别',
  test_date: '测试日期',
  area: '所属区域',
  // de_obsolete_equipment (淘汰产品设备装置目录)
  start_use_date: '开始使用日期',
  plan_complete_date: '计划完成日期',
  source_unit: '来源单元',
  target_unit: '目标单元',
  energy_product: '能源产品',
  physical_quantity: '实物量',
  standard_quantity: '标准量',
  section_type: '区段类型',
  year_label: '年份标签',
  decline_rate: '下降率',
  product_name: '产品名称',
  indicator_value: '指标值',
  actual_value: '实际值',
  energy_control_total: '能源管控总量',
  product_unit_consumption: '单耗',
  saving_amount: '节约量',
}

const NUMERIC_COLS = new Set([
  'registered_capital', 'is_central_enterprise', 'energy_cert', 'has_energy_center',
  'fulltime_staff_count', 'parttime_staff_count', 'five_year_target_value',
  'indicator_year', 'gross_output', 'sales_revenue', 'tax_paid',
  'energy_total_cost', 'production_cost', 'energy_cost_ratio',
  'total_energy_equiv', 'total_energy_equal', 'total_energy_excl_material',
  'unit_output_energy_equiv', 'unit_output_energy_equal',
  'saving_project_count', 'saving_invest_total', 'saving_capacity',
  'saving_benefit', 'coal_target', 'coal_actual',
  'employee_count', 'energy_manager_count',
  'total_energy_equiv_excl_green', 'total_energy_equal_excl_green',
  'raw_material_energy', 'electrification_rate', 'total_energy_equal_excl_material',
  'opening_stock', 'purchase_total', 'purchase_from_province', 'purchase_amount',
  'industrial_consumption', 'material_consumption', 'transport_consumption',
  'closing_stock', 'external_supply', 'equiv_factor', 'equal_factor', 'standard_coal',
  'conversion_input_total', 'conv_power_gen', 'conv_heating', 'conv_coal_washing',
  'conv_coking', 'conv_refining', 'conv_gas_making', 'conv_lng', 'conv_coal_product',
  'conversion_output', 'conversion_output_std', 'recovery_utilization',
  'conversion_factor', 'current_indicator', 'current_numerator', 'current_denominator',
  'previous_indicator', 'previous_numerator', 'previous_denominator',
  'quantity', 'annual_runtime_hours', 'annual_energy',
  'emission_factor', 'activity_data', 'co2_emission', 'energy_value',
  'physical_quantity', 'standard_quantity', 'seq_no',
  'investment', 'designed_saving', 'payback_period', 'actual_saving', 'multiplier',
  'l1_standard_rate', 'l1_required_count', 'l1_actual_count', 'l1_actual_rate',
  'l2_standard_rate', 'l2_required_count', 'l2_actual_count', 'l2_actual_rate',
  'l3_standard_rate', 'l3_required_count', 'l3_actual_count', 'l3_actual_rate',
  'qualified_value', 'decline_rate', 'indicator_value', 'actual_value',
  'energy_control_total', 'product_unit_consumption', 'saving_amount',
  'energy_equiv', 'energy_equal', 'unit_energy_equiv', 'unit_energy_equal',
])

function isNumericCol(col: string): boolean {
  return NUMERIC_COLS.has(col)
}

function formatCellValue(value: unknown, col: string): string {
  if (value == null || value === '') return '—'
  if (!isNumericCol(col)) return String(value)
  const num = Number(value)
  if (isNaN(num)) return String(value)
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

function getColLabel(col: string): string {
  return COL_LABELS[col] || col.replace(/_/g, ' ')
}

async function loadSummary() {
  loadingSummary.value = true
  try {
    tables.value = await getExtractedTables(selectedYear.value)
    if (tables.value.length > 0 && !activeTab.value) {
      activeTab.value = tables.value[0].tableName
    }
  } catch (e: any) {
    ElMessage.error('加载表摘要失败: ' + (e?.message ?? ''))
  } finally {
    loadingSummary.value = false
  }
}

async function loadTableData() {
  if (!activeTab.value) return
  loadingData.value = true
  try {
    const res = await queryExtractedTable(activeTab.value, {
      auditYear: selectedYear.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    })
    currentRows.value = res.rows ?? []
    currentTotal.value = res.total ?? 0
    if (currentRows.value.length > 0) {
      currentColumns.value = Object.keys(currentRows.value[0]).filter(c => !SYSTEM_COLS.has(c))
    } else {
      currentColumns.value = []
    }
  } catch (e: any) {
    ElMessage.error('加载数据失败: ' + (e?.message ?? ''))
    currentRows.value = []
    currentColumns.value = []
    currentTotal.value = 0
  } finally {
    loadingData.value = false
  }
}

function handleTabChange() {
  currentPage.value = 1
  loadTableData()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadTableData()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  loadTableData()
}

watch(selectedYear, () => {
  currentPage.value = 1
  loadSummary()
  loadTableData()
})

watch(activeTab, () => {
  if (activeTab.value) handleTabChange()
})

onMounted(() => {
  loadSummary()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">抽取数据总览</span>
          <div class="header-right">
            <el-select
              v-model="selectedYear"
              style="width:110px;margin-right:12px"
              size="small"
            >
              <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
            </el-select>
            <el-button @click="loadSummary(); loadTableData()" :loading="loadingSummary" size="small">
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" v-loading="loadingSummary">
        <el-tab-pane
          v-for="t in tables"
          :key="t.tableName"
          :name="t.tableName"
          :label="`${t.label} (${t.count})`"
        />
      </el-tabs>

      <el-table
        v-loading="loadingData"
        :data="currentRows"
        border
        stripe
        size="small"
        style="width:100%"
        max-height="520"
      >
        <el-table-column
          v-for="col in currentColumns"
          :key="col"
          :prop="col"
          :label="getColLabel(col)"
          min-width="130"
          show-overflow-tooltip
          :align="isNumericCol(col) ? 'right' : 'left'"
          :formatter="(_row: any, _column: any, cellValue: any) => formatCellValue(cellValue, col)"
        />
        <template #empty>
          <el-empty description="暂无数据" :image-size="60" />
        </template>
      </el-table>

      <div class="pagination-wrap" v-if="currentTotal > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="currentTotal"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
