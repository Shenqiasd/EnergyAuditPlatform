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
  energy_leader_name: '能源负责人',
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
  coal_target: '煤炭目标',
  coal_actual: '煤炭实际',
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
  loadSummary().then(() => loadTableData())
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
