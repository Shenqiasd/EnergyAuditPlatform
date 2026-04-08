import request from '@/utils/request'

export interface AdminDashboardStats {
  enterpriseTotal: number
  enterpriseNewThisYear: number
  enterpriseNewLastYear: number
  pendingRegistrations: number
  activeAuditTasks: number
  overdueRectifications: number
  publishedTemplates: number
}

export interface AdminAuditOverview {
  totalEnterprises: number
  submittedEnterprises: number
  auditingEnterprises: number
  completedEnterprises: number
  reportedEnterprises: number
}

export interface RecentRegistration {
  id: number
  enterpriseName: string
  creditCode: string
  auditStatus: number
  applyTime: string
}

export interface RecentAuditLog {
  id: number
  action: string
  comment: string | null
  operationTime: string
  operatorName: string | null
  enterpriseName: string | null
}

export interface AdminActivity {
  recentRegistrations: RecentRegistration[]
  recentAuditLogs: RecentAuditLog[]
}

export function getAdminDashboardStats(): Promise<AdminDashboardStats> {
  return request.get('/admin/dashboard/stats')
}

export function getAdminAuditOverview(auditYear: number): Promise<AdminAuditOverview> {
  return request.get('/admin/dashboard/overview', { params: { auditYear } })
}

export function getAdminActivity(): Promise<AdminActivity> {
  return request.get('/admin/dashboard/activity')
}
