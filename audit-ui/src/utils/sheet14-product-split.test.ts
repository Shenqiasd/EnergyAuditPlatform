import { describe, expect, it } from 'vitest'
import {
  normalizeProductUnit,
  deriveProducts,
  deriveProductsFromSheetRows,
  resolveProductNameFromIndicator,
  computeRowDelta,
  generateOps,
  findAnchorRow,
  SHEET12_TAG_MAX_ROWS,
  SHEET14_PRODUCT_AREA_START,
  type ProductRecord,
} from './sheet14-product-split'

// ── normalizeProductUnit ────────────────────────────────────────────────

describe('normalizeProductUnit', () => {
  it('normalizes T to 吨', () => {
    expect(normalizeProductUnit('T')).toBe('吨')
  })

  it('normalizes lowercase t to 吨', () => {
    expect(normalizeProductUnit('t')).toBe('吨')
  })

  it('normalizes 吨 to 吨', () => {
    expect(normalizeProductUnit('吨')).toBe('吨')
  })

  it('keeps other units as-is', () => {
    expect(normalizeProductUnit('千克')).toBe('千克')
    expect(normalizeProductUnit('升')).toBe('升')
  })

  it('trims whitespace', () => {
    expect(normalizeProductUnit(' T ')).toBe('吨')
    expect(normalizeProductUnit('  千克  ')).toBe('千克')
  })

  it('defaults empty string to 吨', () => {
    expect(normalizeProductUnit('')).toBe('吨')
    expect(normalizeProductUnit('  ')).toBe('吨')
  })
})

// ── deriveProducts ──────────────────────────────────────────────────────

describe('deriveProducts', () => {
  it('derives two products with correct sheet12Row references', () => {
    const products: ProductRecord[] = [
      { name: '测试', measurementUnit: 'T' },
      { name: '测试02', measurementUnit: '吨' },
    ]
    const result = deriveProducts(products)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({ name: '测试', unit: '吨', sheet12Row: 5 })
    expect(result[1]).toEqual({ name: '测试02', unit: '吨', sheet12Row: 6 })
  })

  it('filters out products with empty names', () => {
    const products: ProductRecord[] = [
      { name: '钢', measurementUnit: '吨' },
      { name: '', measurementUnit: 'T' },
      { name: null, measurementUnit: 'T' },
      { name: '  ', measurementUnit: 'T' },
    ]
    const result = deriveProducts(products)

    expect(result).toHaveLength(1)
    expect(result[0].name).toBe('钢')
  })

  it('caps products to Sheet 12 tag range max rows (16)', () => {
    const products: ProductRecord[] = Array.from({ length: 20 }, (_, i) => ({
      name: `product-${i}`,
      measurementUnit: '吨',
    }))
    const result = deriveProducts(products)

    expect(result).toHaveLength(SHEET12_TAG_MAX_ROWS)
    expect(result[15].name).toBe('product-15')
  })

  it('caps to custom max rows', () => {
    const products: ProductRecord[] = Array.from({ length: 10 }, (_, i) => ({
      name: `p${i}`,
      measurementUnit: '吨',
    }))
    const result = deriveProducts(products, 5)
    expect(result).toHaveLength(5)
  })

  it('returns empty array for empty product list', () => {
    expect(deriveProducts([])).toEqual([])
  })

  it('returns empty array when all products have empty names', () => {
    const products: ProductRecord[] = [
      { name: '', measurementUnit: 'T' },
      { name: null, measurementUnit: '吨' },
    ]
    expect(deriveProducts(products)).toEqual([])
  })
})

describe('deriveProductsFromSheetRows', () => {
  it('preserves user-entered Sheet12 indicator labels by row', () => {
    const result = deriveProductsFromSheetRows([
      { indicatorName: '钢单产综合能耗', denominatorUnit: '吨' },
      { indicatorName: 'EA052RT-产品B单位产量综合能耗', denominatorUnit: '件' },
    ])

    expect(result).toEqual([
      { name: '钢', unit: '吨', sheet12Row: 5 },
      { name: 'EA052RT-产品B', unit: '件', sheet12Row: 6 },
    ])
  })

  it('falls back to product sequence only when the Sheet12 label is blank', () => {
    const result = deriveProductsFromSheetRows([
      { indicatorName: null, denominatorUnit: null },
      { indicatorName: '铝能耗', denominatorUnit: 'kg' },
    ])

    expect(result).toEqual([
      { name: '产品1', unit: '吨', sheet12Row: 5 },
      { name: '铝', unit: 'kg', sheet12Row: 6 },
    ])
  })
})

