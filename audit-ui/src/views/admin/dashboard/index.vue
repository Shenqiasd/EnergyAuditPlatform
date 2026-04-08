<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAdminDashboardStats,
  getAdminAuditOverview,
  getAdminActivity,
  type AdminDashboardStats,
  type AdminAuditOverview,
  type RecentRegistration,
  type RecentAuditLog,
} from '@/api/admin-dashboard'

const router = useRouter()

const currentYear = new Date().getFullYear()
const selectedYear = ref<number>(currentYear)
const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear - i)

// --- Stats ---
const statsLoading = ref(false)
const dashStats = ref<AdminDashboardStats | null>(null)

// --- Overview ---
const overviewLoading = ref(false)
const overview = ref<AdminAuditOverview | null>(null)

// --- Activity ---
const activityLoading = ref(false)
const recentRegistrations = ref<RecentRegistration[]>([])
const recentAuditLogs = ref<RecentAuditLog[]>([])

interface StatCard {
  label: string
  value: string
  unit: string
  trend: string
  trendType: 'down' | 'up' | 'warning' | 'none'
  highlight?: boolean
  icon: string
}

const stats = ref<StatCard[]>([])

function buildStats(d: AdminDashboardStats): StatCard[] {
  const yoyNew = d.enterpriseNewLastYear > 0
    ? ((d.enterpriseNewThisYear - d.enterpriseNewLastYear) / d.enterpriseNewLastYear * 100).toFixed(1)
    : null

  return [
    {
      label: '企业总数',
      value: String(d.enterpriseTotal),
      unit: '家',
      trend: yoyNew !== null
        ? (Number(yoyNew) >= 0 ? `↑ ${yoyNew}%` : `↓ ${Math.abs(Number(yoyNew)).toFixed(1)}%`)
        : `本年新增 ${d.enterpriseNewThisYear}`,
      trendType: yoyNew !== null ? (Number(yoyNew) >= 0 ? 'up' : 'down') : 'none',
      highlight: true,
      icon: '🏢',
    },
    {
      label: '待审核申请',
      value: String(d.pendingRegistrations),
      unit: '条',
      trend: d.pendingRegistrations > 0 ? '有待处理的注册申请' : '暂无待审核申请',
      trendType: d.pendingRegistrations > 0 ? 'warning' : 'none',
      icon: '📋',
    },
    {
      label: '进行中审计任务',
      value: String(d.activeAuditTasks),
      unit: '个',
      trend: d.overdueRectifications > 0
        ? `⚠ ${d.overdueRectifications} 个整改超期`
        : '暂无超期整改',
      trendType: d.overdueRectifications > 0 ? 'warning' : 'none',
      icon: '📝',
    },
    {
      label: '已发布模板',
      value: String(d.publishedTemplates),
      unit: '个',
      trend: '已发布可用模板',
      trendType: 'none',
      icon: '📄',
    },
  ]
}

// --- Overview bar items ---
interface OverviewItem {
  label: string
  count: number
  total: number
  pct: number
  color: string
}

function buildOverviewItems(o: AdminAuditOverview): OverviewItem[] {
  const t = o.totalEnterprises || 1
  return [
    {
      label: '已提交数据',
      count: o.submittedEnterprises,
      total: o.totalEnterprises,
      pct: Math.round((o.submittedEnterprises / t) * 100),
      color: '#00897B',
    },
    {
      label: '审核中',
      count: o.auditingEnterprises,
      total: o.totalEnterprises,
      pct: Math.round((o.auditingEnterprises / t) * 100),
      color: '#ffa726',
    },
    {
      label: '审计完成',
      count: o.completedEnterprises,
      total: o.totalEnterprises,
      pct: Math.round((o.completedEnterprises / t) * 100),
      color: '#43a047',
    },
    {
      label: '已出报告',
      count: o.reportedEnterprises,
      total: o.totalEnterprises,
      pct: Math.round((o.reportedEnterprises / t) * 100),
      color: '#26a69a',
    },
  ]
}

const overviewItems = ref<OverviewItem[]>([])

// --- Quick links ---
const quickLinks = [
  { label: '企业管理', icon: '🏢', route: '/admin/enterprise', desc: '查看和管理企业列表' },
  { label: '注册审核', icon: '📋', route: '/admin/registration', desc: '审核企业注册申请' },
  { label: '模板管理', icon: '📄', route: '/admin/template', desc: '管理 SpreadJS 模板' },
  { label: '审计管理', icon: '📝', route: '/admin/audit-manage', desc: '分配和跟踪审计任务' },
  { label: '用户管理', icon: '👥', route: '/admin/system/users', desc: '管理系统用户账号' },
  { label: '字典管理', icon: '📖', route: '/admin/system/dict', desc: '维护系统字典数据' },
]

