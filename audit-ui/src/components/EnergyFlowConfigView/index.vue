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
// Thousands separator, default 1 decimal, hide trailing .0,
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
// Percentages: 2 decimals, format as (12.45%)
function fmtPct(v: number | null | undefined): string {
  if (v == null || (typeof v === 'number' && isNaN(v))) return ''
  const pctVal = v * 100
  const fixed = pctVal.toFixed(2)
  const cleaned = fixed.replace(/\.?0+$/, '')
  const parts = cleaned.split('.')
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

// ── Stage layout constants ────────────────────────────────
const STAGE_LABELS = ['购入贮存环节', '加工转换环节', '输送分配环节', '终端使用环节']
const STAGE_MARGIN = 80
const HEADER_Y = 55
const BODY_TOP = HEADER_Y + 20
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

const layoutNodes = computed<LayoutNode[]>(() => {
  const visible = props.nodes.filter(n => (n.visible ?? 1) !== 0)
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
      const cy = BODY_TOP + rowIdx * ROW_H + ROW_H / 2
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

const shapeNodes = computed(() => layoutNodes.value.filter(ln => ln.node.nodeType !== 'product_output'))

const visibleEdges = computed(() => props.edges.filter(e => (e.visible ?? 1) !== 0))

function resolveRecord(e: FlowEdgeConfig): FlowRecord | undefined {
  if (e.flowRecordId) return recordById.value.get(e.flowRecordId)
  return undefined
}

// ── Node rendering helpers ─────────────────────────────────
const NODE_COLORS: Record<string, string> = {
  energy_input: '#E74C3C', unit: '#3498DB', product_output: '#27AE60', custom: '#95A5A6',
}
function nodeColor(n: FlowNodeConfig): string { return n.color || NODE_COLORS[n.nodeType] || '#666' }

// ── Inventory 4-line indicators (energy_input circles) ────
// Order: 期末库存 / 外供 / 期初库存 / 购入 (canonical spec)
// Always show fixed slots with unit; blank value when no data
function inventoryLines(n: FlowNodeConfig): { label: string; value: string }[] {
  if (n.nodeType !== 'energy_input') return []
  const en = n.refId ? energyMap.value.get(n.refId) : undefined
  const unit = en?.measurementUnit ?? ''
  const cons = n.refId ? consumptionByEnergyId.value.get(n.refId) : undefined
  const fmt = (v: number | null | undefined) => {
    const s = fmtNum(v)
    return s ? s + ' ' + unit : ''
  }
  return [
    { label: '期末库存', value: fmt(cons?.closingStock) },
    { label: '外供', value: fmt(cons?.externalSupply) },
    { label: '期初库存', value: fmt(cons?.openingStock) },
    { label: '购入', value: fmt(cons?.purchaseTotal) },
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

// ── Equivalence / equivalent double lines (stage 0->1) ─────
interface EquivLine { y: number; equivVal: string; equalVal: string; equivPct: string; equalPct: string }
const equivLines = computed<EquivLine[]>(() => {
  const energyNodes = layoutNodes.value.filter(ln => ln.node.nodeType === 'energy_input' && ln.node.refId)
  if (!energyNodes.length) return []
  let totalEquiv = 0, totalEqual = 0
  const items: { cy: number; equiv: number; equal: number }[] = []
  for (const ln of energyNodes) {
    const cons = consumptionByEnergyId.value.get(ln.node.refId!)
    const en = energyMap.value.get(ln.node.refId!)
    if (!cons || !en) continue
    const usage = (cons.purchaseTotal ?? 0) + (cons.openingStock ?? 0)
      - (cons.closingStock ?? 0) - (cons.externalSupply ?? 0)
    const equivF = en.equivalentValue ?? cons.equivFactor ?? 0
    const equalF = en.equalValue ?? cons.equalFactor ?? 0
    const equiv = usage * equivF
    const equal = usage * equalF
    totalEquiv += equiv; totalEqual += equal
    items.push({ cy: ln.cy, equiv, equal })
  }
  return items.map(it => ({
    y: it.cy,
    equivVal: fmtNum(it.equiv),
    equalVal: fmtNum(it.equal),
    equivPct: totalEquiv > 0 ? fmtPct(it.equiv / totalEquiv) : '',
    equalPct: totalEqual > 0 ? fmtPct(it.equal / totalEqual) : '',
  }))
})
const equivLineX = computed(() => stageDividers.value[0] ?? stageX.value[1] ?? cw.value * 0.3)

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

function edgePath(edge: FlowEdgeConfig): string {
  const sln = layoutNodeMap.value.get(edge.sourceNodeId)
  const tln = layoutNodeMap.value.get(edge.targetNodeId)
  if (!sln || !tln) return ''
  if (isProductEdge(edge)) return '' // product edges are rendered as productLines
  const s = srcExitPt(sln), t = tgtEntryPt(tln)

  if (edge.routePoints) {
    try {
      const pts = JSON.parse(edge.routePoints) as { x: number; y: number }[]
      if (pts.length) {
        let d = `M ${s.x} ${s.y}`
        for (const p of pts) d += ` L ${p.x} ${p.y}`
        return d + ` L ${t.x} ${t.y}`
      }
    } catch { /* fall through */ }
  }
  if (isBackflow(edge)) {
    const topY = HEADER_Y - 5
    return `M ${s.x} ${s.y} L ${s.x} ${topY} L ${t.x} ${topY} L ${t.x} ${t.y}`
  }
  const midX = (s.x + t.x) / 2
  return `M ${s.x} ${s.y} L ${midX} ${s.y} L ${midX} ${t.y} L ${t.x} ${t.y}`
}

// ── Product output lines: horizontal to right boundary ──────
interface ProductLine { y: number; x1: number; topText: string; bottomText: string }
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
    results.push({ y: sln.cy, x1: sln.cx + sln.w / 2, topText, bottomText })
  }
  return results
})

// ── Dual-line labels on edges (resolved from flowRecords) ──
interface DualLabel { x: number; y: number; top: string; bottom: string }
function edgeDualLabel(edge: FlowEdgeConfig): DualLabel {
  if (isProductEdge(edge)) return { x: 0, y: 0, top: '', bottom: '' } // handled by productLines
  const sln = layoutNodeMap.value.get(edge.sourceNodeId)
  const tln = layoutNodeMap.value.get(edge.targetNodeId)
  if (!sln || !tln) return { x: 0, y: 0, top: '', bottom: '' }
  const s = srcExitPt(sln), t = tgtEntryPt(tln)

  let lx: number, ly: number
  if (isBackflow(edge)) {
    lx = (s.x + t.x) / 2; ly = HEADER_Y - 12
  } else {
    lx = (s.x + t.x) / 2; ly = (s.y + t.y) / 2 - 4
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
    const unit = en?.measurementUnit ?? ''
    topTxt = fmtNum(pq) ? `${name} ${fmtNum(pq)} ${unit}` : name
    bottomTxt = fmtNum(cv) ? `${fmtNum(cv)} tce` : ''
  } else {
    topTxt = edge.labelText || ''
  }
  return { x: lx, y: ly, top: topTxt, bottom: bottomTxt }
}

// ── Arrow marker IDs ───────────────────────────────────────
const markerColors = computed(() => {
  const s = new Set<string>()
  visibleEdges.value.forEach(e => s.add(edgeStrokeColor(e)))
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

      <!-- Equivalence / equivalent double vertical lines between stage 0 and 1 -->
      <g v-if="equivLines.length" class="equiv-lines">
        <line :x1="equivLineX - 3" :y1="HEADER_Y + 12" :x2="equivLineX - 3" :y2="ch - 50" stroke="#666" stroke-width="1.5" />
        <line :x1="equivLineX + 3" :y1="HEADER_Y + 12" :x2="equivLineX + 3" :y2="ch - 50" stroke="#666" stroke-width="1.5" />
        <template v-for="(eq, ei) in equivLines" :key="'eq'+ei">
          <text :x="equivLineX - 10" :y="eq.y - 8" text-anchor="end" font-size="9" fill="#444">{{ eq.equivVal }}</text>
          <text :x="equivLineX - 10" :y="eq.y + 4" text-anchor="end" font-size="8" fill="#888">{{ eq.equivPct }}</text>
          <text :x="equivLineX + 10" :y="eq.y - 8" text-anchor="start" font-size="9" fill="#444">{{ eq.equalVal }}</text>
          <text :x="equivLineX + 10" :y="eq.y + 4" text-anchor="start" font-size="8" fill="#888">{{ eq.equalPct }}</text>
        </template>
      </g>

      <!-- Edges (non-product) -->
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

      <!-- Product output lines: black horizontal to right boundary -->
      <g class="product-lines-layer">
        <template v-for="(pl, pi) in productLines" :key="'pl'+pi">
          <line :x1="pl.x1" :y1="pl.y" :x2="cw - 30" :y2="pl.y" stroke="#000" stroke-width="2" />
          <text
            v-if="pl.topText"
            :x="(pl.x1 + cw - 30) / 2" :y="pl.y - 8"
            text-anchor="middle" font-size="10" fill="#333"
            stroke="#fff" stroke-width="3" stroke-linejoin="round" paint-order="stroke"
          >{{ pl.topText }}</text>
          <text
            v-if="pl.bottomText"
            :x="(pl.x1 + cw - 30) / 2" :y="pl.y + 14"
            text-anchor="middle" font-size="9" fill="#666"
            stroke="#fff" stroke-width="2" stroke-linejoin="round" paint-order="stroke"
          >{{ pl.bottomText }}</text>
        </template>
      </g>

      <!-- Nodes (from layout, excluding product_output which are rendered as lines) -->
      <g class="nodes-layer">
        <template v-for="ln in shapeNodes" :key="ln.node.nodeId">
          <!-- energy_input: circle -->
          <g v-if="ln.isCircle">
            <circle
              :cx="ln.cx" :cy="ln.cy" :r="ln.w / 2"
              :stroke="nodeColor(ln.node)" stroke-width="2" fill="#fff"
            />
            <text :x="ln.cx" :y="ln.cy + 4" text-anchor="middle" font-size="11" :fill="nodeColor(ln.node)">
              {{ ln.node.label }}
            </text>
            <!-- Inventory 4-line indicators -->
            <template v-if="inventoryLines(ln.node).length">
              <text
                v-for="(line, li) in inventoryLines(ln.node)" :key="li"
                :x="ln.cx - ln.w / 2 - 4"
                :y="ln.cy - ln.h / 2 + 8 + li * 13"
                text-anchor="end" font-size="9" fill="#555"
              >{{ line.label }}: {{ line.value }}</text>
            </template>
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
