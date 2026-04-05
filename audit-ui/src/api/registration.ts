import request from '@/utils/request'

export interface Registration {
  id?: number
  enterpriseName: string
  creditCode: string
  contactPerson?: string
  contactEmail?: string
  contactPhone?: string
  applyNo?: string
  applyTime?: string
  auditStatus?: number
  auditUserId?: number
  auditTime?: string
  auditRemark?: string
  createTime?: string
}

export interface RegistrationQuery {
  enterpriseName?: string
  creditCode?: string
  auditStatus?: number
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  total: number
  rows: T[]
}

export function getList(params?: RegistrationQuery): Promise<PageResult<Registration>> {
  return request.get('/registration', { params })
}

export function getById(id: number): Promise<Registration> {
  return request.get(`/registration/${id}`)
}

export function submit(data: Partial<Registration>): Promise<void> {
  return request.post('/public/registration', data)
}

export function approve(id: number, auditRemark?: string): Promise<void> {
  return request.put(`/registration/${id}/approve`, { auditRemark })
}

export function reject(id: number, auditRemark?: string): Promise<void> {
  return request.put(`/registration/${id}/reject`, { auditRemark })
}
