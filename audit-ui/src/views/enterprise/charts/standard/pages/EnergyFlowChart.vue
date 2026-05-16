<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EnergyFlowConfigView from '@/components/EnergyFlowConfigView/index.vue'
import { getEnergyFlowConfig } from '@/api/energyFlowConfig'
import type {
  FlowNodeConfig, FlowEdgeConfig, FlowRecord, EnergyInfo, UnitInfo, ProductInfo,
  EnergyConsumptionInfo, ValidationResult,
} from '@/api/energyFlowConfig'

const currentYear = new Date().getFullYear()
const auditYear = ref(currentYear)
const nodes = ref<FlowNodeConfig[]>([])
const edges = ref<FlowEdgeConfig[]>([])
const energies = ref<EnergyInfo[]>([])
const units = ref<UnitInfo[]>([])
const products = ref<ProductInfo[]>([])
const energyConsumption = ref<EnergyConsumptionInfo[]>([])
const flowRecords = ref<FlowRecord[]>([])
const validation = ref<ValidationResult>({ valid: false, exportReady: false, enterpriseComplete: false, hasUnits: false, hasEnergies: false, hasProducts: false, warnings: [], exportErrors: [] })
const enterpriseName = ref('')
const canvasWidth = ref(1200)
const canvasHeight = ref(800)
const loading = ref(false)
const viewRef = ref<InstanceType<typeof EnergyFlowConfigView>>()

async function loadData() {
  loading.value = true
  try {
    const config = await getEnergyFlowConfig(auditYear.value)
    enterpriseName.value = config.enterpriseInfo?.name || ''
    energies.value = config.energies || []
    units.value = config.units || []
    products.value = config.products || []
    energyConsumption.value = config.energyConsumption || []
    flowRecords.value = config.flowRecords || []
    if (config.validation) validation.value = config.validation
    if (config.diagram) {
      nodes.value = config.diagram.nodes || []
      edges.value = config.diagram.edges || []
      canvasWidth.value = config.diagram.canvasWidth || 1200
      canvasHeight.value = config.diagram.canvasHeight || 800
    } else {
      nodes.value = []
      edges.value = []
    }
  } catch {
    nodes.value = []
    edges.value = []
  } finally {
    loading.value = false
  }
}

async function handleExportPng() {
  if (!validation.value.exportReady) {
    const reasons = validation.value.exportErrors?.length
      ? validation.value.exportErrors.join('\n')
      : '前置资料不完整'
    ElMessageBox.alert(reasons, '无法导出 PNG — 数据验证未通过', { type: 'error' })
    return
  }
  if (!viewRef.value) return
  try {
    const dataUri = await viewRef.value.exportPng()
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

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="energy-flow-page">
    <div class="page-header">
      <h3>表5：能源流程图</h3>
      <div class="header-actions">
        <el-button @click="loadData" :loading="loading" size="small">刷新</el-button>
        <el-button @click="viewRef?.fitView()" size="small">适应画布</el-button>
        <el-button type="primary" @click="handleExportPng" size="small">导出 PNG</el-button>
      </div>
    </div>
    <div class="flow-wrapper" v-loading="loading">
      <EnergyFlowConfigView
        ref="viewRef"
        :nodes="nodes"
        :edges="edges"
        :flow-records="flowRecords"
        :energies="energies"
        :units="units"
        :products="products"
        :energy-consumption="energyConsumption"
        :validation="validation"
        :enterprise-name="enterpriseName"
        :audit-year="auditYear"
        :canvas-width="canvasWidth"
        :canvas-height="canvasHeight"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.energy-flow-page {
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h3 { margin: 0; font-size: 16px; color: #303133; }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.flow-wrapper {
  flex: 1;
  min-height: 0;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
}
</style>
