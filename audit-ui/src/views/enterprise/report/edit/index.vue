<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Editor from '@tinymce/tinymce-vue'
import 'tinymce/tinymce'
import 'tinymce/themes/silver'
import 'tinymce/models/dom'
import 'tinymce/icons/default'
import 'tinymce/plugins/table'
import 'tinymce/plugins/image'
import 'tinymce/plugins/lists'
import 'tinymce/plugins/link'
import 'tinymce/plugins/pagebreak'
import 'tinymce/plugins/fullscreen'
import 'tinymce/plugins/preview'
import 'tinymce/plugins/searchreplace'
import 'tinymce/plugins/wordcount'
import {
  getReportDetail,
  saveReportHtml,
  submitForReview,
  downloadReport,
  REPORT_STATUS_MAP,
  type ArReport,
} from '@/api/report'

const route = useRoute()
const router = useRouter()
const reportId = computed(() => Number(route.query.id))

const loading = ref(true)
const saving = ref(false)
const submitting = ref(false)
const downloading = ref(false)
const report = ref<ArReport | null>(null)
const htmlContent = ref('')
const isDirty = ref(false)
let autoSaveTimer: ReturnType<typeof setInterval> | null = null

const isReadonly = computed(() => {
  if (!report.value) return true
  const s = report.value.status
  // status 4=submitted for review, 5=approved — readonly
  return s === 4 || s === 5
})

const canEdit = computed(() => {
  if (!report.value) return false
  const s = report.value.status
  // status 2=generated, 6=returned — can edit
  return s === 2 || s === 6
})

const canSubmitReview = computed(() => {
  if (!report.value) return false
  return report.value.status === 2 || report.value.status === 6
})

const canDownload = computed(() => {
  if (!report.value) return false
  return report.value.status >= 2
})

const statusInfo = computed(() => {
  if (!report.value) return null
  return REPORT_STATUS_MAP[report.value.status] ?? null
})

const tinymceInit = {
  height: 'calc(100vh - 260px)',
  menubar: 'file edit view insert format table',
  plugins: 'table image lists link pagebreak fullscreen preview searchreplace wordcount',
  toolbar: [
    'undo redo | styles | bold italic underline strikethrough | forecolor backcolor',
    'alignleft aligncenter alignright alignjustify | bullist numlist | outdent indent | table image link pagebreak',
    'fullscreen preview searchreplace | removeformat',
  ].join(' | '),
  skin_url: '/tinymce/skins/ui/oxide',
  content_css: '/tinymce/skins/content/default/content.css',
  language: 'zh_CN',
  language_url: '/tinymce/langs/zh_CN.js',
  branding: false,
  resize: true,
  paste_data_images: true,
  table_responsive_width: true,
  table_default_styles: {
    width: '100%',
    'border-collapse': 'collapse',
  },
  content_style: `
    body {
      font-family: SimSun, "Times New Roman", serif;
      font-size: 14px;
      line-height: 1.6;
      max-width: 210mm;
      margin: 10px auto;
      padding: 20px 30px;
    }
    table {
      border-collapse: collapse;
      width: 100%;
      margin: 10px 0;
    }
    table td, table th {
      border: 1px solid #333;
      padding: 4px 8px;
      font-size: 12px;
    }
    h1, h2, h3 { font-family: SimHei, "Arial Black", sans-serif; }
    img { max-width: 100%; }
  `,
  setup: (editor: any) => {
    editor.on('change', () => {
      isDirty.value = true
    })
    editor.on('input', () => {
      isDirty.value = true
    })
  },
}

