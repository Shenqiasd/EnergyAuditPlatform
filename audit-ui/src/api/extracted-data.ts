import request from '@/utils/request'

export interface TableMeta {
  tableName: string
  label: string
  count: number
}

export function getTableList(): Promise<TableMeta[]> {
  return request.get('/extracted-data/tables')
}

export function queryTableData(tableName: string, auditYear?: number): Promise<Record<string, unknown>[]> {
  return request.get(`/extracted-data/${tableName}`, { params: auditYear ? { auditYear } : {} })
}
