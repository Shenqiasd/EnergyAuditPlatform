<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAuditTaskList,
  AUDIT_STATUS_MAP,
  type AuditTask,
} from '@/api/audit-task'

const router = useRouter()
const loading = ref(false)
const tasks = ref<AuditTask[]>([])
const filterStatus = ref<number | undefined>(undefined)
const filterYear = ref<number | undefined>(undefined)
const yearOptions = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await getAuditTaskList({
      status: filterStatus.value,
      auditYear: filterYear.value,
    })
  } finally {
    loading.value = false
  }
}

function goToReview(task: AuditTask) {
  router.push({ path: '/auditor/review', query: { id: task.id } })
}

function statusTag(status: number | undefined) {
  if (status === undefined) return { label: '未知', type: 'info' as const }
  return AUDIT_STATUS_MAP[status] ?? { label: '未知', type: 'info' as const }
}

onMounted(loadTasks)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">审计任务</span>
          <div class="header-right">
            <el-select
              v-model="filterYear"
              placeholder="审计年度"
              clearable
              style="width:110px;margin-right:8px"
              size="small"
              @change="loadTasks"
            >
              <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
            </el-select>
            <el-select
              v-model="filterStatus"
              placeholder="任务状态"
              clearable
              style="width:110px;margin-right:8px"
              size="small"
              @change="loadTasks"
            >
              <el-option label="待审核" :value="0" />
              <el-option label="审核中" :value="1" />
              <el-option label="已通过" :value="2" />
              <el-option label="已退回" :value="3" />
            </el-select>
            <el-button @click="loadTasks" :loading="loading" size="small">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tasks" border stripe>
        <el-table-column label="企业名称" prop="enterpriseName" min-width="160" />
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
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goToReview(row)">
              {{ row.status === 1 ? '审核' : '查看' }}
            </el-button>
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
.header-right {
  display: flex;
  align-items: center;
}
</style>
