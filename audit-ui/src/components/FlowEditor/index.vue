<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { Graph } from '@antv/x6'
import type { EnergyFlowItem } from '@/api/energyFlow'

/**
 * FlowEditor — Engineering-report style energy flow diagram (Read-only)
 *
 * Three-section layout matching standard energy audit report format:
 *   Section 1: 购入产出环节 — circle nodes for energy sources + data columns
 *   Section 2: 加工转换环节 — conversion equipment boxes
 *   Section 3: 分配/最终消费环节 — terminal consumption boxes with arrows
 */

export interface EnergyBalanceItem {
  energy_name?: string
  energyName?: string
  purchase_amount?: number
  purchaseAmount?: number
  consumption_amount?: number
  consumptionAmount?: number
  opening_stock?: number
  openingStock?: number
  closing_stock?: number
  closingStock?: number
  measurement_unit?: string
  measurementUnit?: string
  standard_amount?: number
  standardAmount?: number
}

const props = defineProps<{
  flowData: EnergyFlowItem[]
  balanceData: EnergyBalanceItem[]
}>()

const graphRef = ref<HTMLDivElement>()
let graph: Graph | null = null

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
  '高炉煤气': '#6C3483',
  '转炉煤气': '#5B2C6F',
  '煤制品': '#4A235A',
  '柴油': '#F39C12',
  '汽油': '#E67E22',
  '燃料油': '#D35400',
  '液化石油气': '#9B59B6',
  '热力': '#E91E63',
  '余热': '#C2185B',
  '太阳能': '#FF9800',
  '综合能源': '#607D8B',
  '外购热力': '#E91E63',
  '压缩空气': '#1ABC9C',
  '水': '#2980B9',
  '外购水': '#2980B9',
  '氢气': '#00BCD4',
}
const DEFAULT_COLORS = ['#1ABC9C', '#8E44AD', '#34495E', '#16A085', '#C0392B', '#2980B9', '#F1C40F']

function getEnergyColor(product: string): string {
  if (ENERGY_COLORS[product]) return ENERGY_COLORS[product]
  for (const key of Object.keys(ENERGY_COLORS)) {
    if (product.includes(key) || key.includes(product)) return ENERGY_COLORS[key]
  }
  let hash = 0
  for (let i = 0; i < product.length; i++) hash = ((hash << 5) - hash + product.charCodeAt(i)) | 0
  return DEFAULT_COLORS[Math.abs(hash) % DEFAULT_COLORS.length]
}

type NodeLayer = 'PURCHASE' | 'CONVERSION' | 'TERMINAL'

interface FlowNode {
  id: string
  label: string
  layer: NodeLayer
  balanceInfo?: EnergyBalanceItem
  energyProducts: string[]
}

interface FlowEdge {
  source: string
  target: string
  energyProduct: string
  physicalQuantity: number
  standardQuantity: number
}

// Layout constants — horizontal positions are fixed; vertical spacing adapts to node count
const MARGIN_LEFT = 30
const HEADER_Y = 20
const SUB_HEADER_Y = 38
const CONTENT_TOP = 58

// Compute dynamic row height based on the maximum number of nodes in any layer
function computeLayout(maxRows: number) {
  // For small datasets keep spacious rows; shrink for large datasets
  const rowH = maxRows <= 6 ? 90 : Math.max(46, Math.round(540 / maxRows))
  const circleR = maxRows <= 6 ? 34 : Math.max(16, Math.min(34, Math.round((rowH - 8) / 2)))
  const equipH = maxRows <= 6 ? 28 : Math.max(18, Math.round(rowH * 0.45))
  const termH = maxRows <= 6 ? 34 : Math.max(20, Math.round(rowH * 0.55))
  const fontSize = maxRows <= 10 ? 9 : Math.max(7, 9 - Math.floor((maxRows - 10) / 5))
  const circleCX = MARGIN_LEFT + 80
  const dataCol1X = circleCX + circleR + 16
  const dataCol2X = dataCol1X + 72
  const divider1X = dataCol2X + 72
  const equipX = divider1X + 30
  const equipW = 66
  const divider2X = equipX + equipW + 50
  const terminalX = divider2X + 20
  const terminalW = 66
  const resultX = terminalX + terminalW + 30
  const resultArrowX = resultX + 80
  const totalWidth = resultArrowX + 90
  return {
    rowH, circleR, equipH, termH, fontSize,
    circleCX, dataCol1X, dataCol2X, divider1X,
    equipX, equipW, divider2X, terminalX, terminalW,
    resultX, resultArrowX, totalWidth,
  }
}

