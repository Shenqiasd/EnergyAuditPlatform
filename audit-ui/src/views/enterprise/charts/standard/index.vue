<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import ChartCard from '@/components/ChartCard/index.vue'
import { getEnergyStructure, getEnergyTrend, getProductConsumption, getGhgEmission } from '@/api/chartData'
import type { EChartsOption } from 'echarts'

const auditYear = ref(new Date().getFullYear())
const loading = reactive({
  structure: false,
  trend: false,
  product: false,
  ghg: false,
})

const structureOption = ref<EChartsOption>({})
const trendOption = ref<EChartsOption>({})
const productOption = ref<EChartsOption>({})
const ghgOption = ref<EChartsOption>({})

const yearOptions = computed(() => {
  const current = new Date().getFullYear()
  return Array.from({ length: 5 }, (_, i) => current - i)
})

onMounted(() => {
  loadAllCharts()
})

async function loadAllCharts() {
  await Promise.all([
    loadEnergyStructure(),
    loadEnergyTrend(),
    loadProductConsumption(),
    loadGhgEmission(),
  ])
}

async function loadEnergyStructure() {
  loading.structure = true
  try {
    const data = await getEnergyStructure(auditYear.value) as any[]
    if (!data || !data.length) {
      structureOption.value = emptyOption('暂无能源消费数据')
      return
    }
    const total = data.reduce((s: number, d: any) => s + (d.STANDARD_COAL_EQUIV || d.VALUE || d.value || 0), 0)
    structureOption.value = {
      tooltip: {
        trigger: 'item',
        formatter: (p: any) => `${p.name}: ${p.value.toFixed(1)} tce (${p.percent}%)`,
      },
      legend: { orient: 'vertical', right: 20, top: 'center' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '50%'],
          avoidLabelOverlap: true,
          itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
          label: { show: true, formatter: '{b}\n{d}%' },
          data: data.map((d: any) => ({
            name: d.NAME || d.name,
            value: d.STANDARD_COAL_EQUIV || d.VALUE || d.value,
          })),
        },
      ],
      title: {
        text: `${total.toFixed(0)} tce`,
        subtext: '总能耗 (折标煤)',
        left: '40%',
        top: '38%',
        textAlign: 'center',
        textStyle: { fontSize: 18, fontWeight: 'bold', color: '#303133' },
        subtextStyle: { fontSize: 12, color: '#909399' },
      },
    }
  } catch (e) {
    structureOption.value = emptyOption('加载失败')
  } finally {
    loading.structure = false
  }
}

async function loadEnergyTrend() {
  loading.trend = true
  try {
    const data = await getEnergyTrend(auditYear.value) as any[]
    if (!data || !data.length) {
      trendOption.value = emptyOption('暂无趋势数据')
      return
    }
    const years = data.map((d: any) => String(d.YEAR || d.year))
    const totalEnergy = data.map((d: any) => d.TOTALENERGY || d.totalEnergy || 0)
    const unitEnergy = data.map((d: any) => d.UNITENERGY || d.unitEnergy || 0)
    const grossOutput = data.map((d: any) => d.GROSSOUTPUT || d.grossOutput || 0)

    trendOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { data: ['综合能耗(tce)', '工业总产值(万元)', '单位产值能耗(tce/万元)'] },
      grid: { left: 70, right: 70, bottom: 40 },
      xAxis: { type: 'category', data: years },
      yAxis: [
        { type: 'value', name: '能耗(tce)', position: 'left' },
        { type: 'value', name: '产值(万元) / 单耗(tce/万元)', position: 'right', splitLine: { show: false } },
      ],
      series: [
        {
          name: '综合能耗(tce)',
          type: 'bar',
          yAxisIndex: 0,
          data: totalEnergy,
          itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 40,
        },
        {
          name: '工业总产值(万元)',
          type: 'bar',
          yAxisIndex: 1,
          data: grossOutput,
          itemStyle: { color: '#67C23A', borderRadius: [4, 4, 0, 0] },
          barMaxWidth: 40,
        },
        {
          name: '单位产值能耗(tce/万元)',
          type: 'line',
          yAxisIndex: 1,
          data: unitEnergy,
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          lineStyle: { width: 2, color: '#E6A23C' },
          itemStyle: { color: '#E6A23C' },
        },
      ],
    }
  } catch (e) {
    trendOption.value = emptyOption('加载失败')
  } finally {
    loading.trend = false
  }
}

