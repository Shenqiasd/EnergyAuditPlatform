<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
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

    // ── Phase 1: fetch template version + submission in parallel ──────
    const [fetchedVersion, fetchedSubmission] = await Promise.all([
      getPublishedVersion(props.templateId),
      getSubmission(props.templateId, props.auditYear),
    ])
    publishedVersion = fetchedVersion
    currentSubmission = fetchedSubmission

    if (!publishedVersion?.templateJson) {
      errorMsg.value = '该模板尚未发布有效版本，请联系管理员'
      workbook.destroy()
      workbook = null
      releaseLockIfOwned()
      return
    }

    const jsonStr = currentSubmission?.submissionJson ?? publishedVersion.templateJson
    workbook.fromJSON(JSON.parse(jsonStr))

    // ── Phase 2: fetch tags + prefill data in parallel (one listTags call) ─
    // Wrapped in its own try-catch so that a failure in supplementary features
    // (prefill / dropdowns / protection) does NOT prevent the core spreadsheet
    // from rendering — matching the original best-effort error handling.
    if (publishedVersion.id) {
      try {
        const [tags, prefillData] = await Promise.all([
          listTags(publishedVersion.id),
          !currentSubmission
            ? getEnterpriseSettingPrefill().catch(() => null)
            : Promise.resolve(null),
        ])

        // Pre-fill enterprise settings (uses pre-fetched tags + prefillData)
        if (!currentSubmission && prefillData) {
          applyPrefill(workbook, tags, prefillData)
        }

        // Inject dictionary-based dropdown validators (uses pre-fetched tags)
        await applyDictValidators(workbook, tags)

        // Bind ValidationError event on every sheet
        bindValidationErrorDialogs(workbook)

        // Apply cell protection + required field markers (uses pre-fetched tags)
        if (publishedVersion.protectionEnabled !== 0) {
          applyDataEntryProtection(workbook, tags)
        }
      } catch (e) {
        console.warn('[phase2] failed to load tags / apply features:', e)
        // Still bind validation error dialogs as fallback
        bindValidationErrorDialogs(workbook)
      }
    } else {
      bindValidationErrorDialogs(workbook)
    }

    // Only force readonly when explicitly set by the parent (e.g. lock held by
    // another user).  A previously-submitted template (status===1) should remain
    // editable when the current user holds the edit lock — the first saveDraft
    // call will reset the submission back to draft (status=0).
    const forceReadonly = props.readonly || (!ownsLock && currentSubmission?.status === 1)
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
 * Apply pre-fetched enterprise settings into SpreadJS cells.
 * Uses pre-fetched tags (avoids duplicate listTags call).
 */
