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
  reportHtml: string | null
  templateId: number | null
  submissionId: number | null
  flowChartPath: string | null
  reviewComment: string | null
  reviewerId: number | null
  createTime: string
  enterpriseName?: string
}

export interface ArReportTemplate {
  id: number
  templateName: string
  templateFilePath: string
  version: number
  status: number
  createTime: string
}

export const REPORT_STATUS_MAP: Record<number, { label: string; type: 'info' | 'warning' | 'success' | 'primary' | 'danger' }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '生成中', type: 'warning' },
  2: { label: '已生成', type: 'success' },
  3: { label: '生成失败', type: 'danger' },
  4: { label: '已提交审核', type: 'primary' },
  5: { label: '审核通过', type: 'success' },
  6: { label: '审核退回', type: 'danger' },
}

// Legacy code-based generation
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

// Template-based report generation (Phase 1)

/**
 * Generate a report from Word template using SpreadJS submission data.
 * The submission must have status=2 (approved) before generation is allowed.
 */
export function generateReportFromTemplate(submissionId: number, flowChartImage?: File) {
  const formData = new FormData()
  formData.append('submissionId', String(submissionId))
  if (flowChartImage) {
    formData.append('flowChartImage', flowChartImage)
  }
  // Let axios auto-detect Content-Type with correct boundary for FormData
  return request.post('/report/generate-from-template', formData)
}

/**
 * Save edited HTML content for a report (from TinyMCE editor).
 */
export function saveReportHtml(reportId: number, html: string) {
  return request.post(`/report/${reportId}/edit`, { html })
}

/**
 * Submit a report for auditor review.
 */
export function submitForReview(reportId: number) {
  return request.post(`/report/${reportId}/submit-for-review`)
}

/**
 * List available report templates.
 */
export function listTemplates() {
  return request.get('/report/templates')
}

// ====== Phase 4: Admin Report Template Management ======

export function uploadReportTemplate(file: File, templateName?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (templateName) {
    formData.append('templateName', templateName)
  }
  return request.post('/report/template/upload', formData)
}

export function getReportTemplate(id: number) {
  return request.get(`/report/template/${id}`)
}

export function activateReportTemplate(id: number) {
  return request.post(`/report/template/${id}/activate`)
}

export function deactivateReportTemplate(id: number) {
  return request.post(`/report/template/${id}/deactivate`)
}

export function deleteReportTemplate(id: number) {
  return request.delete(`/report/template/${id}`)
}

// ====== Phase 3: Report Review Workflow (auditor side) ======

/**
 * List reports for auditor review.
 * @param status filter by status (4=pending, 5=approved, 6=rejected), undefined=all
 * @param auditYear filter by audit year
 */
export function listReportsForReview(status?: number, auditYear?: number) {
  return request.get('/report/review/list', { params: { status, auditYear } })
}

/**
 * Get report detail for review (includes reportHtml).
 */
export function getReportForReview(id: number) {
  return request.get(`/report/review/${id}`)
}

/**
 * Approve a submitted report (status 4 -> 5).
 */
export function approveReport(id: number, reviewComment?: string) {
  return request.post(`/report/review/${id}/approve`, { reviewComment })
}

/**
 * Reject/return a submitted report (status 4 -> 6).
 */
export function rejectReport(id: number, reviewComment: string) {
  return request.post(`/report/review/${id}/reject`, { reviewComment })
}
