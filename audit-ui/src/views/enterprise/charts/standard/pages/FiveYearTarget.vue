<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'
import { splitFiveYearRows, type Row } from '../utils/regulated-rows'

const loading = ref(false)
const summaryRows = ref<Row[]>([])
const productRows = ref<Row[]>([])
const annualRows = ref<Row[]>([])
const tableError = ref('')

// Field names mirror the snake_case keys returned by /extracted-data/{table}.
// The "2025年实际" / "2030年目标" parent labels are preserved in export via
// the hierarchical-header support in `exportTableToExcel`.
const summaryColumns: RegColumn[] = [
  {
    prop: '_actual2025', label: '2025年实际', children: [
      { prop: 'gross_output_actual2025', label: '产值（万元）', minWidth: 120 },
      { prop: 'energy_equiv_actual2025', label: '综合能耗（吨标煤）-当量值', minWidth: 170 },
      { prop: 'energy_equal_actual2025', label: '综合能耗（吨标煤）-等价值', minWidth: 170 },
    ],
  },
  {
    prop: '_target2030', label: '2030年目标', children: [
      { prop: 'gross_output_target2030', label: '产值（万元）', minWidth: 120 },
      { prop: 'energy_equiv_target2030', label: '综合能耗（吨标煤）-当量值', minWidth: 170 },
      { prop: 'energy_equal_target2030', label: '综合能耗（吨标煤）-等价值', minWidth: 170 },
    ],
  },
  { prop: 'decline_rate', label: '万元产值综合能耗下降率（%）', minWidth: 200 },
]

const productColumns: RegColumn[] = [
  { prop: 'product_name', label: '产品名称', minWidth: 120 },
  { prop: 'indicator_name', label: '单耗指标名称', minWidth: 150 },
  { prop: 'indicator_value', label: '2025单耗指标值', minWidth: 130 },
  { prop: 'actual_value', label: '2025单耗实际值', minWidth: 130 },
  { prop: 'target_name', label: '2030产品名称', minWidth: 120 },
  { prop: 'year_label', label: '2030单耗指标名', minWidth: 150 },
  { prop: 'y2030', label: '2030单耗指标值', minWidth: 130 },
  { prop: 'unit_energy_equal', label: '2030单耗实际值', minWidth: 130 },
  { prop: 'decline_rate', label: '单耗指标下降率（%）', minWidth: 150 },
]

const annualColumns: RegColumn[] = [
  { prop: 'target_name', label: '目标名称', minWidth: 200 },
  { prop: 'measurement_unit', label: '计量单位', minWidth: 100 },
  {
    prop: '_years', label: '2026-2030年', children: [
      { prop: 'y2026', label: '2026年', minWidth: 100 },
      { prop: 'y2027', label: '2027年', minWidth: 100 },
      { prop: 'y2028', label: '2028年', minWidth: 100 },
      { prop: 'y2029', label: '2029年', minWidth: 100 },
      { prop: 'y2030', label: '2030年', minWidth: 100 },
    ],
  },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_five_year_target', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    const allRows = (data.rows || []) as Row[]
    const sections = splitFiveYearRows(allRows)
    summaryRows.value = sections.summary
    productRows.value = sections.product
    annualRows.value = sections.annual
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <SectionTitle :title="'表17：\u201c十五五\u201d期间节能目标'" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />

    <RegulationTable
      :columns="summaryColumns"
      :data="summaryRows"
      :loading="loading"
      export-filename="十五五节能目标-总览"
      title="2025年实际 vs 2030年目标"
    />

    <RegulationTable
      :columns="productColumns"
      :data="productRows"
      :loading="loading"
      export-filename="十五五节能目标-产品单耗"
      title="产品单耗指标"
    />

    <RegulationTable
      :columns="annualColumns"
      :data="annualRows"
      :loading="loading"
      export-filename="十五五节能目标-年度"
      title="年度目标进度（2026-2030年）"
    />
  </div>
</template>
