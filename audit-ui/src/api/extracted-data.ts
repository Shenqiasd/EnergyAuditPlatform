import request from '@/utils/request'
import type { PageResult } from '@/api/setting'

export interface TableSummary {
  tableName: string
  label: string
  count: number
}

export function getExtractedTables(auditYear?: number, enterpriseId?: number): Promise<TableSummary[]> {
  return request.get('/extracted-data/tables', { params: { auditYear, enterpriseId } })
}

export function queryExtractedTable(
  tableName: string,
  params?: { auditYear?: number; enterpriseId?: number; pageNum?: number; pageSize?: number }
): Promise<PageResult<Record<string, unknown>>> {
  return request.get(`/extracted-data/${tableName}`, { params })
}
