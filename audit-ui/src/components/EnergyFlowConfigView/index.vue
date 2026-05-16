<script setup lang="ts">
import { ref, computed } from 'vue'
import type {
  FlowNodeConfig, FlowEdgeConfig, FlowRecord,
  EnergyInfo, UnitInfo, ProductInfo, EnergyConsumptionInfo,
  ValidationResult,
} from '@/api/energyFlowConfig'

const props = defineProps<{
  nodes: FlowNodeConfig[]
  edges: FlowEdgeConfig[]
  flowRecords?: FlowRecord[]
  energies?: EnergyInfo[]
  units?: UnitInfo[]
  products?: ProductInfo[]
  energyConsumption?: EnergyConsumptionInfo[]
  enterpriseName?: string
  auditYear?: number
  canvasWidth?: number
  canvasHeight?: number
  validation?: ValidationResult
}>()

defineExpose({ exportPng, fitView })

const svgRef = ref<SVGSVGElement>()
const cw = computed(() => props.canvasWidth || 1200)
const ch = computed(() => props.canvasHeight || 800)

// ── Number formatting (canonical spec) ──────────────────────
// Thousands separator, default 1 decimal, hide trailing .0 for integers,
// 0 is visible, null/undefined/NaN → empty string,
// one space between value and unit
function fmtNum(v: number | null | undefined, decimals = 1): string {
  if (v == null || (typeof v === 'number' && isNaN(v))) return ''
  const fixed = v.toFixed(decimals)
  const cleaned = fixed.replace(/\.0+$/, '')
  const parts = cleaned.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return parts.join('.')
}
// Percentages: exactly 2 decimals always (do NOT strip trailing zeros)
// Format as (12.45%), (12.00%), (0.00%), blank for null/NaN
function fmtPct(v: number | null | undefined): string {
  if (v == null || (typeof v === 'number' && isNaN(v))) return ''
  const pctVal = v * 100
  const fixed = pctVal.toFixed(2)
  const parts = fixed.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return '(' + parts.join('.') + '%)'
}

// ── Lookup maps ────────────────────────────────────────────
const unitMap = computed(() => {
  const m = new Map<number, UnitInfo>()
  props.units?.forEach(u => m.set(u.id, u))
  return m
})
const energyMap = computed(() => {
  const m = new Map<number, EnergyInfo>()
  props.energies?.forEach(e => m.set(e.id, e))
  return m
})
const productMap = computed(() => {
  const m = new Map<number, ProductInfo>()
  props.products?.forEach(p => m.set(p.id, p))
  return m
})
const consumptionByEnergyId = computed(() => {
  const m = new Map<number, EnergyConsumptionInfo>()
  props.energyConsumption?.forEach(c => { if (c.energyId) m.set(c.energyId, c) })
  return m
})
const recordById = computed(() => {
  const m = new Map<number, FlowRecord>()
  props.flowRecords?.forEach(r => { if (r.id) m.set(r.id, r) })
  return m
})
const recordByClientKey = computed(() => {
  const m = new Map<string, FlowRecord>()
  props.flowRecords?.forEach(r => { if (r._clientKey) m.set(r._clientKey, r) })
  return m
})

// ── Stage layout constants ────────────────────────────────
const STAGE_LABELS = ['购入贮存环节', '加工转换环节', '输送分配环节', '终端使用环节']
const STAGE_MARGIN = 80
const BASE_HEADER_Y = 55
const BACKFLOW_LANE_SPACING = 12

// Dynamic top-channel height for return-flow lanes
const backflowLaneCount = computed(() => {
  const groups = new Set<string>()
  for (const e of (props.edges ?? []).filter(e => (e.visible ?? 1) !== 0)) {
    const sln = _rawLayoutNodeMap.value.get(e.sourceNodeId)
    const tln = _rawLayoutNodeMap.value.get(e.targetNodeId)
    if (!sln || !tln || sln.stage <= tln.stage) continue
    const rec = _resolveRecordRaw(e)
    const iId = rec?.itemId ?? e.itemId
    const key = iId ? `bf-${iId}-${e.sourceNodeId}-${e.targetNodeId}` : `bf-${e.edgeId}`
    groups.add(key)
  }
  return groups.size
})
const topChannelHeight = computed(() => backflowLaneCount.value > 0 ? backflowLaneCount.value * BACKFLOW_LANE_SPACING + 10 : 0)
const HEADER_Y = computed(() => BASE_HEADER_Y + topChannelHeight.value)
const BODY_TOP = computed(() => HEADER_Y.value + 20)
const stageWidth = computed(() => (cw.value - STAGE_MARGIN * 2) / 4)
const stageX = computed(() => STAGE_LABELS.map((_, i) => STAGE_MARGIN + i * stageWidth.value))
const stageDividers = computed(() => [stageX.value[1], stageX.value[2], stageX.value[3]])

function nodeStage(n: FlowNodeConfig): number {
  if (n.nodeType === 'energy_input') return 0
  if (n.nodeType === 'product_output') return 3
  if (n.nodeType === 'unit' && n.refId) {
    const u = unitMap.value.get(n.refId)
    if (u) {
      if (u.unitType === 1) return 1
      if (u.unitType === 2) return 2
      if (u.unitType === 3) return 3
    }
  }
  return 2
}

// ── Enforce fixed stage band positions ────────────────────
interface LayoutNode {
  node: FlowNodeConfig
  stage: number
  cx: number
  cy: number
  w: number
  h: number
  isCircle: boolean
}

