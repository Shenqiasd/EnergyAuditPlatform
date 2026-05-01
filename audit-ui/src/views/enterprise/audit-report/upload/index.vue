<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, RefreshRight, Upload as UploadIcon } from '@element-plus/icons-vue'
import {
  downloadActiveReportTemplate,
  downloadUploadedReport,
  getActiveReportTemplate,
  listReports,
  REPORT_STATUS_MAP,
  submitForReview,
  uploadFilledReport,
  type ArReport,
  type ArReportTemplate,
} from '@/api/report'

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 6 }, (_, i) => currentYear - i)
const selectedYear = ref<number>(currentYear)

const loading = ref(false)
const downloadingTemplate = ref(false)
const uploading = ref(false)
const downloadingReport = ref(false)
const submittingReview = ref(false)

const activeTemplate = ref<ArReportTemplate | null>(null)
const myReport = ref<ArReport | null>(null)

const myReportStatus = computed(() => {
  if (!myReport.value) return null
  return REPORT_STATUS_MAP[myReport.value.status] ?? null
})

// Re-upload allowed when there is no record yet, or status is 0 (draft) / 2 (uploaded) / 3 (failed) / 6 (rejected).
// Locked while under review (4) or already approved (5).
const canUpload = computed(() => {
  if (!myReport.value) return true
  return [0, 2, 3, 6].includes(myReport.value.status)
})

// Submit for review allowed only when content is uploaded and not yet under review / approved.
const canSubmitForReview = computed(() => {
  if (!myReport.value) return false
  return [2, 6].includes(myReport.value.status) && !!myReport.value.uploadedFilePath
})

const canDownloadOwn = computed(() => {
  if (!myReport.value) return false
  return !!myReport.value.uploadedFilePath || !!myReport.value.uploadedFileName
})

function formatBytes(bytes: number | null | undefined): string {
  if (!bytes || bytes <= 0) return '—'
  const units = ['B', 'KB', 'MB', 'GB']
  let v = bytes
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i += 1
  }
  return `${v.toFixed(v >= 100 || i === 0 ? 0 : 1)} ${units[i]}`
}

function formatDate(s: string | null | undefined): string {
  if (!s) return '—'
  // backend returns ISO-8601 or "yyyy-MM-dd HH:mm:ss"
  return s.replace('T', ' ').slice(0, 19)
}

