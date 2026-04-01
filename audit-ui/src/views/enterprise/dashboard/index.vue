<script setup lang="ts">
import { ref } from 'vue'

const stats = ref([
  {
    label: '综合能耗（当量值）',
    value: '12,847',
    unit: '吨标煤',
    trend: '↓ 3.2%',
    trendType: 'down',
    trendText: '较上年同期',
    highlight: true,
  },
  {
    label: '碳排放总量',
    value: '8,234',
    unit: 'tCO₂',
    trend: '↓ 5.1%',
    trendType: 'down',
    trendText: '较上年同期',
    barWidth: 65,
  },
  {
    label: '单位产值能耗',
    value: '0.183',
    unit: '吨标煤/万元',
    trend: '↓ 7.8%',
    trendType: 'down',
    trendText: '较上年同期',
    barWidth: 82,
  },
  {
    label: '填报完整度',
    value: '78',
    unit: '%',
    trend: '⚠ 还有 4 个模块未填报',
    trendType: 'warning',
    barWidth: 78,
    barColor: 'linear-gradient(90deg,#ffa726,#ffca28)',
  },
])

const progressItems = ref([
  { name: '基本设置（企业/能源/单元/产品）', pct: 100, color: '#43a047' },
  { name: '企业概况 & 主要技术指标',         pct: 100, color: '#43a047' },
  { name: '能源计量器具汇总',                pct: 80,  color: '#00897B' },
  { name: '能源流程图',                      pct: 60,  color: '#00897B' },
  { name: '温室气体排放表',                  pct: 0,   color: '#ef5350' },
  { name: '审计报告生成与提交',               pct: 0,   color: '#ef5350' },
])

const todos = ref([
  { icon: '🔴', name: '温室气体排放表填报',  meta: '截止 2024-12-31 · 已逾期', chip: '逾期',  chipStyle: 'background:#fdecea;color:#ef5350' },
  { icon: '🟡', name: '节能潜力明细填报',    meta: '截止 2025-01-15',          chip: '待填',  chipStyle: 'background:#fff8e1;color:#ffa726' },
  { icon: '🔵', name: '能源流程图完善',      meta: '进行中 · 60% 完成',        chip: '进行中',chipStyle: 'background:#e0f2f0;color:#00897B' },
  { icon: '✅', name: '基本设置配置',        meta: '完成于 2024-11-20',        chip: '已完成',chipStyle: 'background:#e8f5e9;color:#43a047' },
])
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">工作台</div>
      <div class="page-desc">上海XX制造有限公司 · 2024年度能碳审计数据汇总</div>
    </div>

    <!-- Stats -->
    <div class="stats-grid">
      <div
        v-for="s in stats"
        :key="s.label"
        class="stat-card"
        :class="{ 'stat-card--highlight': s.highlight }"
      >
        <div class="stat-label">
          <span class="stat-dot" :style="s.highlight ? 'background:rgba(255,255,255,0.5)' : 'background:#4db6ac'"></span>
          {{ s.label }}
        </div>
        <div class="stat-value-row">
          <span class="stat-value">{{ s.value }}</span>
          <span class="stat-unit">{{ s.unit }}</span>
        </div>
        <div
          class="stat-trend"
          :class="{
            'trend-down': s.trendType === 'down',
            'trend-warning': s.trendType === 'warning',
          }"
        >
          {{ s.trend }} <span v-if="s.trendText" class="trend-text">{{ s.trendText }}</span>
        </div>
        <div v-if="s.barWidth !== undefined" class="stat-bar">
          <div
            class="stat-bar-fill"
            :style="{ width: s.barWidth + '%', background: s.barColor || 'linear-gradient(90deg, #00897B, #43a047)' }"
          ></div>
        </div>
      </div>
    </div>

    <!-- Bottom grid -->
    <div class="bottom-grid">
      <!-- Progress -->
      <div class="g-card">
        <div class="card-header">
          <div class="card-title">填报进度</div>
          <div class="card-action">查看详情 →</div>
        </div>
        <div class="progress-list">
          <div v-for="item in progressItems" :key="item.name" class="progress-item">
            <div class="progress-header">
              <span class="progress-name">{{ item.name }}</span>
              <span class="progress-pct">{{ item.pct }}%</span>
            </div>
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: item.pct + '%', background: item.color }"
              ></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Todo -->
      <div class="g-card">
        <div class="card-header">
          <div class="card-title">当前待办</div>
        </div>
        <div class="todo-list">
          <div v-for="item in todos" :key="item.name" class="todo-item">
            <span class="todo-icon">{{ item.icon }}</span>
            <div class="todo-info">
              <div class="todo-name">{{ item.name }}</div>
              <div class="todo-meta">{{ item.meta }}</div>
            </div>
            <span class="todo-chip" :style="item.chipStyle">{{ item.chip }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: $card-bg;
  border-radius: $radius-lg;
  padding: 18px 20px;
  border: 1px solid $border;

  &--highlight {
    background: linear-gradient(135deg, #00897B, #43a047);
    border: none;
    .stat-label, .stat-unit, .stat-trend { color: rgba(255,255,255,0.7); }
    .stat-value { color: #fff; }
  }
}

.stat-label {
  font-size: 12.5px; color: $text-tertiary;
  margin-bottom: 8px;
  display: flex; align-items: center; gap: 6px;
  .stat-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
}

.stat-value-row { margin-bottom: 4px; }
.stat-value  { font-size: 26px; font-weight: 700; color: $text-primary; }
.stat-unit   { font-size: 12px; color: $text-tertiary; margin-left: 4px; }

.stat-trend {
  font-size: 12px; color: $text-tertiary; margin-top: 4px;
  &.trend-down    { color: $primary; }
  &.trend-warning { color: $warning; }
  .trend-text { font-size: 11px; color: $text-tertiary; }
}

.stat-bar {
  height: 3px; background: $border; border-radius: 2px;
  margin-top: 12px; overflow: hidden;
  .stat-bar-fill { height: 100%; border-radius: 2px; }
}

.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 14px;
}

.progress-list { display: flex; flex-direction: column; gap: 12px; }
.progress-item { }
.progress-header {
  display: flex; justify-content: space-between; margin-bottom: 5px;
  .progress-name { font-size: 13px; color: $text-secondary; }
  .progress-pct  { font-size: 12px; color: $text-tertiary; }
}
.progress-bar {
  height: 5px; background: $border; border-radius: 3px; overflow: hidden;
  .progress-fill { height: 100%; border-radius: 3px; transition: width 0.5s ease; }
}

.todo-list { display: flex; flex-direction: column; gap: 8px; }
.todo-item {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 12px;
  background: #f8faf8; border-radius: 8px; border: 1px solid $border;
  .todo-icon { font-size: 15px; flex-shrink: 0; }
  .todo-info { flex: 1; min-width: 0; }
  .todo-name { font-size: 13px; color: $text-primary; font-weight: 500; }
  .todo-meta { font-size: 11.5px; color: $text-tertiary; margin-top: 1px; }
  .todo-chip {
    font-size: 11px; padding: 2px 8px; border-radius: 4px;
    font-weight: 500; white-space: nowrap; flex-shrink: 0;
  }
}
</style>
