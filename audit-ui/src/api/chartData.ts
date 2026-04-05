import request from '@/utils/request'

export function getEnergyStructure(auditYear: number) {
  return request.get('/chart-data/energy-structure', { params: { auditYear } })
}

export function getEnergyTrend(auditYear: number) {
  return request.get('/chart-data/energy-trend', { params: { auditYear } })
}

export function getProductConsumption(auditYear: number) {
  return request.get('/chart-data/product-consumption', { params: { auditYear } })
}

export function getGhgEmission(auditYear: number) {
  return request.get('/chart-data/ghg-emission', { params: { auditYear } })
}

export function getChartSummary(auditYear: number) {
  return request.get('/chart-data/summary', { params: { auditYear } })
}
