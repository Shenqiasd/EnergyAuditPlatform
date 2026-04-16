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
  getRowCount(): number
  getColumnCount(): number
}

export interface GCDataValidator {
  inCellDropdown(value: boolean): void
  showInputMessage(value: boolean): void
  inputTitle(title: string): void
  inputMessage(msg: string): void
  showErrorMessage(value: boolean): void
  errorTitle(title: string): void
  errorMessage(msg: string): void
  errorStyle(style: number): void
  errorStyle(): number
}

export interface GCDataValidation {
  createListValidator(list: string): GCDataValidator
}

export interface GCCellRange {
  locked(value: boolean): void
  locked(): boolean
  backColor(value: string | null): void
  backColor(): string | null
  comment(value: GCComment | null): void
  comment(): GCComment | null
  value(): unknown
}

export interface GCComment {
  text(value: string): void
  text(): string
}

export interface GCCommentConstructor {
  new (): GCComment
}

export interface GCRange {
  locked(value: boolean): void
}

export interface GCSpreadSheetProtectionOptions {
  allowSelectLockedCells?: boolean
  allowSelectUnlockedCells?: boolean
  allowResizeRows?: boolean
  allowResizeColumns?: boolean
  allowEditObjects?: boolean
  allowDragInsertRows?: boolean
  allowDragInsertColumns?: boolean
  allowInsertRows?: boolean
  allowInsertColumns?: boolean
  allowDeleteRows?: boolean
  allowDeleteColumns?: boolean
  allowSort?: boolean
  allowFilter?: boolean
}

export interface GCSpreadSheet {
  options: {
    isProtected: boolean
    protectionOptions: GCSpreadSheetProtectionOptions
  }
  getRowCount(): number
  getColumnCount(): number
  getColumnWidth(col: number): number
  getTag(row: number, col: number): unknown
  getValue(row: number, col: number): unknown
  setValue(row: number, col: number, value: unknown): void
  getCell(row: number, col: number): GCCellRange
  getRange(row: number, col: number, rowCount: number, colCount: number): GCRange
  setDataValidator(row: number, col: number, validator: GCDataValidator): void
  setRowVisible(row: number, visible: boolean): void
  getCustomName(name: string): GCSpreadNameInfo | null
  name(): string
  visible(): boolean
  zoom(factor: number): void
  getDefaultStyle(): GCDefaultStyle | null
  setDefaultStyle(style: GCDefaultStyle): void
  bind(eventType: string, handler: (...args: unknown[]) => void): void
}

export interface GCDefaultStyle {
  locked?: boolean
}

export interface GCSpreadWorkbook {
  fromJSON(json: object): void
  toJSON(): object
  getSheetCount(): number
  getSheet(index: number): GCSpreadSheet
  getActiveSheetIndex(): number
  setActiveSheet(index: number): void
  getCustomName(name: string): GCSpreadNameInfo | null
  commandManager(): GCSpreadCommandManager
  suspendPaint(): void
  resumePaint(): void
  repaint(): void
  destroy(): void
  options: GCSpreadWorkbookOptions & { tabStripVisible?: boolean }
  bind(eventType: string, handler: (...args: unknown[]) => void): void
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

export interface GCSpreadSheetsEvents {
  ValidationError: string
  ActiveSheetChanged: string
  CellChanged: string
  ViewZoomed: string
}

export interface GCSpreadSheets {
  Workbook: GCSpreadSheetsWorkbookConstructor
  Designer: GCSpreadDesignerConstructor | { Designer: GCSpreadDesignerConstructor; DefaultConfig: object }
  DataValidation: GCDataValidation
  Comments: { Comment: GCCommentConstructor }
  Events: GCSpreadSheetsEvents
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
