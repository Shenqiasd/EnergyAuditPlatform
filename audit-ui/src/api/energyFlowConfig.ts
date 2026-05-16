import request from '@/utils/request'

// ============================================================
// Types
// ============================================================

export interface EnterpriseInfo {
  id: number
  name: string
  address?: string
  industry?: string
}

export interface UnitInfo {
  id: number
  name: string
  unitType: number
  subCategory?: string
}

export interface EnergyInfo {
  id: number
  name: string
  category?: string
  measurementUnit?: string
  equivalentValue?: number
}

export interface ProductInfo {
  id: number
  name: string
  measurementUnit?: string
  unitPrice?: number
}

export interface FlowRecord {
  id?: number
  enterpriseId?: number
  auditYear?: number
  flowStage?: string
  seqNo?: number
  sourceUnit?: string
  sourceUnitId?: number | null
  targetUnit?: string
  targetUnitId?: number | null
  energyProduct?: string
  physicalQuantity?: number | null
  standardQuantity?: number | null
  remark?: string
  sourceType?: string
  sourceRefId?: number | null
  targetType?: string
  targetRefId?: number | null
  itemType?: string
  itemId?: number | null
  calculatedValue?: number | null
}

export interface FlowNodeConfig {
  id?: number
  nodeId: string
  nodeType: string
  refType?: string
  refId?: number | null
  label: string
  positionX: number
  positionY: number
  width?: number
  height?: number
  color?: string
  visible?: number
  locked?: number
}

export interface FlowEdgeConfig {
  id?: number
  edgeId: string
  sourceNodeId: string
  targetNodeId: string
  flowRecordId?: number | null
  itemType?: string
  itemId?: number | null
  physicalQuantity?: number | null
  calculatedValue?: number | null
  labelText?: string
  color?: string
  lineWidth?: number
  routePoints?: string
  visible?: number
}

export interface DiagramConfig {
  id?: number
  name?: string
  diagramType?: number
  canvasWidth?: number
  canvasHeight?: number
  backgroundColor?: string
  nodes: FlowNodeConfig[]
  edges: FlowEdgeConfig[]
}

export interface ValidationResult {
  valid: boolean
  enterpriseComplete: boolean
  hasUnits: boolean
  hasEnergies: boolean
  hasProducts: boolean
  warnings: string[]
}

export interface EnergyFlowConfig {
  enterpriseInfo: EnterpriseInfo | null
  units: UnitInfo[]
  energies: EnergyInfo[]
  products: ProductInfo[]
  flowRecords: FlowRecord[]
  diagram: DiagramConfig | null
  validation: ValidationResult
}

export interface SaveEnergyFlowConfig {
  flowRecords: FlowRecord[]
  diagram: DiagramConfig
}

// ============================================================
// API
// ============================================================

export function getEnergyFlowConfig(auditYear: number): Promise<EnergyFlowConfig> {
  return request.get('/energy-flow/config', { params: { auditYear } })
}

export function saveEnergyFlowConfig(auditYear: number, data: SaveEnergyFlowConfig): Promise<void> {
  return request.put('/energy-flow/config', data, { params: { auditYear } })
}
