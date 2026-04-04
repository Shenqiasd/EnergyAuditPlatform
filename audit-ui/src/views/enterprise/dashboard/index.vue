<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMyRectificationList,
  updateRectificationProgress,
  type RectificationItem,
} from '@/api/rectification'

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

const rectItems = ref<RectificationItem[]>([])
const rectLoading = ref(false)

const progressDialogVisible = ref(false)
const currentRectId = ref<number | null>(null)
const progressForm = ref({ status: 1 as number, result: '' })

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

onMounted(loadRectItems)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">工作台</div>
      <div class="page-desc">上海XX制造有限公司 · 2024年度能碳审计数据汇总</div>
    </div>

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

    <div class="bottom-grid">
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
