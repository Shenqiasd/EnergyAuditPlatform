<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getDashboardStats,
  getDashboardProgress,
  type DashboardStats,
  type ProgressItem,
} from '@/api/dashboard'
import {
  getMyRectificationList,
  updateRectificationProgress,
  type RectificationItem,
} from '@/api/rectification'

const router = useRouter()
const userStore = useUserStore()

const currentYear = new Date().getFullYear()
const selectedYear = ref<number>(userStore.userInfo?.auditYear ?? currentYear)
const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear - i)

const enterpriseName = computed(() => userStore.userInfo?.enterpriseName ?? '—')

// --- Stats ---
const statsLoading = ref(false)
const dashStats = ref<DashboardStats | null>(null)

interface StatCard {
  label: string
  value: string
  unit: string
  trend: string
  trendType: 'down' | 'up' | 'warning' | 'none'
  trendText: string
  highlight?: boolean
  barWidth?: number
  barColor?: string
}

function formatNumber(val: number | null | undefined, decimals = 0): string {
  if (val == null) return '—'
  const fixed = val.toFixed(decimals)
  const [intPart, decPart] = fixed.split('.')
  const formatted = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return decPart ? formatted + '.' + decPart : formatted
}

function calcTrend(curr: number | null | undefined, prev: number | null | undefined): { text: string; type: 'down' | 'up' | 'none' } {
  if (curr == null || prev == null || prev === 0) {
    return { text: '暂无同比数据', type: 'none' }
  }
  const pct = ((curr - prev) / Math.abs(prev)) * 100
  if (pct < 0) {
    return { text: `↓ ${Math.abs(pct).toFixed(1)}%`, type: 'down' }
  } else if (pct > 0) {
    return { text: `↑ ${pct.toFixed(1)}%`, type: 'up' }
  }
  return { text: '持平', type: 'none' }
}

const stats = computed<StatCard[]>(() => {
  const d = dashStats.value
  if (!d) return []

  const energyTrend = calcTrend(d.totalEnergyEquiv, d.totalEnergyEquivPrev)
  const carbonTrend = calcTrend(d.totalCarbonEmission, d.totalCarbonEmissionPrev)
  const unitTrend = calcTrend(d.unitOutputEnergy, d.unitOutputEnergyPrev)

  const completePct = d.totalTemplateCount > 0
    ? Math.round((d.submittedCount / d.totalTemplateCount) * 100)
    : 0
  const remaining = d.totalTemplateCount - d.submittedCount

  return [
    {
      label: '综合能耗（当量值）',
      value: formatNumber(d.totalEnergyEquiv),
      unit: '吨标煤',
      trend: energyTrend.text,
      trendType: energyTrend.type,
      trendText: energyTrend.type !== 'none' ? '较上年同期' : '',
      highlight: true,
    },
    {
      label: '碳排放总量',
      value: formatNumber(d.totalCarbonEmission),
      unit: 'tCO₂',
      trend: carbonTrend.text,
      trendType: carbonTrend.type,
      trendText: carbonTrend.type !== 'none' ? '较上年同期' : '',
      barWidth: d.totalCarbonEmission != null ? 65 : 0,
    },
    {
      label: '单位产值能耗',
      value: d.unitOutputEnergy != null ? formatNumber(d.unitOutputEnergy, 3) : '—',
      unit: '吨标煤/万元',
      trend: unitTrend.text,
      trendType: unitTrend.type,
      trendText: unitTrend.type !== 'none' ? '较上年同期' : '',
      barWidth: d.unitOutputEnergy != null ? 82 : 0,
    },
    {
      label: '填报完整度',
      value: d.totalTemplateCount > 0 ? String(completePct) : '—',
      unit: d.totalTemplateCount > 0 ? '%' : '',
      trend: remaining > 0
        ? `⚠ 还有 ${remaining} 个模板未提交`
        : (d.totalTemplateCount > 0 ? '全部提交完成' : '暂无已发布模板'),
      trendType: remaining > 0 ? 'warning' : 'none',
      trendText: '',
      barWidth: d.totalTemplateCount > 0 ? completePct : 0,
      barColor: remaining > 0 ? 'linear-gradient(90deg,#ffa726,#ffca28)' : undefined,
    },
  ]
})

