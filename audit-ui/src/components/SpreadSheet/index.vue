<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import {
  getPublishedVersion,
  getSubmission,
  saveDraft,
  renewLock,
  releaseLock,
  listTags,
  type TplSubmission,
  type TplTemplateVersion,
  type TplTagMapping,
} from '@/api/template'
import { getEnterpriseSettingPrefill } from '@/api/enterpriseSetting'
import { getDataByTypes, type DictData } from '@/api/dict'
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

    // Pre-fill enterprise settings into tagged cells when loading a fresh template
    if (!currentSubmission && publishedVersion.id) {
      await prefillEnterpriseSettings(workbook, publishedVersion.id)
    }

    // Inject dictionary-based dropdown validators for EQUIPMENT_BENCHMARK and TABLE tags
    if (publishedVersion.id) {
      await applyDictValidators(workbook, publishedVersion.id)
    }

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

/**
 * Pre-fill enterprise settings into SpreadJS cells that have tag mappings
 * targeting ent_enterprise_setting. This enables bidirectional sync:
 * enterprise settings page → SpreadJS template.
 */
async function prefillEnterpriseSettings(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  versionId: number,
) {
  try {
    const [tags, prefillData] = await Promise.all([
      listTags(versionId),
      getEnterpriseSettingPrefill(),
    ])
    if (!prefillData || Object.keys(prefillData).length === 0) return

    // Filter to only ent_enterprise_setting SCALAR mappings
    const entTags = tags.filter(
      (t: TplTagMapping) =>
        t.targetTable === 'ent_enterprise_setting' &&
        (!t.mappingType || t.mappingType === 'SCALAR'),
    )
    if (entTags.length === 0) return

    wb.suspendPaint()
    try {
      for (const tag of entTags) {
        const fieldName = tag.fieldName
        if (!fieldName || !(fieldName in prefillData)) continue
        const value = prefillData[fieldName]
        if (value == null || value === '') continue

        const filled = fillTaggedCell(wb, tag, value)
        if (!filled) {
          console.debug(`[prefill] could not locate cell for tag "${tag.tagName}"`)
        }
      }
    } finally {
      wb.resumePaint()
    }
  } catch (e) {
    // Pre-fill is best-effort; don't block template loading
    console.warn('[prefill] failed to pre-fill enterprise settings:', e)
  }
}

/**
 * Find the cell for a tag mapping and set its value.
 * Supports both cell-tag and named-range source types.
 */
function fillTaggedCell(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
  value: unknown,
): boolean {
  const sheetCount = wb.getSheetCount()

  // Try named range first
  if (tag.sourceType === 'NAMED_RANGE' && tag.tagName) {
    for (let si = 0; si < sheetCount; si++) {
      const sheet = wb.getSheet(si)
      const nr = sheet.getCustomName(tag.tagName)
      if (nr) {
        const row = nr.getRow()
        const col = nr.getColumn()
        sheet.setValue(row, col, value)
        return true
      }
    }
    // Also try workbook-level custom name
    const wbName = wb.getCustomName(tag.tagName)
    if (wbName) {
      const sheetIdx = tag.sheetIndex ?? 0
      if (sheetIdx >= 0 && sheetIdx < sheetCount) {
        const sheet = wb.getSheet(sheetIdx)
        const row = wbName.getRow()
        const col = wbName.getColumn()
        sheet.setValue(row, col, value)
        return true
      }
    }
  }

  // Fall back to cell tag scan: iterate cells on the target sheet (or all sheets)
  const startSheet = tag.sheetIndex ?? 0
  const endSheet = tag.sheetIndex != null ? tag.sheetIndex + 1 : sheetCount
  for (let si = startSheet; si < endSheet && si < sheetCount; si++) {
    const sheet = wb.getSheet(si)
    const rowCount = sheet.getRowCount()
    const colCount = sheet.getColumnCount()
    for (let r = 0; r < rowCount; r++) {
      for (let c = 0; c < colCount; c++) {
        const cellTag = sheet.getTag(r, c)
        if (cellTag === tag.tagName) {
          sheet.setValue(r, c, value)
          return true
        }
      }
    }
  }

  return false
}

/**
 * Inject SpreadJS DataValidation dropdowns for columns that have dictType
 * configured in EQUIPMENT_BENCHMARK or TABLE tag mappings' columnMappings.
 * This enforces selection from dictionary values (no free text).
 */
