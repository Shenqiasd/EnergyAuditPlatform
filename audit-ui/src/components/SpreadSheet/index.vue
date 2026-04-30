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

// ── Add Row Feature: runtime state ──────────────────────────────────────
/** Runtime-adjusted cellRanges for TABLE/EQUIPMENT_BENCHMARK tags.
 *  Key: tag mapping ID → 0-based {startRow, endRow, startCol, endCol}. */
const dynamicRanges = new Map<number, { startRow: number; endRow: number; startCol: number; endCol: number }>()

/** Original cellRange per TABLE tag (as loaded from template/DB).
 *  Used to determine which rows are "original" vs manually added. Key: tag ID. */
const originalRanges = new Map<number, { startRow: number; endRow: number; startCol: number; endCol: number }>()

/** Set of absolute row indices manually added by the user. Key: tag ID. */
const manuallyAddedRows = new Map<number, Set<number>>()

/** Protected CONFIG_PREFILL row ranges per sheet index.
 *  User cannot insert into or delete these rows. */
const protectedPrefillRanges = new Map<number, Array<{ startRow: number; endRow: number }>>()

/** Maximum rows allowed per TABLE range */
const MAX_TABLE_ROWS = 1000

/** Context menu state for add/delete row */
const ctxMenuVisible = ref(false)
const ctxMenuX = ref(0)
const ctxMenuY = ref(0)
interface CtxMenuItem { label: string; action: string; disabled?: boolean; danger?: boolean }
const ctxMenuItems = ref<CtxMenuItem[]>([])
let ctxMenuRow = -1
let ctxMenuSheetIndex = -1
let ctxMenuTag: TplTagMapping | null = null

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

