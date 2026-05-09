import { describe, expect, it, vi } from 'vitest'

describe('initSpreadJSLicense', () => {
  it('injects both SpreadJS license keys from Vite env', async () => {
    vi.stubEnv('VITE_SPREADJS_LICENSE', 'test-sheets-license')
    vi.stubEnv('VITE_SPREADJS_DESIGNER_LICENSE', 'test-designer-license')
    vi.resetModules()

    ;(window as any).GC = {
      Spread: {
        Sheets: {
          LicenseKey: '',
          Designer: {
            LicenseKey: '',
          },
        },
      },
    }

    const { initSpreadJSLicense } = await import('./spreadjs-license')
    initSpreadJSLicense()

    expect((window as any).GC.Spread.Sheets.LicenseKey).toBe('test-sheets-license')
    expect((window as any).GC.Spread.Sheets.Designer.LicenseKey).toBe('test-designer-license')
  })

  it('warns when license env vars are missing without throwing', async () => {
    vi.resetModules()
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    ;(window as any).GC = { Spread: { Sheets: { LicenseKey: '', Designer: {} } } }

    const { initSpreadJSLicense } = await import('./spreadjs-license')

    expect(() => initSpreadJSLicense()).not.toThrow()
    expect(warn).toHaveBeenCalledWith(expect.stringContaining('SpreadJS license env vars are missing'))
  })
})
