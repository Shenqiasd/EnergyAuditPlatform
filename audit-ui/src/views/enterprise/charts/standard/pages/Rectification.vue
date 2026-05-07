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
  { prop: 'projectName', label: '整改项目名称', minWidth: 150 },
  { prop: 'projectType', label: '项目类型', minWidth: 100 },
  { prop: 'detailContent', label: '整改具体措施', minWidth: 200 },
  { prop: 'rectifyDate', label: '整改日期', minWidth: 110 },
  { prop: 'responsiblePerson', label: '责任人', minWidth: 80 },
  { prop: 'estimatedCost', label: '整改预计费用（万元）', minWidth: 160 },
  { prop: 'savingAmount', label: '年节能量（吨标准煤）', minWidth: 150 },
  { prop: 'carbonReduction', label: '年降碳量（吨二氧化碳）', minWidth: 160 },
  { prop: 'economicBenefit', label: '年经济效益（万元）', minWidth: 140 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_rectification', { pageSize: 200 }).catch((e: Error) => {
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
    <SectionTitle title="表16：节能整改措施表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="节能整改措施表"
      title="节能整改措施表"
    />
  </div>
</template>