function triggerBlobDownload(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

async function loadData() {
  loading.value = true
  try {
    const [tpl, reports] = await Promise.all([
      getActiveReportTemplate().catch(() => null),
      listReports(selectedYear.value),
    ])
    activeTemplate.value = tpl
    // Pick the matching record for the selected year (defensive — listReports already filters).
    const match = (reports || []).find(r =>
      r.auditYear === selectedYear.value && (r.reportType == null || r.reportType === 2),
    )
    myReport.value = match ?? null
  } finally {
    loading.value = false
  }
}

async function handleDownloadTemplate() {
  if (!activeTemplate.value) return
  downloadingTemplate.value = true
  try {
    const blob = await downloadActiveReportTemplate()
    const fallback = (activeTemplate.value.templateName || 'report-template') + '.docx'
    triggerBlobDownload(blob as Blob, activeTemplate.value.originalFileName || fallback)
    ElMessage.success('模板已开始下载')
  } catch (e: unknown) {
    ElMessage.error('下载失败：' + extractMsg(e))
  } finally {
    downloadingTemplate.value = false
  }
}

function beforeUpload(file: File): boolean {
  const lower = file.name.toLowerCase()
  if (!lower.endsWith('.docx')) {
    ElMessage.error('请上传 .docx 格式的报告')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50 MB')
    return false
  }
  return true
}

async function handleUpload(opts: { file: File }) {
  const file = opts.file
  if (!file || !beforeUpload(file)) return
  if (myReport.value && [4, 5].includes(myReport.value.status)) {
    ElMessage.warning('当前报告已提交审核或已通过，无法重新上传')
    return
  }
  if (myReport.value && [2, 6].includes(myReport.value.status)) {
    try {
      await ElMessageBox.confirm(
        '已存在该年度的报告文件，重新上传将覆盖原文件并清除审核退回意见，确定继续吗？',
        '确认覆盖',
        { type: 'warning' },
      )
    } catch {
      return
    }
  }
  uploading.value = true
  try {
    const updated = await uploadFilledReport(file, selectedYear.value, 2)
    myReport.value = updated
    ElMessage.success('报告上传成功')
  } catch (e: unknown) {
    ElMessage.error('上传失败：' + extractMsg(e))
  } finally {
    uploading.value = false
  }
}

async function handleDownloadOwnReport() {
  if (!myReport.value) return
  downloadingReport.value = true
  try {
    const blob = await downloadUploadedReport(myReport.value.id)
    const fallback = `审计报告-${selectedYear.value}.docx`
    triggerBlobDownload(blob as Blob, myReport.value.uploadedFileName || fallback)
  } catch (e: unknown) {
    ElMessage.error('下载失败：' + extractMsg(e))
  } finally {
    downloadingReport.value = false
  }
}

async function handleSubmitForReview() {
  if (!myReport.value) return
  try {
    await ElMessageBox.confirm(
      `确认将 ${selectedYear.value} 年度报告提交审核员审核？提交后无法再次修改，需等待审核结果。`,
      '提交审核确认',
      { type: 'warning' },
    )
  } catch {
    return
  }
  submittingReview.value = true
  try {
    const updated = await submitForReview(myReport.value.id)
    myReport.value = updated
    ElMessage.success('已提交审核')
  } catch (e: unknown) {
    ElMessage.error('提交失败：' + extractMsg(e))
  } finally {
    submittingReview.value = false
  }
}

function extractMsg(e: unknown): string {
  if (e instanceof Error) return e.message
  if (typeof e === 'string') return e
  return '未知错误'
}

onMounted(loadData)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="header">
      <div class="title-block">
        <h2 class="page-title">审计报告</h2>
        <p class="page-subtitle">下载模板，线下用 Word 填写后上传，再提交审核员审核</p>
      </div>
      <div class="year-picker">
        <span class="year-label">审计年度</span>
        <el-select v-model="selectedYear" style="width:120px" @change="loadData">
          <el-option v-for="y in yearOptions" :key="y" :label="y + ' 年'" :value="y" />
        </el-select>
        <el-button :icon="RefreshRight" :loading="loading" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- Card 1: Active template download -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">第一步 · 下载报告模板</span>
        </div>
      </template>

      <template v-if="activeTemplate">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="模板名称">
            {{ activeTemplate.templateName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="原始文件名">
            {{ activeTemplate.originalFileName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="版本">
            v{{ activeTemplate.version }}
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">
            {{ formatBytes(activeTemplate.fileSize) }}
          </el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">
            {{ formatDate(activeTemplate.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="action-row">
          <el-button
            type="primary"
            :icon="Download"
            :loading="downloadingTemplate"
            @click="handleDownloadTemplate"
          >下载模板（.docx）</el-button>
          <span class="help-text">请使用 Microsoft Word 打开后填写，保存为 .docx 后回到此页面上传。</span>
        </div>
      </template>

      <el-empty v-else description="管理员尚未上传报告模板，请联系管理员" />
    </el-card>

    <!-- Card 2: My report -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">第二步 · 上传已填写的报告</span>
          <el-tag v-if="myReportStatus" :type="myReportStatus.type" size="small">
            {{ myReportStatus.label }}
          </el-tag>
        </div>
      </template>

      <template v-if="myReport">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="文件名">
            {{ myReport.uploadedFileName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">
            {{ formatBytes(myReport.uploadedFileSize) }}
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            {{ formatDate(myReport.uploadedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核状态">
            <el-tag v-if="myReportStatus" :type="myReportStatus.type" size="small">
              {{ myReportStatus.label }}
            </el-tag>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="myReport.status === 6" label="退回原因" :span="2">
            <el-text type="danger">{{ myReport.reviewComment || '审核员未填写原因' }}</el-text>
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty
        v-else
        description="您尚未上传该年度报告，请下载模板填写后上传"
      >
        <template #image><span style="font-size:48px">📤</span></template>
      </el-empty>

      <div class="action-row">
        <el-upload
          v-if="canUpload"
          :auto-upload="true"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          accept=".docx"
        >
          <el-button type="primary" :icon="UploadIcon" :loading="uploading">
            {{ myReport ? '重新上传报告' : '上传报告' }}
          </el-button>
        </el-upload>

        <el-button
          v-if="canDownloadOwn"
          :icon="Download"
          :loading="downloadingReport"
          @click="handleDownloadOwnReport"
        >下载已上传报告</el-button>

        <el-button
          v-if="canSubmitForReview"
          type="success"
          :loading="submittingReview"
          @click="handleSubmitForReview"
        >提交审核</el-button>

        <span v-if="myReport && myReport.status === 4" class="help-text">
          报告已提交审核，请等待审核员处理
        </span>
        <span v-if="myReport && myReport.status === 5" class="help-text">
          报告已审核通过
        </span>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.title-block {
  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
  .page-subtitle {
    font-size: 13px;
    color: #909399;
    margin: 4px 0 0;
  }
}

.year-picker {
  display: flex;
  align-items: center;
  gap: 8px;
  .year-label {
    color: #606266;
    font-size: 13px;
  }
}

.section-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.action-row {
  margin-top: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.help-text {
  font-size: 13px;
  color: #909399;
}
</style>