describe('resolveProductNameFromIndicator', () => {
  it('keeps the entered product prefix before common indicator suffixes', () => {
    expect(resolveProductNameFromIndicator('EA052RT-产品B单位产量综合能耗', 2)).toBe('EA052RT-产品B')
    expect(resolveProductNameFromIndicator('钢单产综合能耗', 1)).toBe('钢')
  })
})

// ── computeRowDelta ─────────────────────────────────────────────────────

describe('computeRowDelta', () => {
  it('inserts rows when more products than existing rows', () => {
    // 2 products → 4 needed rows, 0 existing → insert 4
    const delta = computeRowDelta(0, 2)
    expect(delta.neededRows).toBe(4)
    expect(delta.diff).toBe(4)
    expect(delta.insertAt).toBe(SHEET14_PRODUCT_AREA_START)
  })

  it('deletes rows when fewer products than existing rows', () => {
    // 1 product → 2 needed rows, 6 existing → delete 4
    const delta = computeRowDelta(6, 1)
    expect(delta.neededRows).toBe(2)
    expect(delta.diff).toBe(-4)
    expect(delta.deleteAt).toBe(SHEET14_PRODUCT_AREA_START + 2)
    expect(delta.deleteCount).toBe(4)
  })

  it('no change when product count matches', () => {
    const delta = computeRowDelta(4, 2)
    expect(delta.neededRows).toBe(4)
    expect(delta.diff).toBe(0)
    expect(delta.insertAt).toBeUndefined()
    expect(delta.deleteAt).toBeUndefined()
  })

  it('deletes all rows when products reduced to zero', () => {
    const delta = computeRowDelta(4, 0)
    expect(delta.neededRows).toBe(0)
    expect(delta.diff).toBe(-4)
    expect(delta.deleteAt).toBe(SHEET14_PRODUCT_AREA_START)
    expect(delta.deleteCount).toBe(4)
  })
})

// ── generateOps ─────────────────────────────────────────────────────────

describe('generateOps', () => {
  it('generates correct ops for two products', () => {
    const products = deriveProducts([
      { name: '测试', measurementUnit: 'T' },
      { name: '测试02', measurementUnit: '吨' },
    ])
    const ops = generateOps(products, '12.单位产品能耗数据')

    // 2 products × 2 rows × 3 cells = 12 ops
    expect(ops).toHaveLength(12)

    // Product 1 output row
    expect(ops[0]).toEqual({ type: 'setValue', row: 5, col: 0, value: '测试产量（吨）' })
    expect(ops[1]).toEqual({ type: 'setFormula', row: 5, col: 1, value: "'12.单位产品能耗数据'!G5" })
    expect(ops[2]).toEqual({ type: 'setFormula', row: 5, col: 2, value: "'12.单位产品能耗数据'!J5" })

    // Product 1 consumption row
    expect(ops[3]).toEqual({ type: 'setValue', row: 6, col: 0, value: '测试单耗（千克/吨）' })
    expect(ops[4]).toEqual({ type: 'setFormula', row: 6, col: 1, value: "'12.单位产品能耗数据'!E5" })
    expect(ops[5]).toEqual({ type: 'setFormula', row: 6, col: 2, value: "'12.单位产品能耗数据'!H5" })

    // Product 2 output row
    expect(ops[6]).toEqual({ type: 'setValue', row: 7, col: 0, value: '测试02产量（吨）' })
    expect(ops[7]).toEqual({ type: 'setFormula', row: 7, col: 1, value: "'12.单位产品能耗数据'!G6" })
    expect(ops[8]).toEqual({ type: 'setFormula', row: 7, col: 2, value: "'12.单位产品能耗数据'!J6" })

    // Product 2 consumption row
    expect(ops[9]).toEqual({ type: 'setValue', row: 8, col: 0, value: '测试02单耗（千克/吨）' })
    expect(ops[10]).toEqual({ type: 'setFormula', row: 8, col: 1, value: "'12.单位产品能耗数据'!E6" })
    expect(ops[11]).toEqual({ type: 'setFormula', row: 8, col: 2, value: "'12.单位产品能耗数据'!H6" })
  })

  it('generates 千克/吨 unit display for T unit product', () => {
    const products = deriveProducts([{ name: '钢', measurementUnit: 'T' }])
    const ops = generateOps(products, 'Sheet12')

    // consumption row label
    const consumptionLabel = ops.find(
      o => o.type === 'setValue' && o.row === SHEET14_PRODUCT_AREA_START + 1,
    )
    expect(consumptionLabel?.value).toBe('钢单耗（千克/吨）')
  })

  it('returns empty ops for empty product list', () => {
    expect(generateOps([], 'Sheet12')).toEqual([])
  })
})

