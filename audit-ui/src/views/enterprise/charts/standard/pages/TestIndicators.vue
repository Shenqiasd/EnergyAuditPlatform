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
  { prop: 'deviceNo', label: '设备编号', minWidth: 100 },
  { prop: 'deviceName', label: '设备名称', minWidth: 120 },
  { prop: 'modelSpec', label: '型号规格', minWidth: 120 },
  {
    prop: '_testIndicator', label: '测试指标', children: [
      { prop: 'testIndicatorName', label: '名称', minWidth: 120 },
      { prop: 'measurementUnit', label: '计量单位', minWidth: 80 },
      { prop: 'qualifiedValue', label: '合格值或限值', minWidth: 110 },
      { prop: 'actualValue', label: '实测值', minWidth: 80 },
      { prop: 'judgement', label: '判别', width: 70 },
      { prop: 'testDate', label: '测试日期', minWidth: 100 },
    ],
  },
  { prop: 'remark', label: '备注', minWidth: 100 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_equipment_test', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    rows.value = data.rows || []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <SectionTitle title="表12：设备测试报告主要指标汇总表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="设备测试报告主要指标汇总表"
      title="设备测试报告主要指标汇总表"
    />
  </div>
</template>
