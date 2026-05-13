import * as XLSX from 'xlsx'

export interface ExportColumn {
  prop: string
  label: string
  children?: ExportColumn[]
}

interface CellRange {
  s: { r: number; c: number }
  e: { r: number; c: number }
}

interface HeaderLayout {
  headerRows: string[][]
  leafColumns: ExportColumn[]
  merges: CellRange[]
}

function countLeaves(col: ExportColumn): number {
  if (!col.children || col.children.length === 0) return 1
  return col.children.reduce((sum, c) => sum + countLeaves(c), 0)
}

function buildHeaderLayout(columns: ExportColumn[]): HeaderLayout {
  let maxDepth = 1
  const depthOf = (col: ExportColumn, d: number): number => {
    if (!col.children || col.children.length === 0) return d
    return Math.max(...col.children.map((c) => depthOf(c, d + 1)))
  }
  for (const col of columns) {
    maxDepth = Math.max(maxDepth, depthOf(col, 1))
  }

  const headerRows: string[][] = Array.from({ length: maxDepth }, () => [])
  const leafColumns: ExportColumn[] = []
  const merges: CellRange[] = []

  const place = (col: ExportColumn, rowIdx: number, startCol: number): number => {
    const span = countLeaves(col)
    headerRows[rowIdx][startCol] = col.label
    if (span > 1) {
      merges.push({ s: { r: rowIdx, c: startCol }, e: { r: rowIdx, c: startCol + span - 1 } })
      // fill placeholder slots with empty strings so the row array is dense
      for (let i = 1; i < span; i++) {
        headerRows[rowIdx][startCol + i] = ''
      }
    }
    if (!col.children || col.children.length === 0) {
      if (rowIdx < maxDepth - 1) {
        // leaf column lives in a higher header row — span the remaining rows
        merges.push({
          s: { r: rowIdx, c: startCol },
          e: { r: maxDepth - 1, c: startCol },
        })
        for (let r = rowIdx + 1; r < maxDepth; r++) {
          headerRows[r][startCol] = ''
        }
      }
      leafColumns.push(col)
      return span
    }
    let offset = 0
    for (const child of col.children) {
      offset += place(child, rowIdx + 1, startCol + offset)
    }
    return span
  }

  let cursor = 0
  for (const col of columns) {
    cursor += place(col, 0, cursor)
  }

  return { headerRows, leafColumns, merges }
}

/**
 * Export a table to XLSX. Supports nested `children` in column definitions to
 * render multi-row grouped headers (e.g. "2025年实际 / 2030年目标" group bands
 * over their child columns), which is required for the regulated-chart
 * exports of table8 and table17.
 */
export function exportTableToExcel(
  columns: ExportColumn[],
  rows: Record<string, unknown>[],
  filename: string,
) {
  const { headerRows, leafColumns, merges } = buildHeaderLayout(columns)
  const dataRows = rows.map((row) => leafColumns.map((c) => row[c.prop] ?? ''))
  const sheetMatrix: (string | unknown)[][] = [...headerRows, ...dataRows]
  const ws = XLSX.utils.aoa_to_sheet(sheetMatrix)
  if (merges.length > 0) {
    ws['!merges'] = (ws['!merges'] || []).concat(merges)
  }
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, filename.substring(0, 31))
  XLSX.writeFile(wb, `${filename}.xlsx`)
}
