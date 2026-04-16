<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  getTemplateList,
  listSubmissions,
  getPublishedVersion,
  submitSubmission,
  type TplTemplate,
  type TplSubmission,
} from '@/api/template'
import {
  submitForAudit,
  getMyAuditStatus,
  AUDIT_STATUS_MAP,
  type AuditTask,
} from '@/api/audit-task'
import {
  generateReport,
  listReports,
  downloadReport,
  REPORT_STATUS_MAP,
  type ArReport,
} from '@/api/report'

const router = useRouter()
const loading = ref(false)
const submissions = ref<TplSubmission[]>([])
const templates = ref<TplTemplate[]>([])
const selectedYear = ref<number>(new Date().getFullYear())
const yearOptions = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)
const submittingId = ref<number | null>(null)
const auditTask = ref<AuditTask | null>(null)
const submittingAudit = ref(false)
const generatingReport = ref(false)
const reports = ref<ArReport[]>([])

interface Row {
  template: TplTemplate
  submission: TplSubmission | null
  status: -1 | 0 | 1 | 2 | 3
}

const rows = computed<Row[]>(() => {
  const subMap = new Map<number, TplSubmission>()
  submissions.value
    .filter(s => s.auditYear === selectedYear.value)
    .forEach(s => { if (s.templateId) subMap.set(s.templateId, s) })

  return templates.value.map(t => {
    const sub = t.id != null ? subMap.get(t.id) ?? null : null
    const status: -1 | 0 | 1 | 2 | 3 = sub == null ? -1 : (sub.status as 0 | 1 | 2 | 3)
    return { template: t, submission: sub, status }
  })
})

const allSubmitted = computed(() => {
  if (rows.value.length === 0) return false
  return rows.value.every(r => r.status === 1 || r.status === 2)
})

const canSubmitAudit = computed(() => {
  if (!allSubmitted.value) return false
  if (!auditTask.value) return true
  return auditTask.value.status === 3
})

const auditStatusInfo = computed(() => {
  if (!auditTask.value) return null
  const s = auditTask.value.status ?? -1
  return AUDIT_STATUS_MAP[s] ?? null
})

const STATUS_MAP: Record<number, { label: string; type: 'info' | 'warning' | 'success' | 'danger' }> = {
  [-1]: { label: '未开始', type: 'info' },
  0: { label: '草稿', type: 'warning' },
  1: { label: '已提交', type: 'success' },
  2: { label: '审核通过', type: 'success' },
  3: { label: '已退回', type: 'danger' },
}

async function loadData() {
  loading.value = true
  try {
    const [subs, tpls, task, rpts] = await Promise.all([
      listSubmissions(),
      getTemplateList({ status: 1, pageSize: 200 }).then(r => r.rows ?? []),
      getMyAuditStatus(selectedYear.value),
      listReports(selectedYear.value),
    ])
    submissions.value = subs
    templates.value = tpls
    auditTask.value = task
    reports.value = (rpts as ArReport[]) || []
  } finally {
    loading.value = false
  }
}

