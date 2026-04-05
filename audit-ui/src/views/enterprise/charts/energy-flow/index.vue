<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import FlowEditor from '@/components/FlowEditor/index.vue'
import { getEnergyFlowList, saveEnergyFlowBatch } from '@/api/energyFlow'
import type { EnergyFlowItem } from '@/api/energyFlow'

const currentYear = new Date().getFullYear()
const auditYear = ref(currentYear)
const flowData = ref<EnergyFlowItem[]>([])
const loading = ref(false)
const flowEditorRef = ref<InstanceType<typeof FlowEditor>>()

const sampleData: EnergyFlowItem[] = [
  { flowStage: 'purchased', sourceUnit: '电力公司', targetUnit: '变电站', energyProduct: '电力', physicalQuantity: 50000, standardQuantity: 6145 },
  { flowStage: 'purchased', sourceUnit: '天然气公司', targetUnit: '锅炉房', energyProduct: '天然气', physicalQuantity: 20000, standardQuantity: 2428 },
  { flowStage: 'purchased', sourceUnit: '煤炭供应商', targetUnit: '锅炉房', energyProduct: '原煤', physicalQuantity: 10000, standardQuantity: 7143 },
  { flowStage: 'conversion', sourceUnit: '锅炉房', targetUnit: '蒸汽管网', energyProduct: '蒸汽', physicalQuantity: 15000, standardQuantity: 5357 },
  { flowStage: 'distribution', sourceUnit: '变电站', targetUnit: '生产车间A', energyProduct: '电力', physicalQuantity: 30000, standardQuantity: 3687 },
  { flowStage: 'distribution', sourceUnit: '变电站', targetUnit: '办公区', energyProduct: '电力', physicalQuantity: 10000, standardQuantity: 1229 },
  { flowStage: 'distribution', sourceUnit: '蒸汽管网', targetUnit: '生产车间A', energyProduct: '蒸汽', physicalQuantity: 10000, standardQuantity: 3571 },
  { flowStage: 'terminal', sourceUnit: '生产车间A', targetUnit: '产品A', energyProduct: '综合能源', physicalQuantity: 0, standardQuantity: 7258 },
  { flowStage: 'terminal', sourceUnit: '办公区', targetUnit: '照明暖通', energyProduct: '电力', physicalQuantity: 10000, standardQuantity: 1229 },
]

async function loadData() {
  loading.value = true
  try {
    flowData.value = await getEnergyFlowList(auditYear.value)
  } catch {
    flowData.value = []
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!flowData.value.length) {
    ElMessage.warning('暂无数据可保存')
    return
  }
  try {
    await saveEnergyFlowBatch(auditYear.value, flowData.value)
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
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
    ElMessage.success('已导出 PNG')
  } catch {
    ElMessage.error('导出失败')
  }
}

function loadSampleData() {
  ElMessageBox.confirm('加载示例数据将替换当前数据，是否继续？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    flowData.value = [...sampleData]
    ElMessage.success('已加载示例数据')
  }).catch(() => {})
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
        <el-date-picker
          v-model="auditYear"
          type="year"
          placeholder="审计年度"
          format="YYYY"
          value-format="YYYY"
          style="width: 120px"
          @change="loadData"
        />
        <el-button @click="loadSampleData">加载示例</el-button>
        <el-button @click="handleFitView">适应画布</el-button>
        <el-button type="primary" @click="handleSave" :loading="loading">保存</el-button>
        <el-button @click="handleExportPng">导出 PNG</el-button>
      </div>
    </div>
    <div class="flow-wrapper" v-loading="loading">
      <FlowEditor ref="flowEditorRef" :data="flowData" />
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
