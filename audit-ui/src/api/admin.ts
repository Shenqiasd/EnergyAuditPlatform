import request from '@/utils/request'

// ============================================================
// Energy Catalog (全局能源品类管理 — admin only)
// ============================================================

export interface EnergyCatalog {
  id?: number
  name: string
  category?: string
  attribution?: string
  measurementUnit?: string
  equivalentValue?: number
  equalValue?: number
  lowHeatValue?: number
  carbonContent?: number
  oxidationRate?: number
  color?: string
  isActive?: number
  sortOrder?: number
  remark?: string
  createTime?: string
}

export interface EnergyCatalogQuery {
  name?: string
  category?: string
  isActive?: number
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  total: number
  rows: T[]
}

export function listEnergyCatalog(params?: EnergyCatalogQuery): Promise<PageResult<EnergyCatalog>> {
  return request.get('/admin/energy-catalog', { params })
}

export function getEnergyCatalogById(id: number): Promise<EnergyCatalog> {
  return request.get(`/admin/energy-catalog/${id}`)
}

export function createEnergyCatalog(data: Partial<EnergyCatalog>): Promise<void> {
  return request.post('/admin/energy-catalog', data)
}

export function updateEnergyCatalog(id: number, data: Partial<EnergyCatalog>): Promise<void> {
  return request.put(`/admin/energy-catalog/${id}`, data)
}

export function deleteEnergyCatalog(id: number): Promise<void> {
  return request.delete(`/admin/energy-catalog/${id}`)
}

// ============================================================
// Emission Factor (碳排放因子管理 — admin only)
// ============================================================

export interface EmissionFactor {
  id?: number
  factorName: string
  energyType?: string
  factorValue?: number
  measurementUnit?: string
  source?: string
  effectiveYear?: number
  status?: number
  remark?: string
  createTime?: string
}

export interface EmissionFactorQuery {
  factorName?: string
  energyType?: string
  effectiveYear?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export function listEmissionFactor(params?: EmissionFactorQuery): Promise<PageResult<EmissionFactor>> {
  return request.get('/admin/emission-factor', { params })
}

export function getEmissionFactorById(id: number): Promise<EmissionFactor> {
  return request.get(`/admin/emission-factor/${id}`)
}

export function createEmissionFactor(data: Partial<EmissionFactor>): Promise<void> {
  return request.post('/admin/emission-factor', data)
}

export function updateEmissionFactor(id: number, data: Partial<EmissionFactor>): Promise<void> {
  return request.put(`/admin/emission-factor/${id}`, data)
}

export function deleteEmissionFactor(id: number): Promise<void> {
  return request.delete(`/admin/emission-factor/${id}`)
}