function applyPrefill(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tags: TplTagMapping[],
  prefillData: Record<string, unknown>,
) {
  try {
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
  tags: TplTagMapping[],
) {
  try {
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
        dv.showErrorMessage(true)
        dv.errorTitle('输入错误')
        dv.errorMessage('请输入下拉列表中的值')

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

/**
 * Bind the SpreadJS ValidationError event on every sheet so that entering an
 * invalid value in a cell with DataValidation (e.g. a dropdown list) shows an
 * error dialog to the user.
 *
 * The SpreadJS *Designer* component has built-in validation popups, but the
 * plain *Workbook* component does not — we must handle the event ourselves.
 * When the validator's errorStyle is "stop", we also cancel the edit so the
 * invalid value is rejected (matching Excel / Designer behaviour).
 */
function bindValidationErrorDialogs(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
) {
  const Events = window.GC?.Spread?.Sheets?.Events
  if (!Events?.ValidationError) return

  const sheetCount = wb.getSheetCount()
  for (let i = 0; i < sheetCount; i++) {
    const sheet = wb.getSheet(i)
    sheet.bind(Events.ValidationError, (_sender: unknown, args: {
      validationResult?: number
      validator?: { errorTitle?(): string; errorMessage?(): string; errorStyle?(): number }
      row?: number
      col?: number
    }) => {
      const dv = args.validator
      const title = dv?.errorTitle?.() || '错误提示'
      const message = dv?.errorMessage?.() || '请输入下拉列表中的值'
      // errorStyle: 0 = stop (reject), 1 = warning, 2 = information
      const style = dv?.errorStyle?.() ?? 0
      if (style === 0) {
        // Reject the invalid value by reverting the cell edit
        args.validationResult = 1 // GC.Spread.Sheets.DataValidation.DataValidationResult.forceApply → 0, discard → 1
      }
      ElMessageBox.alert(message, title, {
        type: style === 0 ? 'error' : 'warning',
        confirmButtonText: '确定',
      })
    })
  }
}

/** Required‐field background colour (light orange) */
const REQUIRED_BG = '#FFF3E0'

/**
 * Core cell‐protection logic:
 *  1. Set every cell's default style to locked=true
 *  2. Based on Tag Mapping, unlock the data‐entry cells (SCALAR → single cell; TABLE → data range)
 *  3. Mark required fields with background colour + comment
 *  4. Enable sheet protection with options that still allow selecting cells
 */
function applyDataEntryProtection(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tags: TplTagMapping[],
) {
  try {
    if (!tags || tags.length === 0) return
    cachedTags = tags

    const scalarCount = tags.filter(t => (t.mappingType ?? 'SCALAR') === 'SCALAR').length
    const tableCount = tags.length - scalarCount
    console.log(`[protection] 收到 ${tags.length} 个 tag mappings (SCALAR: ${scalarCount}, TABLE/BENCH: ${tableCount})`)

    wb.suspendPaint()
    try {
      const sheetCount = wb.getSheetCount()

      // Step 1: Lock all cells by default on every sheet
      for (let si = 0; si < sheetCount; si++) {
        const sheet = wb.getSheet(si)
        const rows = sheet.getRowCount()
        const cols = sheet.getColumnCount()
        const allRange = sheet.getRange(0, 0, rows, cols)
        allRange.locked(true)
      }

      // Step 2 & 3: Unlock mapped cells and mark required ones
      // Each tag is wrapped in its own try-catch so one failure
      // does NOT prevent remaining tags from being unlocked.
      let unlocked = 0
      let failed = 0
      for (const tag of tags) {
        try {
          const mappingType = tag.mappingType ?? 'SCALAR'

          if (mappingType === 'SCALAR') {
            unlockScalarCell(wb, tag)
          } else if (mappingType === 'TABLE' || mappingType === 'EQUIPMENT_BENCHMARK') {
            unlockTableRange(wb, tag)
          }
          unlocked++
        } catch (e) {
          failed++
          console.warn(`[protection] 解锁 tag 失败: ${tag.tagName} (${tag.mappingType}/${tag.sourceType})`, e)
        }
      }
      if (failed > 0) {
        console.warn(`[protection] ${failed}/${tags.length} 个 tag 解锁失败`)
      }
      console.log(`[protection] 解锁完成: 成功 ${unlocked}, 失败 ${failed}`)

      // Step 4: Enable sheet protection on every sheet
      for (let si = 0; si < sheetCount; si++) {
        const sheet = wb.getSheet(si)
        sheet.options.protectionOptions = {
          allowSelectLockedCells: true,
          allowSelectUnlockedCells: true,
          allowResizeRows: false,
          allowResizeColumns: false,
          allowEditObjects: false,
          allowDragInsertRows: false,
          allowDragInsertColumns: false,
          allowInsertRows: false,
          allowInsertColumns: false,
          allowDeleteRows: false,
          allowDeleteColumns: false,
          allowSort: false,
          allowFilter: true,
        }
        sheet.options.isProtected = true
      }
    } finally {
      wb.resumePaint()
    }
  } catch (e) {
    console.warn('[protection] failed to apply data entry protection:', e)
  }
}

/**
 * Unlock a single SCALAR cell and optionally mark it as required.
 */
function unlockScalarCell(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
) {
  const sheetCount = wb.getSheetCount()
  const hint = tag.fieldName ?? tag.tagName ?? '此字段'

  // For NAMED_RANGE source types, try named range first (consistent with fillTaggedCell)
  if (tag.sourceType === 'NAMED_RANGE' && tag.tagName) {
    for (let si = 0; si < sheetCount; si++) {
      const sheet = wb.getSheet(si)
      const nr = sheet.getCustomName(tag.tagName)
      if (nr) {
        const cell = sheet.getCell(nr.getRow(), nr.getColumn())
        cell.locked(false)
        if (tag.required === 1) markCellRequired(cell, hint)
        return
      }
    }
    // Also try workbook-level custom name
    const wbName = wb.getCustomName(tag.tagName)
    if (wbName) {
      const sheetIdx = tag.sheetIndex ?? 0
      if (sheetIdx >= 0 && sheetIdx < sheetCount) {
        const sheet = wb.getSheet(sheetIdx)
        const cell = sheet.getCell(wbName.getRow(), wbName.getColumn())
        cell.locked(false)
        if (tag.required === 1) markCellRequired(cell, hint)
        return
      }
    }
  }

  // Fall back to cell tag scan (or primary path for non-NAMED_RANGE tags)
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
          const cell = sheet.getCell(r, c)
          cell.locked(false)
          if (tag.required === 1) markCellRequired(cell, hint)
          return
        }
      }
    }
  }
  console.warn(
    `[protection] SCALAR tag 未找到匹配单元格: ${tag.tagName}`,
    `(source=${tag.sourceType}, sheet=${tag.sheetName ?? tag.sheetIndex})`,
  )
}

