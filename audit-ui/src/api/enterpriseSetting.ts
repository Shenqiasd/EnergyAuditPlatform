import request from '@/utils/request'

export interface EnterpriseSetting {
  id?: number
  enterpriseId?: number

  // 地区 / 行业
  region?: string
  industryField?: string
  industryName?: string
  unitNature?: string

  // 工商注册
  registeredDate?: string
  registeredCapital?: number

  // 法人 / 联系人
  legalRepresentative?: string
  legalPhone?: string
  isCentralEnterprise?: number
  groupName?: string

  // 地址 / 通讯
  enterpriseAddress?: string
  unitAddress?: string
  postalCode?: string
  adminDivisionCode?: string
  enterpriseEmail?: string
  fax?: string

  // 能源管理
  energyMgmtOrg?: string
  energyLeaderName?: string
  energyLeaderPhone?: string
  energyManagerName?: string
  energyManagerMobile?: string
  energyManagerCert?: string
  energyDeptLeaderPhone?: string

  // 能源认证
  energyCert?: number
  certPassDate?: string
  certAuthority?: string
  hasEnergyCenter?: number

  // 其他
  enterpriseContact?: string
  enterpriseMobile?: string
  compilerContact?: string
  compilerName?: string
  compilerMobile?: string
  compilerEmail?: string
  industryCategory?: string
  industryCode?: string
  superiorDepartment?: string
  energyEnterpriseType?: string
  energyUsageType?: string
  energyLeaderTitle?: string
  energyDeptName?: string
  energyAuditContactName?: string
  energyAuditContactPhone?: string
  remark?: string
}

/** Get enterprise setting for the current enterprise (returns null if not yet configured) */
export function getEnterpriseSetting(): Promise<EnterpriseSetting | null> {
  return request.get('/enterprise/setting')
}

/** Create or update enterprise setting (upsert) — strict, runs full validation */
export function upsertEnterpriseSetting(data: Partial<EnterpriseSetting>): Promise<void> {
  return request.put('/enterprise/setting', data)
}

/** Draft-save enterprise setting — partial data allowed, no mandatory-field validation */
export function draftSaveEnterpriseSetting(data: Partial<EnterpriseSetting>): Promise<void> {
  return request.put('/enterprise/setting/draft', data)
}

/** Get enterprise setting as flat field map for SpreadJS pre-fill (bidirectional sync) */
export function getEnterpriseSettingPrefill(): Promise<Record<string, unknown>> {
  return request.get('/enterprise/setting/prefill')
}

/** Config data for CONFIG_PREFILL tag mappings */
export interface ConfigPrefillData {
  bs_energy: Array<Record<string, unknown>>
  bs_product: Array<Record<string, unknown>>
  bs_unit: Array<Record<string, unknown>>
  [key: string]: Array<Record<string, unknown>>
}

/** Get all config data (energy types + products) for CONFIG_PREFILL — no pagination */
export function getConfigPrefillData(): Promise<ConfigPrefillData> {
  return request.get('/enterprise/setting/config-prefill-data')
}

/** Prerequisite check result */
export interface PrerequisiteCheckResult {
  passed: boolean
  errors: string[]
}

/** Check whether all prerequisite tables are completed before data entry / chart export */
export function checkPrerequisites(): Promise<PrerequisiteCheckResult> {
  return request.get('/enterprise/setting/prerequisite-check')
}