async function loadProductConsumption() {
  loading.product = true
  try {
    const data = await getProductConsumption(auditYear.value) as any[]
    if (!data || !data.length) {
      productOption.value = emptyOption('暂无产品单耗数据')
      return
    }
    const products = [...new Set(data.map((d: any) => d.PRODUCTNAME || d.productName))]
    const yearTypes = [...new Set(data.map((d: any) => d.YEARTYPE || d.yearType))]
    const colorMap: Record<string, string> = { '审计年': '#409EFF', '上年度': '#E6A23C' }

    productOption.value = {
      tooltip: { trigger: 'axis' },
      legend: { data: yearTypes },
      grid: { left: 60, right: 20, bottom: 40 },
      xAxis: { type: 'category', data: products },
      yAxis: { type: 'value', name: '单耗 (tce/t)' },
      series: yearTypes.map((yt: string) => ({
        name: yt,
        type: 'bar',
        barMaxWidth: 36,
        itemStyle: { color: colorMap[yt] || '#909399', borderRadius: [4, 4, 0, 0] },
        data: products.map((p: string) => {
          const row = data.find((d: any) => (d.PRODUCTNAME || d.productName) === p && (d.YEARTYPE || d.yearType) === yt)
          return row ? (row.UNITCONSUMPTION || row.unitConsumption || 0) : 0
        }),
      })),
    }
  } catch (e) {
    productOption.value = emptyOption('加载失败')
  } finally {
    loading.product = false
  }
}

async function loadGhgEmission() {
  loading.ghg = true
  try {
    const data = await getGhgEmission(auditYear.value) as any[]
    if (!data || !data.length) {
      ghgOption.value = emptyOption('暂无排放数据')
      return
    }
    const byType: Record<string, number> = {}
    const byEnergy: { name: string; value: number }[] = []
    for (const d of data) {
      const et = d.EMISSIONTYPE || d.emissionType
      const en = d.ENERGYNAME || d.energyName
      const val = d.ANNUALEMISSION || d.annualEmission || 0
      byType[et] = (byType[et] || 0) + val
      byEnergy.push({ name: en, value: val })
    }

    ghgOption.value = {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [
        {
          name: '排放类型',
          type: 'pie',
          radius: [0, '35%'],
          label: { position: 'inner', fontSize: 11, color: '#fff' },
          data: Object.entries(byType).map(([name, value]) => ({ name, value })),
          itemStyle: { borderColor: '#fff', borderWidth: 2 },
        },
        {
          name: '能源品种',
          type: 'pie',
          radius: ['45%', '65%'],
          label: { formatter: '{b}\n{c} tCO\u2082', fontSize: 11 },
          data: byEnergy.map((d) => ({ name: d.name, value: d.value })),
          itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        },
      ],
    }
  } catch (e) {
    ghgOption.value = emptyOption('加载失败')
  } finally {
    loading.ghg = false
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
  <div class="standard-charts-page">
    <div class="page-header">
      <h3>规定图表</h3>
      <el-select v-model="auditYear" style="width: 120px" @change="onYearChange">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <ChartCard title="能源消费结构" :option="structureOption" :loading="loading.structure" />
      </el-col>
      <el-col :xs="24" :lg="12">
        <ChartCard title="温室气体排放构成" :option="ghgOption" :loading="loading.ghg" />
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <ChartCard title="能耗趋势与单位产值能耗" :option="trendOption" :loading="loading.trend" />
      </el-col>
      <el-col :xs="24" :lg="12">
        <ChartCard title="产品单位能耗对比" :option="productOption" :loading="loading.product" />
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.standard-charts-page {
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
