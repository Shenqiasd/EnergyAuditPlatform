<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

// EChartsOption's generated type has deeply-nested variance mismatches
// (e.g. `graphic.elements.children` widens to `never[]` in some paths) that
// reject perfectly valid option objects built by callers. Accept the same
// loose shape echarts.setOption itself takes.
type EChartsOptionLoose = echarts.EChartsOption | Record<string, unknown>

const props = defineProps<{
  title: string
  option: EChartsOptionLoose
  height?: string
  loading?: boolean
}>()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

onMounted(() => {
  nextTick(() => {
    initChart()
  })
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})

watch(
  () => props.option,
  (val) => {
    if (chart && val) {
      chart.setOption(val as echarts.EChartsOption, true)
    }
  },
  { deep: true }
)

function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  if (props.option) {
    chart.setOption(props.option as echarts.EChartsOption)
  }
}

function handleResize() {
  chart?.resize()
}

defineExpose({
  getChart: () => chart,
  resize: handleResize,
})
</script>

<template>
  <el-card class="chart-card" shadow="hover">
    <template #header>
      <span class="chart-title">{{ title }}</span>
    </template>
    <div v-loading="!!loading" class="chart-body">
      <div ref="chartRef" class="chart-container" :style="{ height: height || '360px' }"></div>
    </div>
  </el-card>
</template>

<style scoped lang="scss">
.chart-card {
  margin-bottom: 16px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-body {
  width: 100%;
}

.chart-container {
  width: 100%;
}
</style>
