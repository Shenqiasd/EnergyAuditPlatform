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
  '柴油': '#F39C12',
  '汽油': '#E67E22',
  '燃料油': '#D35400',
  '液化石油气': '#9B59B6',
  '热力': '#E91E63',
  '太阳能': '#FF9800',
  '综合能源': '#607D8B',
  '外购热力': '#E91E63',
  '压缩空气': '#1ABC9C',
  '水': '#2980B9',
  '外购水': '#2980B9',
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

// Layout constants matching reference engineering diagram
const MARGIN_LEFT = 30
const CIRCLE_CX = MARGIN_LEFT + 80
const CIRCLE_R = 34
const DATA_COL1_X = CIRCLE_CX + CIRCLE_R + 16
const DATA_COL2_X = DATA_COL1_X + 72
const DIVIDER_1_X = DATA_COL2_X + 72
const EQUIP_X = DIVIDER_1_X + 30
const EQUIP_W = 66
const EQUIP_H = 28
const DIVIDER_2_X = EQUIP_X + EQUIP_W + 50
const TERMINAL_X = DIVIDER_2_X + 20
const TERMINAL_W = 66
const TERMINAL_H = 34
const RESULT_X = TERMINAL_X + TERMINAL_W + 30
const RESULT_ARROW_X = RESULT_X + 80
const TOTAL_WIDTH = RESULT_ARROW_X + 90