// Raw layout for backflow lane count calculation (avoids circular dependency)
const _rawLayoutNodeMap = computed(() => {
  const m = new Map<string, { stage: number }>()
  for (const n of props.nodes.filter(nn => (nn.visible ?? 1) !== 0 && nn.nodeType !== 'product_output')) {
    m.set(n.nodeId, { stage: nodeStage(n) })
  }
  return m
})
function _resolveRecordRaw(e: FlowEdgeConfig): FlowRecord | undefined {
  if (e._flowRecordClientKey) {
    const byKey = recordByClientKey.value.get(e._flowRecordClientKey)
    if (byKey) return byKey
  }
  if (e.flowRecordId) return recordById.value.get(e.flowRecordId)
  return undefined
}

const layoutNodes = computed<LayoutNode[]>(() => {
  const visible = props.nodes.filter(n => (n.visible ?? 1) !== 0 && n.nodeType !== 'product_output')
  const buckets = new Map<number, FlowNodeConfig[]>()
  for (const n of visible) {
    const s = nodeStage(n)
    if (!buckets.has(s)) buckets.set(s, [])
    buckets.get(s)!.push(n)
  }
  const ROW_H = 90
  const result: LayoutNode[] = []
  for (const [stage, nodes] of buckets) {
    const sx = stageX.value[stage]
    const sw = stageWidth.value
    nodes.forEach((n, rowIdx) => {
      const isCircle = n.nodeType === 'energy_input'
      const w = isCircle ? 60 : 100
      const h = isCircle ? 60 : 50
      const cx = sx + sw / 2
      const cy = BODY_TOP.value + rowIdx * ROW_H + ROW_H / 2
      result.push({ node: n, stage, cx, cy, w, h, isCircle })
    })
  }
  return result
})

const layoutNodeMap = computed(() => {
  const m = new Map<string, LayoutNode>()
  layoutNodes.value.forEach(ln => m.set(ln.node.nodeId, ln))
  return m
})

const shapeNodes = computed(() => layoutNodes.value)

const visibleEdges = computed(() => props.edges.filter(e => (e.visible ?? 1) !== 0))

function resolveRecord(e: FlowEdgeConfig): FlowRecord | undefined {
  if (e._flowRecordClientKey) {
    const byKey = recordByClientKey.value.get(e._flowRecordClientKey)
    if (byKey) return byKey
  }
  if (e.flowRecordId) return recordById.value.get(e.flowRecordId)
  return undefined
}

// ── Node rendering helpers ─────────────────────────────────
const NODE_COLORS: Record<string, string> = {
  energy_input: '#E74C3C', unit: '#3498DB', product_output: '#27AE60', custom: '#95A5A6',
}
function nodeColor(n: FlowNodeConfig): string { return n.color || NODE_COLORS[n.nodeType] || '#666' }

// ── Energy node: name + total quantity/unit (two lines in circle) ─
// Blank when ANY required inventory term is missing (not partial-zero coercion)
function energyNodeTotalLine(n: FlowNodeConfig): string {
  if (n.nodeType !== 'energy_input' || !n.refId) return ''
  const cons = consumptionByEnergyId.value.get(n.refId)
  const en = energyMap.value.get(n.refId)
  if (!cons) return ''
  // ALL four inventory terms must be present to compute a total
  if (cons.purchaseAmount == null || cons.openingStock == null
      || cons.closingStock == null || cons.externalSupply == null) return ''
  const usage = cons.purchaseAmount + cons.openingStock
    - cons.closingStock - cons.externalSupply
  const unit = en?.measurementUnit ?? cons.measurementUnit ?? ''
  const v = fmtNum(usage)
  return v ? v + ' ' + unit : ''
}

// ── Inventory 4-line indicators (energy_input circles) ────
// Order: 期末库存 / 外供 / 期初库存 / 购入 (canonical spec)
// Always show fixed slots; blank value when no data but preserve slot position
// Each line has: label, value text, direction arrow (← for outgoing, → for incoming)
interface InventorySlot {
  label: string
  value: string
  arrow: string // '→' incoming (购入/期初库存), '←' outgoing (期末库存/外供), '' if none
}
function inventoryLines(n: FlowNodeConfig): InventorySlot[] {
  if (n.nodeType !== 'energy_input') return []
  const en = n.refId ? energyMap.value.get(n.refId) : undefined
  const unit = en?.measurementUnit ?? ''
  const cons = n.refId ? consumptionByEnergyId.value.get(n.refId) : undefined
  const fmt = (v: number | null | undefined) => {
    const s = fmtNum(v)
    return s ? s + ' ' + unit : ''
  }
  const cs = fmt(cons?.closingStock)
  const es = fmt(cons?.externalSupply)
  const os = fmt(cons?.openingStock)
  const pt = fmt(cons?.purchaseAmount)
  return [
    { label: '期末库存', value: cs, arrow: cs ? '←' : '' },
    { label: '外供', value: es, arrow: es ? '←' : '' },
    { label: '期初库存', value: os, arrow: os ? '→' : '' },
    { label: '购入', value: pt, arrow: pt ? '→' : '' },
  ]
}

// ── Unit efficiency (加工转换 / 输送分配 only) ──────────────
function unitEfficiency(n: FlowNodeConfig): string {
  if (n.nodeType !== 'unit' || !n.refId) return ''
  const u = unitMap.value.get(n.refId)
  if (!u || (u.unitType !== 1 && u.unitType !== 2)) return ''
  let inputTotal = 0, outputTotal = 0
  for (const e of visibleEdges.value) {
    const rec = resolveRecord(e)
    const iType = rec?.itemType ?? e.itemType
    if (iType !== 'energy') continue
    const v = rec?.calculatedValue ?? e.calculatedValue ?? 0
    if (e.targetNodeId === n.nodeId) inputTotal += v
    if (e.sourceNodeId === n.nodeId) outputTotal += v
  }
  if (inputTotal <= 0) return ''
  return fmtPct(outputTotal / inputTotal)
}

