import request from '@/utils/request'

export interface RectificationItem {
  id?: number
  taskId?: number
  enterpriseId?: number
  auditYear?: number
  itemName?: string
  requirement?: string
  status?: number
  deadline?: string
  completeTime?: string
  result?: string
  createBy?: string
  createTime?: string
  updateTime?: string
  enterpriseName?: string
  taskTitle?: string
}

export const RECTIFICATION_STATUS_MAP: Record<number, { label: string; type: 'info' | 'warning' | 'primary' | 'success' | 'danger' }> = {
  0: { label: '未启动', type: 'info' },
  1: { label: '进行中', type: 'primary' },
  2: { label: '已完成', type: 'success' },
  3: { label: '超期', type: 'danger' },
}

export function createRectificationItems(taskId: number, items: Partial<RectificationItem>[]): Promise<void> {
  return request.post('/audit/rectification/create', items, { params: { taskId } })
}

export function getRectificationList(taskId: number): Promise<RectificationItem[]> {
  return request.get('/audit/rectification/list', { params: { taskId } })
}

export function getMyRectificationList(): Promise<RectificationItem[]> {
  return request.get('/audit/rectification/my-list')
}

export function getOverdueCount(taskId: number): Promise<{ count: number }> {
  return request.get('/audit/rectification/overdue-count', { params: { taskId } })
}

export function getOverdueCounts(taskIds: number[]): Promise<Record<number, number>> {
  return request.post('/audit/rectification/overdue-counts', taskIds)
}

export function updateRectificationProgress(id: number, status: number, result?: string): Promise<void> {
  return request.post(`/audit/rectification/${id}/update-progress`, { status, result })
}

export function acceptRectificationItem(id: number): Promise<void> {
  return request.post(`/audit/rectification/${id}/accept`)
}
