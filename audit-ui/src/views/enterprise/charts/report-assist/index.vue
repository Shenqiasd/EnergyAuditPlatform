<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import ChartCard from '@/components/ChartCard/index.vue'
import { getEnergyStructure, getEnergyTrend, getProductConsumption, getGhgEmission } from '@/api/chartData'
import type { EChartsOption } from 'echarts'

const auditYear = ref(new Date().getFullYear())
const loading = reactive({
  energyCost: false,
  energyCategory: false,
  emissionBar: false,
  outputEnergy: false,
})

const energyCostOption = ref<EChartsOption>({})
const energyCategoryOption = ref<EChartsOption>({})
const emissionBarOption = ref<EChartsOption>({})
const outputEnergyOption = ref<EChartsOption>({})

const yearOptions = computed(() => {
  const current = new Date().getFullYear()
  return Array.from({ length: 5 }, (_, i) => current - i)
})

onMounted(() => {
  loadAllCharts()
})

async function loadAllCharts() {
  await Promise.all([
    loadEnergyCostRatio(),
    loadEnergyByCategory(),
    loadEmissionBarChart(),
    loadOutputVsEnergy(),
  ])
}

async function loadEnergyCostRatio() {
  loading.energyCost = true
  try {
    const data = await getEnergyTrend(auditYear.value) as any[]
    if (!data || !data.length) {
      energyCostOption.value = emptyOption('暂无数据')
      return
    }
    const years = data.map((d: any) => String(d.YEAR || d.year))
    const totalEnergy = data.map((d: any) => d.TOTALENERGY || d.totalEnergy || 0)
    const totalEnergyEqual = data.map((d: any) => d.TOTALENERGYEQUAL || d.totalEnergyEqual || 0)

    energyCostOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['当量值(tce)', '等价值(tce)'] },
      grid: { left: 60, right: 20, bottom: 40 },
      xAxis: { type: 'category', data: years },
      yAxis: { type: 'value', name: 'tce' },
      series: [
        {
          name: '当量值(tce)',
          type: 'bar',
          data: totalEnergy,
          itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 36,
        },
        {
          name: '等价值(tce)',
          type: 'bar',
          data: totalEnergyEqual,
          itemStyle: { color: '#67C23A', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 36,
        },
      ],
    }
  } catch (e) {
    energyCostOption.value = emptyOption('加载失败')
  } finally {
    loading.energyCost = false
  }
}

async function loadEnergyByCategory() {
  loading.energyCategory = true
  try {
    const data = await getEnergyStructure(auditYear.value) as any[]
    if (!data || !data.length) {
      energyCategoryOption.value = emptyOption('暂无数据')
      return
    }
    const categoryMap: Record<string, number> = {}
    for (const d of data) {
      const cat = d.CATEGORY || d.category || '其他'
      const val = d.STANDARD_COAL_EQUIV || d.VALUE || d.value || 0
      categoryMap[cat] = (categoryMap[cat] || 0) + val
    }
    const categories = Object.keys(categoryMap)
    const values = Object.values(categoryMap)

    energyCategoryOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 80, right: 20, bottom: 40 },
      xAxis: { type: 'category', data: categories },
      yAxis: { type: 'value', name: 'tce' },
      series: [
        {
          type: 'bar',
          data: values,
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: (params: any) => {
              const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
              return colors[params.dataIndex % colors.length]
            },
          },
          barMaxWidth: 50,
          label: { show: true, position: 'top', formatter: '{c}', fontSize: 11 },
        },
      ],
    }
  } catch (e) {
    energyCategoryOption.value = emptyOption('加载失败')
  } finally {
    loading.energyCategory = false
  }
}

