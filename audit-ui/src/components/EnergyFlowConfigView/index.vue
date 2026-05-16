<script setup lang="ts">
import { ref, computed } from 'vue'
import type {
  FlowNodeConfig, FlowEdgeConfig, FlowRecord,
  EnergyInfo, UnitInfo, ProductInfo, EnergyConsumptionInfo,
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
}>()

defineExpose({ exportPng, fitView })

const svgRef = ref<SVGSVGElement>()
const cw = computed(() => props.canvasWidth || 1200)
const ch = computed(() => props.canvasHeight || 800)

// ── Number formatting ──────────────────────────────────────
function fmtNum(v: number | null | undefined, decimals = 1): string {
  if (v == null || isNaN(v)) return ''
  const fixed = v.toFixed(decimals)
  const parts = fixed.split('.')
  parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
  return parts.join('.')
}
function fmtPct(v: number | null | undefined): string {
  if (v == null || isNaN(v)) return ''
  return fmtNum(v * 100, 2) + '%'
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
const nodeById = computed(() => {
  const m = new Map<string, FlowNodeConfig>()
  props.nodes.forEach(n => m.set(n.nodeId, n))
  return m
})

// ── Visible elements ───────────────────────────────────────
const visibleNodes = computed(() => props.nodes.filter(n => (n.visible ?? 1) !== 0))
const visibleEdges = computed(() => props.edges.filter(e => (e.visible ?? 1) !== 0))

// ── Stage classification ───────────────────────────────────
const STAGE_LABELS = ['购入贮存环节', '加工转换环节', '输送分配环节', '终端使用环节']
const STAGE_MARGIN = 80
const stageWidth = computed(() => (cw.value - STAGE_MARGIN * 2) / 4)
const stageX = computed(() => STAGE_LABELS.map((_, i) => STAGE_MARGIN + i * stageWidth.value))
const stageDividers = computed(() => [stageX.value[1], stageX.value[2], stageX.value[3]])
const HEADER_Y = 55

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
  const sx = stageX.value
  for (let i = 3; i >= 0; i--) { if (n.positionX >= sx[i] - stageWidth.value * 0.1) return i }
  return 0
}

// ── Node rendering helpers ─────────────────────────────────
const NODE_COLORS: Record<string, string> = {
  energy_input: '#E74C3C', unit: '#3498DB', product_output: '#27AE60', custom: '#95A5A6',
}
function nodeColor(n: FlowNodeConfig): string { return n.color || NODE_COLORS[n.nodeType] || '#666' }
function nodeCx(n: FlowNodeConfig): number { return n.positionX + (n.width || 60) / 2 }
function nodeCy(n: FlowNodeConfig): number { return n.positionY + (n.height || 60) / 2 }
function nodeR(n: FlowNodeConfig): number { return (n.width || 60) / 2 }

// ── Inventory 4-line indicators (energy_input circles) ─────
function inventoryLines(n: FlowNodeConfig): { label: string; value: string }[] | null {
  if (n.nodeType !== 'energy_input' || !n.refId) return null
  const cons = consumptionByEnergyId.value.get(n.refId)
  if (!cons) return null
  return [
    { label: '期初', value: fmtNum(cons.openingStock) },
    { label: '购入', value: fmtNum(cons.purchaseTotal) },
    { label: '外供', value: fmtNum(cons.externalSupply) },
    { label: '期末', value: fmtNum(cons.closingStock) },
  ]
}

// ── Unit efficiency (加工转换 / 输送分配 only) ──────────────
function unitEfficiency(n: FlowNodeConfig): string {
  if (n.nodeType !== 'unit' || !n.refId) return ''
  const u = unitMap.value.get(n.refId)
  if (!u || (u.unitType !== 1 && u.unitType !== 2)) return ''
  let inputTotal = 0, outputTotal = 0
  for (const e of visibleEdges.value) {
    if (e.itemType !== 'energy') continue
    const v = e.calculatedValue ?? 0
    if (e.targetNodeId === n.nodeId) inputTotal += v
    if (e.sourceNodeId === n.nodeId) outputTotal += v
  }
  if (inputTotal <= 0) return ''
  return fmtPct(outputTotal / inputTotal)
}

