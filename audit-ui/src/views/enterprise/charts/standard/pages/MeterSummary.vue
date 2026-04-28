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
  { prop: 'managementId', label: '管理编号', minWidth: 100 },
  { prop: 'modelSpec', label: '型号规格', minWidth: 120 },
  { prop: 'manufacturer', label: '生产厂家', minWidth: 120 },
  { prop: 'factoryNo', label: '出厂编号', minWidth: 100 },
  { prop: 'meterName', label: '计量表名称', minWidth: 120 },
  { prop: 'ratio', label: '倍率', width: 80 },
  { prop: 'grade', label: '级别', width: 80 },
  { prop: 'energyAttribute', label: '能源属性', minWidth: 100 },
  { prop: 'measuringRange', label: '测量范围', minWidth: 100 },
  { prop: 'department', label: '所属部门', minWidth: 100 },
  { prop: 'accuracyGrade', label: '准确度等级', minWidth: 100 },
  { prop: 'installLocation', label: '安装地点或计量区域', minWidth: 140 },
  { prop: 'status', label: '状态', minWidth: 80 },
  { prop: 'remark', label: '备注', minWidth: 100 },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_meter_instrument', { pageSize: 500 }).catch((e: Error) => {
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
    <SectionTitle title="表6：能源计量器具汇总表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="能源计量器具汇总表"
      title="能源计量器具汇总表"
    />
  </div>
</template>