function formatNum(n: number | undefined | null): string {
  if (n === undefined || n === null || n === 0) return ''
  if (Math.abs(n) >= 10000) return n.toFixed(0)
  if (Math.abs(n) >= 100) return n.toFixed(1)
  return n.toFixed(2)
}

function buildGraph(flowData: EnergyFlowItem[], balanceData: EnergyBalanceItem[]) {
  if (!graph) return
  graph.clearCells()
  if (!flowData.length) return

  const sourceNames = new Set<string>()
  const targetNames = new Set<string>()
  flowData.forEach(f => {
    sourceNames.add(f.sourceUnit)
    targetNames.add(f.targetUnit)
  })

  const nodeMap = new Map<string, FlowNode>()
  for (const name of sourceNames) {
    if (!targetNames.has(name)) {
      nodeMap.set(name, { id: name, label: name, layer: 'PURCHASE', energyProducts: [] })
    } else {
      nodeMap.set(name, { id: name, label: name, layer: 'CONVERSION', energyProducts: [] })
    }
  }
  for (const name of targetNames) {
    if (!nodeMap.has(name)) {
      nodeMap.set(name, { id: name, label: name, layer: 'TERMINAL', energyProducts: [] })
    }
  }

  flowData.forEach(f => {
    const sn = nodeMap.get(f.sourceUnit)
    if (sn && !sn.energyProducts.includes(f.energyProduct)) {
      sn.energyProducts.push(f.energyProduct)
    }
  })

  const balanceMap = new Map<string, EnergyBalanceItem>()
  balanceData.forEach(b => {
    const name = b.energy_name || b.energyName || ''
    if (name) balanceMap.set(name, b)
  })

  // Fallback: derive balance data from purchase-stage flow records when
  // de_energy_balance has no data (e.g. Audit11.1 not yet submitted).
  // Aggregate physicalQuantity → purchase_amount, standardQuantity → standard_amount
  const flowBalanceMap = new Map<string, EnergyBalanceItem>()
  if (balanceMap.size === 0) {
    flowData.forEach(f => {
      if (f.flowStage !== 'purchased') return
      const key = f.sourceUnit
      const existing = flowBalanceMap.get(key)
      if (existing) {
        existing.purchase_amount = (existing.purchase_amount ?? 0) + (f.physicalQuantity ?? 0)
        existing.standard_amount = (existing.standard_amount ?? 0) + (f.standardQuantity ?? 0)
        existing.consumption_amount = existing.purchase_amount
      } else {
        flowBalanceMap.set(key, {
          energy_name: key,
          purchase_amount: f.physicalQuantity ?? 0,
          standard_amount: f.standardQuantity ?? 0,
          consumption_amount: f.physicalQuantity ?? 0,
        })
      }
    })
  }

  for (const node of nodeMap.values()) {
    if (node.layer === 'PURCHASE') {
      if (balanceMap.has(node.label)) {
        node.balanceInfo = balanceMap.get(node.label)
      } else {
        for (const ep of node.energyProducts) {
          if (balanceMap.has(ep)) {
            node.balanceInfo = balanceMap.get(ep)
            break
          }
        }
      }
      // Fallback to flow-derived balance when no balance record matched
      if (!node.balanceInfo && flowBalanceMap.has(node.label)) {
        node.balanceInfo = flowBalanceMap.get(node.label)
      }
    }
  }

  const edges: FlowEdge[] = flowData.map(f => ({
    source: f.sourceUnit,
    target: f.targetUnit,
    energyProduct: f.energyProduct,
    physicalQuantity: f.physicalQuantity,
    standardQuantity: f.standardQuantity,
  }))

  const layers: Record<NodeLayer, FlowNode[]> = { PURCHASE: [], CONVERSION: [], TERMINAL: [] }
  for (const node of nodeMap.values()) {
    layers[node.layer].push(node)
  }

  const maxRows = Math.max(layers.PURCHASE.length, layers.CONVERSION.length, layers.TERMINAL.length, 1)
  const L = computeLayout(maxRows)
  const totalHeight = CONTENT_TOP + maxRows * L.rowH + 30

  // Section headers
  drawLine('top-line', MARGIN_LEFT, HEADER_Y - 6, L.totalWidth - 20, HEADER_Y - 6, '#999', 1, false)

  const sec1Mid = (MARGIN_LEFT + L.divider1X) / 2
  const sec2Mid = (L.divider1X + L.divider2X) / 2
  const sec3Mid = (L.divider2X + L.totalWidth - 20) / 2
  addLabel('hdr-1', sec1Mid - 40, HEADER_Y - 4, '购入产出环节', 11, '#333', 'bold', 90)
  addLabel('hdr-2', sec2Mid - 35, HEADER_Y - 4, '加工转换环节', 11, '#333', 'bold', 80)
  addLabel('hdr-3', sec3Mid - 45, HEADER_Y - 4, '分配/最终消费环节', 11, '#333', 'bold', 110)

  drawLine('hdr-arrow-1', MARGIN_LEFT, HEADER_Y, MARGIN_LEFT + 16, HEADER_Y, '#666', 1, true)
  drawLine('hdr-arrow-2', L.divider1X - 2, HEADER_Y, L.divider1X + 14, HEADER_Y, '#666', 1, true)
  drawLine('hdr-arrow-3', L.divider2X - 2, HEADER_Y, L.divider2X + 14, HEADER_Y, '#666', 1, true)

  addLabel('sub-equiv', L.dataCol1X, SUB_HEADER_Y, '等价值', 9, '#666', 'normal', 50)
  addLabel('sub-calor', L.dataCol2X, SUB_HEADER_Y, '当量值', 9, '#666', 'normal', 50)

  drawLine('sep-line', MARGIN_LEFT, CONTENT_TOP - 4, L.totalWidth - 20, CONTENT_TOP - 4, '#CCC', 0.5, false)

  drawLine('vdiv-1', L.divider1X, HEADER_Y - 6, L.divider1X, totalHeight, '#AAA', 1, false)
  drawLine('vdiv-2', L.divider2X, HEADER_Y - 6, L.divider2X, totalHeight, '#AAA', 1, false)

  // PURCHASE nodes: circles + data columns
  layers.PURCHASE.forEach((node, idx) => {
    const cy = CONTENT_TOP + idx * L.rowH + L.rowH / 2
    const color = node.energyProducts.length > 0 ? getEnergyColor(node.energyProducts[0]) : '#409EFF'
    const balance = node.balanceInfo
    const unit = balance ? (balance.measurement_unit ?? balance.measurementUnit ?? '') : ''
    const purchaseAmt = balance ? (balance.purchase_amount ?? balance.purchaseAmount ?? 0) : 0
    const stdAmt = balance ? (balance.standard_amount ?? balance.standardAmount ?? 0) : 0
    const consumeAmt = balance ? (balance.consumption_amount ?? balance.consumptionAmount ?? 0) : 0

    graph!.addNode({
      id: node.id,
      x: L.circleCX - L.circleR,
      y: cy - L.circleR,
      width: L.circleR * 2,
      height: L.circleR * 2,
      shape: 'ellipse',
      attrs: {
        body: { fill: '#FFFFFF', stroke: color, strokeWidth: 2 },
        label: {
          text: purchaseAmt ? (node.label + '\n' + formatNum(purchaseAmt) + unit) : node.label,
          fill: color,
          fontSize: L.fontSize,
          fontWeight: 'bold',
          lineHeight: L.fontSize + 4,
        },
      },
      ports: {
        items: [{ id: node.id + '-out', group: 'right' }],
        groups: {
          right: {
            position: { name: 'absolute', args: { x: L.circleR * 2, y: L.circleR } },
            attrs: { circle: { r: 0 } },
          },
        },
      },
    })

    const purchaseStr = formatNum(purchaseAmt)
    const purchaseStdStr = stdAmt ? '(' + formatNum(stdAmt) + ')' : ''
    if (purchaseStr) {
      addLabel('d1v-' + node.id, L.dataCol1X, cy - 8, purchaseStr, L.fontSize, '#333', 'normal', 60)
      if (purchaseStdStr) {
        addLabel('d1s-' + node.id, L.dataCol1X, cy + 5, purchaseStdStr, L.fontSize - 1, '#888', 'normal', 60)
      }
    }

    const consumeStr = formatNum(consumeAmt || purchaseAmt)
    const consumeStdStr = stdAmt ? '(' + formatNum(stdAmt) + ')' : ''
    if (consumeStr) {
      addLabel('d2v-' + node.id, L.dataCol2X, cy - 8, consumeStr, L.fontSize, '#333', 'normal', 60)
      if (consumeStdStr) {
        addLabel('d2s-' + node.id, L.dataCol2X, cy + 5, consumeStdStr, L.fontSize - 1, '#888', 'normal', 60)
      }
    }

    drawLine('hconn-' + node.id, L.circleCX + L.circleR, cy, L.dataCol1X - 4, cy, color, 1, false)
  })

  // CONVERSION nodes: equipment boxes
  const convOffsetY = layers.CONVERSION.length < layers.PURCHASE.length
    ? ((layers.PURCHASE.length - layers.CONVERSION.length) * L.rowH) / 2
    : 0
  layers.CONVERSION.forEach((node, idx) => {
    const cy = CONTENT_TOP + idx * L.rowH + L.rowH / 2 + convOffsetY

    graph!.addNode({
      id: node.id,
      x: L.equipX,
      y: cy - L.equipH / 2,
      width: L.equipW,
      height: L.equipH,
      shape: 'rect',
      attrs: {
        body: { fill: '#FFFFFF', stroke: '#333', strokeWidth: 1.5 },
        label: { text: node.label, fill: '#333', fontSize: L.fontSize, fontWeight: 'bold' },
      },
      ports: {
        items: [
          { id: node.id + '-in', group: 'left' },
          { id: node.id + '-out', group: 'right' },
        ],
        groups: {
          left: { position: 'left', attrs: { circle: { r: 0 } } },
          right: { position: 'right', attrs: { circle: { r: 0 } } },
        },
      },
    })
  })

  // TERMINAL nodes: consumption boxes + right arrows
  const termOffsetY = layers.TERMINAL.length < layers.PURCHASE.length
    ? ((layers.PURCHASE.length - layers.TERMINAL.length) * L.rowH) / 2
    : 0
  layers.TERMINAL.forEach((node, idx) => {
    const cy = CONTENT_TOP + idx * L.rowH + L.rowH / 2 + termOffsetY

    graph!.addNode({
      id: node.id,
      x: L.terminalX,
      y: cy - L.termH / 2,
      width: L.terminalW,
      height: L.termH,
      shape: 'rect',
      attrs: {
        body: { fill: '#FFFFFF', stroke: '#333', strokeWidth: 1.5 },
        label: { text: node.label, fill: '#333', fontSize: L.fontSize, fontWeight: 'bold' },
      },
      ports: {
        items: [
          { id: node.id + '-in', group: 'left' },
        ],
        groups: {
          left: { position: 'left', attrs: { circle: { r: 0 } } },
        },
      },
    })

    const terminalEdges = edges.filter(e => e.target === node.id)
    const totalTce = terminalEdges.reduce((sum, e) => sum + (e.standardQuantity || 0), 0)

    if (totalTce > 0) {
      drawLine('tarrow-' + node.id, L.terminalX + L.terminalW + 2, cy, L.resultX - 4, cy, '#333', 1, true)
      addLabel('res-name-' + node.id, L.resultX, cy - 10, node.label, L.fontSize, '#333', 'bold', 70)
      addLabel('res-tce-' + node.id, L.resultX, cy + 3, formatNum(totalTce) + 'tce', L.fontSize - 1, '#666', 'normal', 70)
      drawLine('farrow-' + node.id, L.resultX + 64, cy, L.resultArrowX, cy, '#333', 1, true)
    }
  })

  // Flow edges (color-coded)
  let edgeIdx = 0
  const edgesBySource = new Map<string, FlowEdge[]>()
  edges.forEach(e => {
    if (!edgesBySource.has(e.source)) edgesBySource.set(e.source, [])
    edgesBySource.get(e.source)!.push(e)
  })

  edgesBySource.forEach((srcEdges) => {
    srcEdges.forEach((edge, i) => {
      const color = getEnergyColor(edge.energyProduct)
      const sourceNode = nodeMap.get(edge.source)
      const targetNode = nodeMap.get(edge.target)
      if (!sourceNode || !targetNode) return

      const sourceLayerIdx = layers[sourceNode.layer].indexOf(sourceNode)
      const targetLayerIdx = layers[targetNode.layer].indexOf(targetNode)
      const sOffY = sourceNode.layer !== 'PURCHASE' ? convOffsetY : 0
      const tOffY = targetNode.layer === 'TERMINAL' ? termOffsetY
        : targetNode.layer === 'CONVERSION' ? convOffsetY : 0

      const sourceCy = CONTENT_TOP + sourceLayerIdx * L.rowH + L.rowH / 2 + sOffY
      const targetCy = CONTENT_TOP + targetLayerIdx * L.rowH + L.rowH / 2 + tOffY

      const physStr = edge.physicalQuantity ? formatNum(edge.physicalQuantity) : ''
      const stdStr = edge.standardQuantity ? '(' + formatNum(edge.standardQuantity) + 'tce)' : ''
      let labelText = ''
      if (physStr && stdStr) labelText = physStr + ' ' + stdStr
      else if (physStr) labelText = physStr
      else if (stdStr) labelText = stdStr

      const parallelOffset = (i - (srcEdges.length - 1) / 2) * 5

      graph!.addEdge({
        id: 'edge-' + (edgeIdx++),
        source: { cell: edge.source, port: edge.source + '-out' },
        target: { cell: edge.target, port: edge.target + '-in' },
        attrs: {
          line: {
            stroke: color,
            strokeWidth: 1.5,
            targetMarker: { name: 'block', width: 6, height: 4 },
          },
        },
        labels: labelText
          ? [{
              attrs: {
                label: { text: labelText, fill: color, fontSize: Math.max(6, L.fontSize - 1), fontWeight: 'bold' },
                rect: { fill: '#fff', stroke: 'none', rx: 2, ry: 2, opacity: 0.9 },
              },
              position: { distance: 0.45, offset: { x: 0, y: -10 + parallelOffset } },
            }]
          : [],
        router: {
          name: 'er',
          args: {
            offset: sourceCy !== targetCy ? 24 + i * 10 : 0,
            direction: 'H',
          },
        },
        connector: { name: 'rounded', args: { radius: 4 } },
      })
    })
  })

  nextTick(() => {
    requestAnimationFrame(() => {
      graph?.zoomToFit({ padding: 30, maxScale: 1.2 })
      graph?.centerContent()
    })
  })
}

