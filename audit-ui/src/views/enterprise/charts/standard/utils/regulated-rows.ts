/**
 * Helpers for regulated-chart pages (table8 GHG emission, table17 five-year
 * target). The backend returns extracted rows in `id DESC` order from
 * `/extracted-data/{tableName}` with snake_case keys; these helpers
 * categorize, stably re-sort, and deduplicate the rows so the UI can always
 * render the fixed customer-template sections in a predictable order.
 *
 * Used by GRA-74 / GRA-75 / GRA-76 to keep section boundaries, totals
 * placement, year columns, and deduplication consistent between page and
 * export.
 */
export type Row = Record<string, unknown>

const FOSSIL_CATEGORY_RE = /化石|fossil|燃料/i
const ELECTRIC_HEAT_CATEGORY_RE = /电力|热力|外购电|外购热|电|热|electric|heat|purchased/i
const PROCESS_CATEGORY_RE = /工艺|生产过程|过程|process/i
const SUMMARY_CATEGORY_RE = /汇总|合计|总|summary|total/i

function lowerOrEmpty(v: unknown): string {
  if (v == null) return ''
  return String(v).trim().toLowerCase()
}

function hasNumericField(row: Row, ...keys: string[]): boolean {
  for (const k of keys) {
    const v = row[k]
    if (typeof v === 'number' && Number.isFinite(v)) return true
    if (typeof v === 'string' && v.trim() !== '' && !Number.isNaN(Number(v))) return true
  }
  return false
}

function hasStringField(row: Row, ...keys: string[]): boolean {
  for (const k of keys) {
    const v = row[k]
    if (typeof v === 'string' && v.trim() !== '') return true
  }
  return false
}

/**
 * Stable sort by `id` ascending. Backend orders by `id DESC` so flipping
 * here recovers template (extraction) order. Rows without numeric id are
 * appended in original order.
 */
export function sortByIdAsc<T extends Row>(rows: T[]): T[] {
  const indexed = rows.map((row, idx) => ({ row, idx }))
  const numericId = (raw: unknown): number | null => {
    if (raw == null || raw === '') return null
    const n = Number(raw)
    return Number.isFinite(n) ? n : null
  }
  indexed.sort((a, b) => {
    const aId = numericId(a.row.id)
    const bId = numericId(b.row.id)
    if (aId !== null && bId !== null) {
      if (aId !== bId) return aId - bId
      return a.idx - b.idx
    }
    if (aId !== null) return -1
    if (bId !== null) return 1
    return a.idx - b.idx
  })
  return indexed.map(({ row }) => row)
}

/**
 * Deduplicate rows by composite key derived from `keys`. Keeps the first
 * occurrence so callers should `sortByIdAsc` first to control which row wins.
 * Rows with empty/missing keys are kept as-is to avoid losing real data.
 */
export function dedupByKey<T extends Row>(rows: T[], keys: (keyof T | string)[]): T[] {
  const seen = new Set<string>()
  const result: T[] = []
  for (const row of rows) {
    const parts = keys.map((k) => lowerOrEmpty(row[k as string]))
    const allEmpty = parts.every((p) => p === '')
    if (allEmpty) {
      result.push(row)
      continue
    }
    const key = parts.join('||')
    if (seen.has(key)) continue
    seen.add(key)
    result.push(row)
  }
  return result
}

// ---------- table8 / de_carbon_emission ----------

export type GhgSection = 'fossil' | 'electric_heat' | 'process' | 'summary'

export function classifyGhgRow(row: Row): GhgSection {
  const cat = lowerOrEmpty(row.emission_category ?? row.section)
  if (cat) {
    if (PROCESS_CATEGORY_RE.test(cat)) return 'process'
    if (ELECTRIC_HEAT_CATEGORY_RE.test(cat)) return 'electric_heat'
    if (FOSSIL_CATEGORY_RE.test(cat)) return 'fossil'
    if (SUMMARY_CATEGORY_RE.test(cat)) return 'summary'
  }
  if (
    hasStringField(row, 'source_name') &&
    hasNumericField(
      row,
      'low_heat_value',
      'carbon_content',
      'oxidation_rate',
      'conversion_output',
      'recovery_amount',
      'activity_data',
      'co2_emission',
    )
  ) {
    return 'fossil'
  }
  return 'summary'
}

export interface GhgSectionRows {
  summary: Row[]
  fossil: Row[]
  electricHeat: Row[]
  process: Row[]
}

/**
 * Split de_carbon_emission rows into fixed template sections, with stable
 * template-order sorting and per-section deduplication. The returned arrays
 * are always defined (possibly empty) so the page can render the fixed
 * structure regardless of data presence.
 */
export function splitGhgRows(rows: Row[]): GhgSectionRows {
  const ordered = sortByIdAsc(rows)
  const buckets: GhgSectionRows = {
    summary: [],
    fossil: [],
    electricHeat: [],
    process: [],
  }
  for (const row of ordered) {
    const section = classifyGhgRow(row)
    if (section === 'fossil') buckets.fossil.push(row)
    else if (section === 'electric_heat') buckets.electricHeat.push(row)
    else if (section === 'process') buckets.process.push(row)
    else buckets.summary.push(row)
  }
  return {
    summary: dedupByKey(buckets.summary, ['emission_category', 'source_name']),
    fossil: dedupByKey(buckets.fossil, ['source_name', 'low_heat_value', 'carbon_content']),
    electricHeat: dedupByKey(buckets.electricHeat, ['emission_category', 'source_name']),
    process: dedupByKey(buckets.process, ['source_name']),
  }
}

