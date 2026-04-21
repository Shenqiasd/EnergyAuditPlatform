<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'
import SheetNav, { type SheetFillStatus } from './SheetNav.vue'
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
import { getEnterpriseSettingPrefill, getConfigPrefillData, type ConfigPrefillData } from '@/api/enterpriseSetting'
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

// ── Sheet navigation state ─────────────────────────────────────────────
const sheetStatuses = ref<SheetFillStatus[]>([])
const activeSheetIndex = ref(0)
const navCollapsed = ref(false)
const currentZoom = ref(100) // percentage display for zoom bar
/** Set of sheet indices where user manually zoomed — skip auto-fit */
const userZoomOverride = new Set<number>()

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

// ── Safe wrappers for optional SpreadJS Workbook APIs ───────────────────
// Some SpreadJS builds / license tiers expose `suspendEvent`, `suspendCalcService`
// and `calculate` while others do not. Wrap each call in a feature-detect so
// that the main init path never fails if an API is missing.
type WB = import('@/types/spreadjs').GCSpreadWorkbook
function suspendEventSafe(wb: WB) {
  try { (wb as unknown as { suspendEvent?: () => void }).suspendEvent?.() } catch { /* best-effort */ }
}
function resumeEventSafe(wb: WB) {
  try { (wb as unknown as { resumeEvent?: () => void }).resumeEvent?.() } catch { /* best-effort */ }
}
function suspendCalcServiceSafe(wb: WB) {
  try { (wb as unknown as { suspendCalcService?: () => void }).suspendCalcService?.() } catch { /* best-effort */ }
}
function resumeCalcServiceSafe(wb: WB) {
  try { (wb as unknown as { resumeCalcService?: () => void }).resumeCalcService?.() } catch { /* best-effort */ }
}
function calculateAllSafe(wb: WB) {
  try {
    const sheets = (window.GC?.Spread?.Sheets ?? {}) as unknown as {
      CalculationType?: { all?: unknown }
    }
    const calcType = sheets.CalculationType?.all
    const fn = (wb as unknown as { calculate?: (t?: unknown) => void }).calculate
    if (typeof fn === 'function') fn.call(wb, calcType)
  } catch { /* best-effort */ }
}

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
  window.removeEventListener('resize', onWindowResize)
  if (statusUpdateTimer) clearTimeout(statusUpdateTimer)
  if (resizeTimer) clearTimeout(resizeTimer)
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
    // Skip recalculation during fromJSON — a single recalc is triggered
    // after all Phase 2 mutations complete, avoiding repeated full-workbook
    // recomputation on templates with heavy cross-sheet formulas.
    console.time('[perf] fromJSON')
    ;(workbook.fromJSON as unknown as (data: unknown, opts?: Record<string, unknown>) => void)(
      JSON.parse(jsonStr),
      { doNotRecalculateAfterLoad: true },
    )
    console.timeEnd('[perf] fromJSON')

    // ── Phase 2: fetch tags + prefill data in parallel (one listTags call) ─
    // Wrapped in its own try-catch so that a failure in supplementary features
    // (prefill / dropdowns / protection) does NOT prevent the core spreadsheet
    // from rendering — matching the original best-effort error handling.
    //
    // NOTE: `fromJSON` was called with `doNotRecalculateAfterLoad: true`, so
    // every path out of Phase 2 (happy / skip / throw) MUST eventually trigger
    // a single `calculateAllSafe(workbook)` — otherwise formula cells render
    // stale or empty. We use an outer try/finally for that guarantee.
    try {
      if (publishedVersion.id) {
        try {
          console.time('[perf] phase2-fetch')
          const [tags, prefillData, configPrefillData] = await Promise.all([
            listTags(publishedVersion.id).catch(e => {
              console.warn('[phase2] listTags failed:', e)
              return [] as TplTagMapping[]
            }),
            !currentSubmission
              ? getEnterpriseSettingPrefill().catch(() => null)
              : Promise.resolve(null),
            // Always fetch config data — dropdowns need it even for existing submissions
            getConfigPrefillData().catch(() => null),
          ])
          console.timeEnd('[perf] phase2-fetch')

          // Master suspend envelope for all Phase 2 mutations — prevents the
          // ~1000 setValue/setStyle/setDataValidator calls below from each
          // triggering a full-workbook repaint / event dispatch / recalc.
          // Nested suspend calls inside individual apply* functions are still
          // safe (SpreadJS reference-counts suspend depth).
          console.time('[perf] phase2-mutate')
          workbook.suspendPaint()
          suspendEventSafe(workbook)
          suspendCalcServiceSafe(workbook)
          try {
            // Build cell-tag index ONCE for O(1) lookups (replaces O(tags ×
            // rows × cols) scans). Must be called BEFORE applyPrefill /
            // applyConfigPrefill / applyDataEntryProtection since they all use
            // fillTaggedCell / unlockScalarCell which rely on the index.
            console.time('[perf] buildCellTagIndex')
            buildCellTagIndex(workbook)
            console.timeEnd('[perf] buildCellTagIndex')

            // Pre-fill enterprise settings (uses pre-fetched tags + prefillData)
            if (!currentSubmission && prefillData) {
              console.time('[perf] applyPrefill')
              applyPrefill(workbook, tags, prefillData)
              console.timeEnd('[perf] applyPrefill')
            }

            // Config-driven prefill: always write values + dropdowns + hide empty rows
            if (configPrefillData) {
              console.time('[perf] applyConfigPrefill')
              applyConfigPrefill(workbook, tags, configPrefillData)
              console.timeEnd('[perf] applyConfigPrefill')
            }

            // Inject dictionary-based dropdown validators (uses pre-fetched tags)
            console.time('[perf] applyDictValidators')
            await applyDictValidators(workbook, tags)
            console.timeEnd('[perf] applyDictValidators')

            // Bind ValidationError event on every sheet
            console.time('[perf] bindValidationErrorDialogs')
            bindValidationErrorDialogs(workbook)
            console.timeEnd('[perf] bindValidationErrorDialogs')

            // Apply cell protection + required field markers (uses pre-fetched tags)
            if (publishedVersion.protectionEnabled !== 0) {
              console.time('[perf] applyDataEntryProtection')
              applyDataEntryProtection(workbook, tags)
              console.timeEnd('[perf] applyDataEntryProtection')
            }
          } finally {
            resumeCalcServiceSafe(workbook)
            resumeEventSafe(workbook)
            workbook.resumePaint()
            console.timeEnd('[perf] phase2-mutate')
          }
        } catch (e) {
          console.warn('[phase2] failed to load tags / apply features:', e)
          // Still bind validation error dialogs as fallback
          bindValidationErrorDialogs(workbook)
        }
      } else {
        bindValidationErrorDialogs(workbook)
      }
    } finally {
      // Trigger a single recalculation now that all mutations are done (or
      // skipped / failed) — this MUST run to compensate for the
      // doNotRecalculateAfterLoad flag passed to fromJSON above. Otherwise
      // formula cells would render stale/unevaluated.
      console.time('[perf] calculate')
      calculateAllSafe(workbook)
      console.timeEnd('[perf] calculate')
    }

    // Force readonly when:
    //  1. Parent explicitly says readonly (e.g. lock held by another user), OR
    //  2. Submission has been submitted (status=1) or approved (status=2).
    // A rejected submission (status=3) reverts to editable so the user can fix & re-submit.
    const submissionStatus = currentSubmission?.status ?? 0
    const isSubmittedOrApproved = submissionStatus === 1 || submissionStatus === 2
    const forceReadonly = props.readonly || isSubmittedOrApproved
    if (forceReadonly) {
      applyReadonlyProtection()
      releaseLockIfOwned()
    } else {
      startHeartbeat()
    }

    // ── Sheet navigation setup ──────────────────────────────────────────
    bindSheetNavEvents(workbook)
    activeSheetIndex.value = workbook.getActiveSheetIndex()
    computeAllSheetStatuses()
    // Auto-fit the initial sheet after a tick to allow layout to settle
    await nextTick()
    autoFitCurrentSheet()
    window.addEventListener('resize', onWindowResize)
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
 * Config-driven prefill: for each CONFIG_PREFILL tag, fill rows from enterprise
 * config data (bs_energy / bs_product) into the designated SpreadJS region.
 */