function addLabel(id: string, x: number, y: number, text: string, fontSize: number, fill: string, fontWeight: string, width: number) {
  if (!graph) return
  graph.addNode({
    id,
    x,
    y: y - fontSize / 2,
    width,
    height: fontSize + 4,
    shape: 'rect',
    zIndex: 5,
    attrs: {
      body: { fill: 'transparent', stroke: 'none' },
      label: {
        text,
        fill,
        fontSize,
        fontWeight,
        textAnchor: 'start',
        refX: 0,
        refY: 0.5,
        textVerticalAnchor: 'middle',
      },
    },
  })
}

function drawLine(id: string, x1: number, y1: number, x2: number, y2: number, stroke: string, strokeWidth: number, hasArrow: boolean) {
  if (!graph) return
  graph.addEdge({
    id,
    source: { x: x1, y: y1 },
    target: { x: x2, y: y2 },
    zIndex: 1,
    attrs: {
      line: {
        stroke,
        strokeWidth,
        targetMarker: hasArrow ? { name: 'block', width: 6, height: 4 } : null,
        sourceMarker: null,
      },
    },
  })
}

onMounted(() => {
  nextTick(() => {
    initGraph()
  })
})

onBeforeUnmount(() => {
  destroyGraph()
})

function initGraph() {
  if (!graphRef.value) return
  graph = new Graph({
    container: graphRef.value,
    width: graphRef.value.offsetWidth || 900,
    height: graphRef.value.offsetHeight || 650,
    autoResize: true,
    background: { color: '#FFFFFF' },
    grid: false,
    panning: { enabled: true },
    mousewheel: {
      enabled: true,
      modifiers: ['ctrl', 'meta'],
      zoomAtMousePosition: true,
      minScale: 0.3,
      maxScale: 3,
    },
    interacting: { nodeMovable: false, edgeMovable: false, edgeLabelMovable: false },
    connecting: { enabled: false },
  })
  if (props.flowData.length) {
    buildGraph(props.flowData, props.balanceData)
  }
}

