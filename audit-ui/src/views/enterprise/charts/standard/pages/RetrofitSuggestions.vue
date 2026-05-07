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
  { prop: 'projectName', label: '项目名称', minWidth: 150 },
  { prop: 'projectType', label: '项目类型', minWidth: 100 },
  { prop: 'mainContent', label: '主要内容', minWidth: 200 },
  { prop: 'energyCategory', label: '能源品类', minWidth: 100 },
  { prop: 'physicalQuantity', label: '实物量', minWidth: 100 },
  { prop: 'investment', label: '投资（万元）', minWidth: 110 },
  { prop: 'annualSaving', label: '年节能量（吨标煤）', minWidth: 140 },
  { prop: 'annualCarbonReduction', label: '年减碳量（吨二氧化碳）', minWidth: 150 },
  { prop: 'paybackPeriod', label: '投资回收期（年）', minWidth: 130 },
  { prop: 'remark', label: '备注', minWidth: 100 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_retrofit_suggestion', { pageSize: 200 }).catch((e: Error) => {
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
    <SectionTitle title="表15：节能技术改造建议汇总表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="节能技术改造建议汇总表"
      title="节能技术改造建议汇总表"
    />
  </div>
</template>
