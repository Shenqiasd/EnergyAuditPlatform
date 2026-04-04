<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAuditTask,
  getAuditLogs,
  approveAuditTask,
  rejectAuditTask,
  addAuditComment,
  getTaskEnterpriseInfo,
  AUDIT_STATUS_MAP,
  ACTION_LABEL_MAP,
  type AuditTask,
  type AuditLog,
  type EnterpriseInfo,
} from '@/api/audit-task'
import {
  getExtractedTables,
  queryExtractedTable,
  type TableSummary,
} from '@/api/extracted-data'
import {
  getRectificationList,
  createRectificationItems,
  acceptRectificationItem,
  RECTIFICATION_STATUS_MAP,
  type RectificationItem,
} from '@/api/rectification'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const task = ref<AuditTask | null>(null)
const logs = ref<AuditLog[]>([])
const logsLoading = ref(false)
const enterpriseInfo = ref<EnterpriseInfo | null>(null)

const tables = ref<TableSummary[]>([])
const activeTab = ref('')
const tableData = ref<Record<string, unknown>[]>([])
const tableColumns = ref<string[]>([])
const tableLoading = ref(false)

const commentText = ref('')
const actionLoading = ref(false)

const rectItems = ref<RectificationItem[]>([])
const rectLoading = ref(false)

const rectDialogVisible = ref(false)
const newRectItems = ref<{ itemName: string; requirement: string; deadline: string }[]>([])

const canReview = computed(() => task.value?.status === 1)

