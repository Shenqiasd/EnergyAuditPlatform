import request from '@/utils/request'

export interface PageResult<T> {
  total: number
  rows: T[]
}

// ============================================================
// Energy (企业能源品种 3.2)
// ============================================================

export interface BsEnergy {
  id?: number
  enterpriseId?: number
  name: string
  category?: string
  measurementUnit?: string
  equivalentValue?: number
  equalValue?: number
  lowHeatValue?: number
  carbonContent?: number
  oxidationRate?: number
  color?: string
  isActive?: number
  remark?: string
}

export function getEnergyList(params?: Record<string, unknown>): Promise<PageResult<BsEnergy>> {
  return request.get('/setting/energy', { params })
}

export function createEnergy(data: Partial<BsEnergy>): Promise<void> {
  return request.post('/setting/energy', data)
}

export function updateEnergy(id: number, data: Partial<BsEnergy>): Promise<void> {
  return request.put(`/setting/energy/${id}`, data)
}

export function removeEnergy(id: number): Promise<void> {
  return request.delete(`/setting/energy/${id}`)
}

/** Import energy types from global catalog (batch) */
export function importFromCatalog(catalogIds: number[]): Promise<void> {
  return request.post('/setting/energy/import-from-catalog', { catalogIds })
}

// ============================================================
// Unit (用能单元 3.3)
// ============================================================

export interface BsUnit {
  id?: number
  enterpriseId?: number
  name: string
  unitType: number
  subCategory?: string
  remark?: string
}

export interface BsUnitEnergy {
  unitId?: number
  energyId: number
  energyName?: string
  measurementUnit?: string
}

export function getUnitList(params?: Record<string, unknown>): Promise<PageResult<BsUnit>> {
  return request.get('/setting/unit', { params })
}

export function createUnit(data: Partial<BsUnit>): Promise<void> {
  return request.post('/setting/unit', data)
}

export function updateUnit(id: number, data: Partial<BsUnit>): Promise<void> {
  return request.put(`/setting/unit/${id}`, data)
}

export function removeUnit(id: number): Promise<void> {
  return request.delete(`/setting/unit/${id}`)
}

export function getUnitEnergies(unitId: number): Promise<BsUnitEnergy[]> {
  return request.get(`/setting/unit/${unitId}/energies`)
}

export function addUnitEnergy(unitId: number, energyId: number): Promise<void> {
  return request.post(`/setting/unit/${unitId}/energies/${energyId}`)
}

export function removeUnitEnergy(unitId: number, energyId: number): Promise<void> {
  return request.delete(`/setting/unit/${unitId}/energies/${energyId}`)
}

// ============================================================
// Product (产品 3.4)
// ============================================================

export interface BsProduct {
  id?: number
  enterpriseId?: number
  name: string
  measurementUnit?: string
  unitPrice?: number
  remark?: string
}

export function getProductList(params?: Record<string, unknown>): Promise<PageResult<BsProduct>> {
  return request.get('/setting/product', { params })
}

export function createProduct(data: Partial<BsProduct>): Promise<void> {
  return request.post('/setting/product', data)
}

export function updateProduct(id: number, data: Partial<BsProduct>): Promise<void> {
  return request.put(`/setting/product/${id}`, data)
}

export function removeProduct(id: number): Promise<void> {
  return request.delete(`/setting/product/${id}`)
}
