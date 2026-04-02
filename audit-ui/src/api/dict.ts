import request from '@/utils/request'

export interface DictType {
  id?: number
  dictName: string
  dictType: string
  status?: number
  remark?: string
  createTime?: string
}

export interface DictData {
  id?: number
  dictType: string
  dictLabel: string
  dictValue: string
  dictSort?: number
  cssClass?: string
  status?: number
  remark?: string
}

export interface PageResult<T> {
  total: number
  list: T[]
}

export interface DictTypeQuery {
  dictName?: string
  dictType?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface DictDataQuery {
  dictType?: string
  dictLabel?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

// === Dict Type ===
export function listTypes(params?: DictTypeQuery): Promise<PageResult<DictType>> {
  return request.get('/system/dicts/type', { params })
}

export function getTypeById(id: number): Promise<DictType> {
  return request.get(`/system/dicts/type/${id}`)
}

export function createType(data: Partial<DictType>): Promise<void> {
  return request.post('/system/dicts/type', data)
}

export function updateType(id: number, data: Partial<DictType>): Promise<void> {
  return request.put(`/system/dicts/type/${id}`, data)
}

export function deleteType(id: number): Promise<void> {
  return request.delete(`/system/dicts/type/${id}`)
}

// === Dict Data ===
export function listData(params?: DictDataQuery): Promise<PageResult<DictData>> {
  return request.get('/system/dicts/data', { params })
}

export function getDataByType(dictType: string): Promise<DictData[]> {
  return request.get(`/system/dicts/data/${dictType}`)
}

export function createData(data: Partial<DictData>): Promise<void> {
  return request.post('/system/dicts/data', data)
}

export function updateData(id: number, data: Partial<DictData>): Promise<void> {
  return request.put(`/system/dicts/data/${id}`, data)
}

export function deleteData(id: number): Promise<void> {
  return request.delete(`/system/dicts/data/${id}`)
}
