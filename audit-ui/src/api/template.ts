import request from '@/utils/request'
import type { PageResult } from '@/api/setting'

export interface TplTemplate {
  id?: number
  templateCode?: string
  templateName?: string
  moduleType?: string
  description?: string
  currentVersion?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface TplTemplateVersion {
  id?: number
  templateId?: number
  version?: number
  templateJson?: string
  changeLog?: string
  published?: number
  publishTime?: string
  protectionEnabled?: number
  createTime?: string
}

export interface TplTagMapping {
  id?: number
  templateVersionId?: number
  tagName?: string
  fieldName?: string
  targetTable?: string
  dataType?: string
  dictType?: string
  required?: number
  sheetIndex?: number
  sheetName?: string
  cellRange?: string
  mappingType?: string
  sourceType?: string
  rowKeyColumn?: number
  columnMappings?: string
  headerRow?: number
  remark?: string
}

export interface ColumnMappingItem {
  col: number
  field: string
  type: string
}

export interface TplSubmission {
  id?: number
  enterpriseId?: number
  templateId?: number
  templateVersion?: number
  auditYear?: number
  submissionJson?: string
  extractedData?: string
  status?: number
  submitTime?: string
  createTime?: string
  updateTime?: string
}

export interface TplEditLock {
  id?: number
  enterpriseId?: number
  templateId?: number
  auditYear?: number
  lockUserId?: number
  lockTime?: string
  expireTime?: string
  updateBy?: string
}

export interface TemplateQuery {
  templateName?: string
  moduleType?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

// ── Template CRUD ──────────────────────────────────────────────────────────────

export function getTemplateList(params?: TemplateQuery): Promise<PageResult<TplTemplate>> {
  return request.get('/template', { params })
}

export function getTemplateById(id: number): Promise<TplTemplate> {
  return request.get(`/template/${id}`)
}

export function createTemplate(data: Partial<TplTemplate>): Promise<void> {
  return request.post('/template', data)
}

export function updateTemplate(id: number, data: Partial<TplTemplate>): Promise<void> {
  return request.put(`/template/${id}`, data)
}

export function deleteTemplate(id: number): Promise<void> {
  return request.delete(`/template/${id}`)
}

export function publishTemplate(id: number): Promise<void> {
  return request.post(`/template/${id}/publish`)
}

// ── Template Versions ──────────────────────────────────────────────────────────

export function listVersions(templateId: number): Promise<TplTemplateVersion[]> {
  return request.get(`/template/${templateId}/versions`)
}

export function getPublishedVersion(templateId: number): Promise<TplTemplateVersion | null> {
  return request.get(`/template/${templateId}/version/published`)
}

export function getVersionById(versionId: number): Promise<TplTemplateVersion> {
  return request.get(`/template/versions/${versionId}`)
}

export function createDraftVersion(templateId: number): Promise<TplTemplateVersion> {
  return request.post(`/template/${templateId}/versions`)
}

export function saveVersionJson(
  versionId: number,
  templateJson: string,
  changeLog?: string,
  protectionEnabled?: number,
): Promise<void> {
  return request.put(`/template/versions/${versionId}/json`, {
    templateJson,
    changeLog,
    ...(protectionEnabled != null ? { protectionEnabled: String(protectionEnabled) } : {}),
  })
}

export function publishVersion(templateId: number, versionId: number): Promise<void> {
  return request.post(`/template/${templateId}/versions/${versionId}/publish`)
}

// ── Tag Mappings ──────────────────────────────────────────────────────────────

export function listTags(versionId: number): Promise<TplTagMapping[]> {
  return request.get(`/template/versions/${versionId}/tags`)
}

export function syncTagsFromJson(versionId: number): Promise<void> {
  return request.post(`/template/versions/${versionId}/tags/sync`)
}

export function replaceTags(versionId: number, mappings: TplTagMapping[]): Promise<void> {
  return request.put(`/template/versions/${versionId}/tags`, mappings)
}

// ── Submissions ───────────────────────────────────────────────────────────────

export function getSubmission(templateId: number, auditYear: number): Promise<TplSubmission | null> {
  return request.get('/template/submission', { params: { templateId, auditYear } })
}

export function listSubmissions(): Promise<TplSubmission[]> {
  return request.get('/template/submissions')
}

export function saveDraft(params: {
  templateId: number
  auditYear: number
  submissionJson: string
  templateVersion: number
}): Promise<TplSubmission> {
  return request.post('/template/submission/draft', params)
}

export function submitSubmission(submissionId: number, templateVersionId: number): Promise<void> {
  return request.post(`/template/submission/${submissionId}/submit`, null, {
    params: { templateVersionId },
  })
}

// ── Edit Lock ─────────────────────────────────────────────────────────────────

export function acquireLock(templateId: number, auditYear: number): Promise<TplEditLock> {
  return request.post('/template/lock', null, { params: { templateId, auditYear } })
}

export function releaseLock(templateId: number, auditYear: number): Promise<void> {
  return request.delete('/template/lock', { params: { templateId, auditYear } })
}

export function checkLock(templateId: number, auditYear: number): Promise<TplEditLock | null> {
  return request.get('/template/lock', { params: { templateId, auditYear } })
}

export function renewLock(templateId: number, auditYear: number): Promise<TplEditLock> {
  return request.put('/template/lock', null, { params: { templateId, auditYear } })
}