async function loadReport() {
  if (!reportId.value) {
    ElMessage.error('缺少报告ID参数')
    return
  }
  loading.value = true
  try {
    const data = await getReportDetail(reportId.value)
    report.value = data as ArReport
    htmlContent.value = data.reportHtml || '<p>报告内容为空，请先生成报告。</p>'
  } catch (e: any) {
    ElMessage.error('加载报告失败：' + (e?.message ?? '未知错误'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!report.value || isReadonly.value) return
  saving.value = true
  try {
    const updated = await saveReportHtml(report.value.id, htmlContent.value)
    report.value = updated as ArReport
    isDirty.value = false
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error('保存失败：' + (e?.message ?? '未知错误'))
  } finally {
    saving.value = false
  }
}

async function handleSubmitReview() {
  if (!report.value) return
  if (isDirty.value) {
    try {
      await ElMessageBox.confirm('您有未保存的更改，是否先保存再提交审核？', '提示', {
        confirmButtonText: '保存并提交',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await handleSave()
      // If save failed, isDirty is still true — abort submission
      if (isDirty.value) {
        return
      }
    } catch {
      return
    }
  }
  try {
    await ElMessageBox.confirm(
      '确认提交报告审核？提交后在审核完成前将无法编辑。',
      '提交审核',
      { type: 'warning' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    const updated = await submitForReview(report.value.id)
    report.value = updated as ArReport
    ElMessage.success('已提交审核')
  } catch (e: any) {
    ElMessage.error('提交审核失败：' + (e?.message ?? '未知错误'))
  } finally {
    submitting.value = false
  }
}

async function handleDownload() {
  if (!report.value) return
  downloading.value = true
  try {
    const blob = await downloadReport(report.value.id) as unknown as Blob
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = (report.value.reportName || '审计报告') + '.docx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error('下载失败：' + (e?.message ?? '未知错误'))
  } finally {
    downloading.value = false
  }
}

function goBack() {
  if (isDirty.value) {
    ElMessageBox.confirm('您有未保存的更改，确定离开？', '提示', {
      confirmButtonText: '离开',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      router.push('/enterprise/report/generate')
    }).catch(() => {})
  } else {
    router.push('/enterprise/report/generate')
  }
}

onMounted(() => {
  loadReport()
  // Auto-save every 60 seconds if dirty
  autoSaveTimer = setInterval(() => {
    if (isDirty.value && !isReadonly.value && !saving.value && report.value) {
      handleSave()
    }
  }, 60000)
})

onBeforeUnmount(() => {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = null
  }
})
</script>

<template>
  <div class="report-editor-page">
    <!-- Header Bar -->
    <div class="editor-header">
      <div class="header-left">
        <el-button @click="goBack" size="small">
          <el-icon><i class="el-icon-arrow-left" /></el-icon>
          返回
        </el-button>
        <span class="report-title">{{ report?.reportName || '审计报告' }}</span>
        <el-tag v-if="statusInfo" :type="statusInfo.type" size="small" style="margin-left: 8px">
          {{ statusInfo.label }}
        </el-tag>
        <span v-if="isDirty" class="unsaved-hint">( 有未保存的修改 )</span>
      </div>
      <div class="header-right">
        <el-button
          v-if="canEdit"
          type="primary"
          size="small"
          :loading="saving"
          @click="handleSave"
        >
          保存
        </el-button>
        <el-button
          v-if="canDownload"
          size="small"
          :loading="downloading"
          @click="handleDownload"
        >
          下载 DOCX
        </el-button>
        <el-button
          v-if="canSubmitReview"
          type="success"
          size="small"
          :loading="submitting"
          @click="handleSubmitReview"
        >
          提交审核
        </el-button>
      </div>
    </div>

    <!-- Readonly Banner -->
    <el-alert
      v-if="isReadonly && report"
      type="warning"
      :closable="false"
      style="margin-bottom: 8px"
    >
      <template #title>
        <span v-if="report.status === 4">报告已提交审核，等待审核结果。审核通过前无法编辑。</span>
        <span v-else-if="report.status === 5">报告已审核通过。</span>
        <span v-else>当前状态下不可编辑。</span>
      </template>
    </el-alert>

    <!-- Return Banner -->
    <el-alert
      v-if="report?.status === 6"
      type="error"
      :closable="false"
      style="margin-bottom: 8px"
    >
      <template #title>
        <strong>审核已退回</strong> — {{ report.reviewComment || '请根据审核意见修改后重新提交' }}
      </template>
    </el-alert>

    <!-- Editor -->
    <div v-loading="loading" class="editor-container">
      <Editor
        v-if="!loading"
        v-model="htmlContent"
        :init="tinymceInit"
        :disabled="isReadonly"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.report-editor-page {
  padding: 12px 16px;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 8px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.report-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.unsaved-hint {
  font-size: 12px;
  color: #e6a23c;
}

.editor-container {
  flex: 1;
  overflow: hidden;
}
</style>
