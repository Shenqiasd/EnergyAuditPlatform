import request from '@/utils/request'

export interface LoginForm {
  username: string
  password: string
  portal?: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  realName: string
  userType: number
  enterpriseId?: number
  enterpriseName?: string
  passwordChanged: boolean
}

export interface UserInfoResult {
  userId: number
  username: string
  realName: string
  phone?: string
  email?: string
  userType: number
  enterpriseId?: number
  enterpriseName?: string
  auditYear?: number
  passwordChanged: boolean
}

export function login(data: LoginForm): Promise<LoginResult> {
  return request.post('/auth/login', data)
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

export function getUserInfo(): Promise<UserInfoResult> {
  return request.get('/auth/info')
}

export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
  return request.put('/auth/password', data)
}
