<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  getTemplateList,
  acquireLock,
  checkLock,
  submitSubmission,
  type TplTemplate,
  type TplEditLock,
} from '@/api/template'
import { submitForAudit } from '@/api/audit-task'
import { checkPrerequisites } from '@/api/enterpriseSetting'
import SpreadSheet from '@/components/SpreadSheet/index.vue'

const route = useRoute()
const router = useRouter()

const templates = ref<TplTemplate[]>([])
const selectedTemplateId = ref<number | null>(null)
const selectedYear = ref<number>(new Date().getFullYear())
const yearOptions = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)

const lockLoading = ref(false)
const submitting = ref(false)
const lockedBy = ref<TplEditLock | null>(null)
const isReadonly = ref(false)
const isActive = ref(false)
const lockAcquired = ref(false)

const spreadRef = ref<InstanceType<typeof SpreadSheet>>()

const LAST_TEMPLATE_KEY = 'audit_last_template'

function saveLastTemplate() {
  if (selectedTemplateId.value && selectedYear.value) {
    localStorage.setItem(LAST_TEMPLATE_KEY, JSON.stringify({
      templateId: selectedTemplateId.value,
      auditYear: selectedYear.value,
    }))
  }
}

function loadLastTemplate(): { templateId: number; auditYear: number } | null {
  try {
    const raw = localStorage.getItem(LAST_TEMPLATE_KEY)
    if (!raw) return null
    const obj = JSON.parse(raw)
    if (obj?.templateId && obj?.auditYear) return obj
  } catch { /* ignore corrupt data */ }
  return null
}

async function loadTemplates() {
  const res = await getTemplateList({ status: 1, pageSize: 200 })
  templates.value = res.rows ?? []
}

async function runPrerequisiteCheck(): Promise<boolean> {
  try {
    const result = await checkPrerequisites()
    if (!result.passed) {
      const lines = result.errors.map(e => `• ${e}`).join('\n')
      await ElMessageBox.alert(
        `请先完成以下前置配置：\n\n${lines}\n\n请前往「基础设置」完成配置后再进行填报。`,
        '前置校验未通过',
        {
          type: 'warning',
          confirmButtonText: '前往设置',
          dangerouslyUseHTMLString: false,
        }
      )
      router.push('/enterprise/settings/company')
      return false
    }
    return true
  } catch (e: any) {
    ElMessage.error('前置校验失败：' + (e?.message ?? '未知错误'))
    return false
  }
}

async function openTemplate() {
  if (!selectedTemplateId.value) return

  // Prerequisite check — block entry if not all prerequisites are met
  const passed = await runPrerequisiteCheck()
  if (!passed) return
  isActive.value = false
  lockedBy.value = null
  isReadonly.value = false
  lockAcquired.value = false
  lockLoading.value = true
  try {
    await acquireLock(selectedTemplateId.value, selectedYear.value)
    isReadonly.value = false
    lockedBy.value = null
    lockAcquired.value = true
  } catch (e: any) {
    const msg: string = e?.response?.data?.message ?? e?.message ?? ''
    if (msg.includes('其他用户') || msg.includes('locked') || msg.includes('锁定')) {
      isReadonly.value = true
      lockAcquired.value = false
      try {
        lockedBy.value = await checkLock(selectedTemplateId.value, selectedYear.value)
      } catch {
        lockedBy.value = null
      }
    } else {
      ElMessage.error('获取编辑锁失败：' + msg)
      lockLoading.value = false
      return
    }
  } finally {
    lockLoading.value = false
  }
  isActive.value = true
  saveLastTemplate()
}

function onTemplateChange() {
  isActive.value = false
  lockedBy.value = null
  openTemplate()
}

async function handleSaveDraft() {
  if (!spreadRef.value) return
  try {
    await spreadRef.value.save()
    ElMessage.success('草稿已保存')
  } catch (e: any) {
    ElMessage.error('保存失败：' + (e?.message ?? '未知错误'))
  }
}

