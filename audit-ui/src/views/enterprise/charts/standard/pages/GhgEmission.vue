<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'

const loading = ref(false)
const summaryRows = ref<Record<string, unknown>[]>([])
const fossilRows = ref<Record<string, unknown>[]>([])
const electricHeatRows = ref<Record<string, unknown>[]>([])
const processRows = ref<Record<string, unknown>[]>([])
const tableError = ref('')

const summaryColumns: RegColumn[] = [
  { prop: 'category', label: '排放类别', minWidth: 120 },
  { prop: 'itemName', label: '项目', minWidth: 200 },
  { prop: 'emission', label: '排放量（tCO₂）', minWidth: 150 },
]

const fossilColumns: RegColumn[] = [
  { prop: 'energyName', label: '能源品种', minWidth: 120 },
  { prop: 'lowerHeatingValue', label: '收到基低位发热值', minWidth: 130 },
  { prop: 'carbonContent', label: '单位热值含碳量', minWidth: 120 },
  { prop: 'oxidationRate', label: '碳氧化率', minWidth: 100 },
  { prop: 'industrialConsumption', label: '工业生产消耗量', minWidth: 130 },
  { prop: 'conversionOutput', label: '能源加工转换产出', minWidth: 130 },
  { prop: 'recycled', label: '回收利用', minWidth: 100 },
  { prop: 'co2Emission', label: 'CO₂排放量（t）', minWidth: 140 },
]

const electricHeatColumns: RegColumn[] = [
  { prop: 'itemName', label: '项目', minWidth: 160 },
  { prop: 'unit', label: '计量单位', minWidth: 100 },
  { prop: 'emissionFactor', label: '排放因子', minWidth: 120 },
  { prop: 'consumption', label: '消耗量', minWidth: 120 },
  { prop: 'co2Emission', label: '排放量（tCO₂）', minWidth: 140 },
]

const processColumns: RegColumn[] = [
  { prop: 'rawMaterialName', label: '原料名称', minWidth: 140 },
  { prop: 'emissionFactor', label: '排放因子（tCO₂/t）', minWidth: 150 },
  { prop: 'consumption', label: '净消耗量（t）', minWidth: 120 },
  { prop: 'co2Emission', label: 'CO₂排放量（t）', minWidth: 140 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_carbon_emission', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    const allRows = data.rows || []
    summaryRows.value = allRows.filter((r: Record<string, unknown>) => r.section === 'summary' || r.category)
    fossilRows.value = allRows.filter((r: Record<string, unknown>) => r.section === 'fossil')
    electricHeatRows.value = allRows.filter((r: Record<string, unknown>) => r.section === 'electric_heat')
    processRows.value = allRows.filter((r: Record<string, unknown>) => r.section === 'process')
    if (!summaryRows.value.length && !fossilRows.value.length) {
      summaryRows.value = allRows
    }
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

    <SectionTitle v-if="fossilRows.length" title="化石燃料排放量明细" />
    <RegulationTable
      v-if="fossilRows.length"
      :columns="fossilColumns"
      :data="fossilRows"
      :loading="loading"
      export-filename="温室气体排放-化石燃料"
      title=""
    />

    <SectionTitle v-if="electricHeatRows.length" title="净购入电力热力引用排放" />
    <RegulationTable
      v-if="electricHeatRows.length"
      :columns="electricHeatColumns"
      :data="electricHeatRows"
      :loading="loading"
      export-filename="温室气体排放-电力热力"
      title=""
    />

    <SectionTitle v-if="processRows.length" title="生产过程排放" />
    <RegulationTable
      v-if="processRows.length"
      :columns="processColumns"
      :data="processRows"
      :loading="loading"
      export-filename="温室气体排放-生产过程"
      title=""
    />
  </div>
</template>
