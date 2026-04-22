import request from '@/utils/request'

export interface EnergyFlowItem {
  id?: number
  enterpriseId?: number
  auditYear?: number
  flowStage: string
  seqNo?: number
  sourceUnit: string
  /** v2 PR #2: bs_unit.id 外键（可空；虚拟节点"外购"/"产出"无对应记录时保持 null）。 */
  sourceUnitId?: number | null
  targetUnit: string
  /** v2 PR #2: bs_unit.id 外键（可空）。 */
  targetUnitId?: number | null
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

export function deleteEnergyFlow(id: number): Promise<void> {
  return request.delete(`/energy-flow/${id}`)
}