function navigateTo(route: string) {
  router.push(route)
}

// --- Audit status labels ---
const auditStatusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
}

const actionLabelMap: Record<string, string> = {
  SUBMIT: '提交审核',
  APPROVE: '审核通过',
  REJECT: '审核退回',
  COMMENT: '审核意见',
}

function formatDate(dt: string | null | undefined) {
  if (!dt) return '—'
  return dt.substring(0, 16).replace('T', ' ')
}

// --- Load functions ---
let loadSeq = 0

async function loadStats() {
  statsLoading.value = true
  try {
    const data = await getAdminDashboardStats()
    dashStats.value = data
    stats.value = buildStats(data)
  } catch {
    dashStats.value = null
    stats.value = []
  } finally {
    statsLoading.value = false
  }
}

async function loadOverview() {
  const seq = ++loadSeq
  overviewLoading.value = true
  try {
    const data = await getAdminAuditOverview(selectedYear.value)
    if (seq !== loadSeq) return
    overview.value = data
    overviewItems.value = buildOverviewItems(data)
  } catch {
    if (seq !== loadSeq) return
    overview.value = null
    overviewItems.value = []
  } finally {
    if (seq === loadSeq) {
      overviewLoading.value = false
    }
  }
}

async function loadActivity() {
  activityLoading.value = true
  try {
    const data = await getAdminActivity()
    recentRegistrations.value = data.recentRegistrations ?? []
    recentAuditLogs.value = data.recentAuditLogs ?? []
  } catch {
    recentRegistrations.value = []
    recentAuditLogs.value = []
  } finally {
    activityLoading.value = false
  }
}

watch(selectedYear, () => loadOverview())

onMounted(() => {
  loadStats()
  loadOverview()
  loadActivity()
})
</script>

