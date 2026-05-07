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
  { prop: 'investment', label: '投资（万）', minWidth: 100 },
  { prop: 'designedSaving', label: '年节能量（吨标煤）', minWidth: 140 },
  { prop: 'paybackPeriod', label: '投资回收期（年）', minWidth: 130 },
  { prop: 'completionDate', label: '完成时间', minWidth: 110 },
  { prop: 'actualSaving', label: '实际节能量（吨标煤）', minWidth: 150 },
  { prop: 'isContractEnergy', label: '是否合同能源管理模式', minWidth: 160 },
  { prop: 'remark', label: '备注', minWidth: 100 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_tech_reform_history', { pageSize: 200 }).catch((e: Error) => {
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
    <SectionTitle title="表2：上一轮已实施的节能技改项目表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="上一轮已实施的节能技改项目表"
      title="上一轮已实施的节能技改项目表"
    />
  </div>
</template>
