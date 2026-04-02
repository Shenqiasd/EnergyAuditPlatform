import request from '@/utils/request'

export interface Enterprise {
  id?: number
  enterpriseName: string
  creditCode: string
  contactPerson?: string
  contactEmail?: string
  contactPhone?: string
  remark?: string
  expireDate?: string
  isLocked?: number
  isActive?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

export interface EnterpriseQuery {
  enterpriseName?: string
  creditCode?: string
  isLocked?: number
  isActive?: number
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  total: number
  list: T[]
}

export function getList(params?: EnterpriseQuery): Promise<PageResult<Enterprise>> {
  return request.get('/enterprise', { params })
}

export function getById(id: number): Promise<Enterprise> {
  return request.get(`/enterprise/${id}`)
}

export function create(data: Partial<Enterprise>): Promise<void> {
  return request.post('/enterprise', data)
}

export function update(id: number, data: Partial<Enterprise>): Promise<void> {
  return request.put(`/enterprise/${id}`, data)
}

export function remove(id: number): Promise<void> {
  return request.delete(`/enterprise/${id}`)
}

export function lock(id: number): Promise<void> {
  return request.put(`/enterprise/${id}/lock`)
}

export function unlock(id: number): Promise<void> {
  return request.put(`/enterprise/${id}/unlock`)
}

export function updateExpireDate(id: number, expireDate: string): Promise<void> {
  return request.put(`/enterprise/${id}/expire`, { expireDate })
}
