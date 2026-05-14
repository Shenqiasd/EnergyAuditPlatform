import { describe, it, expect } from 'vitest'
import { convertBoolFieldForDisplay } from './bool-display'

describe('convertBoolFieldForDisplay', () => {
  it('should convert energyCert=1 to 是', () => {
    expect(convertBoolFieldForDisplay('energyCert', 1)).toBe('是')
  })

  it('should convert energyCert=0 to 否', () => {
    expect(convertBoolFieldForDisplay('energyCert', 0)).toBe('否')
  })

  it('should convert energyCert="1" (string) to 是', () => {
    expect(convertBoolFieldForDisplay('energyCert', '1')).toBe('是')
  })

  it('should convert energyCert="0" (string) to 否', () => {
    expect(convertBoolFieldForDisplay('energyCert', '0')).toBe('否')
  })

  it('should pass through null/undefined unchanged for energyCert', () => {
    expect(convertBoolFieldForDisplay('energyCert', null)).toBeNull()
    expect(convertBoolFieldForDisplay('energyCert', undefined)).toBeUndefined()
  })

  it('should not convert non-boolean fields', () => {
    expect(convertBoolFieldForDisplay('enterpriseContact', 1)).toBe(1)
    expect(convertBoolFieldForDisplay('compilerName', 0)).toBe(0)
  })
})
