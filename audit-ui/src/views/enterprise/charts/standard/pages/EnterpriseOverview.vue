<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getEnterpriseSetting } from '@/api/enterpriseSetting'
import type { EnterpriseSetting } from '@/api/enterpriseSetting'
import { getById as getEnterpriseById } from '@/api/enterprise'
import type { Enterprise } from '@/api/enterprise'
import { queryExtractedTable } from '@/api/extracted-data'
import { useUserStore } from '@/stores/user'
import InfoRow from '../components/InfoRow.vue'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'

const userStore = useUserStore()
const loading = ref(false)
const info = ref<EnterpriseSetting>({})
const enterprise = ref<Enterprise | null>(null)
const indicatorRows = ref<Record<string, unknown>[]>([])
const energyRows = ref<Record<string, unknown>[]>([])
const tableLoading = ref(false)
const tableError = ref('')

const indicatorColumns: RegColumn[] = [
  { prop: 'code', label: '代码', width: 60 },
  { prop: 'itemName', label: '项目名称', minWidth: 200 },
  { prop: 'unit', label: '计量单位', minWidth: 100 },
  { prop: 'auditYearValue', label: '今年', minWidth: 120 },
  { prop: 'lastYearValue', label: '去年', minWidth: 120 },
  { prop: 'changeRate', label: '增减%', minWidth: 100 },
]

const energyColumns: RegColumn[] = [
  { prop: 'code', label: '代码', width: 60 },
  { prop: 'itemName', label: '项目名称', minWidth: 200 },
  { prop: 'unit', label: '计量单位', minWidth: 100 },
  { prop: 'auditYearValue', label: '今年', minWidth: 120 },
  { prop: 'lastYearValue', label: '去年', minWidth: 120 },
  { prop: 'changeRate', label: '增减%', minWidth: 100 },
  { prop: 'excludingRawMaterial', label: '扣除原材料后', minWidth: 120 },
]

onMounted(async () => {
  loading.value = true
  tableLoading.value = true
  try {
    const enterpriseId = userStore.userInfo?.enterpriseId
    const [setting, tableData, ent] = await Promise.all([
      getEnterpriseSetting().catch(() => null),
      queryExtractedTable('de_tech_indicator', { pageSize: 200 }).catch((e: Error) => {
        tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
        return { rows: [], total: 0 }
      }),
      enterpriseId ? getEnterpriseById(enterpriseId).catch(() => null) : Promise.resolve(null),
    ])
    if (setting) info.value = setting
    if (ent) enterprise.value = ent
    const allRows = tableData.rows || []
    indicatorRows.value = allRows.filter((r: Record<string, unknown>) => r.section !== 'energy')
    energyRows.value = allRows.filter((r: Record<string, unknown>) => r.section === 'energy')
    if (!energyRows.value.length && !indicatorRows.value.length) {
      indicatorRows.value = allRows
    }
  } finally {
    loading.value = false
    tableLoading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <SectionTitle title="表3：企业概况及主要技术指标一览表" />

    <div class="info-table">
      <InfoRow :items="[
        { label: '单位名称', value: userStore.userInfo?.enterpriseName || enterprise?.enterpriseName },
        { label: '法人代码', value: enterprise?.creditCode || info.adminDivisionCode },
      ]" />
      <InfoRow :items="[
        { label: '节能主管领导姓名/职务', value: [info.energyLeaderName, info.energyLeaderTitle].filter(Boolean).join(' / ') || '—' },
        { label: '节能主管部门名称', value: info.energyDeptName },
      ]" />
      <InfoRow :items="[
        { label: '节能管理部门负责人姓名', value: info.energyManagerName },
        { label: '专职管理人数', value: info.remark },
      ]" />
      <InfoRow :items="[
        { label: '兼职管理人数', value: undefined },
        { label: '&quot;十五五&quot;期间节能目标名称', value: undefined },
      ]" />
      <InfoRow :items="[
        { label: '&quot;十五五&quot;期间节能目标值', value: undefined },
        { label: '&quot;十五五&quot;期间节能目标下达部门', value: info.superiorDepartment },
      ]" />
    </div>

    <SectionTitle title="主要经济技术指标" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="indicatorColumns"
      :data="indicatorRows"
      :loading="tableLoading"
      export-filename="企业概况及主要技术指标"
      title=""
    />

    <SectionTitle v-if="energyRows.length" title="综合能耗指标" />
    <RegulationTable
      v-if="energyRows.length"
      :columns="energyColumns"
      :data="energyRows"
      :loading="tableLoading"
      export-filename="综合能耗指标"
      title=""
    />
  </div>
</template>

<style scoped lang="scss">
.info-table {
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
}
</style>
