/**
 * Fields in ent_enterprise_setting stored as INTEGER 0/1 but displayed as 是/否 in SpreadJS.
 */
const BOOL_DISPLAY_FIELDS: ReadonlySet<string> = new Set(['energyCert'])

/**
 * Convert a boolean-like integer field value to its Chinese display string.
 * Only applies to fields listed in BOOL_DISPLAY_FIELDS; other values pass through unchanged.
 */
export function convertBoolFieldForDisplay(fieldName: string, value: unknown): unknown {
  if (!BOOL_DISPLAY_FIELDS.has(fieldName)) return value
  if (value === 1 || value === '1') return '是'
  if (value === 0 || value === '0') return '否'
  return value
}
