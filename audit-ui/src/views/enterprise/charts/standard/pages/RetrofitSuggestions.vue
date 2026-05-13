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
  { prop: 'investment', label: '投资（万元）', minWidth: 110 },
  { prop: 'annualSaving', label: '年节能量（吨标煤）', minWidth: 140 },
  { prop: 'annualCarbonReduction', label: '年减碳量（吨二氧化碳）', minWidth: 150 },
  { prop: 'paybackPeriod', label: '投资回收期（年）', minWidth: 130 },
  { prop: 'remark', label: '备注', minWidth: 100 },
]

function toNumOrInf(v: unknown): number {
  if (v === null || v === undefined || v === '') return Infinity
  const n = Number(v)
  return Number.isFinite(n) ? n : Infinity
}

function sortByTemplateOrder(items: Record<string, unknown>[]): Record<string, unknown>[] {
  return items
    .map((r, i) => ({ r, i }))
    .sort((a, b) => {
      const seqDiff = toNumOrInf(a.r.seq_no) - toNumOrInf(b.r.seq_no)
      if (seqDiff !== 0) return seqDiff
      const idDiff = toNumOrInf(a.r.id) - toNumOrInf(b.r.id)
      if (idDiff !== 0) return idDiff
      return a.i - b.i
    })
    .map(({ r }) => r)
}

function adaptRow(r: Record<string, unknown>, index: number): Record<string, unknown> {
  return {
    seqNo: r.seq_no ?? index + 1,
    projectName: r.project_name ?? '',
    projectType: r.project_type ?? '',
    mainContent: r.main_content ?? '',
    investment: r.investment ?? '',
    annualSaving: r.annual_saving ?? '',
    annualCarbonReduction: r.annual_carbon_reduction ?? '',
    paybackPeriod: r.payback_period ?? '',
    remark: r.remark ?? '',
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_tech_reform_suggestion', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    rows.value = sortByTemplateOrder(data.rows || []).map(adaptRow)
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
