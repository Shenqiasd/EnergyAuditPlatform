import request from '@/utils/request'

export interface SysUser {
  id?: number
  username: string
  password?: string
  realName?: string
  phone?: string
  email?: string
  userType?: number
  roleId?: number
  enterpriseId?: number
  status?: number
  lastLoginTime?: string
  passwordChanged?: number
  createTime?: string
}

export interface UserQuery {
  username?: string
  realName?: string
  userType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface PageResult<T> {
  total: number
  list: T[]
}

export function listUsers(params?: UserQuery): Promise<PageResult<SysUser>> {
  return request.get('/system/users', { params })
}

export function getUserById(id: number): Promise<SysUser> {
  return request.get(`/system/users/${id}`)
}

export function createUser(data: Partial<SysUser>): Promise<void> {
  return request.post('/system/users', data)
}

export function updateUser(id: number, data: Partial<SysUser>): Promise<void> {
  return request.put(`/system/users/${id}`, data)
}

export function deleteUser(id: number): Promise<void> {
  return request.delete(`/system/users/${id}`)
}

export function resetPassword(id: number, password: string): Promise<void> {
  return request.put(`/system/users/${id}/reset-password`, { password })
}

export function updateStatus(id: number, status: number): Promise<void> {
  return request.put(`/system/users/${id}/status`, { status })
}