async function loadTask() {
  const id = Number(route.query.id)
  if (!id) {
    ElMessage.error('缺少任务ID')
    router.push('/auditor/tasks')
    return
  }
  loading.value = true
  try {
    task.value = await getAuditTask(id)
    loadLogs()
    loadTables()
    loadEnterpriseInfo()
    loadRectItems()
  } catch (e: any) {
    ElMessage.error('加载任务失败：' + (e?.message ?? ''))
    router.push('/auditor/tasks')
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  if (!task.value?.id) return
  logsLoading.value = true
  try {
    logs.value = await getAuditLogs(task.value.id)
  } finally {
    logsLoading.value = false
  }
}

async function loadEnterpriseInfo() {
  if (!task.value?.id) return
  try {
    enterpriseInfo.value = await getTaskEnterpriseInfo(task.value.id)
  } catch {
    enterpriseInfo.value = null
  }
}

async function loadTables() {
  if (!task.value?.enterpriseId) return
  try {
    tables.value = (await getExtractedTables(task.value.auditYear, task.value.enterpriseId)).filter(t => t.count > 0)
    if (tables.value.length > 0) {
      activeTab.value = tables.value[0].tableName
      loadTableData(activeTab.value)
    }
  } catch {
    tables.value = []
  }
}

async function loadTableData(tableName: string) {
  if (!tableName) return
  tableLoading.value = true
  try {
    const result = await queryExtractedTable(tableName, {
      auditYear: task.value?.auditYear,
      enterpriseId: task.value?.enterpriseId,
      pageSize: 100,
    })
    const rows = result.rows ?? []
    tableData.value = rows
    if (rows.length > 0) {
      const excludeKeys = new Set(['id', 'submission_id', 'enterprise_id', 'deleted', 'create_by', 'update_by', 'create_time', 'update_time', 'DELETED', 'CREATE_BY', 'UPDATE_BY', 'CREATE_TIME', 'UPDATE_TIME', 'ID', 'SUBMISSION_ID', 'ENTERPRISE_ID'])
      tableColumns.value = Object.keys(rows[0]).filter(k => !excludeKeys.has(k))
    } else {
      tableColumns.value = []
    }
  } finally {
    tableLoading.value = false
  }
}

async function loadRectItems() {
  if (!task.value?.id) return
  rectLoading.value = true
  try {
    rectItems.value = await getRectificationList(task.value.id)
  } catch {
    rectItems.value = []
  } finally {
    rectLoading.value = false
  }
}

function onTabChange(tab: string | number) {
  activeTab.value = String(tab)
  loadTableData(String(tab))
}

async function handleApprove() {
  try {
    await ElMessageBox.confirm('确认审核通过？通过后可添加整改要求。', '审核通过', { type: 'success' })
  } catch { return }
  actionLoading.value = true
  try {
    await approveAuditTask(task.value!.id!, commentText.value || undefined)
    ElMessage.success('审核已通过')
    commentText.value = ''
    loadTask()
    openRectDialog()
  } catch (e: any) {
    ElMessage.error('操作失败：' + (e?.message ?? ''))
  } finally {
    actionLoading.value = false
  }
}

async function handleReject() {
  if (!commentText.value.trim()) {
    ElMessage.warning('退回时必须填写退回意见')
    return
  }
  try {
    await ElMessageBox.confirm('确认退回？企业将看到退回意见。', '审核退回', { type: 'warning' })
  } catch { return }
  actionLoading.value = true
  try {
    await rejectAuditTask(task.value!.id!, commentText.value)
    ElMessage.success('已退回')
    commentText.value = ''
    loadTask()
    openRectDialog()
  } catch (e: any) {
    ElMessage.error('操作失败：' + (e?.message ?? ''))
  } finally {
    actionLoading.value = false
  }
}

async function handleComment() {
  if (!commentText.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  actionLoading.value = true
  try {
    await addAuditComment(task.value!.id!, commentText.value)
    ElMessage.success('评论已添加')
    commentText.value = ''
    loadLogs()
  } catch (e: any) {
    ElMessage.error('操作失败：' + (e?.message ?? ''))
  } finally {
    actionLoading.value = false
  }
}

function openRectDialog() {
  newRectItems.value = []
  addNewRectRow()
  rectDialogVisible.value = true
}

function addNewRectRow() {
  newRectItems.value.push({ itemName: '', requirement: '', deadline: '' })
}

function removeRectRow(idx: number) {
  newRectItems.value.splice(idx, 1)
}

async function submitRectItems() {
  const valid = newRectItems.value.filter(r => r.itemName.trim())
  if (valid.length === 0) {
    ElMessage.warning('请至少填写一项整改要求')
    return
  }
  actionLoading.value = true
  try {
    await createRectificationItems(
      task.value!.id!,
      valid.map(r => ({
        itemName: r.itemName,
        requirement: r.requirement,
        deadline: r.deadline || undefined,
      })),
    )
    ElMessage.success('整改要求已添加')
    rectDialogVisible.value = false
    loadRectItems()
    loadLogs()
  } catch (e: any) {
    ElMessage.error('添加失败：' + (e?.message ?? ''))
  } finally {
    actionLoading.value = false
  }
}

async function handleAcceptRect(item: RectificationItem) {
  try {
    await ElMessageBox.confirm(`确认验收「${item.itemName}」？`, '验收确认', { type: 'success' })
  } catch { return }
  try {
    await acceptRectificationItem(item.id!)
    ElMessage.success('验收成功')
    loadRectItems()
    loadLogs()
  } catch (e: any) {
    ElMessage.error('验收失败：' + (e?.message ?? ''))
  }
}

function statusTag(status: number | undefined) {
  if (status === undefined) return { label: '未知', type: 'info' as const }
  return AUDIT_STATUS_MAP[status] ?? { label: '未知', type: 'info' as const }
}

function rectStatusTag(status: number | undefined) {
  if (status === undefined) return { label: '未知', type: 'info' as const }
  return RECTIFICATION_STATUS_MAP[status] ?? { label: '未知', type: 'info' as const }
}

function formatColName(col: string) {
  return col.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

function formatDate(dt: string | undefined) {
  if (!dt) return '—'
  return dt.substring(0, 10)
}

onMounted(loadTask)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="back-link">
      <el-button link type="primary" @click="router.push('/auditor/tasks')">
        &larr; 返回任务列表
      </el-button>
    </div>

    <template v-if="task">
      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header>
          <div class="card-header">
            <span class="card-title">任务信息</span>
            <el-tag :type="statusTag(task.status).type">
              {{ statusTag(task.status).label }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="企业名称">{{ task.enterpriseName }}</el-descriptions-item>
          <el-descriptions-item label="审计年度">{{ task.auditYear }}</el-descriptions-item>
          <el-descriptions-item label="任务标题">{{ task.taskTitle }}</el-descriptions-item>
          <el-descriptions-item label="审核员">{{ task.assigneeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ task.createTime }}</el-descriptions-item>
          <el-descriptions-item label="审核结果" v-if="task.result">{{ task.result }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card v-if="enterpriseInfo" shadow="never" style="margin-bottom: 16px">
        <template #header>
          <span class="card-title">企业基本信息</span>
        </template>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="企业名称">{{ enterpriseInfo.enterpriseName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="统一信用代码">{{ enterpriseInfo.creditCode || '—' }}</el-descriptions-item>
          <el-descriptions-item label="法人代表">{{ enterpriseInfo.legalRepresentative || '—' }}</el-descriptions-item>
          <el-descriptions-item label="企业地址">{{ enterpriseInfo.enterpriseAddress || '—' }}</el-descriptions-item>
          <el-descriptions-item label="行业类别">{{ enterpriseInfo.industryCategory || '—' }}</el-descriptions-item>
          <el-descriptions-item label="行业名称">{{ enterpriseInfo.industryName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="单位性质">{{ enterpriseInfo.unitNature || '—' }}</el-descriptions-item>
          <el-descriptions-item label="用能企业类型">{{ enterpriseInfo.energyEnterpriseType || '—' }}</el-descriptions-item>
          <el-descriptions-item label="注册资本(万元)">{{ enterpriseInfo.registeredCapital ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ enterpriseInfo.contactPerson || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ enterpriseInfo.contactPhone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱">{{ enterpriseInfo.contactEmail || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header>
          <span class="card-title">填报数据审查</span>
        </template>
        <div v-if="tables.length === 0" style="color: #909399; text-align: center; padding: 20px">
          该企业暂无抽取数据
        </div>
        <template v-else>
          <el-tabs v-model="activeTab" @tab-change="onTabChange" type="border-card">
            <el-tab-pane
              v-for="t in tables"
              :key="t.tableName"
              :label="`${t.label} (${t.count})`"
              :name="t.tableName"
            >
              <el-table
                v-loading="tableLoading"
                :data="tableData"
                border
                stripe
                size="small"
                max-height="400"
                style="width: 100%"
              >
                <el-table-column
                  v-for="col in tableColumns"
                  :key="col"
                  :prop="col"
                  :label="formatColName(col)"
                  min-width="120"
                  show-overflow-tooltip
                />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-card>

      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header>
          <div class="card-header">
            <span class="card-title">整改管理</span>
            <el-button type="primary" size="small" @click="openRectDialog">
              添加整改要求
            </el-button>
          </div>
        </template>
        <el-table v-loading="rectLoading" :data="rectItems" border stripe size="small" style="width: 100%">
          <el-table-column label="整改项" prop="itemName" min-width="150" />
          <el-table-column label="整改要求" prop="requirement" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="rectStatusTag(row.status).type" size="small">
                {{ rectStatusTag(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止日期" width="110" align="center">
            <template #default="{ row }">{{ formatDate(row.deadline) }}</template>
          </el-table-column>
          <el-table-column label="完成时间" width="110" align="center">
            <template #default="{ row }">{{ formatDate(row.completeTime) }}</template>
          </el-table-column>
          <el-table-column label="整改结果" prop="result" min-width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 1 || row.status === 2"
                link
                type="success"
                size="small"
                @click="handleAcceptRect(row)"
              >
                验收
              </el-button>
              <span v-else style="color: #c0c4cc; font-size: 12px">—</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="rectItems.length === 0 && !rectLoading" style="color: #909399; text-align: center; padding: 20px">
          暂无整改项
        </div>
      </el-card>

      <el-card shadow="never" style="margin-bottom: 16px">
        <template #header>
          <span class="card-title">审核操作</span>
        </template>
        <el-input
          v-model="commentText"
          type="textarea"
          :rows="3"
          placeholder="请输入审核意见..."
          style="margin-bottom: 12px"
        />
        <div class="action-buttons">
          <el-button
            type="success"
            :disabled="!canReview"
            :loading="actionLoading"
            @click="handleApprove"
          >
            审核通过
          </el-button>
          <el-button
            type="danger"
            :disabled="!canReview"
            :loading="actionLoading"
            @click="handleReject"
          >
            退回
          </el-button>
          <el-button
            :loading="actionLoading"
            @click="handleComment"
          >
            添加评论
          </el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <span class="card-title">审核日志</span>
        </template>
        <el-timeline v-loading="logsLoading">
          <el-timeline-item
            v-for="log in logs"
            :key="log.id"
            :timestamp="log.operationTime"
            placement="top"
            :color="log.action === 'APPROVE' ? '#67c23a' : log.action === 'REJECT' ? '#f56c6c' : '#409eff'"
          >
            <strong>{{ ACTION_LABEL_MAP[log.action || ''] || log.action }}</strong>
            <span v-if="log.operatorName"> — {{ log.operatorName }}</span>
            <p v-if="log.comment" style="margin: 4px 0 0; color: #606266">{{ log.comment }}</p>
          </el-timeline-item>
        </el-timeline>
        <div v-if="logs.length === 0 && !logsLoading" style="color: #909399; text-align: center; padding: 20px">
          暂无审核日志
        </div>
      </el-card>
    </template>

    <el-dialog v-model="rectDialogVisible" title="添加整改要求" width="700px" :close-on-click-modal="false">
      <div v-for="(item, idx) in newRectItems" :key="idx" class="rect-form-row">
        <el-input v-model="item.itemName" placeholder="整改项名称" style="flex: 1" />
        <el-input v-model="item.requirement" placeholder="整改要求" style="flex: 1.5" />
        <el-date-picker
          v-model="item.deadline"
          type="date"
          placeholder="截止日期"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 160px"
        />
        <el-button link type="danger" @click="removeRectRow(idx)" :disabled="newRectItems.length <= 1">
          删除
        </el-button>
      </div>
      <el-button type="primary" link @click="addNewRectRow" style="margin-top: 8px">
        + 添加一行
      </el-button>
      <template #footer>
        <el-button @click="rectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitRectItems">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}
.back-link {
  margin-bottom: 16px;
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
.action-buttons {
  display: flex;
  gap: 8px;
}
.rect-form-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
</style>
