import request from '@/utils/request'

export interface AuditTask {
  id?: number
  enterpriseId?: number
  auditYear?: number
  taskType?: number
  taskTitle?: string
  status?: number
  assigneeId?: number
  assignTime?: string
  deadline?: string
  completeTime?: string
  result?: string
  createBy?: string
  createTime?: string
  updateTime?: string
  enterpriseName?: string
  assigneeName?: string
}

export interface AuditLog {
  id?: number
  taskId?: number
  operatorId?: number
  action?: string
  comment?: string
  operationTime?: string
  operatorName?: string
}

export const AUDIT_STATUS_MAP: Record<number, { label: string; type: 'info' | 'warning' | 'primary' | 'success' | 'danger' }> = {
  0: { label: '待审核', type: 'info' },
  1: { label: '审核中', type: 'primary' },
  2: { label: '已通过', type: 'success' },
  3: { label: '已退回', type: 'danger' },
  4: { label: '已完成', type: 'success' },
}

export const ACTION_LABEL_MAP: Record<string, string> = {
  SUBMIT: '提交审核',
  RESUBMIT: '重新提交',
  ASSIGN: '分配审核员',
  AUTO_ASSIGN: '自动分配',
  APPROVE: '审核通过',
  REJECT: '审核退回',
  COMMENT: '添加评论',
  ADD_RECTIFICATION: '添加整改要求',
  UPDATE_RECTIFICATION: '更新整改进度',
  ACCEPT_RECTIFICATION: '验收整改项',
  OVERDUE_DETECTED: '超期检测',
}

export function submitForAudit(auditYear: number): Promise<AuditTask> {
  return request.post('/audit/task/submit', null, { params: { auditYear } })
}

export function getMyAuditStatus(auditYear: number): Promise<AuditTask | null> {
  return request.get('/audit/task/my-status', { params: { auditYear } })
}

export function getAuditTaskList(params?: {
  enterpriseId?: number
  auditYear?: number
  status?: number
}): Promise<AuditTask[]> {
  return request.get('/audit/task/list', { params })
}

export function getAuditTask(id: number): Promise<AuditTask> {
  return request.get(`/audit/task/${id}`)
}

export function getAuditCounts(): Promise<Record<string, number>> {
  return request.get('/audit/task/counts')
}

export function assignAuditTask(id: number, assigneeId: number): Promise<void> {
  return request.post(`/audit/task/${id}/assign`, null, { params: { assigneeId } })
}

export function approveAuditTask(id: number, comment?: string): Promise<void> {
  return request.post(`/audit/task/${id}/approve`, { comment })
}

export function rejectAuditTask(id: number, comment: string): Promise<void> {
  return request.post(`/audit/task/${id}/reject`, { comment })
}

export function addAuditComment(id: number, comment: string): Promise<void> {
  return request.post(`/audit/task/${id}/comment`, { comment })
}

export function getAuditLogs(id: number): Promise<AuditLog[]> {
  return request.get(`/audit/task/${id}/logs`)
}

export interface EnterpriseInfo {
  enterpriseName?: string
  creditCode?: string
  contactPerson?: string
  contactPhone?: string
  contactEmail?: string
  enterpriseAddress?: string
  legalRepresentative?: string
  industryCategory?: string
  industryName?: string
  unitNature?: string
  energyEnterpriseType?: string
  registeredCapital?: number
}

export function getTaskEnterpriseInfo(taskId: number): Promise<EnterpriseInfo> {
  return request.get(`/audit/task/${taskId}/enterprise-info`)
}
