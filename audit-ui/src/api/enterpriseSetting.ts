import request from '@/utils/request'

export interface EnterpriseSetting {
  id?: number
  enterpriseId?: number
  enterpriseAddress?: string
  unitAddress?: string
  postalCode?: string
  fax?: string
  legalRepresentative?: string
  legalPhone?: string
  enterpriseContact?: string
  enterpriseMobile?: string
  enterpriseEmail?: string
  compilerContact?: string
  compilerName?: string
  compilerMobile?: string
  compilerEmail?: string
  energyCert?: number
  certAuthority?: string
  certPassDate?: string
  registeredCapital?: number
  registeredDate?: string
  industryCategory?: string
  industryCode?: string
  industryName?: string
  superiorDepartment?: string
  unitNature?: string
  energyEnterpriseType?: string
  remark?: string
}

/** Get enterprise setting for the current enterprise (returns null if not yet configured) */
export function getEnterpriseSetting(): Promise<EnterpriseSetting | null> {
  return request.get('/enterprise/setting')
}

/** Create or update enterprise setting (upsert) */
export function upsertEnterpriseSetting(data: Partial<EnterpriseSetting>): Promise<void> {
  return request.put('/enterprise/setting', data)
}
