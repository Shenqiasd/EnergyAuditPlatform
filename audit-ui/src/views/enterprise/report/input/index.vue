<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  getTemplateList,
  acquireLock,
  checkLock,
  submitSubmission,
  type TplTemplate,
  type TplEditLock,
} from '@/api/template'
import SpreadSheet from '@/components/SpreadSheet/index.vue'

const route = useRoute()

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

async function loadTemplates() {
  const res = await getTemplateList({ status: 1, pageSize: 200 })
  templates.value = res.rows ?? []
}

async function openTemplate() {
  if (!selectedTemplateId.value) return
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

  // Validate required fields before submission
  const requiredErrors = spreadRef.value.validateRequiredFields()
  if (requiredErrors.length > 0) {
    ElMessageBox.alert(
      requiredErrors.map((e, i) => `${i + 1}. ${e}`).join('\n'),
      '必填字段未填写',
      { type: 'warning', confirmButtonText: '知道了' }
    )
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
    await spreadRef.value.save()
    const submissionId = spreadRef.value.getSubmissionId()
    const versionId = spreadRef.value.getVersionId()
    if (!submissionId || !versionId) {
      ElMessage.error('填报记录未找到，请先保存草稿')
      return
    }
    await submitSubmission(submissionId, versionId)
    ElMessage.success('提交成功，数据已抽取')
    isReadonly.value = true
  } catch (e: any) {
    ElMessage.error('提交失败：' + (e?.message ?? '未知错误'))
  } finally {
    submitting.value = false
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

      <div class="action-bar" v-if="!isReadonly">
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

.action-bar {
  flex-shrink: 0;
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  background: #fff;
}
</style>