// --- Progress ---
const progressLoading = ref(false)
const progressItems = ref<ProgressItem[]>([])

function progressColor(pct: number): string {
  if (pct >= 100) return '#43a047'
  if (pct > 0) return '#00897B'
  return '#ef5350'
}

// --- Rectification ---
const rectItems = ref<RectificationItem[]>([])
const rectLoading = ref(false)

const progressDialogVisible = ref(false)
const currentRectId = ref<number | null>(null)
const progressForm = ref({ status: 1 as number, result: '' })

let loadSeq = 0

async function loadAll() {
  const seq = ++loadSeq
  const year = selectedYear.value
  statsLoading.value = true
  progressLoading.value = true

  try {
    const [statsData, progressData] = await Promise.all([
      getDashboardStats(year),
      getDashboardProgress(year),
    ])
    if (seq !== loadSeq) return // discard stale response
    dashStats.value = statsData
    progressItems.value = progressData
  } catch {
    if (seq !== loadSeq) return
    dashStats.value = null
    progressItems.value = []
  } finally {
    if (seq === loadSeq) {
      statsLoading.value = false
      progressLoading.value = false
    }
  }

  loadRectItems()
}

async function loadRectItems() {
  rectLoading.value = true
  try {
    rectItems.value = await getMyRectificationList()
  } catch {
    rectItems.value = []
  } finally {
    rectLoading.value = false
  }
}

function rectIcon(status: number | undefined) {
  if (status === 3) return '🔴'
  if (status === 0) return '🟡'
  if (status === 1) return '🔵'
  if (status === 2) return '✅'
  return '⚪'
}

function rectChip(status: number | undefined) {
  const map: Record<number, { label: string; style: string }> = {
    0: { label: '未启动', style: 'background:#fff8e1;color:#ffa726' },
    1: { label: '进行中', style: 'background:#e0f2f0;color:#00897B' },
    2: { label: '已完成', style: 'background:#e8f5e9;color:#43a047' },
    3: { label: '超期', style: 'background:#fdecea;color:#ef5350' },
  }
  return map[status ?? 0] ?? map[0]
}

function rectMeta(item: RectificationItem) {
  if (item.status === 3) {
    return `截止 ${formatDate(item.deadline)} · 已逾期`
  }
  if (item.status === 2) {
    return `完成于 ${formatDate(item.completeTime)}`
  }
  if (item.deadline) {
    return `截止 ${formatDate(item.deadline)}`
  }
  return ''
}

function formatDate(dt: string | undefined) {
  if (!dt) return '—'
  return dt.substring(0, 10)
}

function openProgressDialog(item: RectificationItem) {
  currentRectId.value = item.id!
  progressForm.value = {
    status: item.status === 0 ? 1 : (item.status ?? 1),
    result: item.result || '',
  }
  progressDialogVisible.value = true
}

async function submitProgress() {
  if (!currentRectId.value) return
  try {
    await updateRectificationProgress(
      currentRectId.value,
      progressForm.value.status,
      progressForm.value.result || undefined,
    )
    ElMessage.success('整改进度已更新')
    progressDialogVisible.value = false
    loadRectItems()
  } catch (e: any) {
    ElMessage.error('更新失败：' + (e?.message ?? ''))
  }
}

function goToProgress() {
  router.push('/enterprise/report/generate')
}

