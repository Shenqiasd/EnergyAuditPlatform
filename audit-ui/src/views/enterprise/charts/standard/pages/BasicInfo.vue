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
const productRows = ref<Record<string, unknown>[]>([])
const productLoading = ref(false)

const productColumns: RegColumn[] = [
  { prop: 'productName', label: '主要产品名称', minWidth: 120 },
  {
    prop: '_lastYear', label: '上年度', children: [
      { prop: 'lastYearOutput', label: '产品产量' },
      { prop: 'lastYearUnit', label: '产品单位' },
      { prop: 'lastYearConsumption', label: '产品单耗' },
      { prop: 'lastYearConsumptionUnit', label: '单耗单位' },
    ],
  },
  {
    prop: '_thisYear', label: '本年度审计期', children: [
      { prop: 'thisYearOutput', label: '产品产量' },
      { prop: 'thisYearUnit', label: '产品单位' },
      { prop: 'thisYearConsumption', label: '产品单耗' },
      { prop: 'thisYearConsumptionUnit', label: '单耗单位' },
    ],
  },
]

onMounted(async () => {
  loading.value = true
  try {
    const enterpriseId = userStore.userInfo?.enterpriseId
    const [setting, productData, ent] = await Promise.all([
      getEnterpriseSetting().catch(() => null),
      queryExtractedTable('de_product_unit_consumption', { pageSize: 200 }).catch(() => ({ rows: [], total: 0 })),
      enterpriseId ? getEnterpriseById(enterpriseId).catch(() => null) : Promise.resolve(null),
    ])
    if (setting) info.value = setting
    if (ent) enterprise.value = ent
    productRows.value = productData.rows || []
  } finally {
    loading.value = false
    productLoading.value = false
  }
})

function certLabel(val: unknown): string {
  return val === 1 ? '是' : val === 0 ? '否' : '—'
}
</script>

<template>
  <div v-loading="loading">
    <SectionTitle title="表1：用能单位基本情况" />

    <div class="info-table">
      <InfoRow :items="[
        { label: '单位名称', value: userStore.userInfo?.enterpriseName || enterprise?.enterpriseName },
        { label: '统一社会信用代码', value: enterprise?.creditCode || info.adminDivisionCode },
      ]" />
      <InfoRow :items="[
        { label: '所属区县/集团', value: info.groupName || info.region },
        { label: '行业代码', value: info.industryCode },
        { label: '行业分类名称', value: info.industryName },
        { label: '单位类型', value: info.unitNature },
      ]" />
      <InfoRow :items="[
        { label: '单位注册日期', value: info.registeredDate },
        { label: '单位注册资本（万元）', value: info.registeredCapital },
      ]" />
      <InfoRow :items="[
        { label: '单位地址', value: info.unitAddress || info.enterpriseAddress },
        { label: '邮政编码', value: info.postalCode },
      ]" />
      <InfoRow :items="[
        { label: '法定代表人姓名', value: info.legalRepresentative },
        { label: '联系电话', value: info.legalPhone },
      ]" />
      <InfoRow :items="[
        { label: '能源管理机构名称', value: info.energyMgmtOrg },
        { label: '传真（区号）', value: info.fax },
      ]" />
      <InfoRow :items="[
        { label: '单位主管节能领导姓名', value: info.energyLeaderName },
        { label: '联系电话', value: info.energyLeaderPhone },
      ]" />
      <InfoRow :items="[
        { label: '企业联系人', value: info.enterpriseContact },
        { label: '手机号', value: info.enterpriseMobile },
      ]" />
      <InfoRow :items="[
        { label: '联系电话', value: info.energyDeptLeaderPhone },
        { label: '电子邮箱', value: info.enterpriseEmail },
      ]" />
      <InfoRow :items="[
        { label: '编制单位联系人', value: info.compilerContact },
        { label: '手机号', value: info.compilerMobile },
      ]" />
      <InfoRow :items="[
        { label: '编制单位名称', value: info.compilerName },
        { label: '电子邮箱', value: info.compilerEmail },
      ]" />
      <InfoRow :items="[
        { label: '是否通过能源管理体系认证', value: certLabel(info.energyCert) },
        { label: '通过日期', value: info.certPassDate },
        { label: '认证机构', value: info.certAuthority },
      ]" />
    </div>

    <SectionTitle title="主要产品" />
    <RegulationTable
      :columns="productColumns"
      :data="productRows"
      :loading="productLoading"
      export-filename="用能单位基本情况-主要产品"
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
