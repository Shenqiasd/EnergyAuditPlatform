import { describe, expect, it, vi } from 'vitest'

interface MockWS {
  '!merges'?: { s: { r: number; c: number }; e: { r: number; c: number } }[]
  __aoa?: unknown[][]
  __sheetName?: string
}

vi.mock('xlsx', () => {
  const utils = {
    aoa_to_sheet: vi.fn((aoa: unknown[][]) => {
      const ws: MockWS = { __aoa: aoa }
      return ws
    }),
    book_new: vi.fn(() => ({ SheetNames: [], Sheets: {} as Record<string, MockWS> })),
    book_append_sheet: vi.fn(
      (
        wb: { SheetNames: string[]; Sheets: Record<string, MockWS> },
        ws: MockWS,
        name: string,
      ) => {
        ws.__sheetName = name
        wb.SheetNames.push(name)
        wb.Sheets[name] = ws
      },
    ),
  }
  const writeFile = vi.fn()
  return { utils, writeFile, default: { utils, writeFile } }
})

describe('flattenColumns', () => {
  it('passes flat columns through unchanged', async () => {
    const { flattenColumns } = await import('./export')
    const out = flattenColumns([
      { prop: 'a', label: 'A' },
      { prop: 'b', label: 'B' },
    ])
    expect(out).toEqual([
      { prop: 'a', label: 'A' },
      { prop: 'b', label: 'B' },
    ])
  })

  it('drops group bands and keeps only leaf columns (used by non-grouped-export pages like 表 7)', async () => {
    const { flattenColumns } = await import('./export')
    const out = flattenColumns([
      { prop: 'name', label: '名称' },
      {
        prop: '_band',
        label: '分组',
        children: [
          { prop: 'a', label: 'A' },
          { prop: 'b', label: 'B' },
        ],
      },
      { prop: 'c', label: 'C' },
    ])
    expect(out).toEqual([
      { prop: 'name', label: '名称' },
      { prop: 'a', label: 'A' },
      { prop: 'b', label: 'B' },
      { prop: 'c', label: 'C' },
    ])
  })

  it('recursively flattens nested group bands', async () => {
    const { flattenColumns } = await import('./export')
    const out = flattenColumns([
      {
        prop: '_top',
        label: '顶层',
        children: [
          {
            prop: '_mid',
            label: '中层',
            children: [
              { prop: 'x', label: 'X' },
              { prop: 'y', label: 'Y' },
            ],
          },
        ],
      },
    ])
    expect(out).toEqual([
      { prop: 'x', label: 'X' },
      { prop: 'y', label: 'Y' },
    ])
  })
})

describe('exportTableToExcel', () => {
  it('flattens single-row headers when no children are provided', async () => {
    const xlsx: any = await import('xlsx')
    xlsx.utils.aoa_to_sheet.mockClear()
    xlsx.writeFile.mockClear()
    const { exportTableToExcel } = await import('./export')
    exportTableToExcel(
      [
        { prop: 'name', label: '名称' },
        { prop: 'value', label: '值' },
      ],
      [{ name: 'A', value: 1 }],
      '导出',
    )
    const aoa = xlsx.utils.aoa_to_sheet.mock.calls[0][0] as unknown[][]
    expect(aoa[0]).toEqual(['名称', '值'])
    expect(aoa[1]).toEqual(['A', 1])
    expect(xlsx.writeFile).toHaveBeenCalled()
  })

  it('emits multi-row grouped headers with merges for nested children (year-group export)', async () => {
    const xlsx: any = await import('xlsx')
    xlsx.utils.aoa_to_sheet.mockClear()
    xlsx.writeFile.mockClear()
    const { exportTableToExcel } = await import('./export')
    exportTableToExcel(
      [
        { prop: 'target_name', label: '目标名称' },
        {
          prop: '_years',
          label: '2026-2030年',
          children: [
            { prop: 'y2026', label: '2026年' },
            { prop: 'y2027', label: '2027年' },
          ],
        },
      ],
      [{ target_name: 'T1', y2026: 1, y2027: 2 }],
      '十五五年度',
    )
    const aoa = xlsx.utils.aoa_to_sheet.mock.calls[0][0] as unknown[][]
    // Two header rows: parent labels on row 0, leaf labels on row 1.
    expect(aoa[0]).toEqual(['目标名称', '2026-2030年', ''])
    expect(aoa[1]).toEqual(['', '2026年', '2027年'])
    expect(aoa[2]).toEqual(['T1', 1, 2])

    const sheet = (xlsx.utils.aoa_to_sheet as any).mock.results[0].value as MockWS
    // Expect a merge that spans columns 1..2 on the parent header row,
    // and a vertical merge for the leaf "目标名称" across both header rows.
    expect(sheet['!merges']).toContainEqual({
      s: { r: 0, c: 1 },
      e: { r: 0, c: 2 },
    })
    expect(sheet['!merges']).toContainEqual({
      s: { r: 0, c: 0 },
      e: { r: 1, c: 0 },
    })
  })
})