// ---------- table17 / de_five_year_target ----------
//
// de_five_year_target is written by BOTH Sheet20 (表 20_产品碳峰 / 表 20_年度
// 目标) and Sheet21 (表 21_产品单耗 / 表 21_年度节能 + 6 scalar cells). table17
// only renders Sheet21 data, so Sheet20-shape rows are filtered out upfront.
//
// Field-level discriminators (see sql/33-0427v2-template-tag-mappings.sql
// lines 436, 438, 447, 449):
//   * Sheet20_产品碳峰 carbon-peak rows have product_output / gross_output /
//     emission / unit_strength / intensity_drop — none of these appear in any
//     Sheet21 mapping, so any of them is a reliable Sheet20 marker.
//   * Sheet20_年度目标 includes y2025; Sheet21_年度节能 has y2026..y2030 only.
//     A target_name + measurement_unit + y2025 row is therefore Sheet20.
//   * Sheet21_产品单耗 rows legitimately carry product_name + indicator_name
//     AND target_name + y2030 (col4 = “2030产品名称”, col6 = “2030单耗指标值”),
//     so product classification must win before the annual heuristic.

export type FiveYearSection = 'summary' | 'product' | 'annual'

const SUMMARY_SCALAR_KEYS = [
  'gross_output_actual2025',
  'gross_output_target2030',
  'energy_equal_actual2025',
  'energy_equal_target2030',
  'energy_equiv_actual2025',
  'energy_equiv_target2030',
]

const ANNUAL_YEAR_KEYS = ['y2026', 'y2027', 'y2028', 'y2029', 'y2030']

/** Fields that only appear in Sheet20 (产品碳峰) mappings. */
const SHEET20_ONLY_FIELDS = [
  'product_output',
  'gross_output',
  'emission',
  'unit_strength',
  'intensity_drop',
]

/**
 * Heuristic: true when the row is clearly a Sheet20 carbon-peak row that
 * should NOT appear in the table17 (“十五五”节能目标) view.
 *
 * Sheet21 产品单耗 / 年度节能 never populates these columns, so any
 * occurrence is safe to treat as Sheet20.
 */
export function isSheet20FiveYearRow(row: Row): boolean {
  const sectionType = lowerOrEmpty(row.section_type)
  if (
    sectionType === 'carbon_peak'
    || sectionType === 'sheet20'
    || sectionType.includes('碳峰')
  ) {
    return true
  }
  if (hasNumericField(row, ...SHEET20_ONLY_FIELDS)) {
    return true
  }
  // Sheet20_年度目标 has y2025, Sheet21_年度节能 does not.
  // Restrict to rows that look like annual templates so Sheet21 summary
  // scalars (gross_output_actual2025 etc.) are not mistaken for Sheet20.
  if (hasNumericField(row, 'y2025') && hasStringField(row, 'target_name', 'measurement_unit')) {
    return true
  }
  return false
}

export function classifyFiveYearRow(row: Row): FiveYearSection {
  const section = lowerOrEmpty(row.section_type)
  if (section === 'summary') return 'summary'
  if (section === 'product') return 'product'
  if (section === 'annual' || section === 'year' || section === 'yearly') return 'annual'

  // Sheet21 scalar summary rows: e.g. {gross_output_actual2025: 12345}.
  if (hasNumericField(row, ...SUMMARY_SCALAR_KEYS)) {
    return 'summary'
  }
  // Sheet21 产品单耗 rows: have product_name + indicator_name in column 0/1.
  // Must win before the annual heuristic because these rows also carry
  // target_name (“2030产品名称”) + y2030 (“2030单耗指标值”).
  if (hasStringField(row, 'product_name') && hasStringField(row, 'indicator_name')) {
    return 'product'
  }
  // Sheet21 年度节能 rows: target_name + measurement_unit + any of y2026..y2030,
  // and explicitly NOT a product-unit-consumption row.
  if (
    hasStringField(row, 'target_name', 'measurement_unit')
    && hasNumericField(row, ...ANNUAL_YEAR_KEYS)
    && !hasStringField(row, 'product_name')
    && !hasStringField(row, 'indicator_name')
  ) {
    return 'annual'
  }
  return 'summary'
}

export interface FiveYearSectionRows {
  summary: Row[]
  product: Row[]
  annual: Row[]
}

/**
 * Split de_five_year_target rows into fixed Sheet21 template sections with
 * stable template-order sorting and dedup. Sheet20 carbon-peak rows are
 * filtered out so they never appear in the table17 view. Annual rows are
 * deduplicated by (target_name + measurement_unit) so duplicate target names
 * collapse to one row per unit.
 */
export function splitFiveYearRows(rows: Row[]): FiveYearSectionRows {
  const sheet21Only = rows.filter((row) => !isSheet20FiveYearRow(row))
  const ordered = sortByIdAsc(sheet21Only)
  const buckets: FiveYearSectionRows = { summary: [], product: [], annual: [] }
  for (const row of ordered) {
    const section = classifyFiveYearRow(row)
    buckets[section].push(row)
  }
  return {
    summary: dedupByKey(buckets.summary, SUMMARY_SCALAR_KEYS as string[]),
    product: dedupByKey(buckets.product, ['product_name', 'indicator_name', 'target_name']),
    annual: dedupByKey(buckets.annual, ['target_name', 'measurement_unit']),
  }
}