watch(selectedYear, () => loadAll())
onMounted(loadAll)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">工作台</div>
      <div class="page-desc">
        {{ enterpriseName }} · {{ selectedYear }}年度能碳审计数据汇总
      </div>
      <div class="year-select">
        <el-select v-model="selectedYear" size="small" style="width: 110px">
          <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
        </el-select>
      </div>
    </div>

    <div v-loading="statsLoading" class="stats-grid">
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
            'trend-up': s.trendType === 'up',
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
      <div v-if="stats.length === 0 && !statsLoading" class="stat-card stat-card--empty">
        <div class="stat-label">暂无数据</div>
      </div>
    </div>

    <div class="bottom-grid">
      <div class="g-card">
        <div class="card-header">
          <div class="card-title">填报进度</div>
          <div class="card-action" @click="goToProgress">查看详情 →</div>
        </div>
        <div v-loading="progressLoading" class="progress-list">
          <div v-if="progressItems.length === 0 && !progressLoading" style="color: #909399; text-align: center; padding: 20px; font-size: 13px">
            暂无进度数据
          </div>
          <div v-for="item in progressItems" :key="item.name" class="progress-item">
            <div class="progress-header">
              <span class="progress-name">{{ item.name }}</span>
              <span class="progress-pct">{{ item.pct }}%</span>
            </div>
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: item.pct + '%', background: progressColor(item.pct) }"
              ></div>
            </div>
            <div v-if="item.detail" class="progress-detail">{{ item.detail }}</div>
          </div>
        </div>
      </div>

      <div class="g-card">
        <div class="card-header">
          <div class="card-title">整改任务</div>
        </div>
        <div v-loading="rectLoading">
          <div v-if="rectItems.length === 0 && !rectLoading" style="color: #909399; text-align: center; padding: 20px; font-size: 13px">
            暂无整改任务
          </div>
          <div class="todo-list">
            <div v-for="item in rectItems" :key="item.id" class="todo-item">
              <span class="todo-icon">{{ rectIcon(item.status) }}</span>
              <div class="todo-info">
                <div class="todo-name">{{ item.itemName }}</div>
                <div v-if="item.requirement" class="todo-requirement">{{ item.requirement }}</div>
                <div class="todo-meta">{{ rectMeta(item) }}</div>
              </div>
              <span class="todo-chip" :style="rectChip(item.status).style">{{ rectChip(item.status).label }}</span>
              <el-button
                v-if="item.status !== 2"
                link
                type="primary"
                size="small"
                @click="openProgressDialog(item)"
                style="margin-left: 4px"
              >
                更新
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="progressDialogVisible" title="更新整改进度" width="450px">
      <el-form label-width="80px">
        <el-form-item label="状态">
          <el-select v-model="progressForm.status" style="width: 100%">
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="整改结果">
          <el-input v-model="progressForm.result" type="textarea" :rows="3" placeholder="请描述整改结果..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProgress">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.page-header {
  position: relative;
  .year-select {
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}

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

  &--empty {
    grid-column: 1 / -1;
    text-align: center;
    padding: 32px;
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
  &.trend-up      { color: #ef5350; }
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
  grid-template-columns: 1fr 380px;
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
.progress-detail {
  font-size: 11.5px; color: $text-tertiary; margin-top: 3px;
}

.todo-list { display: flex; flex-direction: column; gap: 8px; }
.todo-item {
  display: flex; align-items: center; gap: 10px;
  padding: 11px 12px;
  background: #f8faf8; border-radius: 8px; border: 1px solid $border;
  .todo-icon { font-size: 15px; flex-shrink: 0; }
  .todo-info { flex: 1; min-width: 0; }
  .todo-name { font-size: 13px; color: $text-primary; font-weight: 500; }
  .todo-requirement { font-size: 12px; color: $text-secondary; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .todo-meta { font-size: 11.5px; color: $text-tertiary; margin-top: 1px; }
  .todo-chip {
    font-size: 11px; padding: 2px 8px; border-radius: 4px;
    font-weight: 500; white-space: nowrap; flex-shrink: 0;
  }
}
</style>
