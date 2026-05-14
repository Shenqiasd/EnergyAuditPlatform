import { describe, it, expect } from 'vitest'
import {
  ENERGY_USAGE_TYPE_OPTIONS,
  normalizeEnergyUsageType,
} from './enterprise-options'

describe('ENERGY_USAGE_TYPE_OPTIONS', () => {
  it('exposes the renamed option label (GRA-68)', () => {
    expect(ENERGY_USAGE_TYPE_OPTIONS).toContain('非能源加工转换企业')
    expect(ENERGY_USAGE_TYPE_OPTIONS).not.toContain('非能源加工转换行业')
  })
})

describe('normalizeEnergyUsageType', () => {
  it('migrates the legacy 非能源加工转换行业 label to the current option', () => {
    expect(normalizeEnergyUsageType('非能源加工转换行业')).toBe('非能源加工转换企业')
  })

  it('leaves current option labels untouched', () => {
    for (const label of ENERGY_USAGE_TYPE_OPTIONS) {
      expect(normalizeEnergyUsageType(label)).toBe(label)
    }
  })

  it('passes through null/undefined/empty without rewriting', () => {
    expect(normalizeEnergyUsageType(null)).toBeNull()
    expect(normalizeEnergyUsageType(undefined)).toBeUndefined()
    expect(normalizeEnergyUsageType('')).toBe('')
  })
})