<template>
  <div class="page-container">
    <!-- Header -->
    <div class="page-header">
      <div class="page-title">管理首页</div>
      <div class="page-desc">
        能碳审计平台运营数据总览
      </div>
    </div>

    <!-- Stat Cards -->
    <div v-loading="statsLoading" class="stats-grid">
      <div
        v-for="s in stats"
        :key="s.label"
        class="stat-card"
        :class="{ 'stat-card--highlight': s.highlight }"
      >
        <div class="stat-label">
          <span class="stat-icon">{{ s.icon }}</span>
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
          {{ s.trend }}
        </div>
      </div>
      <div v-if="stats.length === 0 && !statsLoading" class="stat-card stat-card--empty">
        <div class="stat-label">暂无数据</div>
      </div>
    </div>

    <!-- Middle Row: Overview + Quick Links -->
    <div class="middle-grid">
      <!-- Audit Overview -->
      <div class="g-card">
        <div class="card-header">
          <div class="card-title">{{ selectedYear }}年度 审计进度总览</div>
          <div class="year-select">
            <el-select v-model="selectedYear" size="small" style="width: 110px">
              <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
            </el-select>
          </div>
        </div>
        <div v-loading="overviewLoading" class="overview-body">
          <div v-if="overview && overview.totalEnterprises > 0" class="overview-list">
            <div v-for="item in overviewItems" :key="item.label" class="overview-item">
              <div class="overview-header">
                <span class="overview-name">{{ item.label }}</span>
                <span class="overview-count">{{ item.count }} / {{ item.total }} 家</span>
              </div>
              <div class="overview-bar">
                <div
                  class="overview-bar-fill"
                  :style="{ width: item.pct + '%', background: item.color }"
                ></div>
              </div>
              <div class="overview-pct">{{ item.pct }}%</div>
            </div>
          </div>
          <div v-else-if="!overviewLoading" class="empty-state">
            暂无 {{ selectedYear }} 年度审计数据
          </div>
        </div>
      </div>

      <!-- Quick Links -->
      <div class="g-card">
        <div class="card-header">
          <div class="card-title">快捷入口</div>
        </div>
        <div class="quick-links">
          <div
            v-for="link in quickLinks"
            :key="link.route"
            class="quick-link-item"
            @click="navigateTo(link.route)"
          >
            <span class="quick-link-icon">{{ link.icon }}</span>
            <div class="quick-link-info">
              <div class="quick-link-label">{{ link.label }}</div>
              <div class="quick-link-desc">{{ link.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Row: Recent Activity -->
    <div class="bottom-grid">
      <!-- Recent Registrations -->
      <div class="g-card">
        <div class="card-header">
          <div class="card-title">最近注册申请</div>
          <div class="card-action" @click="navigateTo('/admin/registration')">查看全部 →</div>
        </div>
        <div v-loading="activityLoading">
          <div v-if="recentRegistrations.length === 0 && !activityLoading" class="empty-state">
            暂无注册申请
          </div>
          <div class="activity-list">
            <div v-for="reg in recentRegistrations" :key="reg.id" class="activity-item">
              <div class="activity-main">
                <span class="activity-name">{{ reg.enterpriseName }}</span>
                <el-tag
                  :type="(auditStatusMap[reg.auditStatus]?.type as any) || 'info'"
                  size="small"
                >
                  {{ auditStatusMap[reg.auditStatus]?.label || '未知' }}
                </el-tag>
              </div>
              <div class="activity-meta">
                {{ reg.creditCode }} · {{ formatDate(reg.applyTime) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Recent Audit Logs -->
      <div class="g-card">
        <div class="card-header">
          <div class="card-title">最近审计动态</div>
          <div class="card-action" @click="navigateTo('/admin/audit-manage')">查看全部 →</div>
        </div>
        <div v-loading="activityLoading">
          <div v-if="recentAuditLogs.length === 0 && !activityLoading" class="empty-state">
            暂无审计动态
          </div>
          <div class="activity-list">
            <div v-for="logItem in recentAuditLogs" :key="logItem.id" class="activity-item">
              <div class="activity-main">
                <span class="activity-action-badge">
                  {{ actionLabelMap[logItem.action] || logItem.action }}
                </span>
                <span class="activity-name">{{ logItem.enterpriseName || '—' }}</span>
              </div>
              <div class="activity-meta">
                {{ logItem.operatorName || '系统' }} · {{ formatDate(logItem.operationTime) }}
                <span v-if="logItem.comment" class="activity-comment">「{{ logItem.comment }}」</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
@use '@/styles/variables' as *;

.page-header {
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: $text-primary;
}

.page-desc {
  font-size: 13px;
  color: $text-tertiary;
  margin-top: 4px;
}

// --- Stat Cards ---
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
    .stat-icon { opacity: 0.7; }
  }

  &--empty {
    grid-column: 1 / -1;
    text-align: center;
    padding: 32px;
  }
}

.stat-label {
  font-size: 12.5px;
  color: $text-tertiary;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.stat-icon {
  font-size: 14px;
}

.stat-value-row { margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; color: $text-primary; }
.stat-unit { font-size: 12px; color: $text-tertiary; margin-left: 4px; }

.stat-trend {
  font-size: 12px;
  color: $text-tertiary;
  margin-top: 4px;
  &.trend-down { color: $primary; }
  &.trend-up { color: #ef5350; }
  &.trend-warning { color: $warning; }
}

// --- Cards ---
.g-card {
  background: $card-bg;
  border-radius: $radius-lg;
  padding: 18px 20px;
  border: 1px solid $border;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  display: flex;
  align-items: center;
  gap: 6px;
  &::before {
    content: '';
    width: 6px;
    height: 6px;
    background: $primary;
    border-radius: 50%;
    flex-shrink: 0;
  }
}

.card-action {
  font-size: 12px;
  color: $primary;
  cursor: pointer;
  &:hover { text-decoration: underline; }
}

// --- Middle Grid ---
.middle-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 14px;
  margin-bottom: 16px;
}

// --- Overview ---
.overview-body {
  min-height: 80px;
}

.overview-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.overview-item {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto;
  gap: 4px 12px;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  grid-column: 1 / -1;
}

.overview-name {
  font-size: 13px;
  color: $text-secondary;
}

.overview-count {
  font-size: 12px;
  color: $text-tertiary;
}

.overview-bar {
  height: 6px;
  background: $border;
  border-radius: 3px;
  overflow: hidden;
  grid-column: 1;
}

.overview-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.overview-pct {
  font-size: 12px;
  color: $text-tertiary;
  text-align: right;
  line-height: 6px;
}

// --- Quick Links ---
.quick-links {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick-link-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: $radius-md;
  border: 1px solid $border;
  cursor: pointer;
  transition: $transition;

  &:hover {
    background: $primary-extra-light;
    border-color: $primary;
  }
}

.quick-link-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.quick-link-label {
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
}

.quick-link-desc {
  font-size: 11px;
  color: $text-tertiary;
  margin-top: 2px;
}

// --- Bottom Grid ---
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

// --- Activity ---
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.activity-item {
  padding: 10px 0;
  border-bottom: 1px solid $border;
  &:last-child { border-bottom: none; }
}

.activity-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.activity-name {
  font-size: 13px;
  color: $text-primary;
  font-weight: 500;
}

.activity-action-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: $primary-light;
  color: $primary;
  font-weight: 500;
  flex-shrink: 0;
}

.activity-meta {
  font-size: 12px;
  color: $text-tertiary;
}

.activity-comment {
  color: $text-secondary;
  margin-left: 4px;
}

.empty-state {
  color: $text-tertiary;
  text-align: center;
  padding: 24px;
  font-size: 13px;
}
</style>