// ── Equivalence / equivalent double lines (stage 0→1) ─────
// Always-present double solid line region (mandatory layout element)
// Schema mapping: equivalent_value = 当量值 coefficient; equal_value = 等价值 coefficient
// Left column 等价值 = consumeAmount × equalValue (等价值系数)
// Right column 当量值 = consumeAmount × equivalentValue (当量值系数)
// Percentages: 消费值 ÷ 能源总量 (per-node), blank when any required term missing
interface EquivLine { y: number; equivVal: string; equalVal: string; equivPct: string; equalPct: string }
const equivLines = computed<EquivLine[]>(() => {
  const energyNodes = layoutNodes.value.filter(ln => ln.node.nodeType === 'energy_input' && ln.node.refId)
  if (!energyNodes.length) return []
  const items: { cy: number; dengJia: number | null; dangLiang: number | null; pct: number | null }[] = []
  for (const ln of energyNodes) {
    const cons = consumptionByEnergyId.value.get(ln.node.refId!)
    const en = energyMap.value.get(ln.node.refId!)
    // consumeAmount must be present
    if (!cons || cons.consumeAmount == null) {
      items.push({ cy: ln.cy, dengJia: null, dangLiang: null, pct: null })
      continue
    }
    const ca = cons.consumeAmount
    // Schema: equalValue = 等价值 coefficient (left), equivalentValue = 当量值 coefficient (right)
    // Coefficients must be present (not coerced from null to 0)
    const dengJiaF = en?.equalValue ?? cons?.equalFactor
    const dangLiangF = en?.equivalentValue ?? cons?.equivFactor
    const dengJia = dengJiaF != null ? ca * dengJiaF : null
    const dangLiang = dangLiangF != null ? ca * dangLiangF : null
    // Percentage: consumeAmount / energyTotal where energyTotal = openingStock + purchaseAmount - externalSupply - closingStock
    // All four energyTotal terms must be present; otherwise blank
    let pct: number | null = null
    if (cons.openingStock != null && cons.purchaseAmount != null
        && cons.externalSupply != null && cons.closingStock != null) {
      const energyTotal = cons.openingStock + cons.purchaseAmount - cons.externalSupply - cons.closingStock
      pct = energyTotal > 0 ? ca / energyTotal : null
    }
    items.push({ cy: ln.cy, dengJia, dangLiang, pct })
  }
  return items.map(it => ({
    y: it.cy,
    // equivVal = left column = 等价值 = consumeAmount × equalValue
    equivVal: it.dengJia != null ? fmtNum(it.dengJia) : '',
    // equalVal = right column = 当量值 = consumeAmount × equivalentValue
    equalVal: it.dangLiang != null ? fmtNum(it.dangLiang) : '',
    equivPct: it.pct != null ? fmtPct(it.pct) : '',
    equalPct: it.pct != null ? fmtPct(it.pct) : '',
  }))
})
const equivLineX = computed(() => stageDividers.value[0] ?? stageX.value[1] ?? cw.value * 0.3)
// Double line region is always present when energy nodes exist
const hasEnergyNodes = computed(() => layoutNodes.value.some(ln => ln.node.nodeType === 'energy_input'))

// ── Edge classification ─────────────────────────────────────
function isBackflow(e: FlowEdgeConfig): boolean {
  const s = layoutNodeMap.value.get(e.sourceNodeId)
  const t = layoutNodeMap.value.get(e.targetNodeId)
  if (!s || !t) return false
  return s.stage > t.stage
}
function isProductEdge(e: FlowEdgeConfig): boolean {
  const rec = resolveRecord(e)
  if (rec) return rec.itemType === 'product'
  return e.itemType === 'product'
}

const PALETTE = ['#E74C3C', '#3498DB', '#27AE60', '#F39C12', '#9B59B6', '#1ABC9C', '#E67E22']
function edgeStrokeColor(e: FlowEdgeConfig): string {
  if (isProductEdge(e)) return '#000'
  if (e.color) return e.color
  const rec = resolveRecord(e)
  const itemId = rec?.itemId ?? e.itemId
  if (itemId && (rec?.itemType === 'energy' || e.itemType === 'energy')) {
    const en = energyMap.value.get(itemId)
    if (en?.color) return en.color
  }
  return PALETTE[visibleEdges.value.indexOf(e) % PALETTE.length]
}

function srcExitPt(ln: LayoutNode): { x: number; y: number } {
  return { x: ln.cx + ln.w / 2, y: ln.cy }
}
function tgtEntryPt(ln: LayoutNode): { x: number; y: number } {
  return { x: ln.cx - ln.w / 2, y: ln.cy }
}

// ── Canonical 90° orthogonal routing ─────────────────────────
// Every rendered/exported path must be strictly 90° orthogonal.
// Editor routePoints are integrated as constrained Y-level waypoint hints.
// They must produce valid 90° orthogonal segments; non-orthogonal points are rejected.
// Bus expansion: edges sharing the same source energy use the same trunk X.
// Return-flow edges route along the top channel.
// Half-circle line jumps at crossing points.

