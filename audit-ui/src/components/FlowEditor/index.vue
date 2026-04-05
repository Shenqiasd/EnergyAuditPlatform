<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { Graph, Cell } from '@antv/x6'
import type { EnergyFlowItem } from '@/api/energyFlow'

const NODE_COLORS: Record<string, string> = {
  purchased: '#409EFF',
  conversion: '#E6A23C',
  distribution: '#67C23A',
  terminal: '#909399',
  product: '#F56C6C',
  non_production: '#C0C4CC',
}

const NODE_LABELS: Record<string, string> = {
  purchased: '外购能源',
  conversion: '加工转换',
  distribution: '输送分配',
  terminal: '终端使用',
  product: '产品',
  non_production: '非生产系统',
}

const STAGE_ORDER = ['purchased', 'conversion', 'distribution', 'terminal', 'product', 'non_production']

const props = defineProps<{
  data: EnergyFlowItem[]
  readonly?: boolean
}>()

const graphRef = ref<HTMLDivElement>()
let graph: Graph | null = null

onMounted(() => {
  nextTick(() => {
    initGraph()
    if (props.data.length) {
      renderFlow(props.data)
    }
  })
})

watch(
  () => props.data,
  (val) => {
    if (graph && val) {
      renderFlow(val)
    }
  },
  { deep: true }
)

onBeforeUnmount(() => {
  graph?.dispose()
  graph = null
})

function initGraph() {
  if (!graphRef.value) return
  graph = new Graph({
    container: graphRef.value,
    autoResize: true,
    panning: { enabled: true },
    mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'] },
    interacting: () => !props.readonly,
    connecting: {
      allowBlank: false,
      allowMulti: true,
      allowLoop: false,
      highlight: true,
      snap: { radius: 30 },
    },
    background: { color: '#f8f9fa' },
    grid: { visible: true, size: 10, type: 'dot' },
  })
}

interface NodeInfo {
  id: string
  label: string
  stage: string
  color: string
}

