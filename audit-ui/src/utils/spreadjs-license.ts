let initialized = false

export function initSpreadJSLicense(): void {
  if (initialized) return
  const key = import.meta.env.VITE_SPREADJS_LICENSE
  if (key && typeof window.GC !== 'undefined' && window.GC?.Spread?.Sheets) {
    window.GC.Spread.Sheets.LicenseKey = key
    initialized = true
  }
}