/** Parse edge.routePoints JSON string into coordinate array; empty on invalid/missing */
function parseRoutePoints(edge: FlowEdgeConfig): { x: number; y: number }[] {
  if (!edge.routePoints) return []
  try {
    const pts = JSON.parse(edge.routePoints) as { x: number; y: number }[]
    if (!Array.isArray(pts)) return []
    return pts.filter(p => typeof p.x === 'number' && typeof p.y === 'number' && isFinite(p.x) && isFinite(p.y))
  } catch { return [] }
}

/** Check if route points are valid 90° constrained hints (no non-orthogonal jumps) */
function validateRoutePoints(rpts: { x: number; y: number }[], s: { x: number; y: number }, t: { x: number; y: number }): boolean {
  if (!rpts.length) return true
  // Route points must all be within reasonable canvas bounds
  for (const p of rpts) {
    if (p.x < 0 || p.y < 0 || p.x > 5000 || p.y > 5000) return false
  }
  // Build full path and check all segments are orthogonal (H or V)
  const full = [s, ...rpts, t]
  for (let i = 0; i < full.length - 1; i++) {
    const a = full[i], b = full[i + 1]
    // Each segment must be horizontal or vertical (tolerance 1px)
    if (Math.abs(a.x - b.x) > 1 && Math.abs(a.y - b.y) > 1) return false
  }
  return true
}

// Allocate deterministic top-channel lanes for return-flow edges
// Group by itemId + source/target path; separate lanes for different paths
const backflowLaneMap = computed(() => {
  const m = new Map<string, number>()
  const backflowEdges = visibleEdges.value.filter(e => isBackflow(e))
  const groups = new Map<string, FlowEdgeConfig[]>()
  for (const e of backflowEdges) {
    const rec = resolveRecord(e)
    const iId = rec?.itemId ?? e.itemId
    const key = iId ? `bf-${iId}-${e.sourceNodeId}-${e.targetNodeId}` : `bf-${e.edgeId}`
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(e)
  }
  let laneIdx = 0
  for (const [, edges] of groups) {
    for (const e of edges) {
      m.set(e.edgeId, laneIdx)
    }
    laneIdx++
  }
  return m
})

// Build trunk X map: compatible-route-segment logic
// Same-energy flows from same source share a trunk; different sources get separate trunks.
// Each edge gets a trunk key for its shared segment (source node + itemId)
// and a branch slot key for its individual target.
interface TrunkInfo { trunkX: number; branchX: number }
const trunkInfoMap = computed(() => {
  const m = new Map<string, TrunkInfo>()
  // Group forward edges by source node + itemId (shared trunk segment)
  const trunkGroups = new Map<string, FlowEdgeConfig[]>()
  for (const e of visibleEdges.value) {
    if (isProductEdge(e) || isBackflow(e)) continue
    const sln = layoutNodeMap.value.get(e.sourceNodeId)
    if (!sln) continue
    const rec = resolveRecord(e)
    const iId = rec?.itemId ?? e.itemId
    const tk = `${e.sourceNodeId}-${iId ?? ''}`
    if (!trunkGroups.has(tk)) trunkGroups.set(tk, [])
    trunkGroups.get(tk)!.push(e)
  }
  let slotIdx = 0
  for (const [, edges] of trunkGroups) {
    const sln = layoutNodeMap.value.get(edges[0].sourceNodeId)
    if (!sln) continue
    const trunkX = sln.cx + sln.w / 2 + 20 + slotIdx * 16
    // Collect distinct targets to assign branch slots
    const targetSet = new Set(edges.map(e => e.targetNodeId))
    const targets = Array.from(targetSet)
    for (const e of edges) {
      if (targets.length <= 1) {
        // Single target: trunk and branch are same X
        m.set(e.edgeId, { trunkX, branchX: trunkX })
      } else {
        // Multi-target: branch slot offset from trunk
        const tIdx = targets.indexOf(e.targetNodeId)
        const branchX = trunkX + (tIdx + 1) * 8
        m.set(e.edgeId, { trunkX, branchX })
      }
    }
    slotIdx++
  }
  return m
})


// Collect all horizontal segment Y positions for crossing detection
interface HSegment { y: number; x1: number; x2: number; edgeId: string }
interface VSegment { x: number; y1: number; y2: number; edgeId: string }

