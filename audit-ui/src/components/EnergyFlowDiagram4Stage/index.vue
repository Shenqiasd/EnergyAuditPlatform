<script setup lang="ts">
import { computed, ref } from 'vue'
import type { EnergyFlowItem } from '@/api/energyFlow'
import type { BsUnit, BsEnergy } from '@/api/setting'

/**
 * 4-stage energy flow diagram (read-only, rule-based layout, orthogonal routing).
 *
 * Columns (left → right):
 *   ① 购入储存环节  — circle nodes for each purchased energy (raw materials entering the plant)
 *   ② 加工转换环节  — rectangle nodes for bs_unit.unit_type = 1
 *   ③ 输送分配环节  — rectangle nodes for bs_unit.unit_type = 2
 *   ④ 终端使用环节  — rectangle nodes for bs_unit.unit_type = 3 (+ virtual "产出")
 *
 * Edges are routed with strictly horizontal / vertical polylines (Z-shape),
 * using port slots on each node and channel-x offsets between columns to
 * avoid overlap.
 */

type Layer = 1 | 2 | 3 | 4

interface FlowNode {
  id: string
  label: string
  layer: Layer
  shape: 'circle' | 'rect'
  row: number
  cx: number
  cy: number
  w: number
  h: number
  energyProducts: string[]
  physicalSum: number
  standardSum: number
}

interface FlowEdge {
  source: string
  target: string
  energyProduct: string
  physicalQuantity: number
  standardQuantity: number
  color: string
  path: string
  labelX: number
  labelY: number
  labelText: string
}

const props = defineProps<{
  flows: EnergyFlowItem[]
  units: BsUnit[]
  energies: BsEnergy[]
}>()

defineExpose({ exportPng, fitView })

// ------------------------------------------------------------
// Visual constants
// ------------------------------------------------------------
const HEADER_H = 56               // top section header band height
const FOOTER_H = 48               // bottom energy-total summary band height
const CONTENT_TOP = HEADER_H + 8
const ROW_H = 80                  // vertical spacing between rows in a column
const MIN_CANVAS_H = 520

const COL_W = [240, 200, 200, 200]  // column widths (col 1 wider because it has data labels)
const COL_GAP = 60                   // channel width between columns
const CANVAS_PAD_X = 20

const CIRCLE_R = 28
const RECT_W = 90
const RECT_H = 46

const ENERGY_COLORS: Record<string, string> = {
  '电力': '#E74C3C',
  '天然气': '#3498DB',
  '蒸汽': '#27AE60',
  '原煤': '#2C3E50',
  '煤炭': '#2C3E50',
  '无烟煤': '#34495E',
  '烟煤': '#5D6D7E',
  '洗精煤': '#283747',
  '焦炭': '#1B2631',
  '焦炉煤气': '#7D3C98',
  '柴油': '#F39C12',
  '汽油': '#E67E22',
  '燃料油': '#D35400',
  '液化石油气': '#9B59B6',
  '热力': '#E91E63',
  '余热': '#C2185B',
  '太阳能': '#FF9800',
  '综合能源': '#607D8B',
  '压缩空气': '#1ABC9C',
  '水': '#2980B9',
  '氢气': '#00BCD4',
}
const FALLBACK_COLORS = ['#1ABC9C', '#8E44AD', '#34495E', '#16A085', '#C0392B', '#2980B9', '#F1C40F']

function getEnergyColor(product: string): string {
  if (ENERGY_COLORS[product]) return ENERGY_COLORS[product]
  for (const key of Object.keys(ENERGY_COLORS)) {
    if (product.includes(key) || key.includes(product)) return ENERGY_COLORS[key]
  }
  let hash = 0
  for (let i = 0; i < product.length; i++) hash = ((hash << 5) - hash + product.charCodeAt(i)) | 0
  return FALLBACK_COLORS[Math.abs(hash) % FALLBACK_COLORS.length]
}

function formatNum(n: number | undefined | null): string {
  if (n === undefined || n === null) return ''
  if (n === 0) return '0'
  const abs = Math.abs(n)
  if (abs >= 10000) return n.toFixed(0)
  if (abs >= 100) return n.toFixed(1)
  return n.toFixed(2)
}

