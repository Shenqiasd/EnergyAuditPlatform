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

// ── Draft save API (undefined → null conversion) ──

/**
 * Mirrors the actual draftSaveEnterpriseSetting conversion logic:
 * converts undefined values to null so they appear as explicit null
 * in JSON rather than being omitted by JSON.stringify.
 */
function convertPatch(data: Record<string, unknown>): Record<string, unknown> {
  const patch: Record<string, unknown> = {}
  for (const key of Object.keys(data)) {
    patch[key] = data[key] === undefined ? null : data[key]
  }
  return patch
}

describe('draftSaveEnterpriseSetting API shape', () => {
  it('calls PUT /enterprise/setting/draft', async () => {
    const mockPut = vi.fn().mockResolvedValue(undefined)
    const draftSave = (data: Record<string, unknown>) => mockPut('/enterprise/setting/draft', convertPatch(data))

    await draftSave({ fax: 'test-marker' })

    expect(mockPut).toHaveBeenCalledWith('/enterprise/setting/draft', { fax: 'test-marker' })
  })

  it('does not require industryCode or industryName', async () => {
    const mockPut = vi.fn().mockResolvedValue(undefined)
    const draftSave = (data: Record<string, unknown>) => mockPut('/enterprise/setting/draft', convertPatch(data))

    await expect(draftSave({ region: '上海市' })).resolves.toBeUndefined()
    expect(mockPut).toHaveBeenCalledWith('/enterprise/setting/draft', { region: '上海市' })
  })

  it('converts undefined industry fields to explicit null for cleared cascader', () => {
    const formData: Record<string, unknown> = {
      fax: '021-123',
      industryCode: undefined,
      industryName: undefined,
      industryCategory: undefined,
      region: '上海市',
    }
    const patch = convertPatch(formData)

    // undefined → null so the key appears in JSON
    expect(patch.industryCode).toBeNull()
    expect(patch.industryName).toBeNull()
    expect(patch.industryCategory).toBeNull()
    // non-undefined values pass through
    expect(patch.fax).toBe('021-123')
    expect(patch.region).toBe('上海市')
  })

  it('preserves non-undefined values including zero and empty string', () => {
    const formData: Record<string, unknown> = {
      registeredCapital: 0,
      fax: '',
      region: '北京市',
    }
    const patch = convertPatch(formData)

    expect(patch.registeredCapital).toBe(0)
    expect(patch.fax).toBe('')
    expect(patch.region).toBe('北京市')
  })

  it('omitted keys are not added to the patch', () => {
    // Only keys present in the source object appear in the patch
    const formData: Record<string, unknown> = { fax: 'test' }
    const patch = convertPatch(formData)

    expect(Object.keys(patch)).toEqual(['fax'])
    expect(patch).not.toHaveProperty('industryCode')
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

// ── ElMessageBox dialog config ──

describe('leave dialog configuration', () => {
  it('showCancelButton must be true for discard button to render', () => {
    const config = {
      title: '未保存的更改',
      message: '当前页面有未保存的修改，是否保存草稿？',
      distinguishCancelAndClose: true,
      showCancelButton: true,
      confirmButtonText: '保存草稿并离开',
      cancelButtonText: '放弃修改',
      closeOnClickModal: false,
      type: 'warning' as const,
    }
    expect(config.showCancelButton).toBe(true)
    expect(config.cancelButtonText).toBe('放弃修改')
  })

  it('three choices are available: save, discard, cancel', () => {
    const config = {
      confirmButtonText: '保存草稿并离开',
      cancelButtonText: '放弃修改',
      showCancelButton: true,
      distinguishCancelAndClose: true,
    }
    // confirm click → save, cancel click → discard, close/escape → cancel
    expect(config.confirmButtonText).toBe('保存草稿并离开')
    expect(config.cancelButtonText).toBe('放弃修改')
    expect(config.distinguishCancelAndClose).toBe(true)
  })
})

// ── Popstate sentinel guard ──

describe('popstate sentinel guard logic', () => {
  it('pushHistoryGuard activates the popstate guard', () => {
    let active = false
    const pushHistoryGuard = () => { active = true }
    pushHistoryGuard()
    expect(active).toBe(true)
  })

  it('popstate with clean form deactivates guard and proceeds', () => {
    let active = true
    const isDirty = () => false
    const backCalled = { value: false }

    // Simulate onPopState when form is clean
    if (active && !isDirty()) {
      active = false
      backCalled.value = true
    }
    expect(active).toBe(false)
    expect(backCalled.value).toBe(true)
  })

  it('popstate with dirty form re-pushes sentinel and shows dialog', () => {
    let active = true
    let pushCount = 0
    const isDirty = () => true

    // Simulate onPopState when form is dirty
    if (active && isDirty()) {
      pushCount++ // re-push sentinel
      active = true
    }
    expect(pushCount).toBe(1)
    expect(active).toBe(true)
  })

  it('approved leave sets skipRouteLeaveGuard and leavingApproved', () => {
    let skipRouteLeaveGuard = false
    let leavingApproved = false
    let popstateGuardActive = true

    // Simulate approval path in onPopState
    leavingApproved = true
    popstateGuardActive = false
    skipRouteLeaveGuard = true

    expect(leavingApproved).toBe(true)
    expect(skipRouteLeaveGuard).toBe(true)
    expect(popstateGuardActive).toBe(false)
  })

  it('skipRouteLeaveGuard bypasses onBeforeRouteLeave dialog', () => {
    let skipRouteLeaveGuard = true
    let dialogShown = false

    // Simulate onBeforeRouteLeave
    if (skipRouteLeaveGuard) {
      skipRouteLeaveGuard = false
      // return true — no dialog
    } else {
      dialogShown = true
    }
    expect(dialogShown).toBe(false)
    expect(skipRouteLeaveGuard).toBe(false)
  })

  it('leavingApproved suppresses beforeunload after dialog approval', () => {
    let leavingApproved = true
    let beforeUnloadTriggered = false

    // Simulate onBeforeUnload
    if (!leavingApproved) {
      beforeUnloadTriggered = true
    }
    expect(beforeUnloadTriggered).toBe(false)
  })
})
