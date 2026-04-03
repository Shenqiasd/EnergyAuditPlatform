declare const GC: any

export function initSpreadJSLicense(): void {
  const key = import.meta.env.VITE_SPREADJS_LICENSE
  if (key && typeof GC !== 'undefined' && GC.Spread?.Sheets) {
    GC.Spread.Sheets.LicenseKey = key
  }
}
