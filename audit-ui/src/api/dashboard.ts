import request from '@/utils/request'

export interface DashboardStats {
  totalEnergyEquiv: number | null
  totalEnergyEquivPrev: number | null
  totalCarbonEmission: number | null
  totalCarbonEmissionPrev: number | null
  unitOutputEnergy: number | null
  unitOutputEnergyPrev: number | null
  submittedCount: number
  totalTemplateCount: number
}

export interface ProgressItem {
  name: string
  pct: number
  detail: string
}

export function getDashboardStats(auditYear: number): Promise<DashboardStats> {
  return request.get('/enterprise/dashboard/stats', { params: { auditYear } })
}

export function getDashboardProgress(auditYear: number): Promise<ProgressItem[]> {
  return request.get('/enterprise/dashboard/progress', { params: { auditYear } })
}
