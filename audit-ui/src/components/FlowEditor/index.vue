<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { Graph } from '@antv/x6'
import type { EnergyFlowItem } from '@/api/energyFlow'

/**
 * FlowEditor — AntV X6 Energy Flow Diagram (Auto-generation, Read-only)
 *
 * Renders a hierarchical node-edge diagram from de_energy_flow + de_energy_balance data.
 * Layout: left-to-right layers (Purchase -> Conversion -> Terminal).
 * Nodes are NOT draggable. Supports pan & zoom for large datasets.
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
}

const props = defineProps<{
  flowData: EnergyFlowItem[]
  balanceData: EnergyBalanceItem[]
}>()

const graphRef = ref<HTMLDivElement>()
let graph: Graph | null = null

// Color palette for energy products
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

function buildGraph(flowData: EnergyFlowItem[], balanceData: EnergyBalanceItem[]) {
  if (!graph) return
  graph.clearCells()
  if (!flowData.length) return

  // 1. Collect unique node names
  const sourceNames = new Set<string>()
  const targetNames = new Set<string>()
  flowData.forEach(f => {
    sourceNames.add(f.sourceUnit)
    targetNames.add(f.targetUnit)
  })

  // 2. Classify node layers
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

  // 3. Track energy products per node
  flowData.forEach(f => {
    const sn = nodeMap.get(f.sourceUnit)
    if (sn && !sn.energyProducts.includes(f.energyProduct)) {
      sn.energyProducts.push(f.energyProduct)
    }
  })

  // 4. Attach balance info to purchase nodes
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

  // 5. Build edges
  const edges: FlowEdge[] = flowData.map(f => ({
    source: f.sourceUnit,
    target: f.targetUnit,
    energyProduct: f.energyProduct,
    physicalQuantity: f.physicalQuantity,
    standardQuantity: f.standardQuantity,
  }))

  // 6. Layout calculation
  const layers: Record<NodeLayer, FlowNode[]> = { PURCHASE: [], CONVERSION: [], TERMINAL: [] }
  for (const node of nodeMap.values()) {
    layers[node.layer].push(node)
  }

  const LAYER_X: Record<NodeLayer, number> = { PURCHASE: 80, CONVERSION: 420, TERMINAL: 760 }
  const NODE_HEIGHT_PURCHASE = 80
  const NODE_HEIGHT_RECT = 50
  const NODE_VERTICAL_GAP = 40
  const PADDING_TOP = 60

  const nodePositions = new Map<string, { x: number; y: number; width: number; height: number }>()
  for (const layer of ['PURCHASE', 'CONVERSION', 'TERMINAL'] as NodeLayer[]) {
    const nodes = layers[layer]
    const x = LAYER_X[layer]
    const nodeHeight = layer === 'PURCHASE' ? NODE_HEIGHT_PURCHASE : NODE_HEIGHT_RECT
    const totalHeight = nodes.length * nodeHeight + (nodes.length - 1) * NODE_VERTICAL_GAP
    let startY = PADDING_TOP + Math.max(0, (400 - totalHeight) / 2)
    nodes.forEach((node, i) => {
      const width = layer === 'PURCHASE' ? 160 : 140
      const y = startY + i * (nodeHeight + NODE_VERTICAL_GAP)
      nodePositions.set(node.id, { x, y, width, height: nodeHeight })
    })
  }

  // 7. Add nodes to graph
  for (const node of nodeMap.values()) {
    const pos = nodePositions.get(node.id)
    if (!pos) continue

    if (node.layer === 'PURCHASE') {
      const color = node.energyProducts.length > 0 ? getEnergyColor(node.energyProducts[0]) : '#409EFF'
      const balance = node.balanceInfo
      const purchaseAmt = balance ? (balance.purchase_amount ?? balance.purchaseAmount ?? '') : ''
      const unit = balance ? (balance.measurement_unit ?? balance.measurementUnit ?? '') : ''
      const stockInfo = balance
        ? `库存: ${balance.opening_stock ?? balance.openingStock ?? 0} -> ${balance.closing_stock ?? balance.closingStock ?? 0}`
        : ''
      let labelText = node.label
      if (purchaseAmt) labelText += `\n购入: ${purchaseAmt}${unit ? ' ' + unit : ''}`
      if (stockInfo) labelText += `\n${stockInfo}`

      graph.addNode({
        id: node.id,
        x: pos.x,
        y: pos.y,
        width: pos.width,
        height: pos.height,
        shape: 'rect',
        attrs: {
          body: { fill: color, stroke: color, rx: 20, ry: 20, opacity: 0.9 },
          label: {
            text: labelText,
            fill: '#fff',
            fontSize: 11,
            fontWeight: 'bold',
            textWrap: { width: pos.width - 20, ellipsis: true },
          },
        },
        ports: {
          items: [{ id: `${node.id}-out`, group: 'right' }],
          groups: { right: { position: 'right', attrs: { circle: { r: 0 } } } },
        },
      })
    } else {
      const borderColor = node.layer === 'CONVERSION' ? '#409EFF' : '#67C23A'
      graph.addNode({
        id: node.id,
        x: pos.x,
        y: pos.y,
        width: pos.width,
        height: pos.height,
        shape: 'rect',
        attrs: {
          body: { fill: '#fff', stroke: borderColor, strokeWidth: 2, rx: 6, ry: 6 },
          label: { text: node.label, fill: '#333', fontSize: 12, fontWeight: 'bold' },
        },
        ports: {
          items: [
            { id: `${node.id}-in`, group: 'left' },
            { id: `${node.id}-out`, group: 'right' },
          ],
          groups: {
            left: { position: 'left', attrs: { circle: { r: 0 } } },
            right: { position: 'right', attrs: { circle: { r: 0 } } },
          },
        },
      })
    }
  }

  // 8. Add edges
  edges.forEach((edge, i) => {
    const color = getEnergyColor(edge.energyProduct)
    const physStr = edge.physicalQuantity ? `${edge.physicalQuantity}` : ''
    const stdStr = edge.standardQuantity ? `${edge.standardQuantity}tce` : ''
    let labelText = ''
    if (physStr && stdStr) labelText = `${physStr} (${stdStr})`
    else if (physStr) labelText = physStr
    else if (stdStr) labelText = stdStr

    graph!.addEdge({
      id: `edge-${i}`,
      source: { cell: edge.source, port: `${edge.source}-out` },
      target: { cell: edge.target, port: `${edge.target}-in` },
      attrs: {
        line: {
          stroke: color,
          strokeWidth: 2,
          targetMarker: { name: 'block', width: 8, height: 6 },
        },
      },
      labels: labelText
        ? [{
            attrs: {
              label: { text: labelText, fill: color, fontSize: 10, fontWeight: 'bold' },
              rect: { fill: '#fff', stroke: 'none', rx: 3, ry: 3 },
            },
            position: { distance: 0.5, offset: { x: 0, y: -10 } },
          }]
        : [],
      router: { name: 'manhattan', args: { padding: 20 } },
      connector: { name: 'rounded', args: { radius: 8 } },
    })
  })

  // 9. Fit to view — use requestAnimationFrame to ensure ResizeObserver has fired
  //    and the graph has correct internal dimensions before fitting
  nextTick(() => {
    requestAnimationFrame(() => {
      graph?.zoomToFit({ padding: 40, maxScale: 1.2 })
      graph?.centerContent()
    })
  })
}

onMounted(() => {
  // Delay init to ensure container is fully laid out and has proper dimensions
  nextTick(() => {
    initGraph()
  })
})

onBeforeUnmount(() => {
  destroyGraph()
})

function initGraph() {
  if (!graphRef.value) return
  // Provide explicit fallback dimensions so X6 has a non-zero viewport even if
  // the container is transitioning from display:none. autoResize will take over
  // once ResizeObserver fires.
  graph = new Graph({
    container: graphRef.value,
    width: graphRef.value.offsetWidth || 800,
    height: graphRef.value.offsetHeight || 600,
    autoResize: true,
    background: { color: '#fafafa' },
    grid: { visible: true, type: 'dot', args: { color: '#ddd', thickness: 1 } },
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
  graph?.zoomToFit({ padding: 40, maxScale: 1.2 })
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
  height: 600px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  &.hidden { display: none; }
}

.empty-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 600px;
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