async function applyDictValidators(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  versionId: number,
) {
  try {
    const tags = await listTags(versionId)
    // Collect all dictType references from EQUIPMENT_BENCHMARK columnMappings
    const dictTypesNeeded = new Set<string>()
    interface DropdownTarget {
      sheetName?: string
      sheetIndex?: number
      startRow: number
      endRow: number
      col: number
      dictType: string
    }
    const targets: DropdownTarget[] = []

    for (const tag of tags) {
      if (tag.mappingType !== 'EQUIPMENT_BENCHMARK' || !tag.columnMappings || !tag.cellRange) continue

      let colMapRoot: Record<string, unknown>
      try {
        colMapRoot = JSON.parse(tag.columnMappings)
      } catch {
        continue
      }

      // Parse cellRange to get row bounds (e.g. "A4:AJ200")
      const rangeMatch = tag.cellRange.toUpperCase().trim().match(/([A-Z]+)(\d+):([A-Z]+)(\d+)/)
      if (!rangeMatch) continue
      const startRow = parseInt(rangeMatch[2]) - 1 // 0-based
      const endRow = parseInt(rangeMatch[4]) - 1
      const startCol = letterToColIndex(rangeMatch[1])

      // Scan commonColumns and typeColumns for dictType fields
      const allColMaps: Array<{ col: number; dictType?: string }> = []
      const commonCols = colMapRoot.commonColumns as Array<Record<string, unknown>> | undefined
      if (Array.isArray(commonCols)) {
        for (const cm of commonCols) {
          if (cm.dictType) allColMaps.push({ col: cm.col as number, dictType: cm.dictType as string })
        }
      }
      const typeCols = colMapRoot.typeColumns as Record<string, Array<Record<string, unknown>>> | undefined
      if (typeCols && typeof typeCols === 'object') {
        for (const cols of Object.values(typeCols)) {
          if (!Array.isArray(cols)) continue
          for (const cm of cols) {
            if (cm.dictType) allColMaps.push({ col: cm.col as number, dictType: cm.dictType as string })
          }
        }
      }

      // Deduplicate by col+dictType and build targets
      const seen = new Set<string>()
      for (const cm of allColMaps) {
        const key = `${cm.col}:${cm.dictType}`
        if (seen.has(key)) continue
        seen.add(key)
        dictTypesNeeded.add(cm.dictType!)
        targets.push({
          sheetName: tag.sheetName,
          sheetIndex: tag.sheetIndex,
          startRow,
          endRow,
          col: startCol + cm.col,
          dictType: cm.dictType!,
        })
      }
    }

    if (dictTypesNeeded.size === 0 || targets.length === 0) return

    // Fetch all needed dictionary data in one batch call
    const dictMap = await getDataByTypes([...dictTypesNeeded])
    if (!dictMap || Object.keys(dictMap).length === 0) return

    const DataValidation = window.GC?.Spread?.Sheets?.DataValidation
    if (!DataValidation) {
      console.warn('[dropdown] SpreadJS DataValidation not available')
      return
    }

    wb.suspendPaint()
    try {
      for (const target of targets) {
        const items: DictData[] = dictMap[target.dictType] ?? []
        if (items.length === 0) continue

        // Sanitize labels: replace commas with fullwidth comma to avoid SpreadJS separator conflict
        const listStr = items.map((d) => d.dictLabel.replace(/,/g, '\uff0c')).join(',')
        const dv = DataValidation.createListValidator(listStr)
        dv.inCellDropdown(true)
        dv.showInputMessage(true)
        dv.inputTitle('请选择')
        dv.inputMessage('点击下拉箭头选择')

        // Find target sheet
        const sheet = findSheet(wb, target.sheetName, target.sheetIndex)
        if (!sheet) continue

        for (let r = target.startRow; r <= target.endRow; r++) {
          sheet.setDataValidator(r, target.col, dv)
        }
      }
    } finally {
      wb.resumePaint()
    }
  } catch (e) {
    // Dropdown injection is best-effort; don't block template loading
    console.warn('[dropdown] failed to apply dict validators:', e)
  }
}

function findSheet(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  sheetName?: string,
  sheetIndex?: number,
): import('@/types/spreadjs').GCSpreadSheet | null {
  const count = wb.getSheetCount()
  if (sheetName) {
    for (let i = 0; i < count; i++) {
      const s = wb.getSheet(i)
      if (s.name() === sheetName) return s
    }
  }
  const idx = sheetIndex ?? 0
  return idx >= 0 && idx < count ? wb.getSheet(idx) : null
}

function letterToColIndex(letters: string): number {
  let col = 0
  for (let i = 0; i < letters.length; i++) {
    col = col * 26 + (letters.charCodeAt(i) - 64)
  }
  return col - 1 // 0-based
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