/**
 * Unlock the data range for a TABLE / EQUIPMENT_BENCHMARK mapping.
 *
 * Resolution order:
 *  1. For NAMED_RANGE sources — resolve the Named Range from the SpreadJS
 *     workbook at runtime (more accurate than the stored cellRange which may
 *     be stale after template edits).
 *  2. Fall back to the stored `cellRange` from the database.
 *  3. If neither works, log a warning and return.
 */
function unlockTableRange(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
) {
  let startRow: number | undefined
  let endRow: number | undefined
  let startCol: number | undefined
  let endCol: number | undefined
  let sheet: import('@/types/spreadjs').GCSpreadSheet | null = null

  // ── Strategy 1: resolve Named Range at runtime ────────────────────────
  if (tag.sourceType === 'NAMED_RANGE' && tag.tagName) {
    const sheetCount = wb.getSheetCount()
    // Try sheet-level custom names first
    for (let si = 0; si < sheetCount; si++) {
      const s = wb.getSheet(si)
      const nr = s.getCustomName(tag.tagName)
      if (nr && (nr.getRowCount() > 1 || nr.getColumnCount() > 1)) {
        sheet = s
        startRow = nr.getRow()
        startCol = nr.getColumn()
        endRow = startRow + nr.getRowCount() - 1
        endCol = startCol + nr.getColumnCount() - 1
        break
      }
    }
    // Try workbook-level custom name
    if (!sheet) {
      const nr = wb.getCustomName(tag.tagName)
      if (nr && (nr.getRowCount() > 1 || nr.getColumnCount() > 1)) {
        sheet = findSheet(wb, tag.sheetName, tag.sheetIndex)
        if (sheet) {
          startRow = nr.getRow()
          startCol = nr.getColumn()
          endRow = startRow + nr.getRowCount() - 1
          endCol = startCol + nr.getColumnCount() - 1
        }
      }
    }
  }

  // ── Strategy 2: fall back to stored cellRange ─────────────────────────
  if (!sheet && tag.cellRange) {
    const rangeMatch = tag.cellRange.toUpperCase().trim().match(/([A-Z]+)(\d+):([A-Z]+)(\d+)/)
    if (rangeMatch) {
      startRow = parseInt(rangeMatch[2]) - 1
      endRow = parseInt(rangeMatch[4]) - 1
      startCol = letterToColIndex(rangeMatch[1])
      endCol = letterToColIndex(rangeMatch[3])
      sheet = findSheet(wb, tag.sheetName, tag.sheetIndex)
    }
  }

  if (!sheet || startRow == null || endRow == null || startCol == null || endCol == null) {
    console.warn(
      `[protection] TABLE tag 未能解析 sheet/range: ${tag.tagName}`,
      `(source=${tag.sourceType}, cellRange=${tag.cellRange ?? 'null'}, sheet=${tag.sheetName ?? tag.sheetIndex})`,
    )
    return
  }

  // Unlock the entire data range
  const rowCount = endRow - startRow + 1
  const colCount = endCol - startCol + 1
  if (rowCount > 0 && colCount > 0) {
    sheet.getRange(startRow, startCol, rowCount, colCount).locked(false)
  }

  // If the whole table mapping is required, mark the first row's first cell
  if (tag.required === 1) {
    const cell = sheet.getCell(startRow, startCol)
    markCellRequired(cell, `${tag.tagName ?? tag.targetTable ?? '此表格'} (至少填写1行)`)
  }
}

/**
 * Apply required field visual indicator: light orange background + comment tooltip.
 */
function markCellRequired(
  cell: import('@/types/spreadjs').GCCellRange,
  fieldHint: string,
) {
  cell.backColor(REQUIRED_BG)
  try {
    const Comments = window.GC?.Spread?.Sheets?.Comments
    if (Comments?.Comment) {
      const comment = new Comments.Comment()
      comment.text(`必填字段: ${fieldHint}`)
      cell.comment(comment)
    }
  } catch {
    // Comment creation is best-effort
  }
}

/**
 * Validate that all required fields have been filled before submission.
 * Returns an array of error messages (empty = all valid).
 */
