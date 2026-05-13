import { describe, expect, it } from 'vitest'
import {
  classifyFiveYearRow,
  classifyGhgRow,
  dedupByKey,
  isSheet20FiveYearRow,
  sortByIdAsc,
  splitFiveYearRows,
  splitGhgRows,
} from './regulated-rows'

// ----- Real Sheet20 / Sheet21 mapping shapes -----
// Field shapes mirror sql/33-0427v2-template-tag-mappings.sql:
//   line 436: 表 20_产品碳峰 → product_output / gross_output / emission / ...
//   line 438: 表 20_年度目标 → target_name / measurement_unit / y2025..y2030
//   line 447: 表 21_产品单耗 → product_name / indicator_name / indicator_value / actual_value / target_name / year_label / y2030 / unit_energy_equal / decline_rate
//   line 449: 表 21_年度节能 → target_name / measurement_unit / y2026..y2030
const sheet20PeakRow = {
  year_label: '2025',
  product_output: 1200,
  gross_output: 5000,
  emission: 800,
  unit_strength: 0.16,
  intensity_drop: 12.5,
}
const sheet20AnnualRow = {
  target_name: '营业收入年均增长',
  measurement_unit: '%',
  y2025: 5,
  y2026: 6,
  y2027: 7,
  y2028: 8,
  y2029: 9,
  y2030: 10,
}
const sheet21ProductRow = {
  product_name: '电石',
  indicator_name: '单位能耗',
  indicator_value: 1.2,
  actual_value: 1.18,
  target_name: '电石',
  year_label: '单位能耗',
  y2030: 1.05,
  unit_energy_equal: 1.05,
  decline_rate: 8,
}
const sheet21AnnualRow = {
  target_name: '万元产值能耗下降率',
  measurement_unit: '%',
  y2026: 1.2,
  y2027: 2.4,
  y2028: 3.6,
  y2029: 4.8,
  y2030: 6.0,
}
const sheet21SummaryRow = {
  gross_output_actual2025: 12345,
  gross_output_target2030: 18000,
  energy_equiv_actual2025: 4500,
  energy_equiv_target2030: 5200,
  energy_equal_actual2025: 4600,
  energy_equal_target2030: 5300,
}

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

describe('isSheet20FiveYearRow', () => {
  it('flags Sheet20 产品碳峰 rows via Sheet20-only fields', () => {
    expect(isSheet20FiveYearRow(sheet20PeakRow)).toBe(true)
  })

  it('flags Sheet20 年度目标 rows via the y2025 column (Sheet21 年度节能 has y2026..y2030 only)', () => {
    expect(isSheet20FiveYearRow(sheet20AnnualRow)).toBe(true)
  })

  it('does NOT flag Sheet21 product / annual / summary rows', () => {
    expect(isSheet20FiveYearRow(sheet21ProductRow)).toBe(false)
    expect(isSheet20FiveYearRow(sheet21AnnualRow)).toBe(false)
    expect(isSheet20FiveYearRow(sheet21SummaryRow)).toBe(false)
  })

  it('honors explicit section_type = carbon_peak / contains "碳峰"', () => {
    expect(isSheet20FiveYearRow({ section_type: 'carbon_peak' })).toBe(true)
    expect(isSheet20FiveYearRow({ section_type: '产品碳峰' })).toBe(true)
  })
})