async function handleSubmit(row: Row) {
  if (!row.submission) {
    ElMessage.warning('尚未填报任何数据，请先前往填报页面保存草稿')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认提交「${row.template.templateName}」${selectedYear.value} 年度数据？提交后将触发数据抽取并锁定编辑。`,
      '提交确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  submittingId.value = row.submission.id!
  try {
    const publishedVer = await getPublishedVersion(row.template.id!)
    if (!publishedVer?.id) {
      ElMessage.error('模板尚未发布有效版本，无法提交')
      return
    }
    await submitSubmission(row.submission.id!, publishedVer.id)
    ElMessage.success('提交成功，数据已抽取')
    loadData()
  } catch (e: any) {
    ElMessage.error('提交失败：' + (e?.message ?? '未知错误'))
  } finally {
    submittingId.value = null
  }
}

async function handleSubmitAudit() {
  try {
    await ElMessageBox.confirm(
      `确认将 ${selectedYear.value} 年度所有填报数据提交审核？提交后审核员将审查您的数据。`,
      '提交审核确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  submittingAudit.value = true
  try {
    await submitForAudit(selectedYear.value)
    ElMessage.success('已提交审核')
    loadData()
  } catch (e: any) {
    ElMessage.error('提交审核失败：' + (e?.message ?? '未知错误'))
  } finally {
    submittingAudit.value = false
  }
}

function goToFill(row: Row) {
  router.push({
    path: '/enterprise/report/input',
    query: { templateId: row.template.id, year: selectedYear.value },
  })
}

function viewDetail(row: Row) {
  router.push({ path: '/enterprise/report/detail', query: { id: row.submission?.id } })
}

async function handleGenerateReport() {
  try {
    await ElMessageBox.confirm(
      `确认为 ${selectedYear.value} 年度生成审计报告（Word文档）？系统将根据已提交的数据自动生成报告。`,
      '生成报告',
      { type: 'info' }
    )
  } catch {
    return
  }
  generatingReport.value = true
  try {
    await generateReport(selectedYear.value)
    ElMessage.success('报告生成成功')
    loadData()
  } catch (e: any) {
    ElMessage.error('报告生成失败：' + (e?.message ?? '未知错误'))
  } finally {
    generatingReport.value = false
  }
}

async function handleDownloadReport(report: ArReport) {
  try {
    const blob = await downloadReport(report.id) as unknown as Blob
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = (report.reportName || '审计报告') + '.docx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error('下载失败：' + (e?.message ?? '未知错误'))
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <!-- Audit Status Banner -->
    <el-alert
      v-if="auditTask && auditTask.status === 3"
      type="error"
      :closable="false"
      style="margin-bottom: 16px"
    >
      <template #title>
        <strong>审核已退回</strong> — {{ auditTask.result || '请查看审核意见并修改后重新提交' }}
      </template>
    </el-alert>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="card-title">填报进度概览</span>
            <el-tag
              v-if="auditStatusInfo"
              :type="auditStatusInfo.type"
              size="small"
              style="margin-left: 12px"
            >
              审核状态：{{ auditStatusInfo.label }}
            </el-tag>
          </div>
          <div class="header-right">
            <el-select
              v-model="selectedYear"
              style="width:110px;margin-right:12px"
              size="small"
              @change="loadData"
            >
              <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
            </el-select>
            <el-button @click="loadData" :loading="loading" size="small">刷新</el-button>
            <el-button
              type="primary"
              size="small"
              :disabled="!canSubmitAudit"
              :loading="submittingAudit"
              @click="handleSubmitAudit"
              style="margin-left: 8px"
            >
              {{ auditTask?.status === 3 ? '重新提交审核' : '提交审核' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column label="模板名称" min-width="180">
          <template #default="{ row }">{{ row.template.templateName }}</template>
        </el-table-column>
        <el-table-column label="当前版本" width="90" align="center">
          <template #default="{ row }">v{{ row.template.currentVersion ?? 1 }}</template>
        </el-table-column>
        <el-table-column label="年度" width="90" align="center">
          {{ selectedYear }} 年
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.status === 3 && row.submission?.reviewComment"
              :content="row.submission.reviewComment"
              placement="top"
            >
              <el-tag type="danger" size="small" style="cursor: help">
                已退回
              </el-tag>
            </el-tooltip>
            <el-tag v-else :type="STATUS_MAP[row.status]?.type" size="small">
              {{ STATUS_MAP[row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ row.submission?.submitTime ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="最后保存" width="160">
          <template #default="{ row }">{{ row.submission?.updateTime ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="row.status === 1 || row.status === 2"
              @click="goToFill(row)"
            >
              {{ row.status === 3 ? '去修改' : '去填报' }}
            </el-button>
            <el-button
              link
              type="success"
              :loading="submittingId === row.submission?.id"
              :disabled="row.status !== 0"
              @click="handleSubmit(row)"
            >
              提交数据
            </el-button>
            <el-button
              link
              type="info"
              :disabled="!row.submission"
              @click="viewDetail(row)"
            >
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!allSubmitted && rows.length > 0" class="submit-hint">
        <el-text type="warning" size="small">
          提示：需所有模板填报完成并提交数据后，方可提交审核
        </el-text>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span class="card-title">审计报告</span>
          <el-button
            type="success"
            size="small"
            :loading="generatingReport"
            @click="handleGenerateReport"
          >
            生成审计报告
          </el-button>
        </div>
      </template>

      <el-empty v-if="reports.length === 0" description="暂无报告，点击上方按钮生成" />

      <el-table v-else :data="reports" border stripe>
        <el-table-column label="报告名称" prop="reportName" min-width="240" />
        <el-table-column label="审计年度" prop="auditYear" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="REPORT_STATUS_MAP[row.status]?.type || 'info'" size="small">
              {{ REPORT_STATUS_MAP[row.status]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生成时间" width="170">
          <template #default="{ row }">{{ row.generateTime ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="row.status < 2"
              @click="handleDownloadReport(row)"
            >
              下载
            </el-button>
            <el-button
              link
              type="success"
              :loading="generatingReport"
              @click="handleGenerateReport"
            >
              重新生成
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

.header-left {
  display: flex;
  align-items: center;
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

.submit-hint {
  margin-top: 12px;
  text-align: center;
}
</style>
