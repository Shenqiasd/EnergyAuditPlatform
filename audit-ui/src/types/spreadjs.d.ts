/**
 * Minimal TypeScript declarations for GrapeCity SpreadJS loaded via CDN.
 * Only the surface used by SpreadDesigner is typed here.
 */

interface GCSpreadCommandManager {
  register(name: string, command: { execute: () => boolean; canUndo: boolean }): void
}

interface GCSpreadWorkbookOptions {
  allowUserEditFormula: boolean
}

interface GCSpreadSheet {
  options: {
    isProtected: boolean
  }
}

interface GCSpreadWorkbook {
  fromJSON(json: object): void
  toJSON(): object
  getSheetCount(): number
  getSheet(index: number): GCSpreadSheet
  commandManager(): GCSpreadCommandManager
  options: GCSpreadWorkbookOptions
}

interface GCSpreadDesigner {
  getWorkbook(): GCSpreadWorkbook
  destroy(): void
}

interface GCSpreadDesignerConstructor {
  new (
    host: HTMLElement,
    config: object | null,
    workbook: GCSpreadWorkbook | null
  ): GCSpreadDesigner
  DefaultConfig: object
}

interface GCSpreadDesignerNS {
  Designer: GCSpreadDesignerConstructor
}

interface GCSpreadSheets {
  Designer: GCSpreadDesignerNS
}

interface GCSpread {
  Sheets: GCSpreadSheets
}

interface GC {
  Spread: GCSpread
}

declare global {
  interface Window {
    GC: GC
  }
}

export {}
