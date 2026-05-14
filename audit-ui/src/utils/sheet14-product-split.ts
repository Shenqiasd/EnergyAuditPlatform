/**
 * EA-CUST-053: Sheet 14 product-level split logic.
 *
 * Extracted from SpreadSheet/index.vue for testability.
 * Derives Sheet 14 product rows from Sheet 12 CONFIG_PREFILL products,
 * capped to the Sheet 12 tag range (A5:J20 → max 16 product slots).
 */

// ── Sheet 12 tag range constants ────────────────────────────────────────
/** 0-based start row of the product area in Sheet 12 */
export const SHEET12_PRODUCT_START_ROW = 4 // row 5 in 1-based

/** Maximum product rows the Sheet 12 CONFIG_PREFILL tag range allows (A5:J20) */
export const SHEET12_TAG_MAX_ROWS = 16

/** 0-based start row of the product area in Sheet 14 */
export const SHEET14_PRODUCT_AREA_START = 5 // row 6 in 1-based

// ── Types ───────────────────────────────────────────────────────────────

export interface ProductRecord {
  name?: string | null
  measurementUnit?: string | null
}

export interface DerivedProduct {
  name: string
  unit: string
  /** 1-based row number in Sheet 12 (for formula references) */
  sheet12Row: number
}

export interface Sheet14Op {
  type: 'setValue' | 'setFormula'
  row: number
  col: number
  value: string
}

export interface RowDelta {
  neededRows: number
  diff: number
  /** How many rows to insert (>0) or delete (<0), at which position */
  insertAt?: number
  deleteAt?: number
  deleteCount?: number
}

// ── Pure functions ──────────────────────────────────────────────────────

/**
 * Normalize a product unit string.
 * T / t / 吨 → 吨; empty → 吨; anything else kept as-is.
 */
export function normalizeProductUnit(unit: string): string {
  const lower = unit.toLowerCase().trim()
  if (lower === 't' || lower === '吨') return '吨'
  return unit.trim() || '吨'
}

/**
 * Derive the list of products that Sheet 14 should display,
 * capped to the Sheet 12 tag range max rows.
 */
export function deriveProducts(
  products: ProductRecord[],
  sheet12MaxRows: number = SHEET12_TAG_MAX_ROWS,
): DerivedProduct[] {
  const capped = products.slice(0, sheet12MaxRows)
  return capped
    .filter(p => p.name != null && String(p.name).trim() !== '')
    .map((p, i) => ({
      name: String(p.name ?? '').trim(),
      unit: normalizeProductUnit(String(p.measurementUnit ?? '')),
      sheet12Row: SHEET12_PRODUCT_START_ROW + 1 + i, // 1-based for SpreadJS formulas
    }))
}

/**
 * Compute how many rows to insert or delete in Sheet 14's product area.
 */
export function computeRowDelta(
  existingProductRows: number,
  productCount: number,
  productAreaStart: number = SHEET14_PRODUCT_AREA_START,
): RowDelta {
  const neededRows = productCount * 2 // 2 rows per product: 产量 + 单耗
  const diff = neededRows - existingProductRows
  const result: RowDelta = { neededRows, diff }
  if (diff > 0) {
    result.insertAt = productAreaStart + existingProductRows
  } else if (diff < 0) {
    result.deleteAt = productAreaStart + neededRows
    result.deleteCount = -diff
  }
  return result
}

/**
 * Generate the list of cell operations for Sheet 14 product rows.
 * Each operation is either a setValue or setFormula call.
 */
export function generateOps(
  products: DerivedProduct[],
  sheet12Name: string,
  productAreaStart: number = SHEET14_PRODUCT_AREA_START,
): Sheet14Op[] {
  const ops: Sheet14Op[] = []
  for (let i = 0; i < products.length; i++) {
    const { name, unit, sheet12Row } = products[i]
    const outputRow = productAreaStart + i * 2
    const consumptionRow = productAreaStart + i * 2 + 1

    // 产量 row: B = current output (G), C = base output (J)
    ops.push({ type: 'setValue', row: outputRow, col: 0, value: `${name}产量（${unit}）` })
    ops.push({ type: 'setFormula', row: outputRow, col: 1, value: `'${sheet12Name}'!G${sheet12Row}` })
    ops.push({ type: 'setFormula', row: outputRow, col: 2, value: `'${sheet12Name}'!J${sheet12Row}` })

    // 单耗 row: B = current unit consumption (E), C = base unit consumption (H)
    ops.push({ type: 'setValue', row: consumptionRow, col: 0, value: `${name}单耗（千克/${unit}）` })
    ops.push({ type: 'setFormula', row: consumptionRow, col: 1, value: `'${sheet12Name}'!E${sheet12Row}` })
    ops.push({ type: 'setFormula', row: consumptionRow, col: 2, value: `'${sheet12Name}'!H${sheet12Row}` })
  }
  return ops
}

/**
 * Find the 0-based row index of the "产值单耗" anchor in Sheet 14.
 * Returns -1 if not found.
 *
 * @param getCellValue  function(row, col) returning the cell value
 * @param rowCount      total row count of Sheet 14
 * @param searchStart   0-based row to start searching (default: SHEET14_PRODUCT_AREA_START)
 */
export function findAnchorRow(
  getCellValue: (row: number, col: number) => unknown,
  rowCount: number,
  searchStart: number = SHEET14_PRODUCT_AREA_START,
): number {
  const limit = Math.min(rowCount, searchStart + 100)
  for (let r = searchStart; r < limit; r++) {
    const val = getCellValue(r, 0)
    if (val && String(val).includes('产值单耗')) return r
  }
  return -1
}
