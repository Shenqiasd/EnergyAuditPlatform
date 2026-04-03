let initialized = false

export function initSpreadJSLicense(): void {
  if (initialized) return
  const key = import.meta.env.VITE_SPREADJS_LICENSE
  if (!key) {
    console.warn('[SpreadJS] VITE_SPREADJS_LICENSE not set — running in trial mode')
    return
  }
  if (window.GC?.Spread?.Sheets) {
    window.GC.Spread.Sheets.LicenseKey = key
    initialized = true
  }
}