function renderFlow(items: EnergyFlowItem[]) {
  if (!graph) return
  graph.clearCells()

  const nodeMap = new Map<string, NodeInfo>()
  const edges: { source: string; target: string; label: string }[] = []

  for (const item of items) {
    const srcKey = `${item.flowStage}::${item.sourceUnit}`
    const tgtStageIdx = STAGE_ORDER.indexOf(item.flowStage)
    const nextStage = tgtStageIdx < STAGE_ORDER.length - 1 ? STAGE_ORDER[tgtStageIdx + 1] : item.flowStage
    const tgtKey = `${nextStage}::${item.targetUnit}`

    if (!nodeMap.has(srcKey)) {
      nodeMap.set(srcKey, {
        id: srcKey,
        label: item.sourceUnit,
        stage: item.flowStage,
        color: NODE_COLORS[item.flowStage] || '#909399',
      })
    }
    if (!nodeMap.has(tgtKey)) {
      nodeMap.set(tgtKey, {
        id: tgtKey,
        label: item.targetUnit,
        stage: nextStage,
        color: NODE_COLORS[nextStage] || '#909399',
      })
    }

    const qty = item.standardQuantity ?? item.physicalQuantity ?? 0
    const edgeLabel = `${item.energyProduct}\n${qty} tce`
    edges.push({ source: srcKey, target: tgtKey, label: edgeLabel })
  }

  const stageGroups = new Map<string, NodeInfo[]>()
  for (const node of nodeMap.values()) {
    const list = stageGroups.get(node.stage) || []
    list.push(node)
    stageGroups.set(node.stage, list)
  }

  const STAGE_X_GAP = 260
  const NODE_Y_GAP = 100
  const NODE_W = 160
  const NODE_H = 50
  const START_X = 60
  const START_Y = 60

  const stagesPresent = STAGE_ORDER.filter((s) => stageGroups.has(s))
  const nodePositions = new Map<string, { x: number; y: number }>()

  let maxY = START_Y
  for (let col = 0; col < stagesPresent.length; col++) {
    const stage = stagesPresent[col]
    const nodes = stageGroups.get(stage) || []
    const x = START_X + col * STAGE_X_GAP
    for (let row = 0; row < nodes.length; row++) {
      const y = START_Y + row * NODE_Y_GAP
      nodePositions.set(nodes[row].id, { x, y })
      if (y + NODE_H > maxY) maxY = y + NODE_H
    }
  }

  const cells: Cell[] = []

  for (let col = 0; col < stagesPresent.length; col++) {
    const stage = stagesPresent[col]
    const x = START_X + col * STAGE_X_GAP
    cells.push(
      graph!.createNode({
        id: `header-${stage}`,
        x: x,
        y: START_Y - 45,
        width: NODE_W,
        height: 30,
        label: NODE_LABELS[stage] || stage,
        attrs: {
          body: { fill: 'transparent', stroke: 'none' },
          label: { fontSize: 13, fontWeight: 'bold', fill: '#333' },
        },
      })
    )
  }

  for (const node of nodeMap.values()) {
    const pos = nodePositions.get(node.id)!
    cells.push(
      graph!.createNode({
        id: node.id,
        x: pos.x,
        y: pos.y,
        width: NODE_W,
        height: NODE_H,
        label: node.label,
        attrs: {
          body: {
            fill: node.color,
            stroke: darken(node.color),
            rx: 6,
            ry: 6,
          },
          label: {
            fill: '#fff',
            fontSize: 13,
            fontWeight: 500,
          },
        },
        ports: {
          groups: {
            right: { position: 'right', attrs: { circle: { r: 4, magnet: true, fill: '#999', stroke: '#666' } } },
            left: { position: 'left', attrs: { circle: { r: 4, magnet: true, fill: '#999', stroke: '#666' } } },
          },
          items: [
            { group: 'right', id: `${node.id}-out` },
            { group: 'left', id: `${node.id}-in` },
          ],
        },
      })
    )
  }

  for (const edge of edges) {
    cells.push(
      graph!.createEdge({
        source: { cell: edge.source, port: `${edge.source}-out` },
        target: { cell: edge.target, port: `${edge.target}-in` },
        labels: [
          {
            attrs: {
              label: { text: edge.label, fontSize: 10, fill: '#666' },
              rect: { fill: '#fff', stroke: '#ddd', rx: 3, ry: 3 },
            },
            position: { distance: 0.5 },
          },
        ],
        attrs: {
          line: {
            stroke: '#aab',
            strokeWidth: 1.5,
            targetMarker: { name: 'classic', size: 8 },
          },
        },
        router: { name: 'manhattan', args: { padding: 20 } },
        connector: { name: 'rounded', args: { radius: 8 } },
      })
    )
  }

  graph!.resetCells(cells)
  nextTick(() => {
    graph!.zoomToFit({ padding: 40, maxScale: 1.2 })
  })
}

function darken(hex: string): string {
  const num = parseInt(hex.replace('#', ''), 16)
  const r = Math.max(0, ((num >> 16) & 0xff) - 30)
  const g = Math.max(0, ((num >> 8) & 0xff) - 30)
  const b = Math.max(0, (num & 0xff) - 30)
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`
}

async function exportPng(): Promise<string> {
  if (!graph) return ''
  return new Promise((resolve) => {
    graph!.toPNG(
      (dataUri: string) => {
        resolve(dataUri)
      },
      { padding: 20, backgroundColor: '#fff' }
    )
  })
}

function fitView() {
  graph?.zoomToFit({ padding: 40, maxScale: 1.2 })
}

defineExpose({ exportPng, fitView })
</script>

<template>
  <div class="flow-editor-container">
    <div ref="graphRef" class="x6-graph-host"></div>
    <div class="flow-legend">
      <span v-for="(color, key) in NODE_COLORS" :key="key" class="legend-item">
        <i :style="{ background: color }" class="legend-dot"></i>
        {{ NODE_LABELS[key] || key }}
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.flow-editor-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.x6-graph-host {
  flex: 1;
  min-height: 500px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.flow-legend {
  display: flex;
  gap: 16px;
  padding: 10px 0;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #606266;
}

.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
}
</style>
