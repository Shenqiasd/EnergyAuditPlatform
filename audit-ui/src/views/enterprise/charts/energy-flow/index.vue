<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import FlowEditor from '@/components/FlowEditor/index.vue'
import type { EnergyBalanceItem } from '@/components/FlowEditor/index.vue'
import { getEnergyFlowList } from '@/api/energyFlow'
import type { EnergyFlowItem } from '@/api/energyFlow'
import { queryExtractedTable } from '@/api/extracted-data'

const currentYear = new Date().getFullYear()
const auditYear = ref(currentYear)
const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - i)
const flowData = ref<EnergyFlowItem[]>([])
const balanceData = ref<EnergyBalanceItem[]>([])
const loading = ref(false)
const flowEditorRef = ref<InstanceType<typeof FlowEditor>>()

async function loadData() {
  loading.value = true
  try {
    const [flows, balanceResult] = await Promise.all([
      getEnergyFlowList(auditYear.value).catch(() => [] as EnergyFlowItem[]),
      queryExtractedTable('de_energy_balance', { auditYear: auditYear.value, pageSize: 100 })
        .catch(() => ({ rows: [] as Record<string, unknown>[], total: 0 })),
    ])
    flowData.value = flows
    balanceData.value = (balanceResult.rows || []) as unknown as EnergyBalanceItem[]
  } catch {
    flowData.value = []
    balanceData.value = []
  } finally {
    loading.value = false
  }
}

async function handleExportPng() {
  if (!flowEditorRef.value) return
  try {
    const dataUri = await flowEditorRef.value.exportPng()
    if (!dataUri) {
      ElMessage.warning('导出失败，图表为空')
      return
    }
    const link = document.createElement('a')
    link.download = `energy-flow-${auditYear.value}.png`
    link.href = dataUri
    link.click()
    ElMessage.success('已导出')
  } catch {
    ElMessage.error('导出失败')
  }
}

function handleFitView() {
  flowEditorRef.value?.fitView()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="energy-flow-page">
    <div class="page-header">
      <h2>能源流向图 (C6)</h2>
      <div class="header-actions">
        <el-select v-model="auditYear" placeholder="审计年度" style="width: 120px" @change="loadData">
          <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
        </el-select>
        <el-button @click="loadData" :loading="loading">刷新数据</el-button>
        <el-button @click="handleFitView">适应画布</el-button>
        <el-button @click="handleExportPng">导出 PNG</el-button>
      </div>
    </div>
    <div class="flow-wrapper" v-loading="loading">
      <FlowEditor ref="flowEditorRef" :flow-data="flowData" :balance-data="balanceData" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.energy-flow-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 18px;
    color: #303133;
  }
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.flow-wrapper {
  flex: 1;
  min-height: 0;
}
</style>
