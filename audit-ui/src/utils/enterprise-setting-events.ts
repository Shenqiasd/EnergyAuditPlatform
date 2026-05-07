const ENTERPRISE_SETTING_UPDATED_EVENT = 'enterprise-setting-updated'
const ENTERPRISE_SETTING_UPDATED_STORAGE_KEY = 'enterprise_setting_updated_at'

export function notifyEnterpriseSettingUpdated() {
  const timestamp = String(Date.now())
  window.dispatchEvent(new CustomEvent(ENTERPRISE_SETTING_UPDATED_EVENT))
  try {
    localStorage.setItem(ENTERPRISE_SETTING_UPDATED_STORAGE_KEY, timestamp)
  } catch {
    // ignore storage quota / privacy mode failures
  }
}

export function onEnterpriseSettingUpdated(callback: () => void): () => void {
  const onCustomEvent = () => callback()
  const onStorageEvent = (event: StorageEvent) => {
    if (event.key === ENTERPRISE_SETTING_UPDATED_STORAGE_KEY) {
      callback()
    }
  }

  window.addEventListener(ENTERPRISE_SETTING_UPDATED_EVENT, onCustomEvent)
  window.addEventListener('storage', onStorageEvent)

  return () => {
    window.removeEventListener(ENTERPRISE_SETTING_UPDATED_EVENT, onCustomEvent)
    window.removeEventListener('storage', onStorageEvent)
  }
}
