/**
 * Minimal TypeScript declarations for GrapeCity SpreadJS loaded via CDN.
 * Only the surface used by SpreadDesigner is typed here.
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

export interface GCSpreadSheet {
  options: {
    isProtected: boolean
  }
}

export interface GCSpreadWorkbook {
  fromJSON(json: object): void
  toJSON(): object
  getSheetCount(): number
  getSheet(index: number): GCSpreadSheet
  commandManager(): GCSpreadCommandManager
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

export interface GCSpreadDesignerNS {
  Designer: GCSpreadDesignerConstructor
  DefaultConfig: object
}

export interface GCSpreadSheetsWorkbookConstructor {
  new (host: HTMLElement, options?: object): GCSpreadWorkbook
}

export interface GCSpreadSheets {
  Workbook: GCSpreadSheetsWorkbookConstructor
  Designer: GCSpreadDesignerNS
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
