<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'

const loading = ref(false)
const allRows = ref<Record<string, unknown>[]>([])
const searchKeyword = ref('')
const tableError = ref('')

const columns: RegColumn[] = [
  { prop: 'seqNo', label: '序号', width: 60 },
  { prop: 'deviceName', label: '设备名称', minWidth: 120 },
  { prop: 'category', label: '分类', minWidth: 80 },
  { prop: 'model', label: '型号', minWidth: 100 },
  { prop: 'capacity', label: '容量', minWidth: 80 },
  { prop: 'quantity', label: '数量', width: 70 },
  { prop: 'deviceOverview', label: '设备概况', minWidth: 140 },
  { prop: 'obsoleteUpdateInfo', label: '淘汰更新情况', minWidth: 120 },
  { prop: 'installLocation', label: '安装使用场所', minWidth: 120 },
  { prop: 'annualRuntimeHours', label: '年运行时间（小时）', minWidth: 140 },
  { prop: 'remark', label: '备注', minWidth: 100 },
]

const filteredRows = computed(() => {
  if (!searchKeyword.value) return allRows.value
  const kw = searchKeyword.value.toLowerCase()
  return allRows.value.filter((r) => {
    const name = String(r.deviceName || '').toLowerCase()
    return name.includes(kw)
  })
})

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_equipment_summary', { pageSize: 500 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    allRows.value = (data.rows || []).map((r, i) => ({ ...r, seqNo: i + 1 }))
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <SectionTitle title="表4：主要用能设备汇总表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <div style="margin-bottom: 12px">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索设备名称..."
        clearable
        style="width: 300px"
        prefix-icon="Search"
      />
    </div>
    <RegulationTable
      :columns="columns"
      :data="filteredRows"
      :loading="loading"
      export-filename="主要用能设备汇总表"
      title="主要用能设备汇总表"
    />
  </div>
</template>