function applyConfigPrefill(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tags: TplTagMapping[],
  configData: ConfigPrefillData,
) {
  try {
    const prefillTags = tags.filter(t => t.mappingType === 'CONFIG_PREFILL')
    console.log(`[config-prefill] found ${prefillTags.length} CONFIG_PREFILL tags`)
    if (!prefillTags.length) return

    wb.suspendPaint()
    try {
      for (const tag of prefillTags) {
        try {
          applyOneConfigPrefill(wb, tag, configData)
        } catch (e) {
          console.warn(`[config-prefill] failed for tag "${tag.tagName}":`, e)
        }
      }
    } finally {
      wb.resumePaint()
    }
  } catch (e) {
    console.warn('[config-prefill] failed:', e)
  }
}

/** Column definition type for CONFIG_PREFILL mappings */
interface ConfigPrefillColDef {
  col: string | number
  field: string
  format?: string
  dropdown?: boolean
  prefill?: boolean
  /** When true, the cell is locked after writing — user cannot edit (auto-generated column). */
  locked?: boolean
  /** When set, this column's value is auto-derived from the record matching the master column.
   *  The column is locked and auto-updates when the master column value changes.
   *  Example: { "masterCol": "A", "lookupField": "name" } */
  linkedTo?: { masterCol: string; lookupField: string }
  extraSources?: Array<{ table: string; field: string; filter?: Record<string, unknown> }>
}

/**
 * Build a NEW Style object with the desired overrides applied on top of the
 * existing cell style — without mutating the original. `sheet.getStyle()` can
 * return a shared/default Style instance referenced by many cells; mutating
 * it in place leaks style changes onto unrelated cells and (in some SpreadJS
 * builds) triggers expensive internal invalidation that can manifest as a
 * long synchronous freeze during prefill.
 */
