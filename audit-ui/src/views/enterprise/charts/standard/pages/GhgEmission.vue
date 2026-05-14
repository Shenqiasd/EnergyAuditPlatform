<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedScalars, queryExtractedTable } from '@/api/extracted-data'
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
  { prop: 'emission_category', label: '排放类别', minWidth: 160 },
  { prop: 'source_name', label: '项目 / 排放源', minWidth: 200 },
  { prop: 'co2_emission', label: '排放量（tCO₂）', minWidth: 160 },
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

/**
 * Sheet 15 scalar mappings persist into generic storage (`de_submission_field`)
 * because the `de_carbon_emission` table has no matching columns
 * (see sql/02-wave4-data-extraction.sql + sql/03-wave6-schema-expansion.sql).
 * We fetch them via the dedicated /extracted-data/scalars endpoint and render
 * them as the 排放汇总 fixed section so the customer-template structure
 * matches even when no row-level data exists.
 */
const SCALAR_SUMMARY_FIELDS: { field: string; category: string; source: string }[] = [
  { field: 'directEmission', category: '直接排放', source: '化石燃料燃烧 + 生产过程排放' },
  { field: 'indirectEmission', category: '间接排放', source: '净购入电力 + 热力引用' },
  { field: 'heatEmission', category: '间接排放-热力', source: '热力排放量' },
  { field: 'elecEmission', category: '间接排放-电力', source: '电力排放量' },
  { field: 'greenElecOffset', category: '抵消', source: '购买绿电抵消排放量' },
  { field: 'totalEmission', category: '合计', source: '碳排放量合计' },
]

onMounted(async () => {
  loading.value = true
  try {
    const [tableData, scalars] = await Promise.all([
      queryExtractedTable('de_carbon_emission', { pageSize: 200 }).catch((e: Error) => {
        tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
        return { rows: [], total: 0 }
      }),
      queryExtractedScalars(
        SCALAR_SUMMARY_FIELDS.map((f) => f.field),
      ).catch(() => ({} as Record<string, number | string | null>)),
    ])

    const allRows = (tableData.rows || []) as Row[]
    const sections = splitGhgRows(allRows)

    // Build the fixed 排放汇总 section from scalar fields. Each entry is
    // always present (value may be null) so the customer-template structure
    // renders even before submission data is available. Any extra summary
    // rows that came through de_carbon_emission are appended afterwards.
    const scalarSummary: Row[] = SCALAR_SUMMARY_FIELDS.map((f) => ({
      emission_category: f.category,
      source_name: f.source,
      co2_emission: scalars[f.field] ?? null,
    }))
    summaryRows.value = [...scalarSummary, ...sections.summary]
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
