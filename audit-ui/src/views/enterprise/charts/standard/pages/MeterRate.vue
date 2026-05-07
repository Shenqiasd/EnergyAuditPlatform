<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const tableError = ref('')

const columns: RegColumn[] = [
  { prop: 'energyType', label: '能源种类', minWidth: 120 },
  {
    prop: '_inOut', label: '进出用能单位', children: [
      { prop: 'l1StandardRate', label: '配备率标准（%）' },
      { prop: 'l1RequiredCount', label: '需要配置数' },
      { prop: 'l1ActualCount', label: '实际配置数' },
      { prop: 'l1ActualRate', label: '配备率（%）' },
    ],
  },
  {
    prop: '_secondary', label: '进出主要次级用能单位', children: [
      { prop: 'l2StandardRate', label: '配备率标准（%）' },
      { prop: 'l2RequiredCount', label: '需要配置数' },
      { prop: 'l2ActualCount', label: '实际配置数' },
      { prop: 'l2ActualRate', label: '配备率（%）' },
    ],
  },
  {
    prop: '_equipment', label: '主要用能设备', children: [
      { prop: 'l3StandardRate', label: '配备率标准（%）' },
      { prop: 'l3RequiredCount', label: '需要配置数' },
      { prop: 'l3ActualCount', label: '实际配置数' },
      { prop: 'l3ActualRate', label: '配备率（%）' },
    ],
  },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_meter_config_rate', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    if (data.rows?.length) {
      rows.value = data.rows
    } else {
      rows.value = getDefaultRows()
    }
  } finally {
    loading.value = false
  }
})

function getDefaultRows(): Record<string, unknown>[] {
  return [
    { energyType: '电力', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 95 },
    { energyType: '固态能源-煤', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 90 },
    { energyType: '固态能源-焦炭', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 90 },
    { energyType: '固态能源-原煤', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 90 },
    { energyType: '液态能源-成品油', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 95 },
    { energyType: '液态能源-重油', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 90 },
    { energyType: '液态能源-渣油', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 90 },
    { energyType: '气态能源-天然气', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 90 },
    { energyType: '气态能源-液化气', l1StandardRate: 100, l2StandardRate: 100, l3StandardRate: 90 },
    { energyType: '气态能源-煤气', l1StandardRate: 100, l2StandardRate: 90, l3StandardRate: 80 },
    { energyType: '载热工质-蒸汽', l1StandardRate: 100, l2StandardRate: 80, l3StandardRate: 70 },
    { energyType: '载热工质-热水', l1StandardRate: 100, l2StandardRate: 95, l3StandardRate: 80 },
    { energyType: '可回收余能', l1StandardRate: 100, l2StandardRate: 80, l3StandardRate: 60 },
    { energyType: '其他', l1StandardRate: 100, l2StandardRate: 90, l3StandardRate: 80 },
  ]
}
</script>

<template>
  <div>
    <SectionTitle title="表7：能源计量器具配备率表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="能源计量器具配备率表"
      title="能源计量器具配备率表"
    />
  </div>
</template>
