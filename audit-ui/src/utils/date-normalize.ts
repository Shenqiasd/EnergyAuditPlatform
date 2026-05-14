/**
 * EA-CUST-041: Strict date normalization utilities for SpreadJS date fields.
 *
 * Extracted from SpreadSheet/index.vue so they can be unit-tested independently.
 * The component imports these functions; they are the single source of truth for
 * what constitutes a "legal date value" in the application.
 */

/** Strict yyyy-MM-dd pattern (leading zeros required) */
const DATE_REGEX = /^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/

/** Validate a string is a legal yyyy-MM-dd date (regex + day-of-month check). */
export function isValidDateString(s: string): boolean {
  if (!DATE_REGEX.test(s)) return false
  const [y, m, d] = s.split('-').map(Number)
  // Use JS Date constructor to verify the day actually exists in that month/year
  const dt = new Date(y, m - 1, d)
  return dt.getFullYear() === y && dt.getMonth() === m - 1 && dt.getDate() === d
}

/**
 * Try to parse a date input into yyyy-MM-dd using STRICT format matching only.
 *
 * Accepted inputs:
 *   - Native Date object (from SpreadJS DateTimePicker)
 *   - OADate number (SpreadJS internal serial date)
 *   - String in format yyyy-MM-dd (already canonical)
 *   - String in format yyyy/MM/dd or yyyy.MM.dd (separator normalized)
 *   - String in format yyyyMMdd (compact)
 *
 * All other strings (including loose formats like "2026-1-5", invalid dates
 * like "2026-02-30", or arbitrary text like "abc") are rejected as null.
 *
 * NO new Date(str) / Date.parse fallback — that would silently roll over
 * invalid dates (e.g. Feb 30 → Mar 2) and violate the "no illegal persistence" rule.
 */
export function normalizeDateValue(raw: unknown): string | null {
  if (raw == null || raw === '') return null

  // Native Date object (SpreadJS DateTimePicker may inject this)
  if (raw instanceof Date) {
    if (!isNaN(raw.getTime())) {
      const y = raw.getFullYear()
      const m = String(raw.getMonth() + 1).padStart(2, '0')
      const d = String(raw.getDate()).padStart(2, '0')
      const result = `${y}-${m}-${d}`
      if (isValidDateString(result)) return result
    }
    return null
  }

  // OADate number (SpreadJS internal) → JS Date
  if (typeof raw === 'number') {
    // SpreadJS OADate epoch: 1899-12-30
    const epoch = new Date(1899, 11, 30)
    const ms = epoch.getTime() + raw * 86400000
    const dt = new Date(ms)
    if (!isNaN(dt.getTime())) {
      const y = dt.getFullYear()
      const m = String(dt.getMonth() + 1).padStart(2, '0')
      const d = String(dt.getDate()).padStart(2, '0')
      const result = `${y}-${m}-${d}`
      if (isValidDateString(result)) return result
    }
    return null
  }

  const str = String(raw).trim()
  if (str === '') return null

  // Already yyyy-MM-dd
  if (isValidDateString(str)) return str

  // Try yyyy/MM/dd or yyyy.MM.dd (normalize separator then strict-check)
  const slashDot = str.replace(/[/.]/g, '-')
  if (isValidDateString(slashDot)) return slashDot

  // Try yyyyMMdd (compact format)
  if (/^\d{8}$/.test(str)) {
    const candidate = `${str.slice(0, 4)}-${str.slice(4, 6)}-${str.slice(6, 8)}`
    if (isValidDateString(candidate)) return candidate
  }

  // All other strings are invalid — no Date.parse / new Date(str) fallback
  return null
}
