import { describe, expect, it } from 'vitest'
import {
  classifyFiveYearRow,
  classifyGhgRow,
  dedupByKey,
  sortByIdAsc,
  splitFiveYearRows,
  splitGhgRows,
} from './regulated-rows'

describe('sortByIdAsc', () => {
  it('returns rows in ascending id order (template extraction order)', () => {
    const out = sortByIdAsc([{ id: 30 }, { id: 10 }, { id: 20 }])
    expect(out.map((r) => r.id)).toEqual([10, 20, 30])
  })

  it('keeps rows without numeric id stable at the end', () => {
    const out = sortByIdAsc([{ id: 2 }, { id: null }, { id: 1 }, {}])
    expect(out.map((r) => r.id)).toEqual([1, 2, null, undefined])
  })
})

describe('dedupByKey', () => {
  it('keeps the first occurrence per composite key', () => {
    const out = dedupByKey(
      [
        { name: 'A', unit: 't', val: 1 },
        { name: 'A', unit: 't', val: 2 },
        { name: 'B', unit: 't', val: 3 },
      ],
      ['name', 'unit'],
    )
    expect(out.map((r) => r.val)).toEqual([1, 3])
  })

  it('preserves rows whose key fields are all empty (no dedup signal)', () => {
    const out = dedupByKey(
      [
        { name: '', unit: '', val: 1 },
        { name: '', unit: '', val: 2 },
      ],
      ['name', 'unit'],
    )
    expect(out).toHaveLength(2)
  })
})

describe('classifyGhgRow', () => {
  it('classifies fossil fuel detail rows by source_name + numeric fields', () => {
    expect(
      classifyGhgRow({ source_name: '原煤', low_heat_value: 20.9, carbon_content: 0.6 }),
    ).toBe('fossil')
  })

  it('honors emission_category over heuristics', () => {
    expect(classifyGhgRow({ emission_category: '生产过程排放', source_name: '石灰石' })).toBe('process')
    expect(classifyGhgRow({ emission_category: '净购入电力', source_name: '电' })).toBe('electric_heat')
    expect(classifyGhgRow({ emission_category: '化石燃料', source_name: '煤' })).toBe('fossil')
  })

  it('defaults to summary when no signals are present', () => {
    expect(classifyGhgRow({ emission_category: '合计', co2_emission: 1000 })).toBe('summary')
    expect(classifyGhgRow({})).toBe('summary')
  })
})

describe('splitGhgRows', () => {
  it('renders fixed sections in template order with deduplication', () => {
    const rows = [
      { id: 3, source_name: '原煤', low_heat_value: 20.9, co2_emission: 100 },
      { id: 2, source_name: '原煤', low_heat_value: 20.9, co2_emission: 100 }, // duplicate
      { id: 4, emission_category: '净购入电力', source_name: '电力', activity_data: 1000, co2_emission: 500 },
      { id: 5, emission_category: '生产过程', source_name: '石灰石', activity_data: 10, co2_emission: 5 },
      { id: 6, emission_category: '合计', co2_emission: 605 },
    ]
    const out = splitGhgRows(rows)
    expect(out.fossil).toHaveLength(1)
    expect(out.fossil[0].id).toBe(2)
    expect(out.electricHeat).toHaveLength(1)
    expect(out.process).toHaveLength(1)
    expect(out.summary).toHaveLength(1)
  })

  it('returns empty arrays for each section when given empty input', () => {
    const out = splitGhgRows([])
    expect(out.summary).toEqual([])
    expect(out.fossil).toEqual([])
    expect(out.electricHeat).toEqual([])
    expect(out.process).toEqual([])
  })
})

describe('classifyFiveYearRow', () => {
  it('classifies annual rows by year columns + target_name', () => {
    expect(
      classifyFiveYearRow({ target_name: '能耗下降率', measurement_unit: '%', y2026: 1.2 }),
    ).toBe('annual')
  })

  it('classifies product rows by product_name + indicator_name', () => {
    expect(
      classifyFiveYearRow({ product_name: '电石', indicator_name: '吨产品综合能耗' }),
    ).toBe('product')
  })

  it('classifies summary rows by wide-table scalar fields', () => {
    expect(
      classifyFiveYearRow({
        gross_output_actual2025: 1000,
        energy_equiv_actual2025: 500,
      }),
    ).toBe('summary')
  })

  it('honors explicit section_type', () => {
    expect(classifyFiveYearRow({ section_type: 'product', y2026: 1 })).toBe('product')
    expect(classifyFiveYearRow({ section_type: 'summary' })).toBe('summary')
  })
})

describe('splitFiveYearRows', () => {
  it('splits and dedupes annual rows by (target_name + measurement_unit)', () => {
    const rows = [
      { id: 5, target_name: '能耗下降率', measurement_unit: '%', y2026: 1.2 },
      { id: 3, target_name: '能耗下降率', measurement_unit: '%', y2026: 1.2 }, // dup
      { id: 4, target_name: '能耗下降率', measurement_unit: '吨标煤', y2026: 100 }, // diff unit
      { id: 2, product_name: '电石', indicator_name: '吨产品综合能耗', indicator_value: 1 },
      { id: 1, gross_output_actual2025: 999, energy_equiv_actual2025: 500 },
    ]
    const out = splitFiveYearRows(rows)
    expect(out.summary).toHaveLength(1)
    expect(out.product).toHaveLength(1)
    expect(out.annual).toHaveLength(2)
    expect(out.annual.map((r) => r.id)).toEqual([3, 4])
  })

  it('returns empty arrays for each section when given empty input', () => {
    const out = splitFiveYearRows([])
    expect(out.summary).toEqual([])
    expect(out.product).toEqual([])
    expect(out.annual).toEqual([])
  })
})