describe('classifyFiveYearRow', () => {
  it('classifies summary rows by Sheet21 wide-table scalar fields', () => {
    expect(classifyFiveYearRow(sheet21SummaryRow)).toBe('summary')
  })

  it('classifies Sheet21 产品单耗 rows as product even when they also carry target_name + y2030', () => {
    // sheet21ProductRow has target_name=“电石” AND y2030=1.05, which the old
    // annual-first ordering misrouted to annualRows. Product must win first.
    expect(classifyFiveYearRow(sheet21ProductRow)).toBe('product')
  })

  it('classifies Sheet21 年度节能 rows as annual', () => {
    expect(classifyFiveYearRow(sheet21AnnualRow)).toBe('annual')
  })

  it('rejects a misclassified-as-annual product row even by explicit field combinations', () => {
    // Sheet21 single-product row without indicator_value; still must be product.
    expect(
      classifyFiveYearRow({
        product_name: '生铁',
        indicator_name: '单位能耗',
        target_name: '生铁',
        y2030: 1.4,
      }),
    ).toBe('product')
  })

  it('honors explicit section_type override', () => {
    expect(classifyFiveYearRow({ section_type: 'product', y2026: 1 })).toBe('product')
    expect(classifyFiveYearRow({ section_type: 'summary' })).toBe('summary')
    expect(classifyFiveYearRow({ section_type: 'yearly', target_name: '能耗', y2030: 1 })).toBe('annual')
  })

  // ---- positive-signal-only guards ----

  it('returns null for Sheet20 年度目标 rows with only text labels and blank year columns', () => {
    // SpreadsheetDataExtractor preserves a row when any mapped cell is
    // non-null. So even when y2025..y2030 are all blank, a Sheet20 row may
    // still emit {year_label, target_name, measurement_unit} text. These
    // would have fallen through to summary under the old default-summary
    // policy; under positive-signal-only they classify as null and are
    // dropped by splitFiveYearRows.
    expect(classifyFiveYearRow({ year_label: '2025' })).toBeNull()
    expect(classifyFiveYearRow({ target_name: '某个指标' })).toBeNull()
    expect(
      classifyFiveYearRow({ target_name: '某个指标', measurement_unit: '%' }),
    ).toBeNull()
  })

  it('returns null for Sheet20 产品碳峰-shape rows with only text labels and blank numerics', () => {
    // Sheet20_产品碳峰 mapping (sql/33-0427v2-template-tag-mappings.sql:436)
    // writes year_label as text. If product_output / gross_output / emission
    // / unit_strength / intensity_drop are all blank, the row still surfaces
    // because year_label is non-null. Must be dropped.
    expect(classifyFiveYearRow({ year_label: '某产品' })).toBeNull()
  })

  it('returns null for completely unknown / irrelevant rows', () => {
    expect(classifyFiveYearRow({})).toBeNull()
    expect(classifyFiveYearRow({ foo: 'bar', bar: 1 })).toBeNull()
  })

  it('returns null for Sheet20 marker rows even with an inconsistent Sheet21 section_type', () => {
    // Defense in depth: if a row was somehow tagged section_type="summary"
    // but also has product_output (Sheet20 marker), the Sheet20 marker wins
    // and the row is dropped.
    expect(
      classifyFiveYearRow({ section_type: 'summary', product_output: 100 }),
    ).toBeNull()
    expect(
      classifyFiveYearRow({ section_type: '产品碳峰', target_name: 'foo', y2026: 1 }),
    ).toBeNull()
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

  it('excludes Sheet20 产品碳峰 + Sheet20 年度目标 rows from all sections', () => {
    // Real mapping mix: 2 Sheet20 rows + 3 Sheet21 rows. Only the Sheet21
    // rows should survive in any section.
    const rows = [
      { id: 10, ...sheet20PeakRow },
      { id: 11, ...sheet20AnnualRow },
      { id: 20, ...sheet21SummaryRow },
      { id: 21, ...sheet21ProductRow },
      { id: 22, ...sheet21AnnualRow },
    ]
    const out = splitFiveYearRows(rows)
    expect(out.summary).toHaveLength(1)
    expect(out.summary[0].id).toBe(20)
    expect(out.product).toHaveLength(1)
    expect(out.product[0].id).toBe(21)
    expect(out.annual).toHaveLength(1)
    expect(out.annual[0].id).toBe(22)
    const allIds = [...out.summary, ...out.product, ...out.annual].map((r) => r.id)
    expect(allIds).not.toContain(10) // Sheet20 产品碳峰
    expect(allIds).not.toContain(11) // Sheet20 年度目标
  })

  it('excludes Sheet20 rows even when their numeric columns are blank (text labels only)', () => {
    // Reviewer scenario: Sheet20 产品碳峰 / 年度目标 rows can emit
    // {year_label} or {target_name (+measurement_unit)} text with every
    // numeric cell blank. Under the previous default-to-summary policy,
    // these polluted the table17 summary section. Under positive-signal-only
    // they must NOT appear in any section.
    const rows = [
      // Sheet20 产品碳峰 row, only text label (mimics the customer screenshot)
      { id: 30, year_label: '产品名称' },
      // Sheet20 年度目标 row, only text labels
      { id: 31, target_name: '能耗下降率', measurement_unit: '%' },
      // Sheet20 年度目标 row, target_name only
      { id: 32, target_name: '某指标' },
      // Sheet21 总览 row (only Sheet21 row in the input)
      { id: 40, ...sheet21SummaryRow },
    ]
    const out = splitFiveYearRows(rows)
    expect(out.summary).toHaveLength(1)
    expect(out.summary[0].id).toBe(40)
    expect(out.product).toHaveLength(0)
    expect(out.annual).toHaveLength(0)
    const allIds = [...out.summary, ...out.product, ...out.annual].map((r) => r.id)
    expect(allIds).not.toContain(30)
    expect(allIds).not.toContain(31)
    expect(allIds).not.toContain(32)
  })

  it('preserves Sheet21 summary/product/annual rows in a mixed input alongside text-only Sheet20 rows', () => {
    // Comprehensive regression: even when the input is dominated by text-only
    // Sheet20 rows, every legit Sheet21 row still ends up in the correct
    // section.
    const rows = [
      { id: 50, year_label: '2025' }, // Sheet20 text-only 产品碳峰
      { id: 51, target_name: 'X', measurement_unit: '%' }, // Sheet20 text-only 年度目标
      { id: 52, ...sheet21SummaryRow },
      { id: 53, ...sheet21ProductRow },
      { id: 54, ...sheet21AnnualRow },
    ]
    const out = splitFiveYearRows(rows)
    expect(out.summary.map((r) => r.id)).toEqual([52])
    expect(out.product.map((r) => r.id)).toEqual([53])
    expect(out.annual.map((r) => r.id)).toEqual([54])
  })

  it('routes Sheet21 产品单耗 to product (not annual) even with target_name + y2030', () => {
    const rows = [
      { id: 100, ...sheet21ProductRow },
      { id: 101, ...sheet21AnnualRow },
    ]
    const out = splitFiveYearRows(rows)
    expect(out.product.map((r) => r.id)).toEqual([100])
    expect(out.annual.map((r) => r.id)).toEqual([101])
  })

  it('returns empty arrays for each section when given empty input', () => {
    const out = splitFiveYearRows([])
    expect(out.summary).toEqual([])
    expect(out.product).toEqual([])
    expect(out.annual).toEqual([])
  })
})
