import request from '@/utils/request'

export interface EnergyFlowItem {
  id?: number
  enterpriseId?: number
  auditYear?: number
  flowStage: string
  seqNo?: number
  sourceUnit: string
  targetUnit: string
  energyProduct: string
  physicalQuantity: number
  standardQuantity: number
  remark?: string
}

export function getEnergyFlowList(auditYear: number): Promise<EnergyFlowItem[]> {
  return request.get('/energy-flow/list', { params: { auditYear } })
}

export function saveEnergyFlowBatch(auditYear: number, data: EnergyFlowItem[]): Promise<void> {
  return request.post('/energy-flow/save', data, { params: { auditYear } })
}
