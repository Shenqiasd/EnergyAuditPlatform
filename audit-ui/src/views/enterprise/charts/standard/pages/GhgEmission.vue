<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'
import { splitGhgRows, type Row } from '../utils/regulated-rows'

const loading = ref(false)
const summaryRows = ref<Row[]>([])
const fossilRows = ref<Row[]>([])
const electricHeatRows = ref<Row[]>([])
const processRows = ref<Row[]>([])
const tableError = ref('')

// Field names mirror the snake_case keys returned by /extracted-data/{table}
// (Spring `NamedParameterJdbcTemplate.queryForList` returns raw column names).
const summaryColumns: RegColumn[] = [
  { prop: 'emission_category', label: '排放类别', minWidth: 140 },
  { prop: 'source_name', label: '项目 / 排放源', minWidth: 200 },
  { prop: 'co2_emission', label: '排放量（tCO₂）', minWidth: 150 },
]

const fossilColumns: RegColumn[] = [
  { prop: 'source_name', label: '能源品种', minWidth: 120 },
  { prop: 'low_heat_value', label: '收到基低位发热值', minWidth: 140 },
  { prop: 'carbon_content', label: '单位热值含碳量', minWidth: 130 },
  { prop: 'oxidation_rate', label: '碳氧化率', minWidth: 100 },
  { prop: 'activity_data', label: '工业生产消耗量', minWidth: 140 },
  { prop: 'conversion_output', label: '能源加工转换产出', minWidth: 140 },
  { prop: 'recovery_amount', label: '回收利用', minWidth: 100 },
  { prop: 'co2_emission', label: 'CO₂排放量（t）', minWidth: 140 },
]

const electricHeatColumns: RegColumn[] = [
  { prop: 'source_name', label: '项目', minWidth: 160 },
  { prop: 'measurement_unit', label: '计量单位', minWidth: 100 },
  { prop: 'emission_factor', label: '排放因子', minWidth: 120 },
  { prop: 'activity_data', label: '消耗量', minWidth: 120 },
  { prop: 'co2_emission', label: '排放量（tCO₂）', minWidth: 140 },
]

const processColumns: RegColumn[] = [
  { prop: 'source_name', label: '原料 / 工艺名称', minWidth: 160 },
  { prop: 'emission_factor', label: '排放因子（tCO₂/t）', minWidth: 160 },
  { prop: 'activity_data', label: '净消耗量（t）', minWidth: 130 },
  { prop: 'co2_emission', label: 'CO₂排放量（t）', minWidth: 140 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_carbon_emission', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    const allRows = (data.rows || []) as Row[]
    const sections = splitGhgRows(allRows)
    summaryRows.value = sections.summary
    fossilRows.value = sections.fossil
    electricHeatRows.value = sections.electricHeat
    processRows.value = sections.process
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <SectionTitle title="表8：温室气体排放表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />

    <RegulationTable
      :columns="summaryColumns"
      :data="summaryRows"
      :loading="loading"
      export-filename="温室气体排放-汇总"
      title="排放汇总"
    />

    <SectionTitle title="化石燃料排放量明细" />
    <RegulationTable
      :columns="fossilColumns"
      :data="fossilRows"
      :loading="loading"
      export-filename="温室气体排放-化石燃料"
      title=""
    />

    <SectionTitle title="净购入电力、热力引用排放" />
    <RegulationTable
      :columns="electricHeatColumns"
      :data="electricHeatRows"
      :loading="loading"
      export-filename="温室气体排放-电力热力"
      title=""
    />

    <SectionTitle title="生产过程排放" />
    <RegulationTable
      :columns="processColumns"
      :data="processRows"
      :loading="loading"
      export-filename="温室气体排放-生产过程"
      title=""
    />
  </div>
</template>
