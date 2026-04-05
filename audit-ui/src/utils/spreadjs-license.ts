let initialized = false

export function initSpreadJSLicense(): void {
  if (initialized) return
  if (typeof window.GC === 'undefined') return

  const sheetsKey = import.meta.env.VITE_SPREADJS_LICENSE
  if (!sheetsKey) {
    console.warn('[SpreadJS] VITE_SPREADJS_LICENSE not set — running in trial mode')
    return
  }
  if (window.GC.Spread?.Sheets) {
    window.GC.Spread.Sheets.LicenseKey = sheetsKey
  }

  const designerKey = import.meta.env.VITE_SPREADJS_DESIGNER_LICENSE
  if (designerKey && window.GC.Spread?.Sheets?.Designer) {
    window.GC.Spread.Sheets.Designer.LicenseKey = designerKey
  }

  initialized = true
}