function buildLockedStyle(
  sheet: import('@/types/spreadjs').GCSpreadSheet,
  row: number,
  col: number,
  backColor = '#F5F5F5',
): unknown {
  try {
    const StyleCtor = window.GC?.Spread?.Sheets?.Style as undefined | (new () => { locked?: boolean; backColor?: string; clone?: () => unknown })
    if (!StyleCtor) return null
    const existing = sheet.getStyle(row, col) as null | { clone?: () => { locked?: boolean; backColor?: string } }
    const style = (existing && typeof existing.clone === 'function')
      ? existing.clone() as { locked?: boolean; backColor?: string }
      : new StyleCtor()
    style.locked = true
    style.backColor = backColor
    return style
  } catch {
    return null
  }
}

function applyOneConfigPrefill(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
  configData: ConfigPrefillData,
) {
  const tagLabel = tag.tagName ?? '<unnamed>'
  console.log(`[config-prefill] START "${tagLabel}"`)
  console.time(`[config-prefill] "${tagLabel}" total`)
  if (!tag.targetTable || !tag.cellRange || !tag.columnMappings) {
    console.timeEnd(`[config-prefill] "${tagLabel}" total`)
    return
  }

  // 1. Parse columnMappings JSON first so we can honour the optional `source`
  //    override before reading configData.
  // mode: "prefill" (default) = write values + dropdowns + hide empty rows
  //       "dropdown_only" = only inject dropdowns, no value writing, no row hiding
  // source: optional config-data key to read from when it differs from
  //         targetTable (e.g. target_table='de_product_unit_consumption' is a
  //         submission storage table that isn't returned by the config-prefill
  //         API — the actual data source is bs_product).
  let config: {
    filter?: Record<string, unknown>
    mode?: 'prefill' | 'dropdown_only'
    source?: string
    columns: ConfigPrefillColDef[]
  }
  try {
    config = JSON.parse(tag.columnMappings)
  } catch {
    console.warn(`[config-prefill] invalid columnMappings JSON for tag "${tagLabel}"`)
    console.timeEnd(`[config-prefill] "${tagLabel}" total`)
    return
  }
  const columns = config.columns ?? []
  if (!columns.length) {
    console.timeEnd(`[config-prefill] "${tagLabel}" total`)
    return
  }
  const isDropdownOnly = config.mode === 'dropdown_only'

  // 2. Resolve data source records. Prefer explicit `source` when provided,
  //    otherwise fall back to targetTable.
  const sourceKey = config.source ?? tag.targetTable
  const allRecords = configData[sourceKey] ?? []

  // 3. Apply filter (e.g. { "isActive": 1 })
  let records = allRecords
  if (config.filter) {
    const filterEntries = Object.entries(config.filter)
    records = records.filter(r =>
      filterEntries.every(([k, v]) => r[k] === v),
    )
  }
  // For prefill mode, we need records to fill values; for dropdown_only, proceed even if empty
  if (!records.length && !isDropdownOnly) {
    console.timeEnd(`[config-prefill] "${tagLabel}" total`)
    return
  }

  // 4. Parse cellRange → startRow, startCol, maxRows
  const rangeMatch = tag.cellRange.toUpperCase().trim().match(/([A-Z]+)(\d+):([A-Z]+)(\d+)/)
  if (!rangeMatch) {
    console.warn(`[config-prefill] invalid cellRange "${tag.cellRange}" for tag "${tagLabel}"`)
    console.timeEnd(`[config-prefill] "${tagLabel}" total`)
    return
  }
  const startRow = parseInt(rangeMatch[2]) - 1 // 0-based
  const endRow = parseInt(rangeMatch[4]) - 1
  const startCol = letterToColIndex(rangeMatch[1])
  const maxRows = endRow - startRow + 1

  // 5. Resolve sheet
  const sheet = findSheet(wb, tag.sheetName, tag.sheetIndex)
  if (!sheet) {
    console.warn(`[config-prefill] sheet not found for tag "${tagLabel}"`)
    console.timeEnd(`[config-prefill] "${tagLabel}" total`)
    return
  }

  // 6. Resolve column indices helper
  const resolveColIndex = (colDef: { col: string | number }) => {
    if (typeof colDef.col === 'string' && /^[A-Za-z]+$/.test(colDef.col)) {
      return letterToColIndex(colDef.col.toUpperCase())
    }
    return startCol + Number(colDef.col)
  }

  // Clear old data before writing (skip in dropdown_only mode)
  if (!isDropdownOnly) {
    for (let i = 0; i < maxRows; i++) {
      for (const colDef of columns) {
        if (colDef.prefill === false) continue // don't clear dropdown-only columns
        const colIndex = resolveColIndex(colDef)
        sheet.setValue(startRow + i, colIndex, null)
      }
    }
  }

  // 7. Determine rows to process
  // In dropdown_only mode, apply dropdowns to ALL rows in the range
  // In prefill mode, only fill rows matching records count
  const rowsToFill = isDropdownOnly ? maxRows : Math.min(records.length, maxRows)
  if (records.length > maxRows) {
    console.warn(
      `[config-prefill] "${tag.tagName}": ${records.length} records exceed ${maxRows} available rows, truncated`,
    )
  }

  // 8. Build per-column deduplicated dropdown value lists from ALL filtered records
  const colDropdownValues = new Map<number, string[]>()
  for (const colDef of columns) {
    // Skip dropdown for columns explicitly marked dropdown: false OR linkedTo columns
    if (colDef.dropdown === false || colDef.linkedTo || colDef.locked) continue
    const colIndex = resolveColIndex(colDef)
    const values: string[] = []
    const seen = new Set<string>()
    // Collect values from the primary data source
    for (const rec of records) {
      let val: string
      if (colDef.format) {
        val = colDef.format.replace(/\{(\w+)\}/g, (_, key: string) => String(rec[key] ?? ''))
      } else {
        val = rec[colDef.field] != null ? String(rec[colDef.field]) : ''
      }
      if (val !== '' && !seen.has(val)) { seen.add(val); values.push(val) }
    }
    // Merge values from extraSources (e.g. C column needs both energy + product names)
    if (colDef.extraSources?.length) {
      for (const src of colDef.extraSources) {
        let extraRecords = configData[src.table] ?? []
        if (src.filter) {
          const fe = Object.entries(src.filter)
          extraRecords = extraRecords.filter(r => fe.every(([k, v]) => r[k] === v))
        }
        for (const rec of extraRecords) {
          const val = rec[src.field] != null ? String(rec[src.field]) : ''
          if (val !== '' && !seen.has(val)) { seen.add(val); values.push(val) }
        }
      }
    }
    if (values.length > 0) {
      colDropdownValues.set(colIndex, values)
    }
  }

  // 9. Fill rows with values AND set dropdown validators
  const DataValidation = window.GC?.Spread?.Sheets?.DataValidation
  for (let i = 0; i < rowsToFill; i++) {
    const record = i < records.length ? records[i] : null
    for (const colDef of columns) {
      const colIndex = resolveColIndex(colDef)

      // Write cell value (skip in dropdown_only mode or if prefill: false or no record)
      if (!isDropdownOnly && colDef.prefill !== false && record) {
        let value: unknown
        if (colDef.format) {
          value = colDef.format.replace(/\{(\w+)\}/g, (_, key: string) => String(record[key] ?? ''))
        } else {
          value = record[colDef.field]
        }
        if (value != null && value !== '') {
          sheet.setValue(startRow + i, colIndex, value)
        }
      }

      // Lock linkedTo columns — value is derived, user should not edit directly.
      // Use buildLockedStyle which CLONES the existing style before mutating,
      // to avoid polluting shared/default Style objects referenced by other cells.
      if (colDef.linkedTo) {
        const style = buildLockedStyle(sheet, startRow + i, colIndex)
        if (style) {
          try { sheet.setStyle(startRow + i, colIndex, style) } catch { /* ignore */ }
        }
      }

      // Lock columns with locked: true — auto-generated, user should not edit
      if (colDef.locked && !colDef.linkedTo) {
        const style = buildLockedStyle(sheet, startRow + i, colIndex)
        if (style) {
          try { sheet.setStyle(startRow + i, colIndex, style) } catch { /* ignore */ }
        }
      }

      // Set dropdown validator (skip if dropdown: false or linkedTo or locked)
      if (DataValidation && colDef.dropdown !== false && !colDef.linkedTo && !colDef.locked) {
        const dropdownVals = colDropdownValues.get(colIndex)
        if (dropdownVals?.length) {
          try {
            const listStr = dropdownVals.map(v => v.replace(/,/g, '\uff0c')).join(',')
            const dv = DataValidation.createListValidator(listStr)
            dv.inCellDropdown(true)
            dv.showInputMessage(true)
            dv.inputTitle('请选择')
            dv.inputMessage('点击下拉箭头选择')
            sheet.setDataValidator(startRow + i, colIndex, dv)
          } catch (e) {
            console.warn(`[config-prefill] failed to set dropdown at row ${startRow + i}, col ${colIndex}:`, e)
          }
        }
      }
    }
  }

  // 10. Hide empty rows beyond the filled data (skip in dropdown_only mode)
  if (!isDropdownOnly) {
    for (let i = 0; i < rowsToFill; i++) {
      sheet.setRowVisible(startRow + i, true)
    }
    for (let i = rowsToFill; i < maxRows; i++) {
      sheet.setRowVisible(startRow + i, false)
    }
  }

  // 11. Identify master columns that have linkedTo dependents (for event binding)
  const linkedCols = columns.filter(c => c.linkedTo)
  // Identify master columns with dropdowns (for duplicate prevention)
  const masterColDefs = columns.filter(c => !c.linkedTo && !c.locked && c.dropdown !== false && colDropdownValues.has(resolveColIndex(c)))

  // Helper: rebuild per-row dropdowns excluding values already used in other rows
  const refreshDropdownsExcludingDuplicates = () => {
    if (!DataValidation || !masterColDefs.length) return
    for (const colDef of masterColDefs) {
      const colIndex = resolveColIndex(colDef)
      const allVals = colDropdownValues.get(colIndex)
      if (!allVals?.length) continue

      // Collect values currently used in all rows for this column
      const usedValues = new Set<string>()
      for (let r = 0; r < rowsToFill; r++) {
        const cellVal = sheet.getValue(startRow + r, colIndex)
        if (cellVal != null && String(cellVal) !== '') {
          usedValues.add(String(cellVal))
        }
      }

      // For each row, set dropdown = allVals minus values used in OTHER rows
      for (let r = 0; r < rowsToFill; r++) {
        const currentVal = sheet.getValue(startRow + r, colIndex)
        const currentStr = currentVal != null ? String(currentVal) : ''
        const availableVals = allVals.filter(v => v === currentStr || !usedValues.has(v))
        if (!availableVals.length) continue
        try {
          const listStr = availableVals.map(v => v.replace(/,/g, '\uff0c')).join(',')
          const dv = DataValidation.createListValidator(listStr)
          dv.inCellDropdown(true)
          dv.showInputMessage(true)
          dv.inputTitle('请选择')
          dv.inputMessage('点击下拉箭头选择')
          sheet.setDataValidator(startRow + r, colIndex, dv)
        } catch { /* ignore */ }
      }
    }
  }

  // Apply initial duplicate exclusion on prefill mode (each row pre-filled with unique record)
  if (!isDropdownOnly && masterColDefs.length > 0) {
    refreshDropdownsExcludingDuplicates()
  }

  // Bind CellChanged for linkedTo auto-fill AND duplicate prevention
  if ((linkedCols.length > 0 || masterColDefs.length > 0) && !isDropdownOnly) {
    const Events = window.GC?.Spread?.Sheets?.Events
    if (Events?.CellChanged) {
      sheet.bind(Events.CellChanged, (_sender: unknown, args: { row: number; col: number; newValue: unknown }) => {
        const { row, col: changedCol, newValue } = args
        // Only process changes within our data range
        if (row < startRow || row >= startRow + rowsToFill) return

        // Auto-fill linkedTo columns when master column changes
        for (const linked of linkedCols) {
          const masterColIndex = resolveColIndex({ col: linked.linkedTo!.masterCol })
          if (changedCol !== masterColIndex) continue

          // Look up the record matching the new master value
          const lookupField = linked.linkedTo!.lookupField
          const newValStr = newValue != null ? String(newValue) : ''
          const matchedRecord = records.find(r => String(r[lookupField] ?? '') === newValStr)

          // Auto-fill the linked column with the matched record's field value
          const linkedColIndex = resolveColIndex(linked)
          if (matchedRecord) {
            let fillVal: unknown
            if (linked.format) {
              fillVal = linked.format.replace(/\{(\w+)\}/g, (_, key: string) => String(matchedRecord[key] ?? ''))
            } else {
              fillVal = matchedRecord[linked.field]
            }
            sheet.setValue(row, linkedColIndex, fillVal != null ? fillVal : '')
          } else {
            sheet.setValue(row, linkedColIndex, '')
          }
        }

        // Refresh all master column dropdowns to exclude newly selected value from other rows
        const isMasterCol = masterColDefs.some(c => resolveColIndex(c) === changedCol)
        if (isMasterCol) {
          refreshDropdownsExcludingDuplicates()
        }
      })
      const features: string[] = []
      if (linkedCols.length > 0) features.push(`${linkedCols.length} linkedTo`)
      if (masterColDefs.length > 0) features.push('duplicate prevention')
      console.log(`[config-prefill] "${tag.tagName}": bound CellChanged (${features.join(' + ')})`)
    }
  }

  console.log(
    `[config-prefill] "${tagLabel}" [${isDropdownOnly ? 'dropdown_only' : 'prefill'}]: ` +
    `${rowsToFill} rows processed` + (isDropdownOnly ? '' : `, ${maxRows - rowsToFill} empty rows hidden`) +
    (linkedCols.length > 0 ? `, ${linkedCols.length} linked column(s)` : ''),
  )
  console.timeEnd(`[config-prefill] "${tagLabel}" total`)
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

  // Use pre-built cell-tag index for O(1) lookup (replaces O(rows × cols) scan)
  if (tag.tagName) {
    const found = lookupCellTag(tag.tagName, tag.sheetIndex)
    if (found) {
      const sheet = wb.getSheet(found.sheetIndex)
      sheet.setValue(found.row, found.col, value)
      return true
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

      // Step 1: Lock all cells by default on every sheet. This forces `locked=true`
      // on every cell including those whose template-level style has `locked=false`
      // (e.g. cells authored in SpreadJS Designer / Excel with explicit unlock).
      // Without this, Step 4's `isProtected=true` would leave those cells editable.
      //
      // NOTE: on templates with many large sheets this loop creates a Style object
      // per cell and is itself a few seconds of work. The bigger perf win for
      // template-open time comes from (a) avoiding the O(tags × rows × cols)
      // getTag scans that used to live in unlockScalarCell/fillTaggedCell
      // (addressed by the cell-tag index in a separate PR), and (b) the suspend
      // envelope / single-recalc changes in this PR. We deliberately keep the
      // original "brute-force lock-all" semantics here because `setDefaultStyle`
      // alone does NOT override per-cell explicit `locked=false`.
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
 * Uses the pre-built cellTagIndex for O(1) lookups instead of scanning all cells.
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

  // Use pre-built cell-tag index for O(1) lookup (replaces O(rows × cols) scan)
  if (tag.tagName) {
    const found = lookupCellTag(tag.tagName, tag.sheetIndex)
    if (found) {
      const sheet = wb.getSheet(found.sheetIndex)
      const cell = sheet.getCell(found.row, found.col)
      cell.locked(false)
      if (tag.required === 1) markCellRequired(cell, hint)
      return
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

/**
 * Pre-built cell-tag index: sheetIndex → Map<tagName, {row, col}>.
 * Built ONCE per workbook load to avoid O(tags × rows × cols) repeated scans.
 * The expensive getTag() calls happen exactly once per cell across all sheets.
 */
let cellTagIndex: Map<number, Map<string, { row: number; col: number }>> | null = null

function buildCellTagIndex(wb: import('@/types/spreadjs').GCSpreadWorkbook): void {
  cellTagIndex = new Map()
  const sheetCount = wb.getSheetCount()
  for (let si = 0; si < sheetCount; si++) {
    const sheet = wb.getSheet(si)
    const rowCount = sheet.getRowCount()
    const colCount = sheet.getColumnCount()
    const sheetMap = new Map<string, { row: number; col: number }>()
    for (let r = 0; r < rowCount; r++) {
      for (let c = 0; c < colCount; c++) {
        const tag = sheet.getTag(r, c)
        if (tag && typeof tag === 'string' && !sheetMap.has(tag)) {
          sheetMap.set(tag, { row: r, col: c })
        }
      }
    }
    if (sheetMap.size > 0) {
      cellTagIndex.set(si, sheetMap)
    }
  }
  let totalIndexed = 0
  cellTagIndex.forEach(m => { totalIndexed += m.size })
  console.log(`[perf] cell-tag index built: ${totalIndexed} tags across ${cellTagIndex.size} sheets`)
}

/**
 * Look up a cell-tag from the pre-built index. O(1) per lookup.
 * Returns {sheetIndex, row, col} or null if not found.
 */
function lookupCellTag(
  tagName: string,
  sheetIndex?: number | null,
): { sheetIndex: number; row: number; col: number } | null {
  if (!cellTagIndex) return null
  if (sheetIndex != null) {
    const sheetMap = cellTagIndex.get(sheetIndex)
    if (sheetMap) {
      const pos = sheetMap.get(tagName)
      if (pos) return { sheetIndex, ...pos }
    }
    return null
  }
  // Search all sheets
  for (const [si, sheetMap] of cellTagIndex) {
    const pos = sheetMap.get(tagName)
    if (pos) return { sheetIndex: si, ...pos }
  }
  return null
}

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

  // Use pre-built cell-tag index for O(1) lookup (replaces O(rows × cols) scan)
  if (tag.tagName) {
    const found = lookupCellTag(tag.tagName, tag.sheetIndex)
    if (found) {
      const sheet = wb.getSheet(found.sheetIndex)
      const val = sheet.getValue(found.row, found.col)
      return val == null || val === ''
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
      templateVersionId: publishedVersion.id,
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

// ── Sheet navigation helpers ───────────────────────────────────────────

/**
 * Compute fill status for every sheet based on cached tag mappings.
 */
function computeAllSheetStatuses() {
  if (!workbook) return
  const count = workbook.getSheetCount()
  const result: SheetFillStatus[] = []
  for (let si = 0; si < count; si++) {
    // Skip hidden sheets — they should not appear in navigation
    try {
      if (!workbook.getSheet(si).visible()) continue
    } catch { /* visible() not available, include sheet */ }
    result.push(computeOneSheetStatus(si))
  }
  sheetStatuses.value = result
}

function computeOneSheetStatus(sheetIndex: number): SheetFillStatus {
  const wb = workbook!
  const sheet = wb.getSheet(sheetIndex)
  const name = sheet.name()

  // Filter tags belonging to this sheet
  const sheetTags = cachedTags.filter(t => {
    if (t.sheetName) {
      return t.sheetName.trim().toLowerCase() === name.trim().toLowerCase()
    }
    return (t.sheetIndex ?? 0) === sheetIndex
  })
  const requiredTags = sheetTags.filter(t => t.required === 1)
  const totalRequired = requiredTags.length

  if (totalRequired === 0) {
    return { sheetIndex, sheetName: name, totalRequired: 0, filledRequired: 0, status: 'no_required' }
  }

  let filledRequired = 0
  for (const tag of requiredTags) {
    const mappingType = tag.mappingType ?? 'SCALAR'
    if (mappingType === 'SCALAR') {
      if (!isScalarCellEmpty(wb, tag)) filledRequired++
    } else if (mappingType === 'TABLE' || mappingType === 'EQUIPMENT_BENCHMARK') {
      if (!isTableEmpty(wb, tag)) filledRequired++
    }
  }

  let status: SheetFillStatus['status']
  if (filledRequired === totalRequired) {
    status = 'completed'
  } else if (filledRequired > 0) {
    status = 'in_progress'
  } else {
    status = 'not_started'
  }
  return { sheetIndex, sheetName: name, totalRequired, filledRequired, status }
}

/**
 * Debounced status update — only recalculates the active sheet.
 */
let statusUpdateTimer: ReturnType<typeof setTimeout> | null = null
function debouncedUpdateFillStatus() {
  if (statusUpdateTimer) clearTimeout(statusUpdateTimer)
  statusUpdateTimer = setTimeout(() => {
    if (!workbook) return
    const idx = activeSheetIndex.value
    const updated = computeOneSheetStatus(idx)
    const arr = [...sheetStatuses.value]
    // Find by sheetIndex (not array position) since hidden sheets are filtered out
    const pos = arr.findIndex(s => s.sheetIndex === idx)
    if (pos >= 0) {
      arr[pos] = updated
      sheetStatuses.value = arr
    }
  }, 300)
}

/**
 * Handle sheet selection from the nav panel.
 */
function onSheetSelect(index: number) {
  if (!workbook) return
  workbook.setActiveSheetIndex(index)
  activeSheetIndex.value = index
  autoFitCurrentSheet()
  syncZoomDisplay()
}

/**
 * Bind SpreadJS events for sheet navigation and fill status tracking.
 */
function bindSheetNavEvents(wb: import('@/types/spreadjs').GCSpreadWorkbook) {
  const Events = window.GC?.Spread?.Sheets?.Events
  if (!Events) return

  // Hide native tab bar
  if (wb.options && 'tabStripVisible' in wb.options) {
    wb.options.tabStripVisible = false
  }

  // Track active sheet changes (from any source)
  if (Events.ActiveSheetChanged) {
    wb.bind(Events.ActiveSheetChanged, (_e: unknown, args: { newIndex?: number }) => {
      if (args.newIndex != null) {
        activeSheetIndex.value = args.newIndex
      }
    })
  }

  // Track cell changes for fill status updates
  if (Events.CellChanged) {
    const count = wb.getSheetCount()
    for (let i = 0; i < count; i++) {
      wb.getSheet(i).bind(Events.CellChanged, () => {
        debouncedUpdateFillStatus()
      })
    }
  }

  // Track user zoom overrides
  if (Events.ViewZoomed) {
    const count = wb.getSheetCount()
    for (let i = 0; i < count; i++) {
      const si = i
      wb.getSheet(i).bind(Events.ViewZoomed, () => {
        userZoomOverride.add(si)
        syncZoomDisplay()
      })
    }
  }
}

// ── Auto-fit zoom logic ────────────────────────────────────────────────

/**
 * Auto-fit the currently active sheet's zoom to show all content columns
 * within the visible container width.
 */
function autoFitCurrentSheet() {
  if (!workbook || !spreadRef.value) return
  const idx = activeSheetIndex.value
  if (userZoomOverride.has(idx)) return // user manually zoomed, respect it

  const sheet = workbook.getSheet(idx)
  if (!sheet) return

  // Determine the last used column from tag mappings + content scan
  let lastCol = -1

  // 1. From tag mappings (fast, O(tags))
  for (const tag of cachedTags) {
    // Only consider tags on this sheet
    const tagSheet = tag.sheetName?.trim().toLowerCase()
    const sheetName = sheet.name().trim().toLowerCase()
    const tagIdx = tag.sheetIndex ?? 0
    if (tagSheet ? tagSheet !== sheetName : tagIdx !== idx) continue

    if (tag.cellRange) {
      const match = tag.cellRange.toUpperCase().trim().match(/([A-Z]+)\d+:([A-Z]+)\d+/)
      if (match) {
        lastCol = Math.max(lastCol, letterToColIndex(match[2]))
      }
    }
  }

  // 2. If no tag mappings, do a limited scan (first 50 rows)
  if (lastCol < 0) {
    const scanRows = Math.min(sheet.getRowCount(), 50)
    const scanCols = sheet.getColumnCount()
    for (let r = 0; r < scanRows; r++) {
      for (let c = scanCols - 1; c >= 0; c--) {
        const val = sheet.getValue(r, c)
        if (val != null && val !== '') {
          lastCol = Math.max(lastCol, c)
          break
        }
      }
    }
  }

  if (lastCol < 0) return // empty sheet

  // Calculate total content width at 100% zoom
  let contentWidth = 0
  for (let c = 0; c <= lastCol; c++) {
    try {
      contentWidth += sheet.getColumnWidth(c)
    } catch {
      contentWidth += 64 // default fallback
    }
  }

  // Available width = container width minus nav panel and row header (~40px)
  const navWidth = navCollapsed.value ? 44 : 200
  const rowHeaderWidth = 40
  const containerWidth = spreadRef.value.parentElement?.clientWidth ?? spreadRef.value.clientWidth
  const availableWidth = containerWidth - navWidth - rowHeaderWidth

  if (contentWidth <= availableWidth || availableWidth <= 0) {
    // Content fits, reset to 100% if it was previously shrunk
    sheet.zoom(1)
    return
  }

  let zoomFactor = availableWidth / contentWidth
  zoomFactor = Math.max(zoomFactor, 0.5)  // min 50%
  zoomFactor = Math.min(zoomFactor, 1.0)  // max 100%

  workbook.suspendPaint()
  try {
    sheet.zoom(zoomFactor)
  } finally {
    workbook.resumePaint()
  }
}

let resizeTimer: ReturnType<typeof setTimeout> | null = null
function onWindowResize() {
  if (resizeTimer) clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => autoFitCurrentSheet(), 300)
}

/**
 * Sync the currentZoom display from the active sheet's actual zoom level.
 */
function syncZoomDisplay() {
  if (!workbook) return
  try {
    const sheet = workbook.getSheet(activeSheetIndex.value)
    if (sheet) {
      // SpreadJS zoom() with no args returns current zoom factor (0.0-x.x)
      const factor = (sheet as any).zoom()
      if (typeof factor === 'number' && factor > 0) {
        currentZoom.value = Math.round(factor * 100)
      }
    }
  } catch { /* ignore */ }
}

/**
 * Manual zoom: set zoom to a specific percentage.
 */
function setZoom(percent: number) {
  if (!workbook) return
  const clamped = Math.max(25, Math.min(400, percent))
  const factor = clamped / 100
  const sheet = workbook.getSheet(activeSheetIndex.value)
  if (!sheet) return
  userZoomOverride.add(activeSheetIndex.value)
  workbook.suspendPaint()
  try {
    sheet.zoom(factor)
  } finally {
    workbook.resumePaint()
  }
  currentZoom.value = clamped
}

function zoomIn() {
  setZoom(currentZoom.value + 10)
}

function zoomOut() {
  setZoom(currentZoom.value - 10)
}

function resetZoom() {
  userZoomOverride.delete(activeSheetIndex.value)
  autoFitCurrentSheet()
  syncZoomDisplay()
}

// ── Enhanced validation (per-sheet grouped) ────────────────────────────

export interface SheetValidationError {
  sheetIndex: number
  sheetName: string
  errors: string[]
}

function validateRequiredFieldsBySheet(): SheetValidationError[] {
  if (!workbook || !publishedVersion) return []
  if (publishedVersion.protectionEnabled === 0) return []

  const result: SheetValidationError[] = []
  const count = workbook.getSheetCount()

  for (let si = 0; si < count; si++) {
    const sheet = workbook.getSheet(si)
    const name = sheet.name()
    const errors: string[] = []

    const sheetTags = cachedTags.filter(t => {
      if (t.sheetName) {
        return t.sheetName.trim().toLowerCase() === name.trim().toLowerCase()
      }
      return (t.sheetIndex ?? 0) === si
    })

    for (const tag of sheetTags) {
      if (tag.required !== 1) continue
      const mappingType = tag.mappingType ?? 'SCALAR'
      if (mappingType === 'SCALAR') {
        if (isScalarCellEmpty(workbook, tag)) {
          errors.push(`"${tag.fieldName ?? tag.tagName}" 为必填字段，请填写`)
        }
      } else if (mappingType === 'TABLE' || mappingType === 'EQUIPMENT_BENCHMARK') {
        if (isTableEmpty(workbook, tag)) {
          errors.push(`"${tag.tagName ?? tag.targetTable}" 至少需要填写1行数据`)
        }
      }
    }

    if (errors.length > 0) {
      result.push({ sheetIndex: si, sheetName: name, errors })
    }
  }
  return result
}

defineExpose({
  save,
  getSubmissionId,
  getVersionId,
  isSubmitted,
  validateRequiredFields,
  validateRequiredFieldsBySheet,
  navigateToSheet: onSheetSelect,
  saving,
  loading,
  sheetStatuses,
})
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
    <div v-show="!errorMsg" class="spreadsheet-body">
      <SheetNav
        v-if="sheetStatuses.length > 1"
        :sheets="sheetStatuses"
        :activeIndex="activeSheetIndex"
        :collapsed="navCollapsed"
        @select="onSheetSelect"
        @update:collapsed="navCollapsed = $event"
      />
      <div class="spreadjs-column">
        <div ref="spreadRef" class="spreadjs-host"></div>
        <div class="zoom-bar">
          <button class="zoom-btn" title="缩小" @click="zoomOut">−</button>
          <span class="zoom-value" :title="'点击重置为自适应'" @click="resetZoom">
            {{ currentZoom }}%
          </span>
          <button class="zoom-btn" title="放大" @click="zoomIn">+</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.spreadsheet-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.spreadsheet-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.spreadjs-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.spreadjs-host {
  flex: 1;
  min-height: 500px;
  min-width: 0;
  border: 1px solid #e4e7ed;
  border-radius: 0 4px 4px 0;
}

.zoom-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  padding: 4px 12px;
  background: #fafbfc;
  border: 1px solid #e4e7ed;
  border-top: none;
  border-radius: 0 0 4px 4px;
}

.zoom-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: color 0.2s, border-color 0.2s;

  &:hover {
    color: #409eff;
    border-color: #409eff;
  }

  &:active {
    background: #ecf5ff;
  }
}

.zoom-value {
  min-width: 48px;
  text-align: center;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  user-select: none;

  &:hover {
    color: #409eff;
  }
}
</style>
