import { describe, it, expect } from 'vitest'
import {
  INDUSTRY_CLASSIFICATION,
  buildIndustryLookup,
  normalizeIndustryCode,
  resolveIndustryPath,
} from './industry-classification'

describe('INDUSTRY_CLASSIFICATION (EA-CUST-045)', () => {
  it('contains exactly the 34 majors from the customer attachment (codes 13-46)', () => {
    expect(INDUSTRY_CLASSIFICATION).toHaveLength(34)
    const codes = INDUSTRY_CLASSIFICATION.map((m) => m.value)
    expect(codes[0]).toBe('13')
    expect(codes[codes.length - 1]).toBe('46')
    // No GB/T 4754 门类 letter-prefixed codes leak into the cascader.
    for (const code of codes) {
      expect(code).toMatch(/^\d{2}$/)
    }
  })

  it('contains exactly 165 middle categories', () => {
    const total = INDUSTRY_CLASSIFICATION.reduce(
      (sum, major) => sum + (major.children?.length ?? 0),
      0,
    )
    expect(total).toBe(165)
  })

  it('parents middle codes by their leading two digits', () => {
    for (const major of INDUSTRY_CLASSIFICATION) {
      for (const middle of major.children ?? []) {
        expect(middle.value.startsWith(major.value)).toBe(true)
      }
    }
  })

  it('includes representative middle 281 纤维素纤维原料及纤维制造 under major 28', () => {
    const major = INDUSTRY_CLASSIFICATION.find((m) => m.value === '28')
    expect(major).toBeDefined()
    const middle = major!.children!.find((c) => c.value === '281')
    expect(middle?.label).toBe('281 纤维素纤维原料及纤维制造')
  })
})

describe('normalizeIndustryCode', () => {
  it('strips a legacy 门类 letter prefix', () => {
    expect(normalizeIndustryCode('C281')).toBe('281')
    expect(normalizeIndustryCode('D441')).toBe('441')
  })

  it('leaves a bare numeric code untouched', () => {
    expect(normalizeIndustryCode('281')).toBe('281')
    expect(normalizeIndustryCode('44')).toBe('44')
  })

  it('returns an empty string for null/undefined/empty input', () => {
    expect(normalizeIndustryCode(null)).toBe('')
    expect(normalizeIndustryCode(undefined)).toBe('')
    expect(normalizeIndustryCode('')).toBe('')
  })
})

describe('buildIndustryLookup', () => {
  it('resolves a middle code to its full cascader path', () => {
    const lookup = buildIndustryLookup()
    const entry = lookup.get('281')
    expect(entry).toBeDefined()
    expect(entry!.fullPath).toEqual(['28', '281'])
  })

  it('does not expose legacy prefixed codes — callers must normalize first', () => {
    const lookup = buildIndustryLookup()
    expect(lookup.has('C281')).toBe(false)
    expect(lookup.get(normalizeIndustryCode('C281'))?.fullPath).toEqual(['28', '281'])
  })
})

describe('resolveIndustryPath', () => {
  it('returns a leaf path for a stored middle code', () => {
    expect(resolveIndustryPath('281')).toEqual(['28', '281'])
  })

  it('strips a legacy 门类 letter prefix on a middle code', () => {
    expect(resolveIndustryPath('C281')).toEqual(['28', '281'])
  })

  it('auto-promotes legacy single-leaf majors (C16/C21/C31/C42/D46) to their sole 中类 child', () => {
    expect(resolveIndustryPath('C16')).toEqual(['16', '160'])
    expect(resolveIndustryPath('C21')).toEqual(['21', '210'])
    expect(resolveIndustryPath('C31')).toEqual(['31', '310'])
    expect(resolveIndustryPath('C42')).toEqual(['42', '420'])
    expect(resolveIndustryPath('D46')).toEqual(['46', '460'])
  })

  it('does not promote a major that has multiple middle children', () => {
    // 28 has 3 middles (281/282/283), so a stored bare `28` must NOT be
    // auto-promoted because we cannot pick the user's intended 中类.
    expect(resolveIndustryPath('28')).toEqual(['28'])
  })

  it('returns an empty path for an unknown / empty code', () => {
    expect(resolveIndustryPath('999')).toEqual([])
    expect(resolveIndustryPath('')).toEqual([])
    expect(resolveIndustryPath(null)).toEqual([])
    expect(resolveIndustryPath(undefined)).toEqual([])
  })
})
