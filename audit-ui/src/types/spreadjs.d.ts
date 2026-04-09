/**
 * Minimal TypeScript declarations for GrapeCity SpreadJS loaded via CDN.
 * Supports both V17 (Designer.Designer pattern) and V18 (Designer as constructor).
 *
 * This file is a module (contains export {}), so all interfaces are explicitly
 * exported for use in other files. The Window augmentation is in declare global.
 */

export interface GCSpreadCommandManager {
  register(name: string, command: { execute: () => boolean; canUndo: boolean }): void
}

export interface GCSpreadWorkbookOptions {
  allowUserEditFormula: boolean
}

export interface GCSpreadNameInfo {
  getRow(): number
  getColumn(): number
}

export interface GCSpreadSheet {
  options: {
    isProtected: boolean
  }
  getRowCount(): number
  getColumnCount(): number
  getTag(row: number, col: number): unknown
  setValue(row: number, col: number, value: unknown): void
  getCustomName(name: string): GCSpreadNameInfo | null
}

export interface GCSpreadWorkbook {
  fromJSON(json: object): void
  toJSON(): object
  getSheetCount(): number
  getSheet(index: number): GCSpreadSheet
  getCustomName(name: string): GCSpreadNameInfo | null
  commandManager(): GCSpreadCommandManager
  suspendPaint(): void
  resumePaint(): void
  destroy(): void
  options: GCSpreadWorkbookOptions
}

export interface GCSpreadDesigner {
  getWorkbook(): GCSpreadWorkbook
  destroy(): void
}

export interface GCSpreadDesignerConstructor {
  new (
    host: HTMLElement,
    config: object | null,
    workbook: GCSpreadWorkbook | null
  ): GCSpreadDesigner
  DefaultConfig: object
}

export interface GCSpreadSheetsWorkbookConstructor {
  new (host: HTMLElement, options?: object): GCSpreadWorkbook
}

export interface GCSpreadSheets {
  Workbook: GCSpreadSheetsWorkbookConstructor
  Designer: GCSpreadDesignerConstructor | { Designer: GCSpreadDesignerConstructor; DefaultConfig: object }
  LicenseKey: string
}

export interface GCSpread {
  Sheets: GCSpreadSheets
}

export interface GC {
  Spread: GCSpread
}

declare global {
  interface Window {
    GC: GC
  }
}
