<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAuditCounts,
  getAuditTaskList,
  AUDIT_STATUS_MAP,
  type AuditTask,
} from '@/api/audit-task'

const router = useRouter()
const loading = ref(false)
const counts = ref<Record<string, number>>({})
const recentTasks = ref<AuditTask[]>([])

const statCards = [
  { key: 'pending', label: '待审核', color: '#909399', icon: '📋' },
  { key: 'reviewing', label: '审核中', color: '#409eff', icon: '🔍' },
  { key: 'approved', label: '已通过', color: '#67c23a', icon: '✅' },
  { key: 'rejected', label: '已退回', color: '#f56c6c', icon: '↩️' },
]

async function loadData() {
  loading.value = true
  try {
    const [c, tasks] = await Promise.all([
      getAuditCounts(),
      getAuditTaskList(),
    ])
    counts.value = c
    recentTasks.value = tasks.slice(0, 10)
  } finally {
    loading.value = false
  }
}

function goToReview(task: AuditTask) {
  router.push({ path: '/auditor/review', query: { id: task.id } })
}

function goToTasks() {
  router.push('/auditor/tasks')
}

function statusTag(status: number | undefined) {
  if (status === undefined) return { label: '未知', type: 'info' as const }
  return AUDIT_STATUS_MAP[status] ?? { label: '未知', type: 'info' as const }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <h2 class="page-title">审计工作台</h2>

    <el-row :gutter="16" style="margin-bottom: 20px">
      <el-col :span="6" v-for="card in statCards" :key="card.key">
        <el-card shadow="hover" class="stat-card" @click="goToTasks">
          <div class="stat-icon">{{ card.icon }}</div>
          <div class="stat-value" :style="{ color: card.color }">
            {{ counts[card.key] ?? 0 }}
          </div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">最近任务</span>
          <el-button link type="primary" @click="goToTasks">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentTasks" border stripe size="small">
        <el-table-column label="企业" prop="enterpriseName" min-width="160" />
        <el-table-column label="审计年度" prop="auditYear" width="100" align="center" />
        <el-table-column label="任务标题" prop="taskTitle" min-width="140" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type" size="small">
              {{ statusTag(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goToReview(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
}
.stat-card {
  text-align: center;
  cursor: pointer;
  transition: transform 0.2s;
  &:hover { transform: translateY(-2px); }
}
.stat-icon {
  font-size: 28px;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
</style>
