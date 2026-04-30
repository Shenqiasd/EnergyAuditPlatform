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
  getDataValidator(row: number, col: number): GCDataValidator | null
  addRows(row: number, count: number): void
  deleteRows(row: number, count: number): void
  getSelections(): Array<{ row: number; col: number; rowCount: number; colCount: number }>
  setRowVisible(row: number, visible: boolean): void
  getCustomName(name: string): GCSpreadNameInfo | null
  name(): string
  visible(): boolean
  zoom(factor: number): void
  getDefaultStyle(): GCDefaultStyle | null
  setDefaultStyle(style: GCDefaultStyle): void
  getStyle(row: number, col: number): GCStyle | null
  setStyle(row: number, col: number, style: GCStyle | null): void
  bind<A extends unknown[] = unknown[]>(eventType: string, handler: (...args: A) => void): void
}

export interface GCDefaultStyle {
  locked?: boolean
}

/**
 * Runtime representation of a SpreadJS cell style. Only the subset of
 * properties actually touched from TypeScript is declared here; the class
 * has many more fields (font, borders, alignment, etc.) that are consumed
 * only by the engine itself.
 */
export interface GCStyle {
  locked?: boolean
  backColor?: string | null
  clone?(): GCStyle
}

export interface GCStyleConstructor {
  new (): GCStyle
}

export interface GCSpreadWorkbook {
  fromJSON(json: object): void
  toJSON(): object
  getSheetCount(): number
  getSheet(index: number): GCSpreadSheet
  getActiveSheet(): GCSpreadSheet
  getActiveSheetIndex(): number
  setActiveSheet(name: string): void
  setActiveSheetIndex(index: number): void
  getCustomName(name: string): GCSpreadNameInfo | null
  commandManager(): GCSpreadCommandManager
  suspendPaint(): void
  resumePaint(): void
  suspendEvent(): void
  resumeEvent(): void
  suspendCalcService(): void
  resumeCalcService(options?: { doNotRecalculateAfterResume?: boolean }): void
  calculate(fullRebuild?: number): void
  repaint(): void
  destroy(): void
  options: GCSpreadWorkbookOptions & { tabStripVisible?: boolean }
  bind<A extends unknown[] = unknown[]>(eventType: string, handler: (...args: A) => void): void
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
  // V18 exposes the Designer license as a static property on the
  // constructor itself. V17 attaches it under `Designer.Designer.LicenseKey`,
  // which is handled via the `{ Designer; DefaultConfig }` union variant.
  LicenseKey: string
}

export interface GCSpreadSheetsWorkbookConstructor {
  new (host: HTMLElement, options?: object): GCSpreadWorkbook
}

export interface GCSpreadSheetsEvents {
  ValidationError: string
  ActiveSheetChanged: string
  CellChanged: string
  ViewZoomed: string
  ClipboardPasting?: string
  ClipboardPasted?: string
}

export interface GCSpreadSheets {
  Workbook: GCSpreadSheetsWorkbookConstructor
  Designer:
    | (GCSpreadDesignerConstructor & {
        Designer?: GCSpreadDesignerConstructor
        LicenseKey?: string
      })
    | {
        Designer: GCSpreadDesignerConstructor
        DefaultConfig: object
        LicenseKey?: string
      }
  DataValidation: GCDataValidation
  Comments: { Comment: GCCommentConstructor }
  Events: GCSpreadSheetsEvents
  LicenseKey: string
  Style: GCStyleConstructor
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
