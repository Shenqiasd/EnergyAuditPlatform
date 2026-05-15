<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import { equipmentEnergyColumns } from '../definitions'

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const tableError = ref('')

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_equipment_energy', { pageSize: 100 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    rows.value = (data.rows || []).slice().reverse()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <SectionTitle title="表6：重点设备能耗和效率" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="equipmentEnergyColumns"
      :data="rows"
      :loading="loading"
      export-filename="重点设备能耗和效率"
      title="重点设备能耗和效率"
    />
  </div>
</template>