// ------------------------------------------------------------
// Column x positions (left edge + center)
// ------------------------------------------------------------
const colLeft = computed<number[]>(() => {
  const xs: number[] = []
  let x = CANVAS_PAD_X
  for (let i = 0; i < 4; i++) {
    xs.push(x)
    x += COL_W[i] + COL_GAP
  }
  return xs
})

const colCenter = computed<number[]>(() => colLeft.value.map((x, i) => x + COL_W[i] / 2))

// channelX[i] = x-position of the vertical routing channel immediately to the
// right of column i (0-based). We have 4 columns → 4 channels; the last one
// (index 3) is a virtual channel to the right of column 4, used by same-layer
// edges (e.g. 终端使用 → 产出) that would otherwise overflow the 3-channel
// array and produce NaN SVG paths.
const channelX = computed<number[]>(() =>
  [0, 1, 2, 3].map(i => colLeft.value[i] + COL_W[i] + (i < 3 ? COL_GAP / 2 : CANVAS_PAD_X / 2))
)

// ------------------------------------------------------------
// Layering logic — decides which column a unit name belongs to.
// ------------------------------------------------------------
function layerForUnit(name: string): Layer | null {
  if (!name) return null
  if (name === '产出') return 4
  if (name === '外购') return 1
  // 1) Energy product names live in column 1 (purchase) as circular source nodes
  const energy = props.energies.find(e => e.name === name)
  if (energy) return 1
  // 2) bs_unit.unit_type (1=加工转换, 2=输送分配, 3=终端使用)
  const unit = props.units.find(u => u.name === name)
  if (unit) return (unit.unitType + 1) as Layer
  return null
}

// Fallback layer inference based on flow_stage (for names that are neither in
// bs_unit nor bs_energy — e.g. ad-hoc strings in legacy data).
function stageLayer(stage: string, side: 'source' | 'target'): Layer | null {
  const s = (stage || '').toLowerCase()
  if (s === 'purchased') return side === 'source' ? 1 : 1
  if (s === 'conversion') return side === 'source' ? 2 : 2
  if (s === 'distribution') return side === 'source' ? 2 : 3
  if (s === 'terminal') return side === 'source' ? 3 : 4
  return null
}

// ------------------------------------------------------------
// Build nodes & edges — the heart of the component.
// ------------------------------------------------------------
interface Built {
  nodes: FlowNode[]
  edges: FlowEdge[]
  canvasW: number
  canvasH: number
  energyTotals: { product: string; totalStandard: number; color: string }[]
}

const built = computed<Built>(() => buildDiagram(props.flows))