function validateRequiredFields(): string[] {
  if (!workbook || !publishedVersion) return []
  if (publishedVersion.protectionEnabled === 0) return []

  const errors: string[] = []
  const tags = cachedTags // populated during applyDataEntryProtection

  for (const tag of tags) {
    if (tag.required !== 1) continue
    const mappingType = tag.mappingType ?? 'SCALAR'

    if (mappingType === 'SCALAR') {
      const empty = isScalarCellEmpty(workbook, tag)
      if (empty) {
        errors.push(`"${tag.fieldName ?? tag.tagName}" 为必填字段，请填写`)
      }
    } else if (mappingType === 'TABLE' || mappingType === 'EQUIPMENT_BENCHMARK') {
      const empty = isTableEmpty(workbook, tag)
      if (empty) {
        errors.push(`"${tag.tagName ?? tag.targetTable}" 至少需要填写1行数据`)
      }
    }
  }
  return errors
}

/** Cached tag mappings loaded during protection setup */
let cachedTags: TplTagMapping[] = []

function isScalarCellEmpty(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
): boolean {
  const sheetCount = wb.getSheetCount()

  // For NAMED_RANGE source types, try named range first (consistent with fillTaggedCell)
  if (tag.sourceType === 'NAMED_RANGE' && tag.tagName) {
    for (let si = 0; si < sheetCount; si++) {
      const sheet = wb.getSheet(si)
      const nr = sheet.getCustomName(tag.tagName)
      if (nr) {
        const val = sheet.getValue(nr.getRow(), nr.getColumn())
        return val == null || val === ''
      }
    }
    // Also try workbook-level custom name
    const wbName = wb.getCustomName(tag.tagName)
    if (wbName) {
      const sheetIdx = tag.sheetIndex ?? 0
      if (sheetIdx >= 0 && sheetIdx < sheetCount) {
        const sheet = wb.getSheet(sheetIdx)
        const val = sheet.getValue(wbName.getRow(), wbName.getColumn())
        return val == null || val === ''
      }
    }
  }

  // Fall back to cell tag scan
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
          const val = sheet.getValue(r, c)
          return val == null || val === ''
        }
      }
    }
  }
  return true // if we can't find the cell, treat as empty
}

function isTableEmpty(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
): boolean {
  if (!tag.cellRange) return true
  const rangeMatch = tag.cellRange.toUpperCase().trim().match(/([A-Z]+)(\d+):([A-Z]+)(\d+)/)
  if (!rangeMatch) return true

  const startRow = parseInt(rangeMatch[2]) - 1
  const endRow = parseInt(rangeMatch[4]) - 1
  const startCol = letterToColIndex(rangeMatch[1])
  const endCol = letterToColIndex(rangeMatch[3])

  const sheet = findSheet(wb, tag.sheetName, tag.sheetIndex)
  if (!sheet) return true

  // Check if at least one row has any non-empty cell
  for (let r = startRow; r <= endRow; r++) {
    let rowHasData = false
    for (let c = startCol; c <= endCol; c++) {
      const val = sheet.getValue(r, c)
      if (val != null && val !== '') {
        rowHasData = true
        break
      }
    }
    if (rowHasData) return false
  }
  return true
}

function findSheet(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  sheetName?: string,
  sheetIndex?: number,
): import('@/types/spreadjs').GCSpreadSheet | null {
  const count = wb.getSheetCount()
  if (sheetName) {
    const target = sheetName.trim()
    // Exact match first
    for (let i = 0; i < count; i++) {
      const s = wb.getSheet(i)
      if (s.name() === target) return s
    }
    // Trimmed / case-insensitive fallback (handles encoding or whitespace diffs)
    const targetLower = target.toLowerCase()
    for (let i = 0; i < count; i++) {
      const s = wb.getSheet(i)
      if (s.name().trim().toLowerCase() === targetLower) return s
    }
  }
  const idx = sheetIndex ?? 0
  if (idx >= 0 && idx < count) return wb.getSheet(idx)
  console.warn(`[protection] findSheet 失败: name=${sheetName}, index=${sheetIndex}, sheetCount=${count}`)
  return null
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
  workbook.suspendPaint()
  try {
    for (let i = 0; i < count; i++) {
      const sheet = workbook.getSheet(i)
      // Re-lock ALL cells (including those unlocked by applyDataEntryProtection)
      const rows = sheet.getRowCount()
      const cols = sheet.getColumnCount()
      if (rows > 0 && cols > 0) {
        sheet.getRange(0, 0, rows, cols).locked(true)
      }
      sheet.options.isProtected = true
    }
  } finally {
    workbook.resumePaint()
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

defineExpose({ save, getSubmissionId, getVersionId, isSubmitted, validateRequiredFields, saving, loading })
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
