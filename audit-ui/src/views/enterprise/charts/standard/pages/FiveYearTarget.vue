<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'

const loading = ref(false)
const summaryRows = ref<Record<string, unknown>[]>([])
const productRows = ref<Record<string, unknown>[]>([])
const annualRows = ref<Record<string, unknown>[]>([])
const tableError = ref('')

const summaryColumns: RegColumn[] = [
  {
    prop: '_actual2025', label: '2025年实际', children: [
      { prop: 'actual2025Output', label: '产量', minWidth: 100 },
      { prop: 'actual2025EnergyEquivalent', label: '综合能耗（吨标煤）-等价值', minWidth: 160 },
      { prop: 'actual2025EnergyCalorific', label: '综合能耗（吨标煤）-当量值', minWidth: 160 },
      { prop: 'actual2025UnitConsumption', label: '产品综合能耗（吨标煤/万元）', minWidth: 180 },
    ],
  },
  {
    prop: '_target2030', label: '2030年目标', children: [
      { prop: 'target2030Output', label: '产量', minWidth: 100 },
      { prop: 'target2030EnergyEquivalent', label: '综合能耗（吨标煤）-等价值', minWidth: 160 },
      { prop: 'target2030EnergyCalorific', label: '综合能耗（吨标煤）-当量值', minWidth: 160 },
      { prop: 'target2030UnitConsumption', label: '产品综合能耗（吨标煤/万元）', minWidth: 180 },
    ],
  },
  { prop: 'declineRate', label: '万元产值综合能耗下降率（%）', minWidth: 200 },
]

const productColumns: RegColumn[] = [
  { prop: 'productName', label: '产品名称', minWidth: 120 },
  { prop: 'indicatorName', label: '单耗指标名称', minWidth: 150 },
  { prop: 'indicatorValue', label: '单耗指标值', minWidth: 100 },
  { prop: 'targetProductName', label: '目标产品名称', minWidth: 120 },
  { prop: 'targetIndicatorName', label: '目标单耗指标名称', minWidth: 150 },
  { prop: 'targetIndicatorValue', label: '目标单耗指标值', minWidth: 100 },
  { prop: 'indicatorDeclineRate', label: '单耗指标下降率（%）', minWidth: 150 },
]

const annualColumns: RegColumn[] = [
  { prop: 'targetName', label: '目标名称', minWidth: 200 },
  { prop: 'unit', label: '计量单位', minWidth: 100 },
  { prop: 'y2026', label: '2026年', minWidth: 100 },
  { prop: 'y2027', label: '2027年', minWidth: 100 },
  { prop: 'y2028', label: '2028年', minWidth: 100 },
  { prop: 'y2029', label: '2029年', minWidth: 100 },
  { prop: 'y2030', label: '2030年', minWidth: 100 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_five_year_target', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    const allRows = data.rows || []
    summaryRows.value = allRows.filter((r: Record<string, unknown>) => r.rowType === 'summary')
    productRows.value = allRows.filter((r: Record<string, unknown>) => r.rowType === 'product')
    annualRows.value = allRows.filter((r: Record<string, unknown>) => r.rowType === 'annual' || r.y2026 !== undefined)
    if (!summaryRows.value.length && !annualRows.value.length && !productRows.value.length) {
      summaryRows.value = allRows
    }
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
      v-if="productRows.length"
      :columns="productColumns"
      :data="productRows"
      :loading="loading"
      export-filename="十五五节能目标-产品单耗"
      title="产品单耗指标"
    />

    <RegulationTable
      v-if="annualRows.length"
      :columns="annualColumns"
      :data="annualRows"
      :loading="loading"
      export-filename="十五五节能目标-年度"
      title="年度目标进度（2026-2030年）"
    />
  </div>
</template>