function buildOrthoPath(edge: FlowEdgeConfig): { x: number; y: number }[] {
  const sln = layoutNodeMap.value.get(edge.sourceNodeId)
  const tln = layoutNodeMap.value.get(edge.targetNodeId)
  if (!sln || !tln) return []
  if (isProductEdge(edge)) return []

  const s = srcExitPt(sln)
  const t = tgtEntryPt(tln)

  // Parse editor route points as constrained hints — never used as the rendered path.
  // Route points only influence lane/trunk/midpoint selection within canonical routing.
  const rpts = parseRoutePoints(edge)
  const rptsValid = rpts.length > 0 && validateRoutePoints(rpts, s, t)

  if (isBackflow(edge)) {
    // Return-flow: ALWAYS route through top channel with canonical shape.
    // Route point hint: if valid points suggest a Y level above both nodes, use it as lane Y.
    const lane = backflowLaneMap.value.get(edge.edgeId) ?? 0
    const defaultTopY = HEADER_Y.value - 10 - lane * BACKFLOW_LANE_SPACING
    let topY = defaultTopY
    if (rptsValid) {
      const minNodeY = Math.min(s.y, t.y)
      const hintYs = rpts.filter(p => p.y < minNodeY).map(p => p.y)
      if (hintYs.length > 0) {
        // Use the minimum Y from hints as the channel level (must be above nodes)
        const candidateY = Math.min(...hintYs)
        if (candidateY >= 0 && candidateY < minNodeY) topY = candidateY
      }
    }
    return [s, { x: s.x, y: topY }, { x: t.x, y: topY }, t]
  }

  // Forward edge: use compatible-route-segment trunk/branch routing.
  // Route point hint: if valid points suggest a trunk X offset, adjust within ±30px of canonical trunk.
  const info = trunkInfoMap.value.get(edge.edgeId)
  if (info && Math.abs(info.trunkX - s.x) > 5) {
    let trunkX = info.trunkX
    let branchX = info.branchX
    if (rptsValid) {
      const hintXs = rpts.filter(p => Math.abs(p.x - info.trunkX) <= 30).map(p => p.x)
      if (hintXs.length > 0) {
        trunkX = hintXs[0]
        branchX = info.trunkX === info.branchX ? trunkX : info.branchX
      }
    }
    if (trunkX === branchX) {
      return [s, { x: trunkX, y: s.y }, { x: trunkX, y: t.y }, t]
    }
    const midY = (s.y + t.y) / 2
    return [s, { x: trunkX, y: s.y }, { x: trunkX, y: midY },
            { x: branchX, y: midY }, { x: branchX, y: t.y }, t]
  }

  // Default orthogonal: route point hint adjusts midpoint X within canvas bounds.
  let midX = (s.x + t.x) / 2
  if (rptsValid) {
    const hintXs = rpts.map(p => p.x).filter(x => x >= 0 && x <= 5000)
    if (hintXs.length > 0) midX = hintXs[Math.floor(hintXs.length / 2)]
  }
  return [s, { x: midX, y: s.y }, { x: midX, y: t.y }, t]
}

// Collect all segments for crossing detection
const allSegments = computed(() => {
  const hSegs: HSegment[] = []
  const vSegs: VSegment[] = []
  for (const e of visibleEdges.value) {
    if (isProductEdge(e)) continue
    const pts = buildOrthoPath(e)
    for (let i = 0; i < pts.length - 1; i++) {
      const a = pts[i], b = pts[i + 1]
      if (a.y === b.y) {
        hSegs.push({ y: a.y, x1: Math.min(a.x, b.x), x2: Math.max(a.x, b.x), edgeId: e.edgeId })
      } else if (a.x === b.x) {
        vSegs.push({ x: a.x, y1: Math.min(a.y, b.y), y2: Math.max(a.y, b.y), edgeId: e.edgeId })
      }
    }
  }
  return { hSegs, vSegs }
})

// Find crossings for a given edge: where its segments cross other edges' segments
function findCrossings(edgeId: string): { x: number; y: number }[] {
  const { hSegs, vSegs } = allSegments.value
  const crossings: { x: number; y: number }[] = []
  // Check this edge's H segments against other edges' V segments
  for (const hs of hSegs) {
    if (hs.edgeId !== edgeId) continue
    for (const vs of vSegs) {
      if (vs.edgeId === edgeId) continue
      if (vs.x > hs.x1 + 2 && vs.x < hs.x2 - 2 && hs.y > vs.y1 + 2 && hs.y < vs.y2 - 2) {
        crossings.push({ x: vs.x, y: hs.y })
      }
    }
  }
  // Check this edge's V segments against other edges' H segments
  for (const vs of vSegs) {
    if (vs.edgeId !== edgeId) continue
    for (const hs of hSegs) {
      if (hs.edgeId === edgeId) continue
      if (hs.y > vs.y1 + 2 && hs.y < vs.y2 - 2 && vs.x > hs.x1 + 2 && vs.x < hs.x2 - 2) {
        crossings.push({ x: vs.x, y: hs.y })
      }
    }
  }
  return crossings
}

// Build SVG path with half-circle line jumps at crossings
function edgePath(edge: FlowEdgeConfig): string {
  const pts = buildOrthoPath(edge)
  if (pts.length < 2) return ''

  const crossings = findCrossings(edge.edgeId)
  if (!crossings.length) {
    // Simple orthogonal path
    let d = `M ${pts[0].x} ${pts[0].y}`
    for (let i = 1; i < pts.length; i++) {
      d += ` L ${pts[i].x} ${pts[i].y}`
    }
    return d
  }

  // Build path with half-circle jumps at crossing points
  const JUMP_R = 5
  let d = `M ${pts[0].x} ${pts[0].y}`
  for (let i = 0; i < pts.length - 1; i++) {
    const a = pts[i], b = pts[i + 1]
    const isH = a.y === b.y
    // Find crossings on this segment
    const segCrossings = crossings.filter(c => {
      if (isH) return Math.abs(c.y - a.y) < 1 && c.x > Math.min(a.x, b.x) + 2 && c.x < Math.max(a.x, b.x) - 2
      return Math.abs(c.x - a.x) < 1 && c.y > Math.min(a.y, b.y) + 2 && c.y < Math.max(a.y, b.y) - 2
    })
    if (!segCrossings.length) {
      d += ` L ${b.x} ${b.y}`
      continue
    }
    // Sort crossings along segment direction
    if (isH) {
      const dir = b.x > a.x ? 1 : -1
      segCrossings.sort((p, q) => (p.x - q.x) * dir)
      for (const c of segCrossings) {
        d += ` L ${c.x - JUMP_R * dir} ${c.y}`
        // Half-circle jump (arc over the crossing)
        d += ` A ${JUMP_R} ${JUMP_R} 0 0 ${dir > 0 ? 1 : 0} ${c.x + JUMP_R * dir} ${c.y}`
      }
      d += ` L ${b.x} ${b.y}`
    } else {
      const dir = b.y > a.y ? 1 : -1
      segCrossings.sort((p, q) => (p.y - q.y) * dir)
      for (const c of segCrossings) {
        d += ` L ${c.x} ${c.y - JUMP_R * dir}`
        d += ` A ${JUMP_R} ${JUMP_R} 0 0 ${dir > 0 ? 0 : 1} ${c.x} ${c.y + JUMP_R * dir}`
      }
      d += ` L ${b.x} ${b.y}`
    }
  }
  return d
}

