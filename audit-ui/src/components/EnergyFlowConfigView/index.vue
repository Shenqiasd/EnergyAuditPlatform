<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FlowNodeConfig, FlowEdgeConfig } from '@/api/energyFlowConfig'

const props = defineProps<{
  nodes: FlowNodeConfig[]
  edges: FlowEdgeConfig[]
  enterpriseName?: string
  auditYear?: number
  canvasWidth?: number
  canvasHeight?: number
}>()

defineExpose({ exportPng, fitView })

const svgRef = ref<SVGSVGElement>()

const cw = computed(() => props.canvasWidth || 1200)
const ch = computed(() => props.canvasHeight || 800)

const NODE_COLORS: Record<string, string> = {
  energy_input: '#E74C3C',
  unit: '#3498DB',
  product_output: '#27AE60',
  custom: '#95A5A6',
}
const EDGE_COLORS = ['#E74C3C', '#3498DB', '#27AE60', '#F39C12', '#9B59B6', '#1ABC9C', '#E67E22']

function nodeColor(n: FlowNodeConfig): string {
  return n.color || NODE_COLORS[n.nodeType] || '#666'
}

function edgeColor(e: FlowEdgeConfig, idx: number): string {
  return e.color || EDGE_COLORS[idx % EDGE_COLORS.length]
}

function edgePath(edge: FlowEdgeConfig): string {
  const src = props.nodes.find(n => n.nodeId === edge.sourceNodeId)
  const dst = props.nodes.find(n => n.nodeId === edge.targetNodeId)
  if (!src || !dst) return ''
  const srcW = src.width || 100
  const srcH = src.height || 50
  const dstH = dst.height || 50
  const sx = src.positionX + srcW
  const sy = src.positionY + srcH / 2
  const tx = dst.positionX
  const ty = dst.positionY + dstH / 2
  if (edge.routePoints) {
    try {
      const pts = JSON.parse(edge.routePoints) as { x: number; y: number }[]
      if (pts.length > 0) {
        let d = `M ${sx} ${sy}`
        for (const p of pts) d += ` L ${p.x} ${p.y}`
        d += ` L ${tx} ${ty}`
        return d
      }
    } catch { /* fall through */ }
  }
  const midX = (sx + tx) / 2
  return `M ${sx} ${sy} L ${midX} ${sy} L ${midX} ${ty} L ${tx} ${ty}`
}

function edgeLabelPos(edge: FlowEdgeConfig): { x: number; y: number } {
  const src = props.nodes.find(n => n.nodeId === edge.sourceNodeId)
  const dst = props.nodes.find(n => n.nodeId === edge.targetNodeId)
  if (!src || !dst) return { x: 0, y: 0 }
  const srcW = src.width || 100
  const srcH = src.height || 50
  const dstH = dst.height || 50
  return {
    x: (src.positionX + srcW + dst.positionX) / 2,
    y: (src.positionY + srcH / 2 + dst.positionY + dstH / 2) / 2 - 8,
  }
}

function fitView() {
  svgRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function exportPng(): Promise<string | null> {
  if (!svgRef.value) return null
  const svgEl = svgRef.value
  const serializer = new XMLSerializer()
  let svgStr = serializer.serializeToString(svgEl)
  if (!svgStr.includes('xmlns=')) {
    svgStr = svgStr.replace('<svg', '<svg xmlns="http://www.w3.org/2000/svg"')
  }
  const blob = new Blob([svgStr], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  try {
    const img = new Image()
    img.src = url
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = reject
    })
    const scale = 2
    const canvas = document.createElement('canvas')
    canvas.width = cw.value * scale
    canvas.height = ch.value * scale
    const ctx = canvas.getContext('2d')!
    ctx.fillStyle = '#fff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.scale(scale, scale)
    ctx.drawImage(img, 0, 0)
    return canvas.toDataURL('image/png')
  } finally {
    URL.revokeObjectURL(url)
  }
}

const visibleNodes = computed(() => props.nodes.filter(n => (n.visible ?? 1) !== 0))
const visibleEdges = computed(() => props.edges.filter(e => (e.visible ?? 1) !== 0))
</script>

<template>
  <div class="config-view-wrapper">
    <svg
      ref="svgRef"
      :width="cw"
      :height="ch"
      :viewBox="`0 0 ${cw} ${ch}`"
      xmlns="http://www.w3.org/2000/svg"
      class="config-view-canvas"
    >
      <rect :width="cw" :height="ch" fill="#fff" />

      <text v-if="enterpriseName" :x="cw / 2" y="30" text-anchor="middle" font-size="16" font-weight="bold" fill="#1f3a68">
        {{ enterpriseName }} {{ auditYear }}年 能源流程图
      </text>

      <defs>
        <marker
          v-for="(c, ci) in EDGE_COLORS"
          :key="ci"
          :id="`arrow-view-${c.replace('#', '')}`"
          markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth"
        >
          <path d="M0,0 L8,4 L0,8 z" :fill="c" />
        </marker>
      </defs>

      <!-- Edges -->
      <g class="edges-layer">
        <template v-for="(e, ei) in visibleEdges" :key="e.edgeId">
          <path
            :d="edgePath(e)"
            :stroke="edgeColor(e, ei)"
            :stroke-width="e.lineWidth || 2"
            fill="none"
            :marker-end="`url(#arrow-view-${edgeColor(e, ei).replace('#', '')})`"
          />
          <text
            v-if="e.labelText"
            :x="edgeLabelPos(e).x"
            :y="edgeLabelPos(e).y"
            text-anchor="middle"
            font-size="11"
            :fill="edgeColor(e, ei)"
            stroke="#fff" stroke-width="3" stroke-linejoin="round" paint-order="stroke"
          >{{ e.labelText }}</text>
        </template>
      </g>

      <!-- Nodes -->
      <g class="nodes-layer">
        <template v-for="n in visibleNodes" :key="n.nodeId">
          <g>
            <template v-if="n.nodeType === 'energy_input'">
              <circle
                :cx="n.positionX + (n.width || 60) / 2"
                :cy="n.positionY + (n.height || 60) / 2"
                :r="(n.width || 60) / 2"
                :stroke="nodeColor(n)" stroke-width="2" fill="#fff"
              />
              <text
                :x="n.positionX + (n.width || 60) / 2"
                :y="n.positionY + (n.height || 60) / 2 + 4"
                text-anchor="middle" font-size="11" :fill="nodeColor(n)"
              >{{ n.label }}</text>
            </template>
            <template v-else-if="n.nodeType === 'product_output'">
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
            </template>
            <template v-else>
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
            </template>
          </g>
        </template>
      </g>

      <!-- Legend -->
      <g v-if="visibleNodes.length > 0" :transform="`translate(20, ${ch - 60})`">
        <rect width="200" height="50" fill="#f9f9f9" stroke="#ddd" rx="4" />
        <circle cx="20" cy="15" r="6" fill="#E74C3C" stroke="#E74C3C" />
        <text x="32" y="19" font-size="10" fill="#333">能源输入</text>
        <rect x="90" y="9" width="12" height="12" fill="#fff" stroke="#3498DB" rx="2" />
        <text x="108" y="19" font-size="10" fill="#333">用能单元</text>
        <rect x="8" y="29" width="12" height="12" fill="#f0fff0" stroke="#27AE60" rx="4" />
        <text x="26" y="39" font-size="10" fill="#333">产品产出</text>
        <line x1="90" y1="35" x2="120" y2="35" stroke="#F39C12" stroke-width="2" />
        <text x="126" y="39" font-size="10" fill="#333">能源流向</text>
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