function destroyGraph() {
  graph?.dispose()
  graph = null
}

function fitView() {
  graph?.zoomToFit({ padding: 30, maxScale: 1.2 })
  graph?.centerContent()
}

async function exportPng(): Promise<string | null> {
  if (!graph) return null
  try {
    const dataUri = await exportPngDataUri()
    return dataUri
  } catch {
    return null
  }
}

/**
 * Export the flow chart as a PNG Blob — used by report generation to embed
 * the energy flow diagram image into the Word document.
 */
async function exportPngBlob(): Promise<Blob | null> {
  if (!graph) return null
  try {
    const dataUri = await exportPngDataUri()
    if (!dataUri) return null
    // Convert data URI to Blob
    const resp = await fetch(dataUri)
    return resp.blob()
  } catch {
    return null
  }
}

/** Internal: render SVG to Canvas then export as PNG data URI */
async function exportPngDataUri(): Promise<string | null> {
  if (!graph) return null
  const svgStr = await graph.toSVG()
  if (typeof svgStr !== 'string') return null

  return new Promise<string | null>((resolve) => {
    const img = new Image()
    // Use a larger scale for better quality in printed reports
    const SCALE = 2
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = img.naturalWidth * SCALE
      canvas.height = img.naturalHeight * SCALE
      const ctx = canvas.getContext('2d')
      if (!ctx) { resolve(null); return }
      ctx.fillStyle = '#FFFFFF'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      ctx.scale(SCALE, SCALE)
      ctx.drawImage(img, 0, 0)
      resolve(canvas.toDataURL('image/png'))
    }
    img.onerror = () => resolve(null)
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svgStr)
  })
}

watch(
  () => [props.flowData, props.balanceData],
  () => {
    if (graph && props.flowData.length) {
      buildGraph(props.flowData, props.balanceData)
    }
  },
  { deep: true }
)

defineExpose({ fitView, exportPng, exportPngBlob })
</script>

<template>
  <div class="flow-editor-container">
    <div v-if="!flowData.length" class="empty-placeholder">
      <el-empty description="暂无数据，请先在模板填报中提交 Audit11（能源流程图二维表）和 Audit11.1（能源购入消费储存）" />
    </div>
    <div ref="graphRef" class="x6-graph-host" :class="{ hidden: !flowData.length }"></div>
    <div v-if="flowData.length" class="zoom-hint">Ctrl + 滚轮缩放 | 拖拽平移</div>
  </div>
</template>

<style scoped lang="scss">
.flow-editor-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.x6-graph-host {
  width: 100%;
  height: 650px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  &.hidden { display: none; }
}

.empty-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 650px;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
  background: #fafafa;
}

.zoom-hint {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 11px;
  color: #909399;
  background: rgba(255, 255, 255, 0.8);
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
