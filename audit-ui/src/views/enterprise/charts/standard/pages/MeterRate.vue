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
      { prop: 'inOutStandard', label: '配备率标准（%）' },
      { prop: 'inOutRequired', label: '需要配置数' },
      { prop: 'inOutActual', label: '实际配置数' },
      { prop: 'inOutRate', label: '配备率（%）' },
    ],
  },
  {
    prop: '_secondary', label: '进出主要次级用能单位', children: [
      { prop: 'secondaryStandard', label: '配备率标准（%）' },
      { prop: 'secondaryRequired', label: '需要配置数' },
      { prop: 'secondaryActual', label: '实际配置数' },
      { prop: 'secondaryRate', label: '配备率（%）' },
    ],
  },
  {
    prop: '_equipment', label: '主要用能设备', children: [
      { prop: 'equipmentStandard', label: '配备率标准（%）' },
      { prop: 'equipmentRequired', label: '需要配置数' },
      { prop: 'equipmentActual', label: '实际配置数' },
      { prop: 'equipmentRate', label: '配备率（%）' },
    ],
  },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_meter_rate', { pageSize: 200 }).catch((e: Error) => {
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
    { energyType: '电力', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 95 },
    { energyType: '固态能源-煤', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '固态能源-焦炭', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '固态能源-原煤', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '液态能源-成品油', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 95 },
    { energyType: '液态能源-重油', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '液态能源-渣油', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '气态能源-天然气', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '气态能源-液化气', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '气态能源-煤气', inOutStandard: 100, secondaryStandard: 90, equipmentStandard: 80 },
    { energyType: '载热工质-蒸汽', inOutStandard: 100, secondaryStandard: 80, equipmentStandard: 70 },
    { energyType: '载热工质-热水', inOutStandard: 100, secondaryStandard: 95, equipmentStandard: 80 },
    { energyType: '可回收余能', inOutStandard: 100, secondaryStandard: 80, equipmentStandard: 60 },
    { energyType: '其他', inOutStandard: 100, secondaryStandard: 90, equipmentStandard: 80 },
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