// ── Product output lines: horizontal to right boundary ──────
// Renderer must NOT silently drop invalid product lines — show them with error indicator
interface ProductLine { y: number; x1: number; topText: string; bottomText: string; invalid?: boolean }
const productLines = computed<ProductLine[]>(() => {
  const results: ProductLine[] = []
  for (const e of visibleEdges.value) {
    if (!isProductEdge(e)) continue
    const sln = layoutNodeMap.value.get(e.sourceNodeId)
    if (!sln) continue
    const rec = resolveRecord(e)
    const itemId = rec?.itemId ?? e.itemId
    const pr = itemId ? productMap.value.get(itemId) : undefined
    const topText = pr?.name ?? ''
    const pq = rec?.physicalQuantity ?? e.physicalQuantity
    const unit = pr?.measurementUnit ?? ''
    const bottomText = fmtNum(pq) ? fmtNum(pq) + ' ' + unit : ''
    // Non-stage-3 sources are invalid but still rendered (with error styling)
    const invalid = sln.stage !== 3
    results.push({ y: sln.cy, x1: sln.cx + sln.w / 2, topText, bottomText, invalid })
  }
  return results
})

// ── Dual-line labels on edges (resolved from flowRecords) ──
// Spec: top = energy name + equivalent/actual-value display
//        bottom = physical quantity + energy unit
interface DualLabel { x: number; y: number; top: string; bottom: string }
function edgeDualLabel(edge: FlowEdgeConfig): DualLabel {
  if (isProductEdge(edge)) return { x: 0, y: 0, top: '', bottom: '' }
  const sln = layoutNodeMap.value.get(edge.sourceNodeId)
  const tln = layoutNodeMap.value.get(edge.targetNodeId)
  if (!sln || !tln) return { x: 0, y: 0, top: '', bottom: '' }

  const pts = buildOrthoPath(edge)
  // Place label at the midpoint of the longest horizontal segment
  let lx: number, ly: number
  if (isBackflow(edge)) {
    lx = (pts[1]?.x ?? sln.cx + sln.w / 2 + (tln.cx - tln.w / 2)) / 2
    if (pts.length >= 3) lx = (pts[1].x + pts[2].x) / 2
    ly = HEADER_Y.value - 18
  } else {
    // Find midpoint of first horizontal segment for label placement
    lx = (sln.cx + sln.w / 2 + tln.cx - tln.w / 2) / 2
    ly = (sln.cy + tln.cy) / 2 - 4
    // Better: place on the horizontal segment of the ortho path
    for (let i = 0; i < pts.length - 1; i++) {
      if (pts[i].y === pts[i + 1].y && Math.abs(pts[i].x - pts[i + 1].x) > 20) {
        lx = (pts[i].x + pts[i + 1].x) / 2
        ly = pts[i].y - 8
        break
      }
    }
  }

  const rec = resolveRecord(edge)
  const iType = rec?.itemType ?? edge.itemType
  const iId = rec?.itemId ?? edge.itemId
  const pq = rec?.physicalQuantity ?? edge.physicalQuantity
  const cv = rec?.calculatedValue ?? edge.calculatedValue

  let topTxt = '', bottomTxt = ''
  if (iType === 'energy') {
    const en = iId ? energyMap.value.get(iId) : undefined
    const name = en?.name ?? ''
    const energyUnit = en?.measurementUnit ?? ''
    // Top: energy name + equivalent/actual value from bound record (no hard-coded unit)
    const cvStr = fmtNum(cv)
    topTxt = cvStr ? `${name} ${cvStr}` : name
    // Bottom: physical quantity + energy unit
    bottomTxt = fmtNum(pq) ? `${fmtNum(pq)} ${energyUnit}` : ''
  } else {
    topTxt = edge.labelText || ''
  }
  return { x: lx, y: ly, top: topTxt, bottom: bottomTxt }
}

// ── Arrow marker IDs ───────────────────────────────────────
const markerColors = computed(() => {
  const s = new Set<string>()
  visibleEdges.value.forEach(e => s.add(edgeStrokeColor(e)))
  s.add('#000') // always include black for product lines
  return Array.from(s)
})
function markerId(color: string): string { return `arrow-v-${color.replace('#', '')}` }

// ── Export PNG ──────────────────────────────────────────────
async function exportPng(): Promise<string | null> {
  if (!svgRef.value) return null
  const serializer = new XMLSerializer()
  let svgStr = serializer.serializeToString(svgRef.value)
  if (!svgStr.includes('xmlns=')) svgStr = svgStr.replace('<svg', '<svg xmlns="http://www.w3.org/2000/svg"')
  const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  try {
    const img = new Image()
    img.src = url
    await new Promise<void>((res, rej) => { img.onload = () => res(); img.onerror = rej })
    const scale = 2
    const canvas = document.createElement('canvas')
    canvas.width = cw.value * scale; canvas.height = ch.value * scale
    const ctx = canvas.getContext('2d')!
    ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.scale(scale, scale); ctx.drawImage(img, 0, 0)
    return canvas.toDataURL('image/png')
  } finally { URL.revokeObjectURL(url) }
}

