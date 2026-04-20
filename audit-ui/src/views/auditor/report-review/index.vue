<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listReportsForReview,
  getReportForReview,
  approveReport,
  rejectReport,
  downloadReport,
  REPORT_STATUS_MAP,
  type ArReport,
} from '@/api/report'

const router = useRouter()
const loading = ref(false)
const reports = ref<ArReport[]>([])
const filterStatus = ref<number | undefined>(undefined)
const filterYear = ref<number | undefined>(undefined)

const reviewDialogVisible = ref(false)
const currentReport = ref<ArReport | null>(null)
const reviewComment = ref('')
const actionLoading = ref(false)

const detailDialogVisible = ref(false)
const detailReport = ref<ArReport | null>(null)
const detailLoading = ref(false)

const statusOptions = [
  { value: undefined, label: '全部状态' },
  { value: 4, label: '待审核' },
  { value: 5, label: '已通过' },
  { value: 6, label: '已退回' },
]

const currentYear = new Date().getFullYear()
const yearOptions = computed(() => {
  const years: { value: number | undefined; label: string }[] = [{ value: undefined, label: '全部年度' }]
  for (let y = currentYear; y >= currentYear - 5; y--) {
    years.push({ value: y, label: `${y}年` })
  }
  return years
})

async function loadReports() {
  loading.value = true
  try {
    reports.value = await listReportsForReview(filterStatus.value, filterYear.value)
  } catch (e: any) {
    ElMessage.error('加载报告列表失败：' + (e?.message ?? ''))
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  loadReports()
}

function statusTag(status: number | undefined) {
  if (status === undefined || status === null) return { label: '未知', type: 'info' as const }
  return REPORT_STATUS_MAP[status] ?? { label: '未知', type: 'info' as const }
}

function formatDate(dt: string | null | undefined) {
  if (!dt) return '—'
  return dt.substring(0, 16).replace('T', ' ')
}

async function openDetail(report: ArReport) {
  detailLoading.value = true
  detailDialogVisible.value = true
  try {
    detailReport.value = await getReportForReview(report.id)
  } catch (e: any) {
    ElMessage.error('加载报告详情失败：' + (e?.message ?? ''))
    detailDialogVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function openReviewDialog(report: ArReport) {
  currentReport.value = report
  reviewComment.value = ''
  reviewDialogVisible.value = true
}

async function handleApprove() {
  if (!currentReport.value) return
  try {
    await ElMessageBox.confirm('确认审核通过该报告？', '审核通过', { type: 'success' })
  } catch { return }
  actionLoading.value = true
  try {
    await approveReport(currentReport.value.id, reviewComment.value || undefined)
    ElMessage.success('报告已审核通过')
    reviewDialogVisible.value = false
    loadReports()
  } catch (e: any) {
    ElMessage.error('操作失败：' + (e?.message ?? ''))
  } finally {
    actionLoading.value = false
  }
}

async function handleReject() {
  if (!currentReport.value) return
  if (!reviewComment.value.trim()) {
    ElMessage.warning('退回时必须填写退回理由')
    return
  }
  try {
    await ElMessageBox.confirm('确认退回该报告？企业将看到退回理由。', '退回报告', { type: 'warning' })
  } catch { return }
  actionLoading.value = true
  try {
    await rejectReport(currentReport.value.id, reviewComment.value)
    ElMessage.success('报告已退回')
    reviewDialogVisible.value = false
    loadReports()
  } catch (e: any) {
    ElMessage.error('操作失败：' + (e?.message ?? ''))
  } finally {
    actionLoading.value = false
  }
}

async function handleDownload(report: ArReport) {
  try {
    const blob = await downloadReport(report.id)
    const url = window.URL.createObjectURL(blob as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = report.reportName || `report-${report.id}.docx`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error('下载失败：' + (e?.message ?? ''))
  }
}

onMounted(loadReports)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">报告审核</span>
          <div class="filters">
            <el-select v-model="filterStatus" placeholder="状态" @change="onFilterChange" clearable style="width: 130px">
              <el-option
                v-for="opt in statusOptions"
                :key="String(opt.value)"
                :value="opt.value"
                :label="opt.label"
              />
            </el-select>
            <el-select v-model="filterYear" placeholder="年度" @change="onFilterChange" clearable style="width: 110px; margin-left: 8px">
              <el-option
                v-for="opt in yearOptions"
                :key="String(opt.value)"
                :value="opt.value"
                :label="opt.label"
              />
            </el-select>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="reports" border stripe size="small" style="width: 100%">
        <el-table-column label="报告名称" prop="reportName" min-width="200" show-overflow-tooltip />
        <el-table-column label="企业名称" prop="enterpriseName" min-width="150" show-overflow-tooltip />
        <el-table-column label="审计年度" prop="auditYear" width="90" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type" size="small">
              {{ statusTag(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150" align="center">
          <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="审核意见" prop="reviewComment" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">
              查看
            </el-button>
            <el-button
              v-if="row.status === 4"
              link
              type="success"
              size="small"
              @click="openReviewDialog(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.generatedFilePath"
              link
              type="info"
              size="small"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="reports.length === 0 && !loading" style="color: #909399; text-align: center; padding: 40px">
        暂无报告数据
      </div>
    </el-card>

    <!-- Review Dialog -->
    <el-dialog v-model="reviewDialogVisible" title="报告审核" width="500px" :close-on-click-modal="false">
      <div v-if="currentReport" style="margin-bottom: 16px">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="报告名称">{{ currentReport.reportName }}</el-descriptions-item>
          <el-descriptions-item label="企业名称">{{ currentReport.enterpriseName }}</el-descriptions-item>
          <el-descriptions-item label="审计年度">{{ currentReport.auditYear }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <el-input
        v-model="reviewComment"
        type="textarea"
        :rows="4"
        placeholder="请输入审核意见（退回时必填）..."
      />
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="handleReject">退回</el-button>
        <el-button type="success" :loading="actionLoading" @click="handleApprove">通过</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="报告详情" width="80%" :close-on-click-modal="true">
      <div v-loading="detailLoading">
        <template v-if="detailReport">
          <el-descriptions :column="3" border size="small" style="margin-bottom: 16px">
            <el-descriptions-item label="报告名称">{{ detailReport.reportName }}</el-descriptions-item>
            <el-descriptions-item label="审计年度">{{ detailReport.auditYear }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTag(detailReport.status).type" size="small">
                {{ statusTag(detailReport.status).label }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDate(detailReport.submitTime) }}</el-descriptions-item>
            <el-descriptions-item label="审核意见" :span="2">{{ detailReport.reviewComment || '—' }}</el-descriptions-item>
          </el-descriptions>
          <div v-if="detailReport.reportHtml" class="report-html-preview" v-html="detailReport.reportHtml" />
          <div v-else style="color: #909399; text-align: center; padding: 40px">
            报告内容为空
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.filters {
  display: flex;
  align-items: center;
}
.report-html-preview {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
  background: #fff;
}
.report-html-preview :deep(table) {
  border-collapse: collapse;
  width: 100%;
}
.report-html-preview :deep(td),
.report-html-preview :deep(th) {
  border: 1px solid #ddd;
  padding: 6px 8px;
}
</style>
