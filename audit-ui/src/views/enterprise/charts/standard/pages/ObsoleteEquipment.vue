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
  { prop: 'seqNo', label: '序号', width: 60 },
  { prop: 'equipmentName', label: '淘汰设备名称', minWidth: 140 },
  { prop: 'modelSpec', label: '型号规格', minWidth: 120 },
  { prop: 'quantity', label: '数量', width: 70 },
  { prop: 'startUseDate', label: '开始使用日期', minWidth: 120 },
  { prop: 'plannedRetireDate', label: '计划淘汰日期', minWidth: 120 },
  { prop: 'remark', label: '备注', minWidth: 120 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_obsolete_equipment', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    rows.value = (data.rows || []).map((r, i) => ({ ...r, seqNo: i + 1 }))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <SectionTitle title="表10：淘汰产品、设备、装置、工艺和生产能力目录表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="淘汰产品设备目录表"
      title="淘汰产品、设备、装置、工艺和生产能力目录表"
    />
  </div>
</template>