function fitView() { svgRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' }) }
</script>

<template>
  <div class="config-view-wrapper">
    <svg
      ref="svgRef"
      :width="cw" :height="ch"
      :viewBox="`0 0 ${cw} ${ch}`"
      xmlns="http://www.w3.org/2000/svg"
      class="config-view-canvas"
    >
      <rect :width="cw" :height="ch" fill="#fff" />

      <!-- Title -->
      <text v-if="enterpriseName" :x="cw / 2" y="28" text-anchor="middle" font-size="16" font-weight="bold" fill="#1f3a68">
        {{ enterpriseName }} {{ auditYear }}年 能源流程图
      </text>

      <!-- Stage headers & dividers -->
      <g class="stage-headers">
        <template v-for="(label, si) in STAGE_LABELS" :key="si">
          <text
            :x="stageX[si] + stageWidth / 2"
            :y="HEADER_Y"
            text-anchor="middle" font-size="12" font-weight="600" fill="#555"
          >{{ label }}</text>
        </template>
        <line
          v-for="(dx, di) in stageDividers" :key="'d'+di"
          :x1="dx" :y1="HEADER_Y + 8" :x2="dx" :y2="ch - 40"
          stroke="#ccc" stroke-dasharray="4,3" stroke-width="0.5"
        />
      </g>

      <!-- Arrow markers -->
      <defs>
        <marker
          v-for="c in markerColors" :key="c"
          :id="markerId(c)"
          markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth"
        >
          <path d="M0,0 L8,4 L0,8 z" :fill="c" />
        </marker>
      </defs>

      <!-- Equivalence / equivalent double vertical lines (always present when energy nodes exist) -->
      <g v-if="hasEnergyNodes" class="equiv-lines">
        <line :x1="equivLineX - 3" :y1="HEADER_Y + 12" :x2="equivLineX - 3" :y2="ch - 50" stroke="#666" stroke-width="1.5" />
        <line :x1="equivLineX + 3" :y1="HEADER_Y + 12" :x2="equivLineX + 3" :y2="ch - 50" stroke="#666" stroke-width="1.5" />
        <!-- Column headers for double line -->
        <text :x="equivLineX - 10" :y="HEADER_Y + 10" text-anchor="end" font-size="8" fill="#888">等价值</text>
        <text :x="equivLineX + 10" :y="HEADER_Y + 10" text-anchor="start" font-size="8" fill="#888">当量值</text>
        <template v-for="(eq, ei) in equivLines" :key="'eq'+ei">
          <text :x="equivLineX - 10" :y="eq.y - 8" text-anchor="end" font-size="9" fill="#444">{{ eq.equivVal }}</text>
          <text :x="equivLineX - 10" :y="eq.y + 4" text-anchor="end" font-size="8" fill="#888">{{ eq.equivPct }}</text>
          <text :x="equivLineX + 10" :y="eq.y - 8" text-anchor="start" font-size="9" fill="#444">{{ eq.equalVal }}</text>
          <text :x="equivLineX + 10" :y="eq.y + 4" text-anchor="start" font-size="8" fill="#888">{{ eq.equalPct }}</text>
        </template>
      </g>

      <!-- Edges (non-product) with canonical 90° orthogonal routing -->
      <g class="edges-layer">
        <template v-for="e in visibleEdges" :key="e.edgeId">
          <template v-if="edgePath(e)">
            <path
              :d="edgePath(e)"
              :stroke="edgeStrokeColor(e)"
              :stroke-width="e.lineWidth || 2"
              fill="none"
              :stroke-dasharray="isBackflow(e) ? '6,3' : undefined"
              :marker-end="`url(#${markerId(edgeStrokeColor(e))})`"
            />
            <text
              v-if="edgeDualLabel(e).top"
              :x="edgeDualLabel(e).x"
              :y="edgeDualLabel(e).y"
              text-anchor="middle" font-size="10" fill="#333"
              stroke="#fff" stroke-width="3" stroke-linejoin="round" paint-order="stroke"
            >{{ edgeDualLabel(e).top }}</text>
            <text
              v-if="edgeDualLabel(e).bottom"
              :x="edgeDualLabel(e).x"
              :y="edgeDualLabel(e).y + 13"
              text-anchor="middle" font-size="9" fill="#666"
              stroke="#fff" stroke-width="2" stroke-linejoin="round" paint-order="stroke"
            >{{ edgeDualLabel(e).bottom }}</text>
          </template>
        </template>
      </g>

      <!-- Product output lines: black horizontal to right boundary with arrow -->
      <g class="product-lines-layer">
        <template v-for="(pl, pi) in productLines" :key="'pl'+pi">
          <line :x1="pl.x1" :y1="pl.y" :x2="cw - 30" :y2="pl.y"
            :stroke="pl.invalid ? '#E74C3C' : '#000'" stroke-width="2"
            :stroke-dasharray="pl.invalid ? '6,3' : undefined"
            :marker-end="`url(#${markerId(pl.invalid ? '#E74C3C' : '#000')})`" />
          <text
            v-if="pl.topText || pl.invalid"
            :x="(pl.x1 + cw - 30) / 2" :y="pl.y - 8"
            text-anchor="middle" font-size="10" :fill="pl.invalid ? '#E74C3C' : '#333'"
            stroke="#fff" stroke-width="3" stroke-linejoin="round" paint-order="stroke"
          >{{ pl.invalid ? '⚠ ' + (pl.topText || '非终端使用来源') : pl.topText }}</text>
          <text
            v-if="pl.bottomText"
            :x="(pl.x1 + cw - 30) / 2" :y="pl.y + 14"
            text-anchor="middle" font-size="9" :fill="pl.invalid ? '#E74C3C' : '#666'"
            stroke="#fff" stroke-width="2" stroke-linejoin="round" paint-order="stroke"
          >{{ pl.bottomText }}</text>
        </template>
      </g>

      <!-- Nodes (from layout, excluding product_output) -->
      <g class="nodes-layer">
        <template v-for="ln in shapeNodes" :key="ln.node.nodeId">
          <!-- energy_input: circle with name + total quantity -->
          <g v-if="ln.isCircle">
            <circle
              :cx="ln.cx" :cy="ln.cy" :r="ln.w / 2"
              :stroke="nodeColor(ln.node)" stroke-width="2" fill="#fff"
            />
            <!-- Energy name (line 1) -->
            <text :x="ln.cx" :y="ln.cy - 2" text-anchor="middle" font-size="10" :fill="nodeColor(ln.node)">
              {{ ln.node.label }}
            </text>
            <!-- Total quantity/unit (line 2) -->
            <text v-if="energyNodeTotalLine(ln.node)"
              :x="ln.cx" :y="ln.cy + 11" text-anchor="middle" font-size="8" fill="#666">
              {{ energyNodeTotalLine(ln.node) }}
            </text>
            <!-- Inventory 4-line fixed slots with arrows -->
            <g class="inventory-slots">
              <template v-for="(slot, si) in inventoryLines(ln.node)" :key="si">
                <!-- Arrow line geometry -->
                <line
                  :x1="ln.cx - ln.w / 2 - 50" :y1="ln.cy - ln.h / 2 + 10 + si * 15"
                  :x2="ln.cx - ln.w / 2 - 8" :y2="ln.cy - ln.h / 2 + 10 + si * 15"
                  stroke="#999" stroke-width="1"
                  :stroke-dasharray="slot.value ? undefined : '2,2'"
                />
                <!-- Arrow direction indicator (hidden when no data) -->
                <text v-if="slot.arrow"
                  :x="slot.arrow === '→' ? (ln.cx - ln.w / 2 - 6) : (ln.cx - ln.w / 2 - 52)"
                  :y="ln.cy - ln.h / 2 + 14 + si * 15"
                  font-size="9" fill="#999"
                >{{ slot.arrow }}</text>
                <!-- Label + value (hidden when no data) -->
                <text v-if="slot.value"
                  :x="ln.cx - ln.w / 2 - 55"
                  :y="ln.cy - ln.h / 2 + 14 + si * 15"
                  text-anchor="end" font-size="8" fill="#555"
                >{{ slot.label }}: {{ slot.value }}</text>
              </template>
            </g>
          </g>

          <!-- unit / custom: rectangle -->
          <g v-else>
            <rect
              :x="ln.cx - ln.w / 2" :y="ln.cy - ln.h / 2"
              :width="ln.w" :height="ln.h"
              :stroke="nodeColor(ln.node)" stroke-width="2" fill="#fff" rx="3"
            />
            <text
              :x="ln.cx" :y="ln.cy - 2"
              text-anchor="middle" font-size="11" fill="#222"
            >{{ ln.node.label }}</text>
            <text
              v-if="unitEfficiency(ln.node)"
              :x="ln.cx" :y="ln.cy + 12"
              text-anchor="middle" font-size="9" fill="#888"
            >{{ unitEfficiency(ln.node) }}</text>
          </g>
        </template>
      </g>

      <!-- Legend -->
      <g v-if="shapeNodes.length > 0" :transform="`translate(20, ${ch - 55})`">
        <rect width="320" height="45" fill="#f9f9f9" stroke="#ddd" rx="4" />
        <circle cx="16" cy="14" r="6" fill="#fff" stroke="#E74C3C" stroke-width="1.5" />
        <text x="28" y="18" font-size="9" fill="#333">能源输入(圆)</text>
        <rect x="95" y="8" width="12" height="12" fill="#fff" stroke="#3498DB" stroke-width="1.5" rx="2" />
        <text x="112" y="18" font-size="9" fill="#333">用能单元(矩形)</text>
        <rect x="195" y="8" width="12" height="12" fill="#f0fff0" stroke="#27AE60" stroke-width="1.5" rx="4" />
        <text x="212" y="18" font-size="9" fill="#333">产品产出</text>
        <line x1="12" y1="34" x2="40" y2="34" stroke="#E74C3C" stroke-width="2" />
        <text x="46" y="38" font-size="9" fill="#333">能源流</text>
        <line x1="80" y1="34" x2="108" y2="34" stroke="#000" stroke-width="2" />
        <text x="114" y="38" font-size="9" fill="#333">产品输出</text>
        <line x1="165" y1="34" x2="193" y2="34" stroke="#999" stroke-width="1.5" stroke-dasharray="4,2" />
        <text x="199" y="38" font-size="9" fill="#333">回流(虚线)</text>
        <line x1="252" y1="31" x2="252" y2="37" stroke="#666" stroke-width="1.5" />
        <line x1="256" y1="31" x2="256" y2="37" stroke="#666" stroke-width="1.5" />
        <text x="262" y="38" font-size="9" fill="#333">等价/当量线</text>
      </g>
    </svg>
  </div>
</template>

<style scoped lang="scss">
.config-view-wrapper {
  width: 100%;
  height: 100%;
  overflow: auto;
}
.config-view-canvas {
  display: block;
}
</style>
