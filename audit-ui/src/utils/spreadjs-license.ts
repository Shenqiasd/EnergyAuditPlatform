let initialized = false

export function initSpreadJSLicense(): void {
  if (initialized) return
  if (typeof window.GC === 'undefined') return

  const sheetsKey = import.meta.env.VITE_SPREADJS_LICENSE
  const designerKey = import.meta.env.VITE_SPREADJS_DESIGNER_LICENSE

  if (!sheetsKey || !designerKey) {
    console.warn('SpreadJS license env vars are missing. Set VITE_SPREADJS_LICENSE and VITE_SPREADJS_DESIGNER_LICENSE before building.')
  }

  if (sheetsKey && window.GC.Spread?.Sheets) {
    window.GC.Spread.Sheets.LicenseKey = sheetsKey
  }
  if (designerKey && window.GC.Spread?.Sheets?.Designer) {
    window.GC.Spread.Sheets.Designer.LicenseKey = designerKey
  }

  initialized = true
}