async function loadEmissionBarChart() {
  loading.emissionBar = true
  try {
    const data = await getGhgEmission(auditYear.value) as any[]
    if (!data || !data.length) {
      emissionBarOption.value = emptyOption('暂无排放数据')
      return
    }
    const names = data.map((d: any) => d.ENERGYNAME || d.energyName || '未知')
    const emissions = data.map((d: any) => d.ANNUALEMISSION || d.annualEmission || 0)
    const types = data.map((d: any) => d.EMISSIONTYPE || d.emissionType)

    emissionBarOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 100, right: 20, bottom: 40 },
      yAxis: { type: 'category', data: names, inverse: true },
      xAxis: { type: 'value', name: 'tCO\u2082' },
      series: [
        {
          type: 'bar',
          data: emissions.map((v: number, i: number) => ({
            value: v,
            itemStyle: {
              color: types[i] === '直接排放' ? '#F56C6C' : '#409EFF',
              borderRadius: [0, 4, 4, 0],
            },
          })),
          barMaxWidth: 24,
          label: { show: true, position: 'right', formatter: '{c}', fontSize: 11 },
        },
      ],
    }
  } catch (e) {
    emissionBarOption.value = emptyOption('加载失败')
  } finally {
    loading.emissionBar = false
  }
}

async function loadOutputVsEnergy() {
  loading.outputEnergy = true
  try {
    const data = await getProductConsumption(auditYear.value) as any[]
    if (!data || !data.length) {
      outputEnergyOption.value = emptyOption('暂无数据')
      return
    }
    const auditData = data.filter((d: any) => (d.YEARTYPE || d.yearType) === '审计年')
    if (!auditData.length) {
      outputEnergyOption.value = emptyOption('暂无审计年数据')
      return
    }
    const products = auditData.map((d: any) => d.PRODUCTNAME || d.productName)
    const outputs = auditData.map((d: any) => d.OUTPUT || d.output || 0)
    const energyVals = auditData.map((d: any) => d.ENERGYCONSUMPTION || d.energyConsumption || 0)

    outputEnergyOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['产量(吨)', '能耗(tce)'] },
      grid: { left: 60, right: 60, bottom: 40 },
      xAxis: { type: 'category', data: products },
      yAxis: [
        { type: 'value', name: '产量(吨)', position: 'left' },
        { type: 'value', name: '能耗(tce)', position: 'right', splitLine: { show: false } },
      ],
      series: [
        {
          name: '产量(吨)',
          type: 'bar',
          data: outputs,
          itemStyle: { color: '#67C23A', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 36,
        },
        {
          name: '能耗(tce)',
          type: 'bar',
          yAxisIndex: 1,
          data: energyVals,
          itemStyle: { color: '#E6A23C', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 36,
        },
      ],
    }
  } catch (e) {
    outputEnergyOption.value = emptyOption('加载失败')
  } finally {
    loading.outputEnergy = false
  }
}

function emptyOption(text: string): EChartsOption {
  return {
    title: {
      text,
      left: 'center',
      top: 'center',
      textStyle: { color: '#c0c4cc', fontSize: 14, fontWeight: 'normal' },
    },
  }
}

function onYearChange() {
  loadAllCharts()
}
</script>

<template>
  <div class="report-assist-page">
    <div class="page-header">
      <h3>报告辅助图表</h3>
      <el-select v-model="auditYear" style="width: 120px" @change="onYearChange">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <ChartCard title="综合能耗当量值 vs 等价值" :option="energyCostOption" :loading="loading.energyCost" />
      </el-col>
      <el-col :xs="24" :lg="12">
        <ChartCard title="按能源类别分析" :option="energyCategoryOption" :loading="loading.energyCategory" />
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <ChartCard title="温室气体排放明细" :option="emissionBarOption" :loading="loading.emissionBar" />
      </el-col>
      <el-col :xs="24" :lg="12">
        <ChartCard title="产品产量 vs 能耗" :option="outputEnergyOption" :loading="loading.outputEnergy" />
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.report-assist-page {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h3 {
    margin: 0;
    font-size: 18px;
    color: #303133;
  }
}
</style>