async function handleSubmit() {
  if (!spreadRef.value) return
  // Note: previously-submitted templates can be re-submitted.  The save()
  // call below resets the submission to draft (status=0) before submitting,
  // so we no longer block re-submission here.

  // Validate required fields before submission (grouped by sheet)
  const sheetErrors = spreadRef.value.validateRequiredFieldsBySheet()
  if (sheetErrors.length > 0) {
    const lines: string[] = []
    for (const se of sheetErrors) {
      lines.push(`【${se.sheetName}】`)
      for (const err of se.errors) {
        lines.push(`  • ${err}`)
      }
    }
    await ElMessageBox.alert(
      lines.join('\n'),
      '必填字段未填写',
      {
        type: 'warning',
        confirmButtonText: '去填写',
        dangerouslyUseHTMLString: false,
      }
    )
    // Navigate to the first sheet with errors
    if (sheetErrors[0]) {
      spreadRef.value.navigateToSheet(sheetErrors[0].sheetIndex)
    }
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认提交 ${selectedYear.value} 年度数据？提交后将触发数据抽取并锁定编辑。`,
      '提交确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    // P0: skip extraction during save — submit() will perform its own extraction,
    // avoiding the duplicate extraction that caused 30s+ timeout
    await spreadRef.value.save({ skipExtraction: true })
    const submissionId = spreadRef.value.getSubmissionId()
    const versionId = spreadRef.value.getVersionId()
    if (!submissionId || !versionId) {
      ElMessage.error('填报记录未找到，请先保存草稿')
      return
    }
    await submitSubmission(submissionId, versionId)
    ElMessage.success('提交成功，数据已抽取')
    isReadonly.value = true

    // After successful template submission, prompt user to also submit for audit review
    await promptSubmitForAudit()
  } catch (e: any) {
    ElMessage.error('提交失败：' + (e?.message ?? '未知错误'))
  } finally {
    submitting.value = false
  }
}

async function promptSubmitForAudit() {
  try {
    await ElMessageBox.confirm(
      '模板数据提交成功！是否同时提交审核？\n提交后审核员将收到审核任务并审查您的数据。',
      '提交审核',
      {
        type: 'success',
        confirmButtonText: '提交审核',
        cancelButtonText: '稍后再说',
        distinguishCancelAndClose: true,
      }
    )
  } catch {
    // User clicked "稍后再说" or closed the dialog — skip audit submission
    return
  }
  try {
    await submitForAudit(selectedYear.value)
    ElMessage.success('已提交审核，审核员将收到审核任务')
  } catch (e: any) {
    const msg: string = e?.message ?? '未知错误'
    if (msg.includes('尚未提交')) {
      // Some templates haven't been submitted yet — inform user
      ElMessage.warning('部分模板尚未提交，请完成所有模板填报后再提交审核')
    } else if (msg.includes('已存在审核任务')) {
      ElMessage.info('该年度审核任务已存在，无需重复提交')
    } else {
      ElMessage.error('提交审核失败：' + msg)
    }
  }
}

function onLockLost() {
  isReadonly.value = true
  lockedBy.value = null
  ElMessage.warning({
    message: '编辑锁已过期，文档已切换为只读模式。如需继续编辑，请重新打开。',
    duration: 6000,
  })
}

onMounted(async () => {
  await loadTemplates()
  const qId = route.query.templateId
  const qYear = route.query.year
  if (qId) {
    selectedTemplateId.value = Number(qId)
  }
  if (qYear) {
    selectedYear.value = Number(qYear)
  }
  // Fallback: restore from localStorage if no URL params provided
  if (!selectedTemplateId.value) {
    const last = loadLastTemplate()
    if (last) {
      // Only restore if the template still exists in the published list
      const exists = templates.value.some(t => t.id === last.templateId)
      if (exists) {
        selectedTemplateId.value = last.templateId
        selectedYear.value = last.auditYear
      }
    }
  }
  if (selectedTemplateId.value) {
    openTemplate()
  }
})
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-form-item label="审计模板" style="margin-bottom:0">
          <el-select
            v-model="selectedTemplateId"
            placeholder="请选择已发布的模板"
            filterable
            clearable
            style="width:300px"
            @change="onTemplateChange"
          >
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="`${t.templateName}（v${t.currentVersion ?? 1}）`"
              :value="t.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审计年度" style="margin-bottom:0">
          <el-select
            v-model="selectedYear"
            style="width:120px"
            @change="onTemplateChange"
          >
            <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
        </el-form-item>
        <el-button
          v-if="selectedTemplateId && !isActive"
          type="primary"
          :loading="lockLoading"
          @click="openTemplate"
        >
          打开填报
        </el-button>
        <div v-if="isActive && !isReadonly" class="action-buttons">
          <el-button
            type="primary"
            :loading="spreadRef?.saving"
            @click="handleSaveDraft"
          >
            保存草稿
          </el-button>
          <el-button
            type="success"
            :loading="submitting"
            @click="handleSubmit"
          >
            提交数据
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-if="!selectedTemplateId || !isActive" class="empty-area">
      <el-empty :description="selectedTemplateId ? '正在加载…' : '请在上方选择模板和年度'" />
    </div>

    <template v-if="isActive && selectedTemplateId">
      <el-alert
        v-if="isReadonly && lockedBy"
        type="warning"
        :closable="false"
        :title="`当前文档正在被「${lockedBy.updateBy ?? '其他用户'}」编辑，已进入只读模式`"
        show-icon
      />
      <el-alert
        v-else-if="isReadonly"
        type="warning"
        :closable="false"
        title="当前文档处于只读模式（已提交或被他人锁定）"
        show-icon
      />

      <div class="spreadsheet-area">
        <SpreadSheet
          ref="spreadRef"
          :templateId="selectedTemplateId"
          :auditYear="selectedYear"
          :hasLock="lockAcquired"
          :readonly="isReadonly"
          @drafted="() => {}"
          @lock-lost="onLockLost"
        />
      </div>

    </template>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 80px);
}

.filter-card {
  flex-shrink: 0;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.action-buttons {
  margin-left: auto;
  display: flex;
  gap: 12px;
}

.empty-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spreadsheet-area {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

</style>
