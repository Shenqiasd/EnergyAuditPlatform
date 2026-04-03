<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import {
  getPublishedVersion,
  getSubmission,
  saveDraft,
  renewLock,
  releaseLock,
  type TplSubmission,
  type TplTemplateVersion,
} from '@/api/template'
import { initSpreadJSLicense } from '@/utils/spreadjs-license'

const props = defineProps<{
  templateId: number
  auditYear: number
  /** Whether the parent already acquired the edit lock for this session */
  hasLock: boolean
  readonly?: boolean
}>()

const emit = defineEmits<{
  drafted: [submission: TplSubmission]
  /** Emitted when heartbeat renew fails 2+ consecutive times and the lock is assumed lost */
  lockLost: []
}>()

const spreadRef = ref<HTMLDivElement>()
const loading = ref(false)
const saving = ref(false)
const errorMsg = ref('')

let workbook: import('@/types/spreadjs').GCSpreadWorkbook | null = null
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let publishedVersion: TplTemplateVersion | null = null
let currentSubmission: TplSubmission | null = null

/**
 * ownsLock is initialised from props.hasLock at mount time (before any async work),
 * so every code path — including early returns and exceptions — can call
 * releaseLockIfOwned() and get the correct behaviour.
 */
let ownsLock = false

watch(
  () => props.readonly,
  (isNowReadonly) => {
    if (isNowReadonly && ownsLock) {
      enterReadonly()
    }
  }
)

onMounted(() => {
  ownsLock = props.hasLock
  initWorkbook()
})

onBeforeUnmount(() => {
  stopHeartbeat()
  releaseLockIfOwned()
  workbook?.destroy()
  workbook = null
})

async function initWorkbook() {
  if (!spreadRef.value) return
  if (!window.GC?.Spread?.Sheets?.Workbook) {
    errorMsg.value = 'SpreadJS 未加载，请检查网络连接后刷新页面'
    releaseLockIfOwned()
    return
  }
  initSpreadJSLicense()
  loading.value = true
  errorMsg.value = ''
  try {
    workbook = new window.GC.Spread.Sheets.Workbook(spreadRef.value)

    publishedVersion = await getPublishedVersion(props.templateId)
    if (!publishedVersion?.templateJson) {
      errorMsg.value = '该模板尚未发布有效版本，请联系管理员'
      workbook.destroy()
      workbook = null
      releaseLockIfOwned()
      return
    }

    currentSubmission = await getSubmission(props.templateId, props.auditYear)

    const jsonStr = currentSubmission?.submissionJson ?? publishedVersion.templateJson
    workbook.fromJSON(JSON.parse(jsonStr))

    const forceReadonly = props.readonly || currentSubmission?.status === 1
    if (forceReadonly) {
      applyReadonlyProtection()
      releaseLockIfOwned()
    } else {
      startHeartbeat()
    }
  } catch (e: any) {
    errorMsg.value = '加载模板失败：' + (e?.message ?? '未知错误')
    releaseLockIfOwned()
  } finally {
    loading.value = false
  }
}

function releaseLockIfOwned() {
  if (ownsLock) {
    releaseLock(props.templateId, props.auditYear).catch(() => {})
    ownsLock = false
  }
}

function enterReadonly() {
  stopHeartbeat()
  releaseLockIfOwned()
  applyReadonlyProtection()
}

function applyReadonlyProtection() {
  if (!workbook) return
  const count = workbook.getSheetCount()
  for (let i = 0; i < count; i++) {
    workbook.getSheet(i).options.isProtected = true
  }
}

function startHeartbeat() {
  let failCount = 0
  heartbeatTimer = setInterval(async () => {
    try {
      await renewLock(props.templateId, props.auditYear)
      failCount = 0
    } catch {
      failCount++
      if (failCount >= 2) {
        stopHeartbeat()
        ownsLock = false
        applyReadonlyProtection()
        emit('lockLost')
      }
    }
  }, 5 * 60 * 1000)
}

function stopHeartbeat() {
  if (heartbeatTimer !== null) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

async function save(): Promise<void> {
  if (!workbook || !publishedVersion) {
    throw new Error('工作簿尚未初始化，请稍后重试')
  }
  saving.value = true
  try {
    const json = JSON.stringify(workbook.toJSON())
    const saved = await saveDraft({
      templateId: props.templateId,
      auditYear: props.auditYear,
      submissionJson: json,
      templateVersion: publishedVersion.version ?? 1,
    })
    currentSubmission = saved
    emit('drafted', saved)
  } finally {
    saving.value = false
  }
}

function getSubmissionId(): number | undefined {
  return currentSubmission?.id
}

function getVersionId(): number | undefined {
  return publishedVersion?.id
}

function isSubmitted(): boolean {
  return currentSubmission?.status === 1
}

defineExpose({ save, getSubmissionId, getVersionId, isSubmitted, saving, loading })
</script>

<template>
  <div class="spreadsheet-wrapper" v-loading="loading" element-loading-text="正在加载表格模板…">
    <el-alert
      v-if="errorMsg"
      :title="errorMsg"
      type="error"
      :closable="false"
      style="margin-bottom: 12px"
    />
    <div v-show="!errorMsg" ref="spreadRef" class="spreadjs-host"></div>
  </div>
</template>

<style scoped lang="scss">
.spreadsheet-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.spreadjs-host {
  flex: 1;
  width: 100%;
  min-height: 500px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
</style>