// ── Equivalence / equivalent double lines (stage 0->1) ─────
interface EquivLine { y: number; equivVal: string; equalVal: string; equivPct: string; equalPct: string }
const equivLines = computed<EquivLine[]>(() => {
  if (!props.energyConsumption?.length) return []
  const energyNodes = visibleNodes.value.filter(n => n.nodeType === 'energy_input' && n.refId)
  let totalEquiv = 0, totalEqual = 0
  const items: { cy: number; equiv: number; equal: number }[] = []
  for (const n of energyNodes) {
    const cons = consumptionByEnergyId.value.get(n.refId!)
    const en = energyMap.value.get(n.refId!)
    if (!cons || !en) continue
    const usage = (cons.purchaseTotal ?? 0) + (cons.openingStock ?? 0)
      - (cons.closingStock ?? 0) - (cons.externalSupply ?? 0)
    const equivF = en.equivalentValue ?? cons.equivFactor ?? 0
    const equalF = en.equalValue ?? cons.equalFactor ?? 0
    const equiv = usage * equivF
    const equal = usage * equalF
    totalEquiv += equiv; totalEqual += equal
    items.push({ cy: nodeCy(n), equiv, equal })
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

// ── Edge rendering ─────────────────────────────────────────
function isBackflow(e: FlowEdgeConfig): boolean {
  const s = nodeById.value.get(e.sourceNodeId)
  const t = nodeById.value.get(e.targetNodeId)
  if (!s || !t) return false
  return nodeStage(s) > nodeStage(t)
}
function isProductEdge(e: FlowEdgeConfig): boolean { return e.itemType === 'product' }

function edgeStrokeColor(e: FlowEdgeConfig): string {
  if (e.color) return e.color
  if (isProductEdge(e)) return '#000'
  if (e.itemId && e.itemType === 'energy') {
    const en = energyMap.value.get(e.itemId)
    if (en?.color) return en.color
  }
  const COLORS = ['#E74C3C', '#3498DB', '#27AE60', '#F39C12', '#9B59B6', '#1ABC9C', '#E67E22']
  return COLORS[visibleEdges.value.indexOf(e) % COLORS.length]
}

function srcExitPt(n: FlowNodeConfig): { x: number; y: number } {
  if (n.nodeType === 'energy_input') {
    const r = nodeR(n)
    return { x: n.positionX + r * 2, y: n.positionY + r }
  }
  return { x: n.positionX + (n.width || 100), y: n.positionY + (n.height || 50) / 2 }
}
function tgtEntryPt(n: FlowNodeConfig): { x: number; y: number } {
  if (n.nodeType === 'energy_input') {
    const r = nodeR(n)
    return { x: n.positionX, y: n.positionY + r }
  }
  return { x: n.positionX, y: n.positionY + (n.height || 50) / 2 }
}

function edgePath(edge: FlowEdgeConfig): string {
  const sn = nodeById.value.get(edge.sourceNodeId)
  const tn = nodeById.value.get(edge.targetNodeId)
  if (!sn || !tn) return ''
  const s = srcExitPt(sn), t = tgtEntryPt(tn)

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
    const topY = HEADER_Y + 10
    return `M ${s.x} ${s.y} L ${s.x} ${topY} L ${t.x} ${topY} L ${t.x} ${t.y}`
  }
  if (isProductEdge(edge)) {
    const rightX = cw.value - 30
    return `M ${s.x} ${s.y} L ${rightX} ${s.y}`
  }
  const midX = (s.x + t.x) / 2
  return `M ${s.x} ${s.y} L ${midX} ${s.y} L ${midX} ${t.y} L ${t.x} ${t.y}`
}

// ── Dual-line labels on edges ──────────────────────────────
interface DualLabel { x: number; y: number; top: string; bottom: string }
function edgeDualLabel(edge: FlowEdgeConfig): DualLabel {
  const sn = nodeById.value.get(edge.sourceNodeId)
  const tn = nodeById.value.get(edge.targetNodeId)
  if (!sn || !tn) return { x: 0, y: 0, top: '', bottom: '' }
  const s = srcExitPt(sn), t = tgtEntryPt(tn)

  let lx: number, ly: number
  if (isProductEdge(edge)) {
    lx = (s.x + cw.value - 30) / 2; ly = s.y - 6
  } else if (isBackflow(edge)) {
    lx = (s.x + t.x) / 2; ly = HEADER_Y + 4
  } else {
    lx = (s.x + t.x) / 2; ly = (s.y + t.y) / 2 - 4
  }

  let topTxt = '', bottomTxt = ''
  if (edge.itemType === 'energy') {
    const en = edge.itemId ? energyMap.value.get(edge.itemId) : undefined
    const name = en?.name ?? ''
    const unit = en?.measurementUnit ?? ''
    topTxt = fmtNum(edge.physicalQuantity)
      ? `${name} ${fmtNum(edge.physicalQuantity)}${unit}`
      : name
    bottomTxt = fmtNum(edge.calculatedValue)
      ? `${fmtNum(edge.calculatedValue)} tce`
      : ''
  } else if (edge.itemType === 'product') {
    const pr = edge.itemId ? productMap.value.get(edge.itemId) : undefined
    topTxt = pr?.name ?? ''
    bottomTxt = fmtNum(edge.physicalQuantity)
      ? `${fmtNum(edge.physicalQuantity)} ${pr?.measurementUnit ?? ''}`
      : ''
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

      <!-- Edges -->
      <g class="edges-layer">
        <template v-for="e in visibleEdges" :key="e.edgeId">
          <path
            :d="edgePath(e)"
            :stroke="edgeStrokeColor(e)"
            :stroke-width="e.lineWidth || 2"
            fill="none"
            :stroke-dasharray="isBackflow(e) ? '6,3' : undefined"
            :marker-end="`url(#${markerId(edgeStrokeColor(e))})`"
          />
          <!-- Dual-line label: top + bottom -->
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
      </g>

      <!-- Nodes -->
      <g class="nodes-layer">
        <template v-for="n in visibleNodes" :key="n.nodeId">
          <!-- energy_input: circle -->
          <g v-if="n.nodeType === 'energy_input'">
            <circle
              :cx="nodeCx(n)" :cy="nodeCy(n)" :r="nodeR(n)"
              :stroke="nodeColor(n)" stroke-width="2" fill="#fff"
            />
            <text :x="nodeCx(n)" :y="nodeCy(n) + 4" text-anchor="middle" font-size="11" :fill="nodeColor(n)">
              {{ n.label }}
            </text>
            <!-- Inventory 4-line indicators -->
            <template v-if="inventoryLines(n)">
              <text
                v-for="(line, li) in inventoryLines(n)!" :key="li"
                :x="nodeCx(n) - nodeR(n) - 4"
                :y="n.positionY + 8 + li * 13"
                text-anchor="end" font-size="9" fill="#555"
              >{{ line.label }}: {{ line.value }}</text>
            </template>
          </g>

          <!-- unit: rectangle -->
          <g v-else-if="n.nodeType === 'unit'">
            <rect
              :x="n.positionX" :y="n.positionY"
              :width="n.width || 100" :height="n.height || 50"
              :stroke="nodeColor(n)" stroke-width="2" fill="#fff" rx="3"
            />
            <text
              :x="n.positionX + (n.width || 100) / 2"
              :y="n.positionY + (n.height || 50) / 2 - 2"
              text-anchor="middle" font-size="11" fill="#222"
            >{{ n.label }}</text>
            <text
              v-if="unitEfficiency(n)"
              :x="n.positionX + (n.width || 100) / 2"
              :y="n.positionY + (n.height || 50) / 2 + 12"
              text-anchor="middle" font-size="9" fill="#888"
            >{{ unitEfficiency(n) }}</text>
          </g>

          <!-- product_output: rectangle with green fill -->
          <g v-else-if="n.nodeType === 'product_output'">
            <rect
              :x="n.positionX" :y="n.positionY"
              :width="n.width || 100" :height="n.height || 50"
              :stroke="nodeColor(n)" stroke-width="2" fill="#f0fff0" rx="8"
            />
            <text
              :x="n.positionX + (n.width || 100) / 2"
              :y="n.positionY + (n.height || 50) / 2 + 4"
              text-anchor="middle" font-size="11" fill="#222"
            >{{ n.label }}</text>
          </g>

          <!-- custom / fallback: rectangle -->
          <g v-else>
            <rect
              :x="n.positionX" :y="n.positionY"
              :width="n.width || 100" :height="n.height || 50"
              :stroke="nodeColor(n)" stroke-width="2" fill="#fff" rx="3"
            />
            <text
              :x="n.positionX + (n.width || 100) / 2"
              :y="n.positionY + (n.height || 50) / 2 + 4"
              text-anchor="middle" font-size="11" fill="#222"
            >{{ n.label }}</text>
          </g>
        </template>
      </g>

      <!-- Legend -->
      <g v-if="visibleNodes.length > 0" :transform="`translate(20, ${ch - 55})`">
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