const HEADER_Y = 20
const SUB_HEADER_Y = 38
const CONTENT_TOP = 58
const ROW_HEIGHT = 90

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
  const totalHeight = CONTENT_TOP + maxRows * ROW_HEIGHT + 30

  // Section headers
  drawLine('top-line', MARGIN_LEFT, HEADER_Y - 6, TOTAL_WIDTH - 20, HEADER_Y - 6, '#999', 1, false)

  const sec1Mid = (MARGIN_LEFT + DIVIDER_1_X) / 2
  const sec2Mid = (DIVIDER_1_X + DIVIDER_2_X) / 2
  const sec3Mid = (DIVIDER_2_X + TOTAL_WIDTH - 20) / 2
  addLabel('hdr-1', sec1Mid - 40, HEADER_Y - 4, '购入产出环节', 11, '#333', 'bold', 90)
  addLabel('hdr-2', sec2Mid - 35, HEADER_Y - 4, '加工转换环节', 11, '#333', 'bold', 80)
  addLabel('hdr-3', sec3Mid - 45, HEADER_Y - 4, '分配/最终消费环节', 11, '#333', 'bold', 110)

  drawLine('hdr-arrow-1', MARGIN_LEFT, HEADER_Y, MARGIN_LEFT + 16, HEADER_Y, '#666', 1, true)
  drawLine('hdr-arrow-2', DIVIDER_1_X - 2, HEADER_Y, DIVIDER_1_X + 14, HEADER_Y, '#666', 1, true)
  drawLine('hdr-arrow-3', DIVIDER_2_X - 2, HEADER_Y, DIVIDER_2_X + 14, HEADER_Y, '#666', 1, true)

  addLabel('sub-equiv', DATA_COL1_X, SUB_HEADER_Y, '等价值', 9, '#666', 'normal', 50)
  addLabel('sub-calor', DATA_COL2_X, SUB_HEADER_Y, '当量值', 9, '#666', 'normal', 50)

  drawLine('sep-line', MARGIN_LEFT, CONTENT_TOP - 4, TOTAL_WIDTH - 20, CONTENT_TOP - 4, '#CCC', 0.5, false)

  drawLine('vdiv-1', DIVIDER_1_X, HEADER_Y - 6, DIVIDER_1_X, totalHeight, '#AAA', 1, false)
  drawLine('vdiv-2', DIVIDER_2_X, HEADER_Y - 6, DIVIDER_2_X, totalHeight, '#AAA', 1, false)

  // PURCHASE nodes: circles + data columns
  layers.PURCHASE.forEach((node, idx) => {
    const cy = CONTENT_TOP + idx * ROW_HEIGHT + ROW_HEIGHT / 2
    const color = node.energyProducts.length > 0 ? getEnergyColor(node.energyProducts[0]) : '#409EFF'
    const balance = node.balanceInfo
    const unit = balance ? (balance.measurement_unit ?? balance.measurementUnit ?? '') : ''
    const purchaseAmt = balance ? (balance.purchase_amount ?? balance.purchaseAmount ?? 0) : 0
    const stdAmt = balance ? (balance.standard_amount ?? balance.standardAmount ?? 0) : 0
    const consumeAmt = balance ? (balance.consumption_amount ?? balance.consumptionAmount ?? 0) : 0

    graph!.addNode({
      id: node.id,
      x: CIRCLE_CX - CIRCLE_R,
      y: cy - CIRCLE_R,
      width: CIRCLE_R * 2,
      height: CIRCLE_R * 2,
      shape: 'ellipse',
      attrs: {
        body: { fill: '#FFFFFF', stroke: color, strokeWidth: 2 },
        label: {
          text: node.label + '\n' + (purchaseAmt ? formatNum(purchaseAmt) + unit : ''),
          fill: color,
          fontSize: 9,
          fontWeight: 'bold',
          lineHeight: 13,
        },
      },
      ports: {
        items: [{ id: node.id + '-out', group: 'right' }],
        groups: {
          right: {
            position: { name: 'absolute', args: { x: CIRCLE_R * 2, y: CIRCLE_R } },
            attrs: { circle: { r: 0 } },
          },
        },
      },
    })

    const purchaseStr = formatNum(purchaseAmt)
    const purchaseStdStr = stdAmt ? '(' + formatNum(stdAmt) + ')' : ''
    if (purchaseStr) {
      addLabel('d1v-' + node.id, DATA_COL1_X, cy - 8, purchaseStr, 9, '#333', 'normal', 60)
      if (purchaseStdStr) {
        addLabel('d1s-' + node.id, DATA_COL1_X, cy + 5, purchaseStdStr, 8, '#888', 'normal', 60)
      }
    }

    const consumeStr = formatNum(consumeAmt || purchaseAmt)
    const consumeStdStr = stdAmt ? '(' + formatNum(stdAmt) + ')' : ''
    if (consumeStr) {
      addLabel('d2v-' + node.id, DATA_COL2_X, cy - 8, consumeStr, 9, '#333', 'normal', 60)
      if (consumeStdStr) {
        addLabel('d2s-' + node.id, DATA_COL2_X, cy + 5, consumeStdStr, 8, '#888', 'normal', 60)
      }
    }

    drawLine('hconn-' + node.id, CIRCLE_CX + CIRCLE_R, cy, DATA_COL1_X - 4, cy, color, 1, false)
  })

  // CONVERSION nodes: equipment boxes
  const convOffsetY = layers.CONVERSION.length < layers.PURCHASE.length
    ? ((layers.PURCHASE.length - layers.CONVERSION.length) * ROW_HEIGHT) / 2
    : 0
  layers.CONVERSION.forEach((node, idx) => {
    const cy = CONTENT_TOP + idx * ROW_HEIGHT + ROW_HEIGHT / 2 + convOffsetY

    graph!.addNode({
      id: node.id,
      x: EQUIP_X,
      y: cy - EQUIP_H / 2,
      width: EQUIP_W,
      height: EQUIP_H,
      shape: 'rect',
      attrs: {
        body: { fill: '#FFFFFF', stroke: '#333', strokeWidth: 1.5 },
        label: { text: node.label, fill: '#333', fontSize: 9, fontWeight: 'bold' },
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
    ? ((layers.PURCHASE.length - layers.TERMINAL.length) * ROW_HEIGHT) / 2
    : 0
  layers.TERMINAL.forEach((node, idx) => {
    const cy = CONTENT_TOP + idx * ROW_HEIGHT + ROW_HEIGHT / 2 + termOffsetY

    graph!.addNode({
      id: node.id,
      x: TERMINAL_X,
      y: cy - TERMINAL_H / 2,
      width: TERMINAL_W,
      height: TERMINAL_H,
      shape: 'rect',
      attrs: {
        body: { fill: '#FFFFFF', stroke: '#333', strokeWidth: 1.5 },
        label: { text: node.label, fill: '#333', fontSize: 9, fontWeight: 'bold' },
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
      drawLine('tarrow-' + node.id, TERMINAL_X + TERMINAL_W + 2, cy, RESULT_X - 4, cy, '#333', 1, true)
      addLabel('res-name-' + node.id, RESULT_X, cy - 10, node.label, 9, '#333', 'bold', 70)
      addLabel('res-tce-' + node.id, RESULT_X, cy + 3, formatNum(totalTce) + 'tce', 8, '#666', 'normal', 70)
      drawLine('farrow-' + node.id, RESULT_X + 64, cy, RESULT_ARROW_X, cy, '#333', 1, true)
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

      const sourceCy = CONTENT_TOP + sourceLayerIdx * ROW_HEIGHT + ROW_HEIGHT / 2 + sOffY
      const targetCy = CONTENT_TOP + targetLayerIdx * ROW_HEIGHT + ROW_HEIGHT / 2 + tOffY

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
                label: { text: labelText, fill: color, fontSize: 8, fontWeight: 'bold' },
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
    const svg = await graph.toSVG()
    if (typeof svg === 'string') {
      return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
    }
    return null
  } catch {
    return null
  }
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

defineExpose({ fitView, exportPng })
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