function buildDiagram(flows: EnergyFlowItem[]): Built {
  const nodeMap = new Map<string, FlowNode>()

  // Register a unit name into the node map, deciding its layer.
  function touch(name: string, side: 'source' | 'target', stage: string): FlowNode | null {
    if (!name) return null
    let layer = layerForUnit(name)
    if (layer === null) layer = stageLayer(stage, side)
    // Ignore the virtual '外购' marker — it's an abstract origin, not a rendered node.
    // The rendered "purchased" circle is the energy product (原煤 / 天然气 / 电力).
    if (name === '外购') return null
    if (layer === null) return null
    let existing = nodeMap.get(name)
    if (!existing) {
      existing = {
        id: name,
        label: name,
        layer,
        shape: layer === 1 ? 'circle' : 'rect',
        row: 0,
        cx: 0,
        cy: 0,
        w: layer === 1 ? CIRCLE_R * 2 : RECT_W,
        h: layer === 1 ? CIRCLE_R * 2 : RECT_H,
        energyProducts: [],
        physicalSum: 0,
        standardSum: 0,
      }
      nodeMap.set(name, existing)
    }
    return existing
  }

  // Walk flows to build nodes & aggregate purchase totals.
  const rawEdges: {
    src: FlowNode
    dst: FlowNode
    energyProduct: string
    physical: number
    standard: number
  }[] = []

  for (const f of flows) {
    const srcNode = touch(f.sourceUnit, 'source', f.flowStage)
    const dstNode = touch(f.targetUnit, 'target', f.flowStage)

    // "外购 → 原煤" rows: accumulate purchase amount onto the destination energy circle.
    if (f.sourceUnit === '外购' && dstNode) {
      dstNode.physicalSum += Number(f.physicalQuantity || 0)
      dstNode.standardSum += Number(f.standardQuantity || 0)
      if (!dstNode.energyProducts.includes(f.energyProduct)) {
        dstNode.energyProducts.push(f.energyProduct)
      }
      continue // no rendered edge for external-purchase arrow itself
    }

    if (!srcNode || !dstNode) continue
    if (srcNode.id === dstNode.id) continue
    rawEdges.push({
      src: srcNode,
      dst: dstNode,
      energyProduct: f.energyProduct,
      physical: Number(f.physicalQuantity || 0),
      standard: Number(f.standardQuantity || 0),
    })
  }

  // Group nodes by layer.
  const layerNodes: FlowNode[][] = [[], [], [], []]
  for (const n of nodeMap.values()) layerNodes[n.layer - 1].push(n)

  // Sort each column: purchase by energy name, others by first-seen order.
  layerNodes[0].sort((a, b) => a.label.localeCompare(b.label, 'zh'))

  // Assign row positions (y) in each column.
  const maxRows = Math.max(1, ...layerNodes.map(arr => arr.length))
  const canvasH = Math.max(MIN_CANVAS_H, CONTENT_TOP + maxRows * ROW_H + FOOTER_H + 40)
  const contentH = canvasH - CONTENT_TOP - FOOTER_H
  for (let li = 0; li < 4; li++) {
    const nodes = layerNodes[li]
    const n = nodes.length
    if (n === 0) continue
    const step = contentH / (n + 1)
    nodes.forEach((node, i) => {
      node.row = i
      node.cx = colCenter.value[li]
      node.cy = CONTENT_TOP + step * (i + 1)
    })
  }

  const canvasW = colLeft.value[3] + COL_W[3] + CANVAS_PAD_X

  // ------------- Edge aggregation + orthogonal routing -------------
  // Collapse edges with the same (src, dst, energyProduct) tuple.
  const edgeKey = (s: string, t: string, e: string) => `${s}→${t}|${e}`
  const edgeAgg = new Map<string, typeof rawEdges[number]>()
  for (const re of rawEdges) {
    const k = edgeKey(re.src.id, re.dst.id, re.energyProduct)
    const ex = edgeAgg.get(k)
    if (ex) {
      ex.physical += re.physical
      ex.standard += re.standard
    } else {
      edgeAgg.set(k, { ...re })
    }
  }
  const aggList = Array.from(edgeAgg.values())

  // Port slots: for each node, distribute in/out edges along the right/left side.
  const outByNode = new Map<string, typeof aggList>()
  const inByNode = new Map<string, typeof aggList>()
  for (const e of aggList) {
    const outs = outByNode.get(e.src.id) ?? []
    outs.push(e)
    outByNode.set(e.src.id, outs)
    const ins = inByNode.get(e.dst.id) ?? []
    ins.push(e)
    inByNode.set(e.dst.id, ins)
  }
  // Sort by target y for outs, by source y for ins so port order matches flow direction.
  for (const outs of outByNode.values()) outs.sort((a, b) => a.dst.cy - b.dst.cy)
  for (const ins of inByNode.values()) ins.sort((a, b) => a.src.cy - b.src.cy)

  function portY(n: FlowNode, index: number, count: number, side: 'right' | 'left'): number {
    void side
    if (count <= 1) return n.cy
    const usable = Math.min(n.h - 8, (count - 1) * 6 + 12)
    const step = usable / (count - 1)
    return n.cy - usable / 2 + index * step
  }

  function nodeRightX(n: FlowNode): number {
    return n.shape === 'circle' ? n.cx + CIRCLE_R : n.cx + RECT_W / 2
  }
  function nodeLeftX(n: FlowNode): number {
    return n.shape === 'circle' ? n.cx - CIRCLE_R : n.cx - RECT_W / 2
  }

  // Channel slot allocation — multiple edges sharing the same column gap get different X offsets.
  const channelBuckets: Map<number, typeof aggList> = new Map()
  const maxChIdxAlloc = 3 // channelX length - 1
  for (const e of aggList) {
    const span = e.dst.layer - e.src.layer
    let cIdx: number
    if (span >= 2) cIdx = e.dst.layer - 2
    else if (span === 1) cIdx = e.src.layer - 1
    else cIdx = Math.min(Math.max(e.src.layer - 1, e.dst.layer - 1), maxChIdxAlloc)
    cIdx = Math.min(Math.max(cIdx, 0), maxChIdxAlloc)
    const arr = channelBuckets.get(cIdx) ?? []
    arr.push(e)
    channelBuckets.set(cIdx, arr)
  }
  const channelOffsetMap = new Map<string, number>()
  for (const [cIdx, list] of channelBuckets) {
    // Sort by (src.cy, dst.cy) to get a deterministic, readable order.
    list.sort((a, b) => (a.src.cy - b.src.cy) || (a.dst.cy - b.dst.cy))
    list.forEach((e, i) => {
      const n = list.length
      const offset = (i - (n - 1) / 2) * 8
      channelOffsetMap.set(edgeKey(e.src.id, e.dst.id, e.energyProduct), offset)
      void cIdx
    })
  }

  // Build final edges with SVG path strings.
  const edges: FlowEdge[] = aggList.map(e => {
    const color = getEnergyColor(e.energyProduct)
    const outs = outByNode.get(e.src.id)!
    const ins = inByNode.get(e.dst.id)!
    const outIdx = outs.indexOf(e)
    const inIdx = ins.indexOf(e)
    const sy = portY(e.src, outIdx, outs.length, 'right')
    const ty = portY(e.dst, inIdx, ins.length, 'left')
    const sx = nodeRightX(e.src)
    const tx = nodeLeftX(e.dst)
    const off = channelOffsetMap.get(edgeKey(e.src.id, e.dst.id, e.energyProduct)) ?? 0
    const span = e.dst.layer - e.src.layer
    // Pick the vertical routing channel safely for all spans (including
    // same-layer and backward edges). channelX has 4 slots (one after each
    // column); we clamp the index so layer-4 → layer-4 edges use the virtual
    // right-of-col-4 channel instead of producing NaN.
    const maxIdx = channelX.value.length - 1
    let chIdx: number
    if (span >= 2) {
      chIdx = e.dst.layer - 2                    // forward long-hop: channel just before dst column
    } else if (span === 1) {
      chIdx = e.src.layer - 1                    // forward adjacent: channel right after src column
    } else {
      chIdx = Math.min(Math.max(e.src.layer - 1, e.dst.layer - 1), maxIdx) // same-col or backward
    }
    chIdx = Math.min(Math.max(chIdx, 0), maxIdx)
    const midX = channelX.value[chIdx] + off

    // Z-shape polyline: (sx,sy) → (midX,sy) → (midX,ty) → (tx,ty)
    const path = `M ${sx} ${sy} L ${midX} ${sy} L ${midX} ${ty} L ${tx} ${ty}`
    const labelX = (sx + midX) / 2
    const labelY = sy - 6
    const labelText = e.physical
      ? `${formatNum(e.physical)}${e.energyProduct === '电力' ? ' kWh' : ''}`
      : formatNum(e.standard)
    return {
      source: e.src.id,
      target: e.dst.id,
      energyProduct: e.energyProduct,
      physicalQuantity: e.physical,
      standardQuantity: e.standard,
      color,
      path,
      labelX,
      labelY,
      labelText,
    }
  })

  // Energy totals (bottom summary bar) — sum standard_quantity per energy_product
  // over purchased edges only (treat them as the plant's total input in tce).
  const totalMap = new Map<string, number>()
  for (const f of flows) {
    if ((f.flowStage || '').toLowerCase() !== 'purchased') continue
    const prev = totalMap.get(f.energyProduct) ?? 0
    totalMap.set(f.energyProduct, prev + Number(f.standardQuantity || 0))
  }
  const energyTotals = Array.from(totalMap.entries())
    .sort((a, b) => b[1] - a[1])
    .map(([product, totalStandard]) => ({
      product,
      totalStandard,
      color: getEnergyColor(product),
    }))

  return {
    nodes: Array.from(nodeMap.values()),
    edges,
    canvasW,
    canvasH,
    energyTotals,
  }
}

