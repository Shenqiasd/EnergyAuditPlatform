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
  cellRange?: string
  remark?: string
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
  return request.get('/template', { params }).then((r: any) => r.data)
}

export function getTemplateById(id: number): Promise<TplTemplate> {
  return request.get(`/template/${id}`).then((r: any) => r.data)
}

export function createTemplate(data: Partial<TplTemplate>): Promise<void> {
  return request.post('/template', data).then(() => undefined)
}

export function updateTemplate(id: number, data: Partial<TplTemplate>): Promise<void> {
  return request.put(`/template/${id}`, data).then(() => undefined)
}

export function deleteTemplate(id: number): Promise<void> {
  return request.delete(`/template/${id}`).then(() => undefined)
}

export function publishTemplate(id: number): Promise<void> {
  return request.post(`/template/${id}/publish`).then(() => undefined)
}

// ── Template Versions ──────────────────────────────────────────────────────────

export function listVersions(templateId: number): Promise<TplTemplateVersion[]> {
  return request.get(`/template/${templateId}/versions`).then((r: any) => r.data)
}

export function getPublishedVersion(templateId: number): Promise<TplTemplateVersion | null> {
  return request.get(`/template/${templateId}/version/published`).then((r: any) => r.data)
}

export function getVersionById(versionId: number): Promise<TplTemplateVersion> {
  return request.get(`/template/versions/${versionId}`).then((r: any) => r.data)
}

export function createDraftVersion(templateId: number): Promise<TplTemplateVersion> {
  return request.post(`/template/${templateId}/versions`).then((r: any) => r.data)
}

export function saveVersionJson(versionId: number, templateJson: string, changeLog?: string): Promise<void> {
  return request.put(`/template/versions/${versionId}/json`, { templateJson, changeLog }).then(() => undefined)
}

export function publishVersion(templateId: number, versionId: number): Promise<void> {
  return request.post(`/template/${templateId}/versions/${versionId}/publish`).then(() => undefined)
}

// ── Tag Mappings ──────────────────────────────────────────────────────────────

export function listTags(versionId: number): Promise<TplTagMapping[]> {
  return request.get(`/template/versions/${versionId}/tags`).then((r: any) => r.data)
}

export function syncTagsFromJson(versionId: number): Promise<void> {
  return request.post(`/template/versions/${versionId}/tags/sync`).then(() => undefined)
}

export function replaceTags(versionId: number, mappings: TplTagMapping[]): Promise<void> {
  return request.put(`/template/versions/${versionId}/tags`, mappings).then(() => undefined)
}

// ── Submissions ───────────────────────────────────────────────────────────────

export function getSubmission(templateId: number, auditYear: number): Promise<TplSubmission | null> {
  return request.get('/template/submission', { params: { templateId, auditYear } }).then((r: any) => r.data)
}

export function listSubmissions(): Promise<TplSubmission[]> {
  return request.get('/template/submissions').then((r: any) => r.data)
}

export function saveDraft(params: {
  templateId: number
  auditYear: number
  submissionJson: string
  templateVersion: number
}): Promise<TplSubmission> {
  return request.post('/template/submission/draft', params).then((r: any) => r.data)
}

export function submitSubmission(submissionId: number, templateVersionId: number): Promise<void> {
  return request.post(`/template/submission/${submissionId}/submit`, null, {
    params: { templateVersionId },
  }).then(() => undefined)
}

// ── Edit Lock ─────────────────────────────────────────────────────────────────

export function acquireLock(templateId: number, auditYear: number): Promise<TplEditLock> {
  return request.post('/template/lock', null, { params: { templateId, auditYear } }).then((r: any) => r.data)
}

export function releaseLock(templateId: number, auditYear: number): Promise<void> {
  return request.delete('/template/lock', { params: { templateId, auditYear } }).then(() => undefined)
}

export function checkLock(templateId: number, auditYear: number): Promise<TplEditLock | null> {
  return request.get('/template/lock', { params: { templateId, auditYear } }).then((r: any) => r.data)
}

export function renewLock(templateId: number, auditYear: number): Promise<TplEditLock> {
  return request.put('/template/lock', null, { params: { templateId, auditYear } }).then((r: any) => r.data)
}
