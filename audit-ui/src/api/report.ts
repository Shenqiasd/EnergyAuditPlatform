import request from '@/utils/request'

export interface ArReport {
  id: number
  enterpriseId: number
  auditYear: number
  reportName: string
  reportType: number
  status: number
  generatedFilePath: string | null
  uploadedFilePath: string | null
  generateTime: string | null
  submitTime: string | null
  createTime: string
  enterpriseName?: string
}

export const REPORT_STATUS_MAP: Record<number, { label: string; type: 'info' | 'warning' | 'success' | 'primary' | 'danger' }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '生成中', type: 'warning' },
  2: { label: '已生成', type: 'success' },
  3: { label: '已上传', type: 'primary' },
  4: { label: '已提交', type: 'danger' },
}

export function generateReport(auditYear: number) {
  return request.post('/report/generate', null, { params: { auditYear } })
}

export function listReports(auditYear?: number) {
  return request.get('/report/list', { params: { auditYear } })
}

export function getReportDetail(id: number) {
  return request.get(`/report/${id}`)
}

export function downloadReport(id: number) {
  return request.get(`/report/${id}/download`, { responseType: 'blob' })
}
