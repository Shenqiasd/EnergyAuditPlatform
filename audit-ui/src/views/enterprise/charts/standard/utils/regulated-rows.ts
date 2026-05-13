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

export type FiveYearSection = 'summary' | 'product' | 'annual'

const SUMMARY_SCALAR_KEYS = [
  'gross_output_actual2025',
  'gross_output_target2030',
  'energy_equal_actual2025',
  'energy_equal_target2030',
  'energy_equiv_actual2025',
  'energy_equiv_target2030',
  'decline_rate',
]

const ANNUAL_YEAR_KEYS = ['y2026', 'y2027', 'y2028', 'y2029', 'y2030']

export function classifyFiveYearRow(row: Row): FiveYearSection {
  const section = lowerOrEmpty(row.section_type)
  if (section === 'summary') return 'summary'
  if (section === 'product') return 'product'
  if (section === 'annual' || section === 'year' || section === 'yearly') return 'annual'

  if (hasNumericField(row, ...ANNUAL_YEAR_KEYS) && hasStringField(row, 'target_name')) {
    return 'annual'
  }
  if (hasStringField(row, 'product_name') && hasStringField(row, 'indicator_name')) {
    return 'product'
  }
  if (hasNumericField(row, ...SUMMARY_SCALAR_KEYS)) {
    return 'summary'
  }
  return 'summary'
}

export interface FiveYearSectionRows {
  summary: Row[]
  product: Row[]
  annual: Row[]
}

/**
 * Split de_five_year_target rows into fixed template sections with stable
 * template-order sorting and dedup. Annual rows are deduplicated by
 * (target_name + measurement_unit) — duplicate target names in source data
 * are collapsed so a target appears once per unit.
 */
export function splitFiveYearRows(rows: Row[]): FiveYearSectionRows {
  const ordered = sortByIdAsc(rows)
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
