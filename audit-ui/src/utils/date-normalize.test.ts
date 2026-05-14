import { describe, it, expect } from 'vitest'
import { isValidDateString, normalizeDateValue } from './date-normalize'

// ─── isValidDateString ───────────────────────────────────────────────

describe('isValidDateString', () => {
  it('accepts valid yyyy-MM-dd', () => {
    expect(isValidDateString('2026-01-15')).toBe(true)
    expect(isValidDateString('2026-12-31')).toBe(true)
    expect(isValidDateString('2000-02-29')).toBe(true) // leap year
  })

  it('rejects invalid day-of-month', () => {
    expect(isValidDateString('2026-02-30')).toBe(false) // Feb has no 30th
    expect(isValidDateString('2026-02-29')).toBe(false) // 2026 not leap
    expect(isValidDateString('2026-04-31')).toBe(false) // April has 30 days
  })

  it('rejects impossible months', () => {
    expect(isValidDateString('2026-13-01')).toBe(false)
    expect(isValidDateString('2026-00-01')).toBe(false)
  })

  it('rejects non-date strings', () => {
    expect(isValidDateString('abc')).toBe(false)
    expect(isValidDateString('2026-1-5')).toBe(false) // no leading zeros
    expect(isValidDateString('2026/01/15')).toBe(false) // wrong separator
    expect(isValidDateString('')).toBe(false)
  })
})

// ─── normalizeDateValue ──────────────────────────────────────────────

describe('normalizeDateValue', () => {
  // ── Legal inputs → normalized yyyy-MM-dd ──
  describe('legal inputs', () => {
    it('returns null for null/undefined/empty', () => {
      expect(normalizeDateValue(null)).toBeNull()
      expect(normalizeDateValue(undefined)).toBeNull()
      expect(normalizeDateValue('')).toBeNull()
      expect(normalizeDateValue('   ')).toBeNull()
    })

    it('passes through valid yyyy-MM-dd unchanged', () => {
      expect(normalizeDateValue('2026-01-15')).toBe('2026-01-15')
      expect(normalizeDateValue('2026-12-31')).toBe('2026-12-31')
    })

    it('normalizes yyyy/MM/dd → yyyy-MM-dd', () => {
      expect(normalizeDateValue('2026/01/15')).toBe('2026-01-15')
    })

    it('normalizes yyyy.MM.dd → yyyy-MM-dd', () => {
      expect(normalizeDateValue('2026.01.15')).toBe('2026-01-15')
    })

    it('normalizes yyyyMMdd → yyyy-MM-dd', () => {
      expect(normalizeDateValue('20260115')).toBe('2026-01-15')
    })

    it('handles native Date objects from DateTimePicker', () => {
      const d = new Date(2026, 0, 15) // Jan 15 2026
      expect(normalizeDateValue(d)).toBe('2026-01-15')
    })

    it('handles OADate numbers (SpreadJS serial dates)', () => {
      // 2026-01-15 is OADate 46032
      // OADate epoch is 1899-12-30
      const jan15_2026 = Math.round(
        (new Date(2026, 0, 15).getTime() - new Date(1899, 11, 30).getTime()) / 86400000,
      )
      expect(normalizeDateValue(jan15_2026)).toBe('2026-01-15')
    })
  })

  // ── Illegal inputs → null (MUST NOT be normalized to a different valid date) ──
  describe('illegal inputs MUST return null', () => {
    it('rejects "2026-02-30" (Feb 30 does not exist)', () => {
      // Previously new Date('2026-02-30') rolled over to 2026-03-02
      expect(normalizeDateValue('2026-02-30')).toBeNull()
    })

    it('rejects "2026-99-99" (impossible month/day)', () => {
      expect(normalizeDateValue('2026-99-99')).toBeNull()
    })

    it('rejects "2026/13/40" (impossible month/day with slash)', () => {
      expect(normalizeDateValue('2026/13/40')).toBeNull()
    })

    it('rejects "2026-1-5" (no leading zeros)', () => {
      // This was previously accepted by the new Date(str) fallback
      expect(normalizeDateValue('2026-1-5')).toBeNull()
    })

    it('rejects "abc" (arbitrary text)', () => {
      expect(normalizeDateValue('abc')).toBeNull()
    })

    it('rejects pure Chinese text "测试日期"', () => {
      expect(normalizeDateValue('测试日期')).toBeNull()
    })

    it('rejects "January 15, 2026" (English natural language)', () => {
      expect(normalizeDateValue('January 15, 2026')).toBeNull()
    })

    it('rejects "2026-04-31" (April has only 30 days)', () => {
      expect(normalizeDateValue('2026-04-31')).toBeNull()
    })

    it('rejects "2025-02-29" (2025 is not a leap year)', () => {
      expect(normalizeDateValue('2025-02-29')).toBeNull()
    })

    it('rejects "20261399" (invalid compact format)', () => {
      expect(normalizeDateValue('20261399')).toBeNull()
    })

    it('rejects "2026.13.40" (invalid with dots)', () => {
      expect(normalizeDateValue('2026.13.40')).toBeNull()
    })

    it('rejects invalid Date object', () => {
      expect(normalizeDateValue(new Date('invalid'))).toBeNull()
    })
  })
})
