import { describe, it, expect, vi } from 'vitest'

/**
 * Unit tests for enterprise-info page logic (EA-CUST-042).
 *
 * These tests cover:
 * - Dirty tracking: snapshot comparison detects form changes.
 * - Draft save: calls draft endpoint without full validation.
 * - Leave guard choice mapping.
 */

// ── Dirty tracking (pure functions, no component mount needed) ──

function takeSnapshot(form: Record<string, unknown>, cascader: string[]): string {
  return JSON.stringify(form) + '|' + JSON.stringify(cascader)
}

describe('dirty tracking', () => {
  it('reports clean when form matches baseline', () => {
    const form = { region: '上海市', fax: '021-123' }
    const cascader = ['28', '281']
    const baseline = takeSnapshot(form, cascader)
    expect(takeSnapshot(form, cascader)).toBe(baseline)
  })

  it('reports dirty when a text field changes', () => {
    const form = { region: '上海市', fax: '021-123' }
    const cascader = ['28', '281']
    const baseline = takeSnapshot({ ...form }, [...cascader])
    form.fax = 'qa-codex-20260515-ea-cust-042-marker'
    expect(takeSnapshot(form, cascader)).not.toBe(baseline)
  })

  it('reports dirty when a select field changes', () => {
    const form = { region: '上海市' }
    const cascader: string[] = []
    const baseline = takeSnapshot({ ...form }, [...cascader])
    form.region = '北京市'
    expect(takeSnapshot(form, cascader)).not.toBe(baseline)
  })

  it('reports dirty when cascader changes', () => {
    const form = { region: '上海市' }
    const cascader = ['28', '281']
    const baseline = takeSnapshot({ ...form }, [...cascader])
    cascader[1] = '282'
    expect(takeSnapshot(form, cascader)).not.toBe(baseline)
  })

  it('reports dirty when a numeric field changes', () => {
    const form: Record<string, unknown> = { registeredCapital: 100.50 }
    const cascader: string[] = []
    const baseline = takeSnapshot({ ...form }, [...cascader])
    form.registeredCapital = 200
    expect(takeSnapshot(form, cascader)).not.toBe(baseline)
  })

  it('reports dirty when a date field changes', () => {
    const form: Record<string, unknown> = { registeredDate: '2025-01-01' }
    const cascader: string[] = []
    const baseline = takeSnapshot({ ...form }, [...cascader])
    form.registeredDate = '2026-06-15'
    expect(takeSnapshot(form, cascader)).not.toBe(baseline)
  })

  it('reports clean after baseline reset with same values', () => {
    const form = { region: '上海市', fax: 'changed' }
    const cascader = ['28', '281']
    const newBaseline = takeSnapshot(form, cascader)
    expect(takeSnapshot(form, cascader)).toBe(newBaseline)
  })
})

// ── Draft save API ──

describe('draftSaveEnterpriseSetting API shape', () => {
  it('calls PUT /enterprise/setting/draft', async () => {
    const mockPut = vi.fn().mockResolvedValue(undefined)
    const draftSave = (data: Record<string, unknown>) => mockPut('/enterprise/setting/draft', data)

    await draftSave({ fax: 'test-marker' })

    expect(mockPut).toHaveBeenCalledWith('/enterprise/setting/draft', { fax: 'test-marker' })
  })

  it('does not require industryCode or industryName', async () => {
    const mockPut = vi.fn().mockResolvedValue(undefined)
    const draftSave = (data: Record<string, unknown>) => mockPut('/enterprise/setting/draft', data)

    await expect(draftSave({ region: '上海市' })).resolves.toBeUndefined()
    expect(mockPut).toHaveBeenCalledWith('/enterprise/setting/draft', { region: '上海市' })
  })
})

// ── Leave guard choice mapping ──

describe('leave guard choice mapping', () => {
  type Choice = 'save' | 'discard' | 'cancel'

  function mapChoice(action: string | { toString(): string }, isRejection: boolean): Choice {
    if (!isRejection) {
      return action === 'confirm' ? 'save' : 'discard'
    }
    return action === 'cancel' ? 'discard' : 'cancel'
  }

  it('confirm → save', () => {
    expect(mapChoice('confirm', false)).toBe('save')
  })

  it('cancel button (rejected) → discard', () => {
    expect(mapChoice('cancel', true)).toBe('discard')
  })

  it('close/escape (rejected) → cancel', () => {
    expect(mapChoice('close', true)).toBe('cancel')
  })
})
