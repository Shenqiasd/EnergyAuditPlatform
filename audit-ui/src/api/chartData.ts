import request from '@/utils/request'

// Backend returns loosely-shaped rows (case-varying keys from raw SQL
// aggregates), so callers handle both UPPER/lower field names themselves.
export type ChartRow = Record<string, unknown>

export function getEnergyStructure(auditYear: number): Promise<ChartRow[]> {
  return request.get<ChartRow[]>('/chart-data/energy-structure', { params: { auditYear } })
}

export function getEnergyTrend(auditYear: number): Promise<ChartRow[]> {
  return request.get<ChartRow[]>('/chart-data/energy-trend', { params: { auditYear } })
}

export function getProductConsumption(auditYear: number): Promise<ChartRow[]> {
  return request.get<ChartRow[]>('/chart-data/product-consumption', { params: { auditYear } })
}

export function getGhgEmission(auditYear: number): Promise<ChartRow[]> {
  return request.get<ChartRow[]>('/chart-data/ghg-emission', { params: { auditYear } })
}

export function getChartSummary(auditYear: number): Promise<Record<string, unknown>> {
  return request.get<Record<string, unknown>>('/chart-data/summary', { params: { auditYear } })
}