// ── Persistent trace helper for diagnosing main-thread freezes ─────────
// When Phase 2 mutation hangs the browser, normal console logs are lost
// on reload. `cpTrace` synchronously appends to localStorage so the last
// checkpoint before the freeze is still visible after a hard refresh.
// A traceId is reset at the start of each template load so stale traces
// from previous sessions don't confuse diagnosis.
const CP_TRACE_KEY = '__cpLog'
let cpTraceT0 = 0
function cpTraceReset() {
  cpTraceT0 = (typeof performance !== 'undefined') ? performance.now() : Date.now()
  try { localStorage.setItem(CP_TRACE_KEY, JSON.stringify([{ t: 0, ev: 'reset', ts: new Date().toISOString() }])) } catch { /* ignore */ }
}
function cpTrace(ev: string, data?: Record<string, unknown>) {
  try {
    const now = (typeof performance !== 'undefined') ? performance.now() : Date.now()
    const raw = localStorage.getItem(CP_TRACE_KEY)
    const arr = raw ? JSON.parse(raw) as unknown[] : []
    arr.push({ t: Math.round(now - cpTraceT0), ev, ...(data ?? {}) })
    // Cap at 500 entries to avoid blowing out localStorage
    const trimmed = arr.length > 500 ? arr.slice(-500) : arr
    localStorage.setItem(CP_TRACE_KEY, JSON.stringify(trimmed))
  } catch { /* ignore */ }
}
// Expose a global dumper so we can read the trace via the console
// AFTER a browser freeze + hard reload. Keeps diagnosis possible even
// when the main thread never returns control.
if (typeof window !== 'undefined') {
  (window as unknown as { __dumpCpLog?: () => unknown }).__dumpCpLog = () => {
    try { return JSON.parse(localStorage.getItem(CP_TRACE_KEY) ?? '[]') } catch { return null }
  }
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
  document.removeEventListener('click', onDocumentClickDismissCtxMenu)
  if (spreadRef.value) {
    spreadRef.value.removeEventListener('contextmenu', onSpreadContextMenu)
  }
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
  cpTraceReset()
  cpTrace('initWorkbook.start', { templateId: props.templateId, auditYear: props.auditYear })
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
    // Parse JSON once — extract add-row state before passing to SpreadJS
    const jsonObj = JSON.parse(jsonStr)
    if (currentSubmission) {
      loadDynamicRangesFromObj(jsonObj as Record<string, unknown>)
    }
    // Skip recalculation during fromJSON — a single recalc is triggered
    // after all Phase 2 mutations complete, avoiding repeated full-workbook
    // recomputation on templates with heavy cross-sheet formulas.
    console.time('[perf] fromJSON')
    ;(workbook.fromJSON as unknown as (data: unknown, opts?: Record<string, unknown>) => void)(
      jsonObj,
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
          cpTrace('phase2.fetch.start')
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
          cpTrace('phase2.fetch.done', { tagCount: tags.length })

          // Master suspend envelope for all Phase 2 mutations — prevents the
          // ~1000 setValue/setStyle/setDataValidator calls below from each
          // triggering a full-workbook repaint / event dispatch / recalc.
          // Nested suspend calls inside individual apply* functions are still
          // safe (SpreadJS reference-counts suspend depth).
          console.time('[perf] phase2-mutate')
          workbook.suspendPaint()
          suspendEventSafe(workbook)
          suspendCalcServiceSafe(workbook)
          cpTrace('phase2.mutate.suspended')
          try {
            // Build cell-tag index ONCE for O(1) lookups (replaces O(tags ×
            // rows × cols) scans). Must be called BEFORE applyPrefill /
            // applyConfigPrefill / applyDataEntryProtection since they all use
            // fillTaggedCell / unlockScalarCell which rely on the index.
            console.time('[perf] buildCellTagIndex')
            buildCellTagIndex(workbook)
            console.timeEnd('[perf] buildCellTagIndex')
            cpTrace('buildCellTagIndex.done')

            // Pre-fill enterprise settings (uses pre-fetched tags + prefillData)
            if (!currentSubmission && prefillData) {
              cpTrace('applyPrefill.start')
              console.time('[perf] applyPrefill')
              applyPrefill(workbook, tags, prefillData)
              console.timeEnd('[perf] applyPrefill')
              cpTrace('applyPrefill.done')
            }

            // Config-driven prefill: always write values + dropdowns + hide empty rows
            if (configPrefillData) {
              cpTrace('applyConfigPrefill.start')
              console.time('[perf] applyConfigPrefill')
              applyConfigPrefill(workbook, tags, configPrefillData)
              console.timeEnd('[perf] applyConfigPrefill')
              cpTrace('applyConfigPrefill.done')
            }

            // Inject dictionary-based dropdown validators (uses pre-fetched tags)
            cpTrace('applyDictValidators.start')
            console.time('[perf] applyDictValidators')
            await applyDictValidators(workbook, tags)
            console.timeEnd('[perf] applyDictValidators')
            cpTrace('applyDictValidators.done')

            // Bind ValidationError event on every sheet
            cpTrace('bindValidationErrorDialogs.start')
            console.time('[perf] bindValidationErrorDialogs')
            bindValidationErrorDialogs(workbook)
            bindClipboardPasteValidation(workbook)
            console.timeEnd('[perf] bindValidationErrorDialogs')
            cpTrace('bindValidationErrorDialogs.done')

            // Initialize dynamicRanges from tags (MUST be after config-prefill
            // which may track protected prefill ranges)
            initDynamicRangesFromTags(tags)

            // Apply cell protection + required field markers (uses pre-fetched tags)
            if (publishedVersion.protectionEnabled !== 0) {
              cpTrace('applyDataEntryProtection.start')
              console.time('[perf] applyDataEntryProtection')
              applyDataEntryProtection(workbook, tags)
              console.timeEnd('[perf] applyDataEntryProtection')
              cpTrace('applyDataEntryProtection.done')
            }
          } finally {
            cpTrace('phase2.resume.start')
            resumeCalcServiceSafe(workbook)
            cpTrace('phase2.resumeCalcService.done')
            resumeEventSafe(workbook)
            cpTrace('phase2.resumeEvent.done')
            workbook.resumePaint()
            cpTrace('phase2.resumePaint.done')
            console.timeEnd('[perf] phase2-mutate')
          }
        } catch (e) {
          console.warn('[phase2] failed to load tags / apply features:', e)
          // Still bind validation error dialogs as fallback
          bindValidationErrorDialogs(workbook)
          bindClipboardPasteValidation(workbook)
        }
      } else {
        bindValidationErrorDialogs(workbook)
        bindClipboardPasteValidation(workbook)
      }
    } finally {
      // Trigger a single recalculation now that all mutations are done (or
      // skipped / failed) — this MUST run to compensate for the
      // doNotRecalculateAfterLoad flag passed to fromJSON above. Otherwise
      // formula cells would render stale/unevaluated.
      cpTrace('calculate.start')
      console.time('[perf] calculate')
      calculateAllSafe(workbook)
      console.timeEnd('[perf] calculate')
      cpTrace('calculate.done')
    }

    // Force readonly when:
    //  1. Parent explicitly says readonly (e.g. lock held by another user), OR
    //  2. Submission has been submitted (status=1) or approved (status=2).
    // A rejected submission (status=3) reverts to editable so the user can fix & re-submit.
    const submissionStatus = currentSubmission?.status ?? 0
    const isSubmittedOrApproved = submissionStatus === 1 || submissionStatus === 2
    const forceReadonly = props.readonly || isSubmittedOrApproved
    cpTrace('postcalc.readonlyCheck', { forceReadonly, submissionStatus, propsReadonly: props.readonly })
    if (forceReadonly) {
      cpTrace('postcalc.applyReadonlyProtection.start')
      applyReadonlyProtection()
      cpTrace('postcalc.applyReadonlyProtection.done')
      releaseLockIfOwned()
      cpTrace('postcalc.releaseLockIfOwned.done')
    } else {
      startHeartbeat()
      cpTrace('postcalc.startHeartbeat.done')
    }

    // ── Sheet navigation setup ──────────────────────────────────────────
    cpTrace('postcalc.bindSheetNavEvents.start')
    bindSheetNavEvents(workbook)
    cpTrace('postcalc.bindSheetNavEvents.done')
    activeSheetIndex.value = workbook.getActiveSheetIndex()
    cpTrace('postcalc.computeAllSheetStatuses.start')
    computeAllSheetStatuses()
    cpTrace('postcalc.computeAllSheetStatuses.done')
    // Auto-fit the initial sheet after a tick to allow layout to settle
    cpTrace('postcalc.nextTick.start')
    await nextTick()
    cpTrace('postcalc.nextTick.done')
    cpTrace('postcalc.autoFitCurrentSheet.start')
    autoFitCurrentSheet()
    cpTrace('postcalc.autoFitCurrentSheet.done')
    window.addEventListener('resize', onWindowResize)
    // Bind right-click context menu for add/delete row
    if (spreadRef.value) {
      spreadRef.value.addEventListener('contextmenu', onSpreadContextMenu)
    }
    document.addEventListener('click', onDocumentClickDismissCtxMenu)
    cpTrace('postcalc.allDone')
  } catch (e: any) {
    errorMsg.value = '加载模板失败：' + (e?.message ?? '未知错误')
    cpTrace('initWorkbook.catch', { err: String(e?.message ?? e) })
    releaseLockIfOwned()
  } finally {
    cpTrace('initWorkbook.finally.loadingFalse')
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
      for (let idx = 0; idx < prefillTags.length; idx++) {
        const tag = prefillTags[idx]
        cpTrace('cp.loop.beforeTag', { idx, tag: tag.tagName, sheet: tag.sheetName, range: tag.cellRange })
        try {
          applyOneConfigPrefill(wb, tag, configData)
        } catch (e) {
          console.warn(`[config-prefill] failed for tag "${tag.tagName}":`, e)
          cpTrace('cp.loop.tagThrew', { idx, tag: tag.tagName, err: String(e) })
        }
        cpTrace('cp.loop.afterTag', { idx, tag: tag.tagName })
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
  /** Synthetic string values to inject into the dropdown list (e.g. "外购" / "产出" virtual nodes
   *  in Sheet 11 能源流程图). These are NOT records in any table — they are literal sentinels
   *  that the downstream business logic recognizes. See EnergyFlowPostProcessor. */
  extraValues?: string[]
  /** Where to inject extraValues relative to the table-sourced values. Default "prepend". */
  extraPosition?: 'prepend' | 'append'
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
  cpTrace('cp.tag.start', { tag: tagLabel })
  if (!tag.targetTable || !tag.cellRange || !tag.columnMappings) {
    cpTrace('cp.tag.skip.missingFields', { tag: tagLabel })
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
    cpTrace('cp.tag.skip.noSheet', { tag: tagLabel, sheetName: tag.sheetName, sheetIndex: tag.sheetIndex })
    console.warn(`[config-prefill] sheet not found for tag "${tagLabel}"`)
    console.timeEnd(`[config-prefill] "${tagLabel}" total`)
    return
  }
  cpTrace('cp.tag.sheetResolved', { tag: tagLabel, maxRows, cols: columns.length, records: records.length, isDropdownOnly })

  // 6. Resolve column indices helper
  const resolveColIndex = (colDef: { col: string | number }) => {
    if (typeof colDef.col === 'string' && /^[A-Za-z]+$/.test(colDef.col)) {
      return letterToColIndex(colDef.col.toUpperCase())
    }
    return startCol + Number(colDef.col)
  }

  // Clear old data before writing (skip in dropdown_only mode)
  if (!isDropdownOnly) {
    cpTrace('cp.tag.clear.start', { tag: tagLabel })
    for (let i = 0; i < maxRows; i++) {
      for (const colDef of columns) {
        if (colDef.prefill === false) continue // don't clear dropdown-only columns
        const colIndex = resolveColIndex(colDef)
        sheet.setValue(startRow + i, colIndex, null)
      }
    }
    cpTrace('cp.tag.clear.done', { tag: tagLabel })
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
    // Inject synthetic sentinel values (e.g. "外购" / "产出" in Sheet 11 能源流程图).
    // These do not exist in any table — they are literal strings the frontend diagram
    // and EnergyFlowPostProcessor recognize as virtual source / sink nodes.
    if (colDef.extraValues?.length) {
      const position = colDef.extraPosition ?? 'prepend'
      const toAdd: string[] = []
      for (const raw of colDef.extraValues) {
        const val = raw != null ? String(raw) : ''
        if (val !== '' && !seen.has(val)) { seen.add(val); toAdd.push(val) }
      }
      if (toAdd.length) {
        if (position === 'append') values.push(...toAdd)
        else values.unshift(...toAdd)
      }
    }
    if (values.length > 0) {
      colDropdownValues.set(colIndex, values)
    }
  }

  // 9. Fill rows with values AND set dropdown validators
  const DataValidation = window.GC?.Spread?.Sheets?.DataValidation
  cpTrace('cp.tag.fill.start', { tag: tagLabel, rowsToFill, cols: columns.length })
  let fillOps = 0
  for (let i = 0; i < rowsToFill; i++) {
    const record = i < records.length ? records[i] : null
    for (const colDef of columns) {
      const colIndex = resolveColIndex(colDef)
      // checkpoint every 25 ops so we can see last-reached point if we freeze mid-loop
      if ((++fillOps) % 25 === 0) cpTrace('cp.tag.fill.progress', { tag: tagLabel, fillOps, row: i, col: colIndex })

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
      // end of per-cell style block

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
  cpTrace('cp.tag.fill.done', { tag: tagLabel, fillOps })

  // 10. Hide empty rows beyond the filled data (skip in dropdown_only mode)
  if (!isDropdownOnly) {
    for (let i = 0; i < rowsToFill; i++) {
      sheet.setRowVisible(startRow + i, true)
    }
    for (let i = rowsToFill; i < maxRows; i++) {
      sheet.setRowVisible(startRow + i, false)
    }
    // Track this CONFIG_PREFILL area so add-row logic blocks insert/delete here
    const sheetIdx = resolveSheetIndex(wb, tag)
    trackPrefillRange(sheetIdx, startRow, rowsToFill)
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
    cpTrace('cp.tag.refreshDropdowns.start', { tag: tagLabel, masterCols: masterColDefs.length })
    refreshDropdownsExcludingDuplicates()
    cpTrace('cp.tag.refreshDropdowns.done', { tag: tagLabel })
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
  cpTrace('cp.tag.end', { tag: tagLabel })
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

  const rangeTarget = resolveSingleCellRange(wb, tag)
  if (rangeTarget) {
    const sheet = wb.getSheet(rangeTarget.sheetIndex)
    sheet.setValue(rangeTarget.row, rangeTarget.col, value)
    return true
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

    cpTrace('dict.targets', { dictTypes: dictTypesNeeded.size, targets: targets.length })
    if (dictTypesNeeded.size === 0 || targets.length === 0) return

    // Fetch all needed dictionary data in one batch call
    cpTrace('dict.fetch.start')
    const dictMap = await getDataByTypes([...dictTypesNeeded])
    cpTrace('dict.fetch.done', { keys: dictMap ? Object.keys(dictMap).length : 0 })
    if (!dictMap || Object.keys(dictMap).length === 0) return

    const DataValidation = window.GC?.Spread?.Sheets?.DataValidation
    if (!DataValidation) {
      console.warn('[dropdown] SpreadJS DataValidation not available')
      return
    }

    wb.suspendPaint()
    try {
      let targetIdx = 0
      let setOps = 0
      for (const target of targets) {
        cpTrace('dict.target.start', { idx: targetIdx, dictType: target.dictType, col: target.col, rows: target.endRow - target.startRow + 1 })
        const items: DictData[] = dictMap[target.dictType] ?? []
        if (items.length === 0) { targetIdx++; continue }

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
        if (!sheet) { targetIdx++; continue }

        for (let r = target.startRow; r <= target.endRow; r++) {
          sheet.setDataValidator(r, target.col, dv)
          if ((++setOps) % 100 === 0) cpTrace('dict.setDataValidator.progress', { setOps, row: r, col: target.col })
        }
        cpTrace('dict.target.done', { idx: targetIdx })
        targetIdx++
      }
      cpTrace('dict.all.done', { setOps })
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

/**
 * Convert 0-based column index to letter (0 -> A, 25 -> Z, 26 -> AA)
 */
function colIndexToLetter(col: number): string {
  let result = ''
  let c = col
  while (c >= 0) {
    result = String.fromCharCode((c % 26) + 65) + result
    c = Math.floor(c / 26) - 1
  }
  return result
}

// ── Add Row Feature: utility functions ───────────────────────────────────

/** Parse cellRange string like "A3:K14" to 0-based {startRow, endRow, startCol, endCol} */
function parseCellRangeToObj(cellRange: string): { startRow: number; endRow: number; startCol: number; endCol: number } | null {
  const m = cellRange.toUpperCase().trim().match(/([A-Z]+)(\d+):([A-Z]+)(\d+)/)
  if (!m) return null
  return {
    startRow: parseInt(m[2]) - 1,
    endRow: parseInt(m[4]) - 1,
    startCol: letterToColIndex(m[1]),
    endCol: letterToColIndex(m[3]),
  }
}

/** Get effective range for a TABLE tag: dynamicRange if available, else static cellRange */
function getDynamicRange(tag: TplTagMapping): { startRow: number; endRow: number; startCol: number; endCol: number } | null {
  if (tag.id && dynamicRanges.has(tag.id)) {
    return dynamicRanges.get(tag.id)!
  }
  if (tag.cellRange) {
    return parseCellRangeToObj(tag.cellRange)
  }
  return null
}

/** Resolve the workbook sheet index for a tag */
function resolveSheetIndex(wb: import('@/types/spreadjs').GCSpreadWorkbook, tag: TplTagMapping): number {
  if (tag.sheetName) {
    const count = wb.getSheetCount()
    const target = tag.sheetName.trim().toLowerCase()
    for (let i = 0; i < count; i++) {
      if (wb.getSheet(i).name().trim().toLowerCase() === target) return i
    }
  }
  return tag.sheetIndex ?? 0
}

/** Find the TABLE/EQUIPMENT_BENCHMARK tag whose dynamic range contains the given row */
function findTableTagAtCell(sheetIndex: number, row: number): TplTagMapping | null {
  for (const tag of cachedTags) {
    const mt = tag.mappingType ?? 'SCALAR'
    if (mt !== 'TABLE' && mt !== 'EQUIPMENT_BENCHMARK') continue
    if (!workbook) continue
    const tagSI = resolveSheetIndex(workbook, tag)
    if (tagSI !== sheetIndex) continue
    const range = getDynamicRange(tag)
    if (!range) continue
    if (row >= range.startRow && row <= range.endRow) return tag
  }
  return null
}

/** Initialize dynamicRanges from loaded tags (static cellRange as baseline) */
function initDynamicRangesFromTags(tags: TplTagMapping[]) {
  for (const tag of tags) {
    const mt = tag.mappingType ?? 'SCALAR'
    if (mt !== 'TABLE' && mt !== 'EQUIPMENT_BENCHMARK') continue
    if (!tag.cellRange || !tag.id) continue
    const range = parseCellRangeToObj(tag.cellRange)
    if (!range) continue
    originalRanges.set(tag.id, { ...range })
    if (!dynamicRanges.has(tag.id)) {
      dynamicRanges.set(tag.id, { ...range })
    }
    if (!manuallyAddedRows.has(tag.id)) {
      manuallyAddedRows.set(tag.id, new Set())
    }
  }
}

/** Load _dynamicRanges and _manuallyAddedRows from parsed submission JSON object */
function loadDynamicRangesFromObj(jsonObj: Record<string, unknown>) {
  const dr = jsonObj._dynamicRanges
  if (dr && typeof dr === 'object') {
    for (const [idStr, range] of Object.entries(dr as Record<string, unknown>)) {
      const id = Number(idStr)
      if (isNaN(id)) continue
      const r = range as Record<string, number>
      if (r.startRow != null && r.endRow != null && r.startCol != null && r.endCol != null) {
        dynamicRanges.set(id, { startRow: r.startRow, endRow: r.endRow, startCol: r.startCol, endCol: r.endCol })
      }
    }
  }
  const mr = jsonObj._manuallyAddedRows
  if (mr && typeof mr === 'object') {
    for (const [idStr, rows] of Object.entries(mr as Record<string, unknown>)) {
      const id = Number(idStr)
      if (isNaN(id)) continue
      if (Array.isArray(rows)) {
        manuallyAddedRows.set(id, new Set(rows as number[]))
      }
    }
  }
}

/** Track CONFIG_PREFILL protected range for a sheet (used by add-row to block insert/delete) */
function trackPrefillRange(sheetIndex: number, startRow: number, rowCount: number) {
  if (rowCount <= 0) return
  const ranges = protectedPrefillRanges.get(sheetIndex) ?? []
  ranges.push({ startRow, endRow: startRow + rowCount - 1 })
  protectedPrefillRanges.set(sheetIndex, ranges)
}

/** Check if a row is within a CONFIG_PREFILL protected range */
function isInProtectedPrefillRange(sheetIndex: number, row: number): boolean {
  const ranges = protectedPrefillRanges.get(sheetIndex)
  if (!ranges) return false
  return ranges.some(r => row >= r.startRow && row <= r.endRow)
}

/**
 * Snapshot of cell values saved before a paste operation so that invalid
 * cells can be restored to their original values (instead of cleared).
 */
let prePasteSnapshot: Map<string, unknown> | null = null

/**
 * Bind ClipboardPasting event to save original cell values before paste,
 * and ClipboardPasted event to validate pasted values against DataValidation
 * rules. SpreadJS ValidationError only fires for manual keyboard input, NOT
 * for paste operations. This pair of handlers fills that gap.
 */
function bindClipboardPasteValidation(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
) {
  const Events = window.GC?.Spread?.Sheets?.Events
  if (!Events?.ClipboardPasted || !Events?.ClipboardPasting) return

  const sheetCount = wb.getSheetCount()
  for (let i = 0; i < sheetCount; i++) {
    const sheet = wb.getSheet(i)

    // ── ClipboardPasting: snapshot original values before paste ──────
    sheet.bind(Events.ClipboardPasting, (_sender: unknown, args: {
      cellRange: { row: number; col: number; rowCount: number; colCount: number }
    }) => {
      const { row, col, rowCount, colCount } = args.cellRange
      const snapshot = new Map<string, unknown>()
      for (let r = row; r < row + rowCount; r++) {
        for (let c = col; c < col + colCount; c++) {
          snapshot.set(`${r},${c}`, sheet.getValue(r, c))
        }
      }
      prePasteSnapshot = snapshot
    })

    // ── ClipboardPasted: validate pasted values ─────────────────────
    sheet.bind(Events.ClipboardPasted, (_sender: unknown, args: {
      cellRange: { row: number; col: number; rowCount: number; colCount: number }
    }) => {
      const { row, col, rowCount, colCount } = args.cellRange
      const snapshot = prePasteSnapshot
      prePasteSnapshot = null

      const invalidCells: Array<{ row: number; col: number; value: unknown; message: string }> = []

      for (let r = row; r < row + rowCount; r++) {
        for (let c = col; c < col + colCount; c++) {
          const dv = (sheet as unknown as {
            getDataValidator?: (r: number, c: number) => {
              isValid?: (val: unknown, r: number, c: number) => boolean
              formula1?: () => string
              errorStyle?: () => number
            } | null
          }).getDataValidator?.(r, c)
          if (!dv) continue

          const value = sheet.getValue(r, c)
          if (value == null || value === '') continue

          let isValid = true
          try {
            if (typeof dv.isValid === 'function') {
              isValid = dv.isValid(value, r, c)
            }
          } catch {
            // Fallback: check against formula1 list
            try {
              const formula = dv.formula1?.()
              if (formula) {
                // Skip range-reference based validators (e.g. "=$A$1:$A$10")
                if (formula.startsWith('=')) continue
                const allowedValues = formula.split(',').map((v: string) => v.trim())
                isValid = allowedValues.includes(String(value))
              }
            } catch { /* ignore */ }
          }

          if (!isValid) {
            invalidCells.push({
              row: r,
              col: c,
              value,
              message: `单元格 ${colIndexToLetter(c)}${r + 1} 的值 "${value}" 不在允许的范围内`,
            })
          }
        }
      }

      if (invalidCells.length === 0) return

      // Determine error style from first invalid cell's validator
      const firstDv = (sheet as unknown as {
        getDataValidator?: (r: number, c: number) => { errorStyle?: () => number } | null
      }).getDataValidator?.(invalidCells[0].row, invalidCells[0].col)
      let errorStyle: number | undefined
      try {
        errorStyle = firstDv?.errorStyle?.()
      } catch { /* ignore */ }

      const isStopStyle = (errorStyle ?? 0) === 0

      // Build error message
      const maxShow = 5
      const messages = invalidCells.slice(0, maxShow).map(c => c.message)
      if (invalidCells.length > maxShow) {
        messages.push(`...还有 ${invalidCells.length - maxShow} 个单元格`)
      }

      if (isStopStyle) {
        // Revert pasted values — restore originals from snapshot
        suspendEventSafe(wb)
        for (const cell of invalidCells) {
          const original = snapshot?.get(`${cell.row},${cell.col}`)
          sheet.setValue(cell.row, cell.col, original ?? null)
        }
        resumeEventSafe(wb)

        ElMessageBox.alert(
          messages.join('\n'),
          '粘贴内容校验失败',
          { type: 'error', confirmButtonText: '确定' },
        )
      } else {
        // Warning style — keep values but show warning
        ElMessageBox.alert(
          messages.join('\n'),
          '粘贴内容校验警告',
          { type: 'warning', confirmButtonText: '确定' },
        )
      }
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
      cpTrace('protection.suspendPaint.done', { sheetCount })

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

  const rangeTarget = resolveSingleCellRange(wb, tag)
  if (rangeTarget) {
    const sheet = wb.getSheet(rangeTarget.sheetIndex)
    const cell = sheet.getCell(rangeTarget.row, rangeTarget.col)
    cell.locked(false)
    if (tag.required === 1) markCellRequired(cell, hint)
    return
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

  // ── Strategy 2: fall back to dynamicRange (add-row adjusted) or stored cellRange
  if (!sheet) {
    const dynRange = getDynamicRange(tag)
    if (dynRange) {
      startRow = dynRange.startRow
      endRow = dynRange.endRow
      startCol = dynRange.startCol
      endCol = dynRange.endCol
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
    const mappingType = tag.mappingType ?? 'SCALAR'

    if (mappingType === 'SCALAR') {
      if (tag.required === 1 && isScalarCellEmpty(workbook, tag)) {
        errors.push(`"${tag.fieldName ?? tag.tagName}" 为必填字段，请填写`)
      }
    } else if (mappingType === 'TABLE' || mappingType === 'EQUIPMENT_BENCHMARK') {
      if (tag.required === 1 && isTableEmpty(workbook, tag)) {
        errors.push(`"${tag.tagName ?? tag.targetTable}" 至少需要填写1行数据`)
      }
      const rowErrors = validateTableRowRequired(workbook, tag)
      errors.push(...rowErrors)
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

  const rangeTarget = resolveSingleCellRange(wb, tag)
  if (rangeTarget) {
    const sheet = wb.getSheet(rangeTarget.sheetIndex)
    const val = sheet.getValue(rangeTarget.row, rangeTarget.col)
    return val == null || val === ''
  }
  return true // if we can't find the cell, treat as empty
}

function isTableEmpty(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
): boolean {
  // Use dynamicRange (add-row adjusted) if available, else static cellRange
  const range = getDynamicRange(tag)
  if (!range) return true

  const { startRow, endRow, startCol, endCol } = range

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

/**
 * Validate row-level required fields for TABLE tags.
 * Rule: if a row has ANY non-empty cell, then all columns marked required
 * in columnMappings must also be non-empty.
 * Returns array of error messages (empty = all valid).
 *
 * Skips EQUIPMENT_BENCHMARK tags whose columnMappings use the
 * commonColumns/typeColumns structure (not yet supported for row-level required).
 */
function validateTableRowRequired(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
): string[] {
  const errors: string[] = []
  if (!tag.columnMappings) return errors

  // Use dynamicRange (add-row adjusted) if available, else static cellRange
  const range = getDynamicRange(tag)
  if (!range) return errors

  const startRow = range.startRow
  const endRow = range.endRow
  const startCol = range.startCol

  const sheet = findSheet(wb, tag.sheetName, tag.sheetIndex)
  if (!sheet) return errors

  let colDefs: Array<{ col: string | number; field: string; required?: boolean }>
  try {
    const parsed = JSON.parse(tag.columnMappings)
    colDefs = parsed.columns || parsed
    if (!Array.isArray(colDefs)) return errors
  } catch {
    return errors
  }

  const resolveColIdx = (colDef: { col: string | number }) => {
    if (typeof colDef.col === 'string' && /^[A-Za-z]+$/.test(colDef.col)) {
      return letterToColIndex(colDef.col.toUpperCase())
    }
    return startCol + Number(colDef.col)
  }

  const requiredCols = colDefs
    .filter(c => c.required === true)
    .map(c => {
      const colIdx = resolveColIdx(c)
      return { colIndex: colIdx, label: `${colIndexToLetter(colIdx)}列` }
    })

  if (requiredCols.length === 0) return errors

  const endCol = range.endCol

  for (let r = startRow; r <= endRow; r++) {
    let rowHasData = false
    for (let c = startCol; c <= endCol; c++) {
      const val = sheet.getValue(r, c)
      if (val != null && val !== '') {
        rowHasData = true
        break
      }
    }
    if (!rowHasData) continue

    const missingCols: string[] = []
    for (const rc of requiredCols) {
      const val = sheet.getValue(r, rc.colIndex)
      if (val == null || val === '') {
        missingCols.push(rc.label)
      }
    }
    if (missingCols.length > 0) {
      const rowNum = r - startRow + 1
      errors.push(`"${tag.tagName}" 第${rowNum}行: ${missingCols.join('、')} 为必填`)
    }
  }
  return errors
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

function resolveSingleCellRange(
  wb: import('@/types/spreadjs').GCSpreadWorkbook,
  tag: TplTagMapping,
): { sheetIndex: number; row: number; col: number } | null {
  if (!tag.cellRange) return null
  const rangeMatch = tag.cellRange.toUpperCase().trim().match(/^([A-Z]+)(\d+)(?::([A-Z]+)(\d+))?$/)
  if (!rangeMatch) return null
  const row = parseInt(rangeMatch[2]) - 1
  const col = letterToColIndex(rangeMatch[1])
  const endRow = rangeMatch[4] ? parseInt(rangeMatch[4]) - 1 : row
  const endCol = rangeMatch[3] ? letterToColIndex(rangeMatch[3]) : col
  if (row !== endRow || col !== endCol) return null

  const sheet = findSheet(wb, tag.sheetName, tag.sheetIndex)
  if (!sheet) return null
  let sheetIndex = tag.sheetIndex ?? 0
  const count = wb.getSheetCount()
  for (let i = 0; i < count; i++) {
    if (wb.getSheet(i) === sheet) {
      sheetIndex = i
      break
    }
  }
  return { sheetIndex, row, col }
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
  cpTrace('applyReadonlyProtection.start', { sheets: count })
  // Match Phase 2 suspend envelope: paint + event + calcService.
  // Without suspendEvent/suspendCalcService the subsequent
  // `range.locked(true)` on ~45 sheets × thousands of cells each fires
  // cell-change events and calc re-evaluation per mutation, which locks
  // the main thread for 300+ seconds on submitted templates.
  workbook.suspendPaint()
  suspendEventSafe(workbook)
  suspendCalcServiceSafe(workbook)
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
    resumeCalcServiceSafe(workbook)
    resumeEventSafe(workbook)
    workbook.resumePaint()
    cpTrace('applyReadonlyProtection.done')
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

async function save(options?: { skipExtraction?: boolean }): Promise<void> {
  if (!workbook || !publishedVersion) {
    throw new Error('工作簿尚未初始化，请稍后重试')
  }
  saving.value = true
  try {
    const jsonObj = workbook.toJSON() as Record<string, unknown>
    // Embed add-row dynamic ranges into the JSON for backend extraction
    if (dynamicRanges.size > 0) {
      const drObj: Record<string, unknown> = {}
      for (const [id, range] of dynamicRanges) {
        drObj[String(id)] = range
      }
      jsonObj._dynamicRanges = drObj
    }
    if (manuallyAddedRows.size > 0) {
      const mrObj: Record<string, number[]> = {}
      for (const [id, rows] of manuallyAddedRows) {
        if (rows.size > 0) mrObj[String(id)] = [...rows]
      }
      jsonObj._manuallyAddedRows = mrObj
    }
    const json = JSON.stringify(jsonObj)
    const saved = await saveDraft({
      templateId: props.templateId,
      auditYear: props.auditYear,
      submissionJson: json,
      templateVersion: publishedVersion.version ?? 1,
      templateVersionId: publishedVersion.id,
      skipExtraction: options?.skipExtraction,
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
      const tableNotEmpty = !isTableEmpty(wb, tag)
      const rowErrors = validateTableRowRequired(wb, tag)
      if (tableNotEmpty && rowErrors.length === 0) filledRequired++
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
      const mappingType = tag.mappingType ?? 'SCALAR'
      if (mappingType === 'SCALAR') {
        if (tag.required === 1 && isScalarCellEmpty(workbook, tag)) {
          errors.push(`"${tag.fieldName ?? tag.tagName}" 为必填字段，请填写`)
        }
      } else if (mappingType === 'TABLE' || mappingType === 'EQUIPMENT_BENCHMARK') {
        if (tag.required === 1 && isTableEmpty(workbook, tag)) {
          errors.push(`"${tag.tagName ?? tag.targetTable}" 至少需要填写1行数据`)
        }
        const rowErrors = validateTableRowRequired(workbook, tag)
        errors.push(...rowErrors)
      }
    }

    if (errors.length > 0) {
      result.push({ sheetIndex: si, sheetName: name, errors })
    }
  }
  return result
}

// ── Add Row Feature: context menu + insert/delete/reset ────────────────

/** Handle right-click on spreadsheet to show add/delete row context menu */
function onSpreadContextMenu(e: MouseEvent) {
  e.preventDefault()
  ctxMenuVisible.value = false

  if (!workbook) return
  const submissionStatus = currentSubmission?.status ?? 0
  const isSubmittedOrApproved = submissionStatus === 1 || submissionStatus === 2
  if (props.readonly || isSubmittedOrApproved) return

  const sheet = workbook.getActiveSheet()
  const sheetIndex = workbook.getActiveSheetIndex()

  // Use active selection to determine clicked row
  const selections = sheet.getSelections()
  if (!selections || selections.length === 0) return
  const row = selections[0].row
  if (row < 0) return

  // Find TABLE tag at this row
  const tag = findTableTagAtCell(sheetIndex, row)
  if (!tag || !tag.id) return

  const range = getDynamicRange(tag)
  if (!range) return

  const currentRows = range.endRow - range.startRow + 1
  const canInsert = currentRows < MAX_TABLE_ROWS
  const isProtected = isInProtectedPrefillRange(sheetIndex, row)
  const addedRows = manuallyAddedRows.get(tag.id) ?? new Set()
  const canDelete = addedRows.has(row)

  const items: CtxMenuItem[] = [
    {
      label: `在第 ${row + 1} 行下方插入行`,
      action: 'insertBelow',
      disabled: !canInsert || isProtected,
    },
    {
      label: `删除第 ${row + 1} 行`,
      action: 'deleteRow',
      disabled: !canDelete,
      danger: true,
    },
    {
      label: '重置为模板原始状态',
      action: 'resetToTemplate',
      danger: true,
    },
  ]

  ctxMenuRow = row
  ctxMenuSheetIndex = sheetIndex
  ctxMenuTag = tag
  ctxMenuItems.value = items
  ctxMenuX.value = e.clientX
  ctxMenuY.value = e.clientY
  ctxMenuVisible.value = true
}

/** Execute a context menu action */
function executeCtxAction(action: string) {
  ctxMenuVisible.value = false
  if (action === 'insertBelow') handleInsertRow()
  else if (action === 'deleteRow') handleDeleteRow()
  else if (action === 'resetToTemplate') handleResetToTemplate()
}

/** Dismiss context menu on outside click */
function onDocumentClickDismissCtxMenu() {
  ctxMenuVisible.value = false
}

/** Insert a row below the right-clicked row within its TABLE range */
function handleInsertRow() {
  if (!workbook || !ctxMenuTag) return
  const tag = ctxMenuTag
  const row = ctxMenuRow
  const sheetIndex = ctxMenuSheetIndex
  const range = getDynamicRange(tag)
  if (!range || !tag.id) return

  const sheet = workbook.getSheet(sheetIndex)
  if (!sheet) return

  const insertAt = row + 1
  console.log(`[add-row] Inserting row at ${insertAt} in tag ${tag.tagName} (range ${range.startRow}-${range.endRow})`)

  workbook.suspendPaint()
  suspendEventSafe(workbook)
  try {
    const wasProtected = sheet.options.isProtected
    sheet.options.isProtected = false

    sheet.addRows(insertAt, 1)

    // Update dynamicRanges for ALL TABLE tags on this sheet
    updateDynamicRangesAfterInsert(sheetIndex, insertAt, tag.id)

    // Track the new row as manually added
    const added = manuallyAddedRows.get(tag.id) ?? new Set()
    const shifted = new Set<number>()
    for (const r of added) {
      shifted.add(r >= insertAt ? r + 1 : r)
    }
    shifted.add(insertAt)
    manuallyAddedRows.set(tag.id, shifted)

    // Unlock the new row's cells within the TABLE range
    const updatedRange = getDynamicRange(tag)!
    sheet.getRange(insertAt, updatedRange.startCol, 1, updatedRange.endCol - updatedRange.startCol + 1).locked(false)

    if (wasProtected) {
      sheet.options.isProtected = true
    }
    console.log(`[add-row] Row inserted. New range: ${updatedRange.startRow}-${updatedRange.endRow}`)
  } finally {
    resumeEventSafe(workbook)
    workbook.resumePaint()
  }
  debouncedUpdateFillStatus()
}

/** Delete a manually added row */
function handleDeleteRow() {
  if (!workbook || !ctxMenuTag) return
  const tag = ctxMenuTag
  const row = ctxMenuRow
  const sheetIndex = ctxMenuSheetIndex
  if (!tag.id) return

  const added = manuallyAddedRows.get(tag.id) ?? new Set()
  if (!added.has(row)) {
    ElMessageBox.alert('此行为模板原始行，不可删除。只能删除手动添加的行。', '无法删除')
    return
  }

  const sheet = workbook.getSheet(sheetIndex)
  if (!sheet) return

  console.log(`[add-row] Deleting row ${row} from tag ${tag.tagName}`)

  workbook.suspendPaint()
  suspendEventSafe(workbook)
  try {
    const wasProtected = sheet.options.isProtected
    sheet.options.isProtected = false

    sheet.deleteRows(row, 1)

    updateDynamicRangesAfterDelete(sheetIndex, row, tag.id)

    added.delete(row)
    const shifted = new Set<number>()
    for (const r of added) {
      shifted.add(r > row ? r - 1 : r)
    }
    manuallyAddedRows.set(tag.id, shifted)

    if (wasProtected) {
      sheet.options.isProtected = true
    }
    console.log(`[add-row] Row deleted.`)
  } finally {
    resumeEventSafe(workbook)
    workbook.resumePaint()
  }
  debouncedUpdateFillStatus()
}

/** Reset all dynamic row changes back to template original */
async function handleResetToTemplate() {
  try {
    await ElMessageBox.confirm(
      '将删除所有手动添加的行，恢复模板原始状态。此操作不可撤销。',
      '确认重置',
      { type: 'warning', confirmButtonText: '重置', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  if (!workbook) return
  console.log('[add-row] Resetting to template original state')

  workbook.suspendPaint()
  suspendEventSafe(workbook)
  try {
    // Collect ALL manually added rows grouped by sheet index
    // This avoids both bugs: (1) deleting wrong rows when inserts are mid-range,
    // (2) cascading row-shift when multiple tags share the same sheet.
    const rowsBySheet = new Map<number, number[]>()
    for (const tag of cachedTags) {
      const mt = tag.mappingType ?? 'SCALAR'
      if (mt !== 'TABLE' && mt !== 'EQUIPMENT_BENCHMARK') continue
      if (!tag.id) continue

      const added = manuallyAddedRows.get(tag.id)
      if (!added || added.size === 0) continue

      const si = resolveSheetIndex(workbook, tag)
      const existing = rowsBySheet.get(si) ?? []
      for (const r of added) existing.push(r)
      rowsBySheet.set(si, existing)
    }

    // Delete rows bottom-up (highest index first) to avoid shifting issues
    for (const [si, rows] of rowsBySheet) {
      const sheet = workbook.getSheet(si)
      if (!sheet) continue

      const wasProtected = sheet.options.isProtected
      sheet.options.isProtected = false

      // Sort descending so deleting a higher row doesn't shift lower rows
      const sorted = [...new Set(rows)].sort((a, b) => b - a)
      for (const row of sorted) {
        sheet.deleteRows(row, 1)
      }

      sheet.options.isProtected = wasProtected
    }

    // Reset all dynamic ranges and tracking state
    for (const tag of cachedTags) {
      const mt = tag.mappingType ?? 'SCALAR'
      if (mt !== 'TABLE' && mt !== 'EQUIPMENT_BENCHMARK') continue
      if (!tag.id) continue

      const original = originalRanges.get(tag.id)
      if (original) {
        dynamicRanges.set(tag.id, { ...original })
      }
      manuallyAddedRows.set(tag.id, new Set())
    }
  } finally {
    resumeEventSafe(workbook)
    workbook.resumePaint()
  }
  console.log('[add-row] Reset complete')
  debouncedUpdateFillStatus()
}

/** Update ALL dynamic ranges on the same sheet after a row insertion */
function updateDynamicRangesAfterInsert(sheetIndex: number, insertedRow: number, triggerTagId: number) {
  if (!workbook) return
  for (const tag of cachedTags) {
    const mt = tag.mappingType ?? 'SCALAR'
    if (mt !== 'TABLE' && mt !== 'EQUIPMENT_BENCHMARK') continue
    if (!tag.id) continue
    const tagSI = resolveSheetIndex(workbook, tag)
    if (tagSI !== sheetIndex) continue
    const range = dynamicRanges.get(tag.id)
    if (!range) continue

    if (tag.id === triggerTagId) {
      range.endRow += 1
    } else if (range.startRow >= insertedRow) {
      range.startRow += 1
      range.endRow += 1
      // Also shift manually added rows for this other tag
      const added = manuallyAddedRows.get(tag.id)
      if (added && added.size > 0) {
        const shifted = new Set<number>()
        for (const r of added) shifted.add(r >= insertedRow ? r + 1 : r)
        manuallyAddedRows.set(tag.id, shifted)
      }
    }
  }
  // Shift protected prefill ranges on the same sheet
  const prefillRanges = protectedPrefillRanges.get(sheetIndex)
  if (prefillRanges) {
    for (const pr of prefillRanges) {
      if (pr.startRow >= insertedRow) {
        pr.startRow += 1
        pr.endRow += 1
      } else if (pr.endRow >= insertedRow) {
        pr.endRow += 1
      }
    }
  }
}

/** Update ALL dynamic ranges on the same sheet after a row deletion */
function updateDynamicRangesAfterDelete(sheetIndex: number, deletedRow: number, triggerTagId: number) {
  if (!workbook) return
  for (const tag of cachedTags) {
    const mt = tag.mappingType ?? 'SCALAR'
    if (mt !== 'TABLE' && mt !== 'EQUIPMENT_BENCHMARK') continue
    if (!tag.id) continue
    const tagSI = resolveSheetIndex(workbook, tag)
    if (tagSI !== sheetIndex) continue
    const range = dynamicRanges.get(tag.id)
    if (!range) continue

    if (tag.id === triggerTagId) {
      range.endRow -= 1
    } else if (range.startRow > deletedRow) {
      range.startRow -= 1
      range.endRow -= 1
      const added = manuallyAddedRows.get(tag.id)
      if (added && added.size > 0) {
        const shifted = new Set<number>()
        for (const r of added) shifted.add(r > deletedRow ? r - 1 : r)
        manuallyAddedRows.set(tag.id, shifted)
      }
    }
  }
  const prefillRanges = protectedPrefillRanges.get(sheetIndex)
  if (prefillRanges) {
    for (const pr of prefillRanges) {
      if (pr.startRow > deletedRow) {
        pr.startRow -= 1
        pr.endRow -= 1
      }
    }
  }
}

defineExpose({
  save,
  getSubmissionId,
  getVersionId,
  isSubmitted,
  validateRequiredFields,
  validateRequiredFieldsBySheet,
  navigateToSheet: onSheetSelect,
  handleResetToTemplate,
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
    <!-- Right-click context menu for add/delete row -->
    <teleport to="body">
      <div
        v-if="ctxMenuVisible"
        class="row-context-menu"
        :style="{ left: ctxMenuX + 'px', top: ctxMenuY + 'px' }"
        @click.stop
      >
        <div
          v-for="item in ctxMenuItems"
          :key="item.action"
          class="ctx-menu-item"
          :class="{ disabled: item.disabled, danger: item.danger }"
          @click="!item.disabled && executeCtxAction(item.action)"
        >
          {{ item.label }}
        </div>
      </div>
    </teleport>
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

<style lang="scss">
/* Context menu must NOT be scoped — it's teleported to <body> */
.row-context-menu {
  position: fixed;
  z-index: 99999;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 4px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.12);
  min-width: 180px;
}

.ctx-menu-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: background-color 0.15s;

  &:hover:not(.disabled) {
    background-color: #f5f7fa;
  }

  &.disabled {
    color: #c0c4cc;
    cursor: not-allowed;
  }

  &.danger:not(.disabled) {
    color: #f56c6c;

    &:hover {
      background-color: #fef0f0;
    }
  }
}
</style>
