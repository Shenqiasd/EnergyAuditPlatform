<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAuditTaskList,
  assignAuditTask,
  getAuditLogs,
  AUDIT_STATUS_MAP,
  ACTION_LABEL_MAP,
  type AuditTask,
  type AuditLog,
} from '@/api/audit-task'
import { listUsers, type SysUser } from '@/api/user'
import { getList as getEnterpriseList, type Enterprise } from '@/api/enterprise'

const loading = ref(false)
const tasks = ref<AuditTask[]>([])
const filterStatus = ref<number | undefined>(undefined)
const filterYear = ref<number | undefined>(undefined)
const filterEnterpriseId = ref<number | undefined>(undefined)
const enterprises = ref<Enterprise[]>([])
const yearOptions = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)

const assignDialogVisible = ref(false)
const assigningTaskId = ref<number | null>(null)
const selectedAuditorId = ref<number | undefined>(undefined)
const auditors = ref<SysUser[]>([])
const assigning = ref(false)

const detailDrawerVisible = ref(false)
const currentTask = ref<AuditTask | null>(null)
const currentLogs = ref<AuditLog[]>([])
const logsLoading = ref(false)

async function loadTasks() {
  loading.value = true
  try {
    tasks.value = await getAuditTaskList({
      status: filterStatus.value,
      auditYear: filterYear.value,
      enterpriseId: filterEnterpriseId.value,
    })
  } finally {
    loading.value = false
  }
}

async function loadEnterprises() {
  try {
    const result = await getEnterpriseList({ pageSize: 500 })
    enterprises.value = result.rows ?? []
  } catch {
    enterprises.value = []
  }
}

async function openAssignDialog(task: AuditTask) {
  assigningTaskId.value = task.id!
  selectedAuditorId.value = task.assigneeId ?? undefined
  assignDialogVisible.value = true
  try {
    const result = await listUsers({ userType: 2, status: 1, pageSize: 200 })
    auditors.value = result.rows ?? []
  } catch {
    auditors.value = []
  }
}

async function confirmAssign() {
  if (!assigningTaskId.value || !selectedAuditorId.value) {
    ElMessage.warning('请选择审核员')
    return
  }
  assigning.value = true
  try {
    await assignAuditTask(assigningTaskId.value, selectedAuditorId.value)
    ElMessage.success('分配成功')
    assignDialogVisible.value = false
    loadTasks()
  } catch (e: any) {
    ElMessage.error('分配失败：' + (e?.message ?? '未知错误'))
  } finally {
    assigning.value = false
  }
}

async function openDetail(task: AuditTask) {
  currentTask.value = task
  detailDrawerVisible.value = true
  logsLoading.value = true
  try {
    currentLogs.value = await getAuditLogs(task.id!)
  } finally {
    logsLoading.value = false
  }
}

function statusTag(status: number | undefined) {
  if (status === undefined) return { label: '未知', type: 'info' as const }
  return AUDIT_STATUS_MAP[status] ?? { label: '未知', type: 'info' as const }
}

onMounted(() => {
  loadTasks()
  loadEnterprises()
})
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">审计管理</span>
          <div class="header-right">
            <el-select
              v-model="filterEnterpriseId"
              placeholder="企业筛选"
              clearable
              filterable
              style="width:180px;margin-right:8px"
              size="small"
              @change="loadTasks"
            >
              <el-option
                v-for="e in enterprises"
                :key="e.id"
                :label="e.enterpriseName"
                :value="e.id!"
              />
            </el-select>
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
              <el-option label="已完成" :value="4" />
            </el-select>
            <el-button @click="loadTasks" :loading="loading" size="small">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tasks" border stripe>
        <el-table-column label="企业名称" prop="enterpriseName" min-width="160" />
        <el-table-column label="审计年度" prop="auditYear" width="100" align="center" />
        <el-table-column label="任务标题" prop="taskTitle" min-width="160" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type" size="small">
              {{ statusTag(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核员" width="120" align="center">
          <template #default="{ row }">
            {{ row.assigneeName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="row.status !== 0 && row.status !== 1"
              @click="openAssignDialog(row)"
            >
              分配
            </el-button>
            <el-button
              link
              type="info"
              size="small"
              @click="openDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Assign Dialog -->
    <el-dialog v-model="assignDialogVisible" title="分配审核员" width="420px">
      <el-form label-width="80px">
        <el-form-item label="审核员">
          <el-select v-model="selectedAuditorId" placeholder="请选择审核员" style="width: 100%">
            <el-option
              v-for="a in auditors"
              :key="a.id"
              :label="`${a.realName} (${a.username})`"
              :value="a.id!"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="confirmAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="detailDrawerVisible" title="任务详情" size="500px">
      <template v-if="currentTask">
        <el-descriptions :column="1" border size="small" style="margin-bottom: 20px">
          <el-descriptions-item label="企业">{{ currentTask.enterpriseName }}</el-descriptions-item>
          <el-descriptions-item label="审计年度">{{ currentTask.auditYear }}</el-descriptions-item>
          <el-descriptions-item label="任务标题">{{ currentTask.taskTitle }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(currentTask.status).type" size="small">
              {{ statusTag(currentTask.status).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核员">{{ currentTask.assigneeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="审核结果" v-if="currentTask.result">{{ currentTask.result }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentTask.createTime }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin-bottom: 12px">审核日志</h4>
        <el-timeline v-loading="logsLoading">
          <el-timeline-item
            v-for="log in currentLogs"
            :key="log.id"
            :timestamp="log.operationTime"
            placement="top"
          >
            <strong>{{ ACTION_LABEL_MAP[log.action || ''] || log.action }}</strong>
            <span v-if="log.operatorName"> — {{ log.operatorName }}</span>
            <p v-if="log.comment" style="margin: 4px 0 0; color: #606266">{{ log.comment }}</p>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
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
