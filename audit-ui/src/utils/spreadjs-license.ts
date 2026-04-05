const GC = (window as any).GC

export function initSpreadJSLicense(): void {
  if (!GC?.Spread?.Sheets) return

  const sheetsKey = import.meta.env.VITE_SPREADJS_LICENSE
  if (sheetsKey) {
    GC.Spread.Sheets.LicenseKey = sheetsKey
  }

  const designerKey = import.meta.env.VITE_SPREADJS_DESIGNER_LICENSE
  if (designerKey && GC.Spread.Sheets.Designer) {
    GC.Spread.Sheets.Designer.LicenseKey = designerKey
  }
}
