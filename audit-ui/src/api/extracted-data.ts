import request from '@/utils/request'

export interface TableSummary {
  tableName: string
  label: string
  count: number
}

/**
 * Page envelope returned by `/extracted-data/{tableName}`. Backend wraps the
 * query rows with `PageResult.of(total, rows)` so the JSON shape is
 * `{ total: number, rows: T[] }`.
 */
export interface ExtractedTablePage<T = Record<string, unknown>> {
  rows: T[]
  total: number
}

export function getExtractedTables(auditYear?: number, enterpriseId?: number): Promise<TableSummary[]> {
  return request.get<TableSummary[]>('/extracted-data/tables', { params: { auditYear, enterpriseId } })
}

export function queryExtractedTable(
  tableName: string,
  params?: { auditYear?: number; enterpriseId?: number; pageNum?: number; pageSize?: number }
): Promise<ExtractedTablePage> {
  return request.get<ExtractedTablePage>(`/extracted-data/${tableName}`, { params })
}

/**
 * Fetch the most recent scalar values for a list of field names from the
 * generic `de_submission_field` fallback storage. Returns a flat
 * `{ fieldName: value | null }` map with `null` for fields without data.
 */
export function queryExtractedScalars(
  fieldNames: string[],
  params?: { auditYear?: number; enterpriseId?: number },
): Promise<Record<string, number | string | null>> {
  return request.get<Record<string, number | string | null>>(
    '/extracted-data/scalars',
    { params: { ...params, fieldNames: fieldNames.join(',') } },
  )
}