// ── findAnchorRow ───────────────────────────────────────────────────────

describe('findAnchorRow', () => {
  it('finds the 产值单耗 anchor row', () => {
    const cells: Record<string, unknown> = {
      '5,0': '产品产量',
      '6,0': '产品单位',
      '7,0': '产值单耗',
    }
    const getValue = (r: number, c: number) => cells[`${r},${c}`]

    expect(findAnchorRow(getValue, 20)).toBe(7)
  })

  it('returns -1 when anchor is not found', () => {
    const getValue = () => null
    expect(findAnchorRow(getValue, 20)).toBe(-1)
  })

  it('finds anchor after product rows', () => {
    const cells: Record<string, unknown> = {
      '5,0': '钢产量（吨）',
      '6,0': '钢单耗（千克/吨）',
      '7,0': '铝产量（吨）',
      '8,0': '铝单耗（千克/吨）',
      '9,0': '产值单耗',
    }
    const getValue = (r: number, c: number) => cells[`${r},${c}`]

    expect(findAnchorRow(getValue, 20)).toBe(9)
  })
})

// ── Integration: full flow ──────────────────────────────────────────────

describe('Sheet14 product split full flow', () => {
  it('two products from Sheet 12 become Sheet 14 split rows with 千克/吨', () => {
    const products: ProductRecord[] = [
      { name: '测试', measurementUnit: 'T' },
      { name: '测试02', measurementUnit: '吨' },
    ]
    const derived = deriveProducts(products)
    const ops = generateOps(derived, '12.单位产品能耗数据')

    // Verify product count
    expect(derived).toHaveLength(2)

    // Verify all units normalized to 吨
    expect(derived.every(p => p.unit === '吨')).toBe(true)

    // Verify 千克/吨 in consumption labels
    const consumptionLabels = ops.filter(
      o => o.type === 'setValue' && o.col === 0 && o.value.includes('单耗'),
    )
    expect(consumptionLabels).toHaveLength(2)
    expect(consumptionLabels[0].value).toContain('千克/吨')
    expect(consumptionLabels[1].value).toContain('千克/吨')
  })

  it('product reduction removes stale rows', () => {
    // Start with 3 products → 6 existing rows
    const delta = computeRowDelta(6, 1)
    expect(delta.diff).toBe(-4)
    expect(delta.deleteCount).toBe(4)

    // Verify new ops only cover 1 product
    const derived = deriveProducts([{ name: '钢', measurementUnit: '吨' }])
    const ops = generateOps(derived, 'S12')
    expect(ops).toHaveLength(6) // 1 product × 2 rows × 3 cells
  })

  it('zero products cleans up all existing rows', () => {
    const derived = deriveProducts([])
    expect(derived).toHaveLength(0)

    const delta = computeRowDelta(4, 0) // had 4 rows, now 0
    expect(delta.neededRows).toBe(0)
    expect(delta.diff).toBe(-4)
    expect(delta.deleteAt).toBe(SHEET14_PRODUCT_AREA_START)
    expect(delta.deleteCount).toBe(4)

    const ops = generateOps(derived, 'S12')
    expect(ops).toEqual([])
  })

  it('products exceeding Sheet 12 range are capped', () => {
    const products: ProductRecord[] = Array.from({ length: 20 }, (_, i) => ({
      name: `prod-${i}`,
      measurementUnit: 'T',
    }))
    const derived = deriveProducts(products)
    expect(derived).toHaveLength(SHEET12_TAG_MAX_ROWS) // 16, not 20

    // Formula references should not exceed Sheet 12 row 20 (1-based)
    const lastProduct = derived[derived.length - 1]
    expect(lastProduct.sheet12Row).toBe(20) // row 4 + 1 + 15 = 20
  })
})