// ------------------------------------------------------------
// Export PNG (SVG → Canvas → PNG)
// ------------------------------------------------------------
const svgRef = ref<SVGSVGElement | null>(null)

async function exportPng(): Promise<string> {
  if (!svgRef.value) return ''
  const svg = svgRef.value
  const serializer = new XMLSerializer()
  const svgStr = serializer.serializeToString(svg)
  const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const i = new Image()
      i.onload = () => resolve(i)
      i.onerror = () => reject(new Error('svg image load failed'))
      i.src = url
    })
    const scale = 2
    const canvas = document.createElement('canvas')
    canvas.width = built.value.canvasW * scale
    canvas.height = built.value.canvasH * scale
    const ctx = canvas.getContext('2d')!
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.scale(scale, scale)
    ctx.drawImage(img, 0, 0)
    return canvas.toDataURL('image/png')
  } finally {
    URL.revokeObjectURL(url)
  }
}

function fitView() {
  // Simple scroll-into-view fit; real zoom-to-fit not required since SVG is already sized.
  if (svgRef.value) svgRef.value.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

// ------------------------------------------------------------
// Header labels
// ------------------------------------------------------------
const HEADER_LABELS = ['购入储存环节', '加工转换环节', '输送分配环节', '终端使用环节']
</script>

<template>
  <div class="flow-diagram-wrapper">
    <svg
      ref="svgRef"
      :width="built.canvasW"
      :height="built.canvasH"
      :viewBox="`0 0 ${built.canvasW} ${built.canvasH}`"
      xmlns="http://www.w3.org/2000/svg"
      class="flow-svg"
    >
      <defs>
        <marker
          v-for="color in Array.from(new Set(built.edges.map(e => e.color)))"
          :id="`arrow-${color.replace('#', '')}`"
          :key="color"
          markerWidth="8"
          markerHeight="8"
          refX="7"
          refY="4"
          orient="auto"
          markerUnits="strokeWidth"
        >
          <path d="M0,0 L8,4 L0,8 z" :fill="color" />
        </marker>
      </defs>

      <!-- Top section headers with double-arrow separators -->
      <g class="header-band">
        <template v-for="(label, i) in HEADER_LABELS" :key="`h-${i}`">
          <rect
            :x="colLeft[i]"
            y="14"
            :width="COL_W[i]"
            height="28"
            fill="#f0f4fa"
            stroke="#4a6fa5"
            stroke-width="1"
            rx="3"
          />
          <text
            :x="colLeft[i] + COL_W[i] / 2"
            y="32"
            text-anchor="middle"
            font-size="13"
            font-weight="600"
            fill="#1f3a68"
          >{{ label }}</text>
        </template>
        <!-- Double-arrow separators between columns -->
        <template v-for="i in 3" :key="`sep-${i}`">
          <g :transform="`translate(${colLeft[i - 1] + COL_W[i - 1] + COL_GAP / 2}, 28)`">
            <path d="M -12 0 L -4 -4 L -4 4 Z" fill="#4a6fa5" />
            <path d="M 12 0 L 4 -4 L 4 4 Z" fill="#4a6fa5" />
            <line x1="-4" y1="0" x2="4" y2="0" stroke="#4a6fa5" stroke-width="1" />
          </g>
        </template>
      </g>

      <!-- Sub-header for purchase column only: 等价值 / 当量值 -->
      <g v-if="built.nodes.some(n => n.layer === 1)">
        <text
          :x="colLeft[0] + COL_W[0] * 0.55"
          y="58"
          text-anchor="middle"
          font-size="11"
          fill="#666"
        >等价值</text>
        <text
          :x="colLeft[0] + COL_W[0] * 0.85"
          y="58"
          text-anchor="middle"
          font-size="11"
          fill="#666"
        >当量值</text>
      </g>

      <!-- Edges (drawn before nodes so nodes sit on top) -->
      <g class="edges">
        <path
          v-for="(e, i) in built.edges"
          :key="`e-${i}`"
          :d="e.path"
          :stroke="e.color"
          stroke-width="1.5"
          fill="none"
          :marker-end="`url(#arrow-${e.color.replace('#', '')})`"
        />
        <!-- NOTE: stroke/paint-order applied as inline SVG attributes (not scoped CSS)
             because XMLSerializer does not embed Vue-scoped CSS rules when exporting PNG.
             Previously these lived in `.edge-label {}` below and the white halo vanished
             on export. Keeping the class for potential future overrides, but the visual
             comes from the inline attributes. -->
        <text
          v-for="(e, i) in built.edges"
          :key="`el-${i}`"
          :x="e.labelX"
          :y="e.labelY"
          text-anchor="middle"
          font-size="10"
          :fill="e.color"
          stroke="#fff"
          stroke-width="3"
          stroke-linejoin="round"
          paint-order="stroke"
          class="edge-label"
        >{{ e.labelText }}</text>
      </g>

      <!-- Nodes -->
      <g class="nodes">
        <template v-for="n in built.nodes" :key="n.id">
          <!-- Column 1 purchase circle + data columns -->
          <template v-if="n.layer === 1">
            <circle
              :cx="n.cx"
              :cy="n.cy"
              :r="CIRCLE_R"
              :stroke="getEnergyColor(n.energyProducts[0] || n.label)"
              stroke-width="1.8"
              fill="#fff"
            />
            <text
              :x="n.cx"
              :y="n.cy - 2"
              text-anchor="middle"
              font-size="11"
              font-weight="600"
              :fill="getEnergyColor(n.energyProducts[0] || n.label)"
            >{{ n.label }}</text>
            <text
              :x="n.cx"
              :y="n.cy + 10"
              text-anchor="middle"
              font-size="10"
              :fill="getEnergyColor(n.energyProducts[0] || n.label)"
            >{{ formatNum(n.physicalSum) }}</text>
            <!-- Equivalent / current-value data columns -->
            <text
              :x="colLeft[0] + COL_W[0] * 0.55"
              :y="n.cy - 2"
              text-anchor="middle"
              font-size="10"
              fill="#222"
            >{{ formatNum(n.physicalSum) }}</text>
            <text
              :x="colLeft[0] + COL_W[0] * 0.55"
              :y="n.cy + 10"
              text-anchor="middle"
              font-size="9"
              fill="#888"
            >({{ formatNum((n.physicalSum && n.standardSum) ? (n.standardSum / n.physicalSum) * 100 : 0) }})</text>
            <text
              :x="colLeft[0] + COL_W[0] * 0.85"
              :y="n.cy - 2"
              text-anchor="middle"
              font-size="10"
              fill="#222"
            >{{ formatNum(n.standardSum) }}</text>
            <text
              :x="colLeft[0] + COL_W[0] * 0.85"
              :y="n.cy + 10"
              text-anchor="middle"
              font-size="9"
              fill="#888"
            >({{ formatNum((n.physicalSum && n.standardSum) ? (n.standardSum / n.physicalSum) * 100 : 0) }})</text>
          </template>

          <!-- Columns 2/3/4: rounded rectangles -->
          <template v-else>
            <rect
              :x="n.cx - RECT_W / 2"
              :y="n.cy - RECT_H / 2"
              :width="RECT_W"
              :height="RECT_H"
              fill="#fff"
              stroke="#333"
              stroke-width="1.2"
              rx="2"
            />
            <text
              :x="n.cx"
              :y="n.cy + 4"
              text-anchor="middle"
              font-size="11"
              fill="#222"
            >{{ n.label }}</text>
          </template>
        </template>
      </g>

      <!-- Bottom energy-product totals bar -->
      <g
        v-if="built.energyTotals.length"
        :transform="`translate(${CANVAS_PAD_X}, ${built.canvasH - FOOTER_H + 12})`"
      >
        <line
          x1="0"
          y1="0"
          :x2="built.canvasW - CANVAS_PAD_X * 2"
          y2="0"
          stroke="#ccc"
          stroke-width="1"
        />
        <g
          v-for="(t, i) in built.energyTotals"
          :key="`t-${i}`"
          :transform="`translate(${i * 180}, 20)`"
        >
          <text font-size="12" font-weight="600" :fill="t.color">{{ t.product }}</text>
          <text
            font-size="12"
            :fill="t.color"
            :x="50"
          >({{ formatNum(t.totalStandard) }})</text>
        </g>
      </g>
    </svg>
  </div>
</template>

<style scoped lang="scss">
.flow-diagram-wrapper {
  width: 100%;
  height: 100%;
  overflow: auto;
  background: #fff;
}
.flow-svg {
  display: block;
  background: #fff;
}
/* .edge-label visual (white halo via stroke + paint-order) is applied as
   INLINE SVG attributes on each <text> in the template, NOT as scoped CSS,
   so the styles survive XMLSerializer → Image → Canvas serialization used
   by exportPng(). Do not re-introduce scoped CSS for stroke here. */
</style>
