<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getEnergyFlowConfig,
  saveEnergyFlowConfig,
  type EnergyFlowConfig,
  type FlowRecord,
  type FlowNodeConfig,
  type FlowEdgeConfig,
  type DiagramConfig,
  type UnitInfo,
  type EnergyInfo,
  type ProductInfo,
  type SaveEnergyFlowConfig,
  type EnergyConsumptionInfo,
} from '@/api/energyFlowConfig'
import EnergyFlowConfigView from '@/components/EnergyFlowConfigView/index.vue'

// ============================================================
// State
// ============================================================
const currentYear = new Date().getFullYear()
const auditYear = ref(currentYear)
const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - i)
const loading = ref(false)
const saving = ref(false)

const config = ref<EnergyFlowConfig | null>(null)
const flowRecords = ref<FlowRecord[]>([])
const nodes = ref<FlowNodeConfig[]>([])
const edges = ref<FlowEdgeConfig[]>([])
const canvasWidth = ref(1200)
const canvasHeight = ref(800)

// Undo/redo
const undoStack = ref<string[]>([])
const redoStack = ref<string[]>([])

// Selection
const selectedNodeId = ref<string | null>(null)
const selectedEdgeId = ref<string | null>(null)

// Drag state
const dragging = ref(false)
const dragNodeId = ref<string | null>(null)
const dragOffset = reactive({ x: 0, y: 0 })

// Record editing
const editingRecord = ref<FlowRecord | null>(null)
const showRecordDialog = ref(false)

// Edge creation mode
const creatingEdge = ref(false)
const edgeSourceNodeId = ref<string | null>(null)

// Final-effect preview toggle
const showFinalPreview = ref(false)

// Mode B pending edge (source/target node IDs waiting for dialog confirm)
const pendingEdgeSrcNodeId = ref<string | null>(null)
const pendingEdgeTgtNodeId = ref<string | null>(null)

// Record form ref and conditional validation rules
const recordFormRef = ref<FormInstance | null>(null)
const recordFormRules = computed<FormRules>(() => {
  const rules: FormRules = {
    sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
    targetType: [{ required: true, message: '请选择目的类型', trigger: 'change' }],
    itemType: [{ required: true, message: '请选择品目类型', trigger: 'change' }],
    itemId: [{ required: true, message: '请选择品目', trigger: 'change' }],
    physicalQuantity: [{ required: true, message: '请输入实物量', trigger: 'blur' }],
  }
  if (editingRecord.value?.sourceType === 'unit') {
    rules.sourceRefId = [{ required: true, message: '请选择来源单元', trigger: 'change' }]
  }
  if (editingRecord.value?.sourceType === 'system') {
    rules.sourceUnit = [{ required: true, message: '请输入系统名称', trigger: 'blur' }]
  }
  if (editingRecord.value?.targetType === 'unit') {
    rules.targetRefId = [{ required: true, message: '请选择目的单元', trigger: 'change' }]
  }
  if (editingRecord.value?.targetType === 'production_system') {
    rules.targetUnit = [{ required: true, message: '请输入生产系统名称', trigger: 'blur' }]
  }
  return rules
})

// Generate a unique client key for record tracking
let clientKeySeq = 0
function genClientKey(): string {
  return `ck-${Date.now()}-${++clientKeySeq}`
}

// SVG ref for PNG export
const svgRef = ref<SVGSVGElement | null>(null)

// Off-screen final-effect renderer for PNG export
const exportViewRef = ref<InstanceType<typeof EnergyFlowConfigView>>()
// Inline final-effect preview (visible when toggled)
const previewViewRef = ref<InstanceType<typeof EnergyFlowConfigView>>()

// ============================================================
// Computed
// ============================================================
const enterpriseName = computed(() => config.value?.enterpriseInfo?.name ?? '未知企业')
const units = computed(() => config.value?.units ?? [])
const energies = computed(() => config.value?.energies ?? [])
const products = computed(() => config.value?.products ?? [])
const energyConsumption = computed<EnergyConsumptionInfo[]>(() => config.value?.energyConsumption ?? [])
const validation = computed(() => config.value?.validation ?? {
  valid: false,
  exportReady: false,
  enterpriseComplete: false,
  hasUnits: false,
  hasEnergies: false,
  hasProducts: false,
  warnings: [],
  exportErrors: [],
})

const selectedNode = computed(() =>
  selectedNodeId.value ? nodes.value.find(n => n.nodeId === selectedNodeId.value) ?? null : null
)
const selectedEdge = computed(() =>
  selectedEdgeId.value ? edges.value.find(e => e.edgeId === selectedEdgeId.value) ?? null : null
)

// ============================================================
// Colors
// ============================================================
const NODE_COLORS: Record<string, string> = {
  energy_input: '#E74C3C',
  unit: '#3498DB',
  product_output: '#27AE60',
  custom: '#95A5A6',
}
const EDGE_COLORS = ['#E74C3C', '#3498DB', '#27AE60', '#F39C12', '#9B59B6', '#1ABC9C', '#E67E22', '#000']

function nodeColor(n: FlowNodeConfig): string {
  return n.color || NODE_COLORS[n.nodeType] || '#666'
}

function edgeColor(e: FlowEdgeConfig, idx: number): string {
  if (isEditorProductEdge(e)) return '#000'
  return e.color || EDGE_COLORS[idx % EDGE_COLORS.length]
}

function isEditorProductEdge(e: FlowEdgeConfig): boolean {
  const rec = resolveEdgeRecord(e)
  if (rec) return rec.itemType === 'product'
  return e.itemType === 'product'
}

// ============================================================
// Data Loading
// ============================================================
async function loadData() {
  loading.value = true
  try {
    const res = await getEnergyFlowConfig(auditYear.value)
    config.value = res
    flowRecords.value = res.flowRecords ?? []
    // Assign stable client keys to loaded records
    for (const r of flowRecords.value) {
      r._clientKey = r.id ? `db-${r.id}` : genClientKey()
    }

    if (res.diagram) {
      nodes.value = res.diagram.nodes ?? []
      edges.value = res.diagram.edges ?? []
      canvasWidth.value = res.diagram.canvasWidth || 1200
      canvasHeight.value = res.diagram.canvasHeight || 800
    } else {
      nodes.value = []
      edges.value = []
    }
    // Link edges to records by flowRecordId → _clientKey
    for (const e of edges.value) {
      if (e.flowRecordId != null) {
        const rec = flowRecords.value.find(r => r.id === e.flowRecordId)
        if (rec) e._flowRecordClientKey = rec._clientKey
      }
    }
    undoStack.value = []
    redoStack.value = []
    pushUndo()
  } catch {
    ElMessage.error('加载能流图配置失败')
  } finally {
    loading.value = false
  }
}

// ============================================================
// Save
// ============================================================
async function handleSave() {
  saving.value = true
  try {
    // Normalize all visible edges' routePoints to match buildOrthoPath() output.
    // This guarantees saved routePoints = actual rendered path (no lossy hints).
    normalizeAllRoutePoints()
    // Resolve edge → record binding via stable _clientKey, then set flowRecordIndex
    const edgesWithIndex = edges.value.map(e => {
      let idx = -1
      if (e._flowRecordClientKey) {
        idx = flowRecords.value.findIndex(r => r._clientKey === e._flowRecordClientKey)
      }
      if (idx < 0 && e.flowRecordId != null) {
        idx = flowRecords.value.findIndex(r => r.id === e.flowRecordId)
      }
      return { ...e, flowRecordIndex: idx >= 0 ? idx : null }
    })
    const diagram: DiagramConfig = {
      name: `${enterpriseName.value} ${auditYear.value}年能流图`,
      diagramType: 3,
      canvasWidth: canvasWidth.value,
      canvasHeight: canvasHeight.value,
      backgroundColor: '#ffffff',
      nodes: nodes.value,
      edges: edgesWithIndex,
    }
    const body: SaveEnergyFlowConfig = {
      flowRecords: flowRecords.value,
      diagram,
    }
    await saveEnergyFlowConfig(auditYear.value, body)
    ElMessage.success('保存成功')
    await loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ============================================================
// Undo / Redo
// ============================================================
function stateSnapshot(): string {
  return JSON.stringify({ nodes: nodes.value, edges: edges.value, records: flowRecords.value })
}

function pushUndo() {
  undoStack.value.push(stateSnapshot())
  if (undoStack.value.length > 50) undoStack.value.shift()
  redoStack.value = []
}

function applySnapshot(snap: string) {
  const s = JSON.parse(snap)
  nodes.value = s.nodes
  edges.value = s.edges
  flowRecords.value = s.records
}

function undo() {
  if (undoStack.value.length <= 1) return
  redoStack.value.push(undoStack.value.pop()!)
  applySnapshot(undoStack.value[undoStack.value.length - 1])
}

function redo() {
  if (redoStack.value.length === 0) return
  const snap = redoStack.value.pop()!
  undoStack.value.push(snap)
  applySnapshot(snap)
}

// ============================================================
// Validation
// ============================================================
function handleValidate() {
  if (!config.value) return
  const v = validation.value
  if (v.valid && v.warnings.length === 0) {
    ElMessage.success('校验通过，所有前置资料完整')
  } else {
    const msgs = v.warnings.join('\n')
    ElMessageBox.alert(msgs || '前置资料不完整', '校验结果', { type: 'warning' })
  }
}

// ============================================================
// Auto Layout (Mode A: records -> nodes/edges)
// ============================================================
function autoLayout() {
  pushUndo()
  const newNodes: FlowNodeConfig[] = []
  const newEdges: FlowEdgeConfig[] = []
  const nodeSet = new Set<string>()

  // 4-stage layout: match EnergyFlowConfigView renderer constants
  const STAGE_MARGIN = 80
  const stageW = (canvasWidth.value - STAGE_MARGIN * 2) / 4
  const stageXArr = [0, 1, 2, 3].map(i => STAGE_MARGIN + i * stageW)
  const PAD_Y = 80
  const ROW_H = 80
  const layerNodes: Map<string, { nodeType: string; refType: string; refId?: number | null; layer: number; label?: string }> = new Map()

  for (const r of flowRecords.value) {
    // Source node — key by sourceType+itemType/itemId for external energy, not display string
    const srcKey = r.sourceType === 'external_energy'
      ? (r.itemType === 'energy' && r.itemId ? `外购-energy-${r.itemId}` : '外购')
      : (r.sourceUnit || '外购')
    if (!layerNodes.has(srcKey)) {
      let nodeType = 'custom'
      let refType = 'custom'
      let refId: number | null = null
      let srcLabel: string | undefined
      if (r.sourceType === 'external_energy') {
        nodeType = 'energy_input'
        refType = 'energy'
        refId = r.itemId ?? null
        if (r.itemType === 'energy' && r.itemId) {
          srcLabel = energies.value.find(e => e.id === r.itemId)?.name ?? r.energyProduct ?? '外购能源'
        } else {
          srcLabel = r.energyProduct ?? '外购'
        }
      } else if (r.sourceType === 'unit' || r.sourceUnitId) {
        nodeType = 'unit'
        refType = 'unit'
        refId = r.sourceRefId ?? r.sourceUnitId ?? null
      } else if (srcKey === '外购') {
        nodeType = 'energy_input'
        refType = 'energy'
      }
      const unit = units.value.find(u => u.name === (r.sourceUnit || ''))
      const layer = srcKey === '外购' || srcKey.startsWith('外购-energy-') || nodeType === 'energy_input' ? 0 : unit ? unit.unitType : 1
      layerNodes.set(srcKey, { nodeType, refType, refId, layer, label: srcLabel })
    }

    // Target node
    const tgtName = r.targetUnit || '产出'
    // For product_output with itemId, use product-keyed name for distinct nodes
    const tgtKey = (r.targetType === 'product_output' || tgtName === '产出') && r.itemType === 'product' && r.itemId
      ? `产出-product-${r.itemId}`
      : tgtName
    if (!layerNodes.has(tgtKey)) {
      let nodeType = 'custom'
      let refType = 'custom'
      let refId: number | null = null
      if (r.targetType === 'product_output' || tgtKey.startsWith('产出-product-') || tgtName === '产出') {
        nodeType = 'product_output'
        refType = 'product'
        if (r.itemType === 'product' && r.itemId) refId = r.itemId
      } else if (r.targetType === 'unit' || r.targetUnitId) {
        nodeType = 'unit'
        refType = 'unit'
        refId = r.targetRefId ?? r.targetUnitId ?? null
      }
      const unit = units.value.find(u => u.name === tgtName)
      const layer = tgtKey.startsWith('产出-product-') || tgtName === '产出' || nodeType === 'product_output' ? 3 : unit ? unit.unitType : 2
      const tgtLabel = nodeType === 'product_output' && r.itemType === 'product' && r.itemId
        ? (products.value.find(p => p.id === r.itemId)?.name ?? tgtName)
        : tgtName
      layerNodes.set(tgtKey, { nodeType, refType, refId, layer, label: tgtLabel })
    }
  }

  // Energy input nodes are now keyed by sourceType+itemType/itemId above,
  // so no separate energyProduct pass is needed.

  // Layout by layer
  const layerBuckets: Map<number, string[]> = new Map()
  for (const [name, info] of layerNodes) {
    const arr = layerBuckets.get(info.layer) ?? []
    arr.push(name)
    layerBuckets.set(info.layer, arr)
  }

  // Position nodes using fixed 4-stage X coordinates (0-3)
  for (const stage of [0, 1, 2, 3]) {
    const names = layerBuckets.get(stage) ?? []
    names.forEach((name, rowIdx) => {
      const info = layerNodes.get(name)!
      const nodeW = info.nodeType === 'energy_input' ? 60 : 100
      const nodeH = info.nodeType === 'energy_input' ? 60 : 50
      const node: FlowNodeConfig = {
        nodeId: `node-${name}`,
        nodeType: info.nodeType,
        refType: info.refType,
        refId: info.refId,
        label: info.label || name,
        positionX: stageXArr[stage] + (stageW - nodeW) / 2,
        positionY: PAD_Y + rowIdx * ROW_H,
        width: nodeW,
        height: nodeH,
        color: NODE_COLORS[info.nodeType] || '#666',
        visible: 1,
        locked: 0,
      }
      newNodes.push(node)
      nodeSet.add(name)
    })
  }

  // Create edges from records
  for (let i = 0; i < flowRecords.value.length; i++) {
    const r = flowRecords.value[i]
    let srcNodeId: string
    const tgtKey = (r.targetType === 'product_output' || (r.targetUnit || '产出') === '产出') && r.itemType === 'product' && r.itemId
      ? `产出-product-${r.itemId}`
      : (r.targetUnit || '产出')
    const tgtNodeId = `node-${tgtKey}`

    // Use typed key for external-energy source nodes (matches node keying above)
    if (r.sourceType === 'external_energy') {
      const srcKey = r.itemType === 'energy' && r.itemId ? `外购-energy-${r.itemId}` : '外购'
      srcNodeId = `node-${srcKey}`
    } else {
      srcNodeId = `node-${r.sourceUnit || '外购'}`
    }

    const edge: FlowEdgeConfig = {
      edgeId: `edge-${i}`,
      sourceNodeId: srcNodeId,
      targetNodeId: tgtNodeId,
      flowRecordId: r.id,
      flowRecordIndex: i,
      _flowRecordClientKey: r._clientKey,
      itemType: r.itemType,
      itemId: r.itemId,
      physicalQuantity: r.physicalQuantity,
      calculatedValue: r.calculatedValue,
      labelText: buildEdgeLabel(r),
      color: EDGE_COLORS[i % EDGE_COLORS.length],
      lineWidth: 2,
      visible: 1,
    }
    newEdges.push(edge)
  }

  nodes.value = newNodes
  edges.value = newEdges
  canvasWidth.value = Math.max(1200, STAGE_MARGIN * 2 + 4 * stageW)
  const maxRows = Math.max(1, ...Array.from(layerBuckets.values()).map(v => v.length))
  canvasHeight.value = Math.max(800, maxRows * ROW_H + PAD_Y * 2)
  pushUndo()
  ElMessage.success('自动布局完成')
}

function buildEdgeLabel(r: FlowRecord): string {
  const parts: string[] = []
  if (r.energyProduct) parts.push(r.energyProduct)
  if (r.physicalQuantity != null) parts.push(String(r.physicalQuantity))
  if (r.calculatedValue != null) parts.push(`(${r.calculatedValue})`)
  return parts.join(' ')
}

// ============================================================
// Find matching canvas node for a record's source/target
// ============================================================
function findNodeForRecordSource(r: FlowRecord): FlowNodeConfig | undefined {
  if (r.sourceType === 'unit' && r.sourceRefId) {
    return nodes.value.find(n => n.refType === 'unit' && n.refId === r.sourceRefId)
  }
  if (r.sourceType === 'external_energy') {
    if (r.itemType === 'energy' && r.itemId) {
      return nodes.value.find(n => n.nodeType === 'energy_input' && n.refId === r.itemId)
    }
    // No itemId — use generic fallback only in this case
    return nodes.value.find(n => n.nodeType === 'energy_input')
  }
  if (r.sourceType === 'system' && r.sourceUnit) {
    return nodes.value.find(n => n.label === r.sourceUnit)
  }
  return undefined
}

function findNodeForRecordTarget(r: FlowRecord): FlowNodeConfig | undefined {
  if (r.targetType === 'unit' && r.targetRefId) {
    return nodes.value.find(n => n.refType === 'unit' && n.refId === r.targetRefId)
  }
  if (r.targetType === 'product_output') {
    if (r.itemType === 'product' && r.itemId) {
      return nodes.value.find(n => n.nodeType === 'product_output' && n.refId === r.itemId)
    }
    // No itemId — use generic fallback only in this case
    return nodes.value.find(n => n.nodeType === 'product_output')
  }
  if (r.targetType === 'production_system' && r.targetUnit) {
    return nodes.value.find(n => n.label === r.targetUnit)
  }
  return undefined
}

function ensureNodeForRecordSource(r: FlowRecord): FlowNodeConfig | undefined {
  const existing = findNodeForRecordSource(r)
  if (existing) return existing
  if (r.sourceType === 'unit' && r.sourceRefId) {
    const u = units.value.find(u => u.id === r.sourceRefId)
    if (!u) return undefined
    return createAutoNode('unit', 'unit', r.sourceRefId, u.name)
  }
  if (r.sourceType === 'external_energy') {
    const label = r.itemType === 'energy' && r.itemId
      ? (energies.value.find(e => e.id === r.itemId)?.name ?? '外购能源')
      : '外购能源'
    return createAutoNode('energy_input', 'energy', r.itemId ?? null, label)
  }
  if (r.sourceType === 'system' && r.sourceUnit) {
    return createAutoNode('custom', 'custom', null, r.sourceUnit)
  }
  return undefined
}

function ensureNodeForRecordTarget(r: FlowRecord): FlowNodeConfig | undefined {
  const existing = findNodeForRecordTarget(r)
  if (existing) return existing
  if (r.targetType === 'unit' && r.targetRefId) {
    const u = units.value.find(u => u.id === r.targetRefId)
    if (!u) return undefined
    return createAutoNode('unit', 'unit', r.targetRefId, u.name)
  }
  if (r.targetType === 'product_output') {
    const label = r.itemType === 'product' && r.itemId
      ? (products.value.find(p => p.id === r.itemId)?.name ?? '产品产出')
      : '产品产出'
    return createAutoNode('product_output', 'product', r.itemId ?? null, label)
  }
  if (r.targetType === 'production_system' && r.targetUnit) {
    return createAutoNode('custom', 'custom', null, r.targetUnit)
  }
  return undefined
}

function createAutoNode(nodeType: string, refType: string, refId: number | null, label: string): FlowNodeConfig {
  const maxY = nodes.value.reduce((m, n) => Math.max(m, n.positionY + (n.height || 50)), 100)
  const node: FlowNodeConfig = {
    nodeId: `node-auto-${Date.now()}-${++clientKeySeq}`,
    nodeType,
    refType,
    refId,
    label,
    positionX: nodeType === 'energy_input' ? 50 : nodeType === 'product_output' ? 800 : 400,
    positionY: maxY + 20,
    width: nodeType === 'energy_input' ? 60 : 100,
    height: 50,
    color: NODE_COLORS[nodeType] || '#666',
    visible: 1,
    locked: 0,
  }
  nodes.value.push(node)
  return node
}

// ============================================================
// Node interactions
// ============================================================
function handleNodeMouseDown(nodeId: string, event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation()
  selectedNodeId.value = nodeId
  selectedEdgeId.value = null

  if (creatingEdge.value) {
    if (!edgeSourceNodeId.value) {
      // First click: record the source node
      edgeSourceNodeId.value = nodeId
      ElMessage.info('已选中源节点，请点击目标节点')
      return
    }
    // Second click: complete edge creation
    const srcId = edgeSourceNodeId.value
    creatingEdge.value = false
    edgeSourceNodeId.value = null
    if (srcId !== nodeId) {
      showCreateEdgeDialog(srcId, nodeId)
    }
    return
  }

  const node = nodes.value.find(n => n.nodeId === nodeId)
  if (!node || node.locked) return

  // Drag is disabled: positions are determined by fixed-stage layout.
  // Use row reorder controls (上移/下移) in the properties panel instead.
}

// Row reorder: move a node up or down within its fixed-stage bucket
function moveNodeInStage(nodeId: string, direction: 'up' | 'down') {
  const node = nodes.value.find(n => n.nodeId === nodeId)
  if (!node) return
  const fsn = fixedStageLayout.value.get(nodeId)
  if (!fsn) return
  const stage = fsn.stage
  // Find all visible non-product nodes in same stage, in array order
  const sameStage = nodes.value.filter(n => {
    if ((n.visible ?? 1) === 0 || n.nodeType === 'product_output') return false
    const f = fixedStageLayout.value.get(n.nodeId)
    return f && f.stage === stage
  })
  const idx = sameStage.findIndex(n => n.nodeId === nodeId)
  if (idx < 0) return
  const swapIdx = direction === 'up' ? idx - 1 : idx + 1
  if (swapIdx < 0 || swapIdx >= sameStage.length) return
  // Swap in the main nodes array
  const aIdx = nodes.value.indexOf(sameStage[idx])
  const bIdx = nodes.value.indexOf(sameStage[swapIdx])
  if (aIdx < 0 || bIdx < 0) return
  pushUndo()
  const tmp = nodes.value[aIdx]
  nodes.value[aIdx] = nodes.value[bIdx]
  nodes.value[bIdx] = tmp
  pushUndo()
}

// Computed: current node's row index and stage size for UI
const selectedNodeStageInfo = computed(() => {
  if (!selectedNodeId.value) return null
  const fsn = fixedStageLayout.value.get(selectedNodeId.value)
  if (!fsn) return null
  const stage = fsn.stage
  const sameStage = nodes.value.filter(n => {
    if ((n.visible ?? 1) === 0 || n.nodeType === 'product_output') return false
    const f = fixedStageLayout.value.get(n.nodeId)
    return f && f.stage === stage
  })
  const idx = sameStage.findIndex(n => n.nodeId === selectedNodeId.value)
  const STAGE_LABELS = ['购入', '转换', '输配', '终端使用']
  return { stage, stageLabel: STAGE_LABELS[stage] ?? `阶段${stage}`, rowIdx: idx, stageSize: sameStage.length }
})

function handleCanvasMouseMove(event: MouseEvent) {
  if (!dragging.value || !dragNodeId.value) return
  const node = nodes.value.find(n => n.nodeId === dragNodeId.value)
  if (!node) return
  node.positionX = Math.max(0, event.clientX - dragOffset.x)
  node.positionY = Math.max(0, event.clientY - dragOffset.y)
}

function handleCanvasMouseUp() {
  if (dragging.value) {
    dragging.value = false
    dragNodeId.value = null
    pushUndo()
  }
}

function handleCanvasClick() {
  if (!dragging.value) {
    selectedNodeId.value = null
    selectedEdgeId.value = null
  }
}

// Whether the selected edge's label is data-derived (from bound fill record / master data).
// The final-effect renderer ignores labelText for energy-type and product-output edges,
// rendering labels from resolved fill records instead. Editing labelText for these edges
// has no visible effect in preview or PNG export.
const selectedEdgeLabelDerived = computed(() => {
  if (!selectedEdge.value) return false
  const rec = resolveEdgeRecord(selectedEdge.value)
  const iType = rec?.itemType ?? selectedEdge.value.itemType
  // Energy records: label derived from energy master data (name + calculatedValue + physicalQuantity)
  if (iType === 'energy') return true
  // Product output edges: label derived from product master data
  if (isEditorProductEdge(selectedEdge.value)) return true
  return false
})

// ============================================================
// Edge interactions
// ============================================================
function handleEdgeClick(edgeId: string, event: MouseEvent) {
  event.stopPropagation()
  selectedEdgeId.value = edgeId
  selectedNodeId.value = null
}

function startEdgeCreation() {
  creatingEdge.value = true
  edgeSourceNodeId.value = null
  ElMessage.info('点击源节点开始连线')
}

function showCreateEdgeDialog(srcNodeId: string, tgtNodeId: string) {
  const srcNode = nodes.value.find(n => n.nodeId === srcNodeId)
  const tgtNode = nodes.value.find(n => n.nodeId === tgtNodeId)
  if (!srcNode || !tgtNode) return

  pendingEdgeSrcNodeId.value = srcNodeId
  pendingEdgeTgtNodeId.value = tgtNodeId

  const record: FlowRecord = {
    _clientKey: genClientKey(),
    sourceUnit: srcNode.label,
    targetUnit: tgtNode.label,
    sourceType: srcNode.nodeType === 'energy_input' ? 'external_energy' : 'unit',
    targetType: tgtNode.nodeType === 'product_output' ? 'product_output' : 'unit',
    sourceRefId: srcNode.refId,
    targetRefId: tgtNode.refId,
  }
  editingRecord.value = record
  showRecordDialog.value = true
}

// ============================================================
// Record CRUD
// ============================================================
function handleRecordDialogCancel() {
  showRecordDialog.value = false
  pendingEdgeSrcNodeId.value = null
  pendingEdgeTgtNodeId.value = null
  editingRecord.value = null
}

function handleRecordDialogClose() {
  pendingEdgeSrcNodeId.value = null
  pendingEdgeTgtNodeId.value = null
  editingRecord.value = null
}

function addRecord() {
  pendingEdgeSrcNodeId.value = null
  pendingEdgeTgtNodeId.value = null
  editingRecord.value = {
    _clientKey: genClientKey(),
    sourceType: 'external_energy',
    targetType: 'unit',
    itemType: 'energy',
  }
  showRecordDialog.value = true
}

function editRecord(record: FlowRecord) {
  editingRecord.value = { ...record }
  showRecordDialog.value = true
}

async function saveRecord() {
  if (!editingRecord.value) return

  // Fix #5: validate form before saving
  if (recordFormRef.value) {
    try {
      await recordFormRef.value.validate()
    } catch {
      return
    }
  }

  pushUndo()

  const r = editingRecord.value
  // Sync display fields from typed fields
  if (r.sourceType === 'external_energy') {
    r.sourceUnit = '外购'
  } else if (r.sourceRefId) {
    const unit = units.value.find(u => u.id === r.sourceRefId)
    if (unit) r.sourceUnit = unit.name
  }
  if (r.targetType === 'product_output') {
    r.targetUnit = '产出'
  } else if (r.targetRefId) {
    const unit = units.value.find(u => u.id === r.targetRefId)
    if (unit) r.targetUnit = unit.name
  }
  if (r.itemType === 'energy' && r.itemId) {
    const energy = energies.value.find(e => e.id === r.itemId)
    if (energy) {
      r.energyProduct = energy.name
      if (r.physicalQuantity != null && energy.equivalentValue != null) {
        r.calculatedValue = r.physicalQuantity * energy.equivalentValue
      }
    }
  } else if (r.itemType === 'product' && r.itemId) {
    const product = products.value.find(p => p.id === r.itemId)
    if (product) {
      r.energyProduct = product.name
      if (r.physicalQuantity != null && product.unitPrice != null) {
        r.calculatedValue = r.physicalQuantity * product.unitPrice
      }
    }
  }

  // Find existing row by _clientKey (works for both saved and unsaved records)
  const idx = flowRecords.value.findIndex(f => f._clientKey && f._clientKey === r._clientKey)
  if (idx >= 0) {
    flowRecords.value[idx] = r
  } else {
    if (!r._clientKey) r._clientKey = genClientKey()
    flowRecords.value.push(r)
  }

  // Sync all bound edges with updated record data (including source/target node rebinding)
  if (r._clientKey) {
    for (const e of edges.value) {
      if (e._flowRecordClientKey === r._clientKey) {
        e.flowRecordId = r.id
        e.itemType = r.itemType
        e.itemId = r.itemId
        e.physicalQuantity = r.physicalQuantity
        e.calculatedValue = r.calculatedValue
        e.labelText = buildEdgeLabel(r)
        // Rebind source node when record source changes (create node if needed)
        const newSrcNode = ensureNodeForRecordSource(r)
        if (newSrcNode) e.sourceNodeId = newSrcNode.nodeId
        // Rebind target node when record target changes (create node if needed)
        const newTgtNode = ensureNodeForRecordTarget(r)
        if (newTgtNode) e.targetNodeId = newTgtNode.nodeId
      }
    }
  }

  // Mode B: create visual edge when dialog was opened from edge creation
  if (pendingEdgeSrcNodeId.value && pendingEdgeTgtNodeId.value) {
    // Use ensureNode based on final dialog values (user may have changed source/target)
    const finalSrcNode = ensureNodeForRecordSource(r)
    const finalTgtNode = ensureNodeForRecordTarget(r)
    if (!finalSrcNode || !finalTgtNode) {
      ElMessage.error('无法为该记录的来源/目的创建或匹配画布节点，请检查填报数据')
      pendingEdgeSrcNodeId.value = null
      pendingEdgeTgtNodeId.value = null
      showRecordDialog.value = false
      editingRecord.value = null
      return
    }
    const edgeId = `edge-${Date.now()}`
    const newEdge: FlowEdgeConfig = {
      edgeId,
      sourceNodeId: finalSrcNode.nodeId,
      targetNodeId: finalTgtNode.nodeId,
      flowRecordId: r.id,
      _flowRecordClientKey: r._clientKey,
      itemType: r.itemType,
      itemId: r.itemId,
      physicalQuantity: r.physicalQuantity,
      calculatedValue: r.calculatedValue,
      labelText: buildEdgeLabel(r),
      color: EDGE_COLORS[edges.value.length % EDGE_COLORS.length],
      lineWidth: 2,
      visible: 1,
    }
    edges.value.push(newEdge)
    pendingEdgeSrcNodeId.value = null
    pendingEdgeTgtNodeId.value = null
  }

  showRecordDialog.value = false
  editingRecord.value = null
  pushUndo()
}

function deleteRecord(index: number) {
  pushUndo()
  const record = flowRecords.value[index]
  // Remove edges bound to the deleted record
  if (record._clientKey) {
    edges.value = edges.value.filter(e => e._flowRecordClientKey !== record._clientKey)
  } else if (record.id != null) {
    edges.value = edges.value.filter(e => e.flowRecordId !== record.id)
  }
  flowRecords.value.splice(index, 1)
  pushUndo()
}

// ============================================================
// Drag from sidebar
// ============================================================
function handleSidebarDragStart(event: DragEvent, type: string, item: UnitInfo | EnergyInfo | ProductInfo) {
  event.dataTransfer?.setData('application/json', JSON.stringify({ type, item }))
}

function handleCanvasDrop(event: DragEvent) {
  event.preventDefault()
  const data = event.dataTransfer?.getData('application/json')
  if (!data) return

  const { type, item } = JSON.parse(data)

  pushUndo()

  let nodeType = 'custom'
  const refType = type
  if (type === 'energy') nodeType = 'energy_input'
  else if (type === 'unit') nodeType = 'unit'
  else if (type === 'product') nodeType = 'product_output'

  const nodeId = `node-${type}-${item.id}-${Date.now()}`
  const node: FlowNodeConfig = {
    nodeId,
    nodeType,
    refType,
    refId: item.id,
    label: item.name,
    positionX: 0,
    positionY: 0,
    width: nodeType === 'energy_input' ? 60 : 100,
    height: nodeType === 'energy_input' ? 60 : 50,
    color: NODE_COLORS[nodeType] || '#666',
    visible: 1,
    locked: 0,
  }
  nodes.value.push(node)
  pushUndo()
}

function handleCanvasDragOver(event: DragEvent) {
  event.preventDefault()
}

// ============================================================
// Properties panel updates
// ============================================================
function updateNodeProp(prop: string, value: unknown) {
  if (!selectedNode.value) return
  pushUndo();
  (selectedNode.value as Record<string, unknown>)[prop] = value
  pushUndo()
}

function updateEdgeProp(prop: string, value: unknown) {
  if (!selectedEdge.value) return
  pushUndo();
  (selectedEdge.value as Record<string, unknown>)[prop] = value
  pushUndo()
}

function deleteSelectedNode() {
  if (!selectedNodeId.value) return
  pushUndo()
  nodes.value = nodes.value.filter(n => n.nodeId !== selectedNodeId.value)
  edges.value = edges.value.filter(e => e.sourceNodeId !== selectedNodeId.value && e.targetNodeId !== selectedNodeId.value)
  selectedNodeId.value = null
  pushUndo()
}

function deleteSelectedEdge() {
  if (!selectedEdgeId.value) return
  pushUndo()
  edges.value = edges.value.filter(e => e.edgeId !== selectedEdgeId.value)
  selectedEdgeId.value = null
  pushUndo()
}

// ============================================================
// Fix #4: Route point editing helpers
// ============================================================
const parsedRoutePoints = computed(() => {
  if (!selectedEdge.value?.routePoints) return []
  try {
    return JSON.parse(selectedEdge.value.routePoints) as { x: number; y: number }[]
  } catch {
    return []
  }
})

function setRoutePointsJson(pts: { x: number; y: number }[]) {
  if (!selectedEdge.value) return
  pushUndo()
  selectedEdge.value.routePoints = pts.length > 0 ? JSON.stringify(pts) : undefined
  pushUndo()
}

function addRoutePoint() {
  if (!selectedEdge.value) return
  const edge = selectedEdge.value
  const waypoints = previewViewRef.value?.getRenderedWaypoints(edge.edgeId)
  if (waypoints && waypoints.length > 0) {
    setRoutePointsJson(waypoints.map(p => ({ x: Math.round(p.x), y: Math.round(p.y) })))
  }
}

// clearRoutePoints replaced by resetRouteToDefault below

// ============================================================
// Route editing: constrained controls that map to renderer hints
// ============================================================
type RouteEditType = 'backflow' | 'trunk' | 'default' | null

const selectedEdgeRouteType = computed<RouteEditType>(() => {
  if (!selectedEdge.value) return null
  const edge = selectedEdge.value
  if (isEdgeBackflow(edge)) return 'backflow'
  const info = editorTrunkInfoMap.value.get(edge.edgeId)
  const srcFixed = fixedStageLayout.value.get(edge.sourceNodeId)
  if (info && srcFixed && Math.abs(info.trunkX - (srcFixed.cx + srcFixed.w / 2)) > 5) return 'trunk'
  return 'default'
})

interface RouteEditInfo {
  type: RouteEditType
  value: number
  min: number
  max: number
  label: string
  hasMultiTarget?: boolean
}

const selectedEdgeRouteInfo = computed<RouteEditInfo | null>(() => {
  if (!selectedEdge.value) return null
  const edge = selectedEdge.value
  const srcFixed = fixedStageLayout.value.get(edge.sourceNodeId)
  const tgtFixed = fixedStageLayout.value.get(edge.targetNodeId)
  if (!srcFixed || !tgtFixed) return null

  const sx = srcFixed.cx + srcFixed.w / 2
  const sy = srcFixed.cy
  const tx = tgtFixed.cx - tgtFixed.w / 2
  const ty = tgtFixed.cy
  const rpts = parseEditorRoutePoints(edge)
  const rptsValid = rpts.length > 0 && validateEditorRoutePoints(rpts, { x: sx, y: sy }, { x: tx, y: ty })
  const type = selectedEdgeRouteType.value

  if (type === 'backflow') {
    const lane = editorBackflowLaneMap.value.get(edge.edgeId) ?? 0
    const defaultTopY = Math.min(sy, ty) - 40 - lane * BACKFLOW_LANE_SPACING
    let topY = defaultTopY
    if (rptsValid) {
      const minNodeY = Math.min(sy, ty)
      const hintYs = rpts.filter(p => p.y < minNodeY).map(p => p.y)
      if (hintYs.length > 0) {
        const cand = Math.min(...hintYs)
        if (cand >= 0 && cand < minNodeY) topY = cand
      }
    }
    return {
      type: 'backflow',
      value: Math.round(topY),
      min: 10,
      max: Math.round(Math.min(sy, ty) - 5),
      label: '回流通道 Y 位置',
    }
  }

  if (type === 'trunk') {
    const info = editorTrunkInfoMap.value.get(edge.edgeId)!
    let trunkX = info.trunkX
    if (rptsValid) {
      const hintXs = rpts.filter(p => Math.abs(p.x - info.trunkX) <= 30).map(p => p.x)
      if (hintXs.length > 0) trunkX = hintXs[0]
    }
    const hasMulti = info.trunkX !== info.branchX
    return {
      type: 'trunk',
      value: Math.round(trunkX),
      min: Math.round(info.trunkX - 30),
      max: Math.round(info.trunkX + 30),
      label: hasMulti ? '干线 X 位置 (多目标分支)' : '干线 X 位置',
      hasMultiTarget: hasMulti,
    }
  }

  // Default forward
  let midX = (sx + tx) / 2
  if (rptsValid) {
    const hintXs = rpts.map(p => p.x).filter(x => x >= 0 && x <= 5000)
    if (hintXs.length > 0) midX = hintXs[Math.floor(hintXs.length / 2)]
  }
  return {
    type: 'default',
    value: Math.round(midX),
    min: Math.round(Math.min(sx, tx) + 10),
    max: Math.round(Math.max(sx, tx) - 10),
    label: '中间折点 X 位置',
  }
})

function setRouteHintValue(newValue: number) {
  if (!selectedEdge.value || !selectedEdgeRouteInfo.value) return
  const edge = selectedEdge.value
  const info = selectedEdgeRouteInfo.value
  const srcFixed = fixedStageLayout.value.get(edge.sourceNodeId)
  const tgtFixed = fixedStageLayout.value.get(edge.targetNodeId)
  if (!srcFixed || !tgtFixed) return

  pushUndo()
  const sx = Math.round(srcFixed.cx + srcFixed.w / 2)
  const sy = Math.round(srcFixed.cy)
  const tx = Math.round(tgtFixed.cx - tgtFixed.w / 2)
  const ty = Math.round(tgtFixed.cy)
  const clamped = Math.max(info.min, Math.min(info.max, Math.round(newValue)))

  if (info.type === 'backflow') {
    // Backflow: full orthogonal sequence — two waypoints at topY level
    edge.routePoints = JSON.stringify([
      { x: sx, y: clamped },
      { x: tx, y: clamped },
    ])
  } else if (info.type === 'trunk') {
    // Forward trunk: generate full orthogonal waypoint sequence
    const trunkInfo = editorTrunkInfoMap.value.get(edge.edgeId)
    if (!trunkInfo) return
    const branchX = Math.round(trunkInfo.trunkX === trunkInfo.branchX ? clamped : trunkInfo.branchX)
    if (clamped === branchX) {
      // Single-target trunk: two waypoints
      edge.routePoints = JSON.stringify([
        { x: clamped, y: sy },
        { x: clamped, y: ty },
      ])
    } else {
      // Multi-target branch: four waypoints with midY
      const midY = Math.round((sy + ty) / 2)
      edge.routePoints = JSON.stringify([
        { x: clamped, y: sy },
        { x: clamped, y: midY },
        { x: branchX, y: midY },
        { x: branchX, y: ty },
      ])
    }
  } else {
    // Default forward: two waypoints at midX
    edge.routePoints = JSON.stringify([
      { x: clamped, y: sy },
      { x: clamped, y: ty },
    ])
  }
}

function resetRouteToDefault() {
  if (!selectedEdge.value) return
  pushUndo()
  selectedEdge.value.routePoints = undefined
}

/**
 * Normalize all visible edges' routePoints to match the actual rendered path
 * from buildOrthoPath(). Called before save to guarantee saved = rendered.
 */
function normalizeAllRoutePoints() {
  if (!previewViewRef.value) return
  for (const e of edges.value) {
    const vis = e.visible ?? 1
    if (vis === 0) continue
    if (!e.routePoints) continue
    const waypoints = previewViewRef.value.getRenderedWaypoints(e.edgeId)
    if (waypoints && waypoints.length > 0) {
      e.routePoints = JSON.stringify(waypoints.map(p => ({ x: Math.round(p.x), y: Math.round(p.y) })))
    } else {
      e.routePoints = undefined
    }
  }
}

// ============================================================
// Canonical routing (editor uses the same algorithm as final-effect renderer)
// ============================================================
const BACKFLOW_LANE_SPACING = 12

function isEdgeBackflow(edge: FlowEdgeConfig): boolean {
  // Use fixed-stage layout stage comparison (same as final-effect renderer)
  const srcFixed = fixedStageLayout.value.get(edge.sourceNodeId)
  const dstFixed = fixedStageLayout.value.get(edge.targetNodeId)
  if (!srcFixed || !dstFixed) return false
  return srcFixed.stage > dstFixed.stage
}

function resolveEdgeRecord(edge: FlowEdgeConfig): FlowRecord | undefined {
  if (edge._flowRecordClientKey) {
    return flowRecords.value.find(r => r._clientKey === edge._flowRecordClientKey)
  }
  if (edge.flowRecordId != null) {
    return flowRecords.value.find(r => r.id === edge.flowRecordId)
  }
  return undefined
}

const editorBackflowLaneMap = computed(() => {
  const m = new Map<string, number>()
  const visEdges = edges.value.filter(e => (e.visible ?? 1) !== 0)
  const backflowEdges = visEdges.filter(e => isEdgeBackflow(e))
  const groups = new Map<string, FlowEdgeConfig[]>()
  for (const e of backflowEdges) {
    const rec = resolveEdgeRecord(e)
    const iId = rec?.itemId ?? e.itemId
    const key = iId ? `bf-${iId}-${e.sourceNodeId}-${e.targetNodeId}` : `bf-${e.edgeId}`
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(e)
  }
  let laneIdx = 0
  for (const [, grp] of groups) {
    for (const e of grp) m.set(e.edgeId, laneIdx)
    laneIdx++
  }
  return m
})

interface EditorTrunkInfo { trunkX: number; branchX: number }
const editorTrunkInfoMap = computed(() => {
  const m = new Map<string, EditorTrunkInfo>()
  const visEdges = edges.value.filter(e => (e.visible ?? 1) !== 0)
  const trunkGroups = new Map<string, FlowEdgeConfig[]>()
  for (const e of visEdges) {
    if (isEdgeBackflow(e)) continue
    const src = nodes.value.find(n => n.nodeId === e.sourceNodeId)
    if (!src) continue
    const rec = resolveEdgeRecord(e)
    const iId = rec?.itemId ?? e.itemId
    const tk = `${e.sourceNodeId}-${iId ?? ''}`
    if (!trunkGroups.has(tk)) trunkGroups.set(tk, [])
    trunkGroups.get(tk)!.push(e)
  }
  let slotIdx = 0
  for (const [, grp] of trunkGroups) {
    const srcFixed = fixedStageLayout.value.get(grp[0].sourceNodeId)
    if (!srcFixed) continue
    const trunkX = srcFixed.cx + srcFixed.w / 2 + 20 + slotIdx * 16
    const targetSet = new Set(grp.map(e => e.targetNodeId))
    const targets = Array.from(targetSet)
    for (const e of grp) {
      if (targets.length <= 1) {
        m.set(e.edgeId, { trunkX, branchX: trunkX })
      } else {
        const tIdx = targets.indexOf(e.targetNodeId)
        const branchX = trunkX + (tIdx + 1) * 8
        m.set(e.edgeId, { trunkX, branchX })
      }
    }
    slotIdx++
  }
  return m
})

function parseEditorRoutePoints(edge: FlowEdgeConfig): { x: number; y: number }[] {
  if (!edge.routePoints) return []
  try {
    const pts = JSON.parse(edge.routePoints) as { x: number; y: number }[]
    if (!Array.isArray(pts)) return []
    return pts.filter(p => typeof p.x === 'number' && typeof p.y === 'number' && isFinite(p.x) && isFinite(p.y))
  } catch { return [] }
}

function validateEditorRoutePoints(rpts: { x: number; y: number }[], s: { x: number; y: number }, t: { x: number; y: number }): boolean {
  if (!rpts.length) return true
  for (const p of rpts) {
    if (p.x < 0 || p.y < 0 || p.x > 5000 || p.y > 5000) return false
  }
  const full = [s, ...rpts, t]
  for (let i = 0; i < full.length - 1; i++) {
    const a = full[i], b = full[i + 1]
    if (Math.abs(a.x - b.x) > 1 && Math.abs(a.y - b.y) > 1) return false
  }
  return true
}

// ============================================================
// SVG edge path generation (canonical routing — matches final-effect renderer)
// ============================================================
function edgePath(edge: FlowEdgeConfig): string {
  // Use fixed-stage layout positions (same as final-effect renderer)
  const srcFixed = fixedStageLayout.value.get(edge.sourceNodeId)
  const dstFixed = fixedStageLayout.value.get(edge.targetNodeId)
  if (!srcFixed || !dstFixed) return ''

  const sx = srcFixed.cx + srcFixed.w / 2
  const sy = srcFixed.cy
  const tx = dstFixed.cx - dstFixed.w / 2
  const ty = dstFixed.cy

  // Parse route points as constrained hints (same contract as final renderer)
  const rpts = parseEditorRoutePoints(edge)
  const rptsValid = rpts.length > 0 && validateEditorRoutePoints(rpts, { x: sx, y: sy }, { x: tx, y: ty })

  if (isEdgeBackflow(edge)) {
    const lane = editorBackflowLaneMap.value.get(edge.edgeId) ?? 0
    const defaultTopY = Math.min(sy, ty) - 40 - lane * BACKFLOW_LANE_SPACING
    let topY = defaultTopY
    if (rptsValid) {
      const minNodeY = Math.min(sy, ty)
      const hintYs = rpts.filter(p => p.y < minNodeY).map(p => p.y)
      if (hintYs.length > 0) {
        const candidateY = Math.min(...hintYs)
        if (candidateY >= 0 && candidateY < minNodeY) topY = candidateY
      }
    }
    return `M ${sx} ${sy} L ${sx} ${topY} L ${tx} ${topY} L ${tx} ${ty}`
  }

  // Forward edge: trunk/branch routing
  const info = editorTrunkInfoMap.value.get(edge.edgeId)
  if (info && Math.abs(info.trunkX - sx) > 5) {
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
      return `M ${sx} ${sy} L ${trunkX} ${sy} L ${trunkX} ${ty} L ${tx} ${ty}`
    }
    const midY = (sy + ty) / 2
    return `M ${sx} ${sy} L ${trunkX} ${sy} L ${trunkX} ${midY} L ${branchX} ${midY} L ${branchX} ${ty} L ${tx} ${ty}`
  }

  // Default: midpoint routing with route point X hints
  let midX = (sx + tx) / 2
  if (rptsValid) {
    const hintXs = rpts.map(p => p.x).filter(x => x >= 0 && x <= 5000)
    if (hintXs.length > 0) midX = hintXs[Math.floor(hintXs.length / 2)]
  }
  return `M ${sx} ${sy} L ${midX} ${sy} L ${midX} ${ty} L ${tx} ${ty}`
}

// ============================================================
// Product output lines (editor renders same as final-effect renderer:
// black horizontal line from source unit exit to right boundary)
// ============================================================
interface EditorProductLine { y: number; x1: number; topText: string; bottomText: string; edgeId: string; invalid?: boolean }
const editorProductLines = computed<EditorProductLine[]>(() => {
  const results: EditorProductLine[] = []
  for (const e of edges.value) {
    if ((e.visible ?? 1) === 0) continue
    if (!isEditorProductEdge(e)) continue
    // Use fixed-stage layout positions for source node (same as final-effect renderer)
    const srcFixed = fixedStageLayout.value.get(e.sourceNodeId)
    if (!srcFixed) continue
    const rec = resolveEdgeRecord(e)
    const itemId = rec?.itemId ?? e.itemId
    const pr = itemId ? products.value.find(p => p.id === itemId) : undefined
    const topText = pr?.name ?? ''
    const pq = rec?.physicalQuantity ?? e.physicalQuantity
    const unit = pr?.measurementUnit ?? ''
    const bottomText = pq != null ? `${pq} ${unit}` : ''
    // Non-stage-3 sources are invalid (same check as final-effect renderer)
    const invalid = srcFixed.stage !== 3
    results.push({
      y: srcFixed.cy,
      x1: srcFixed.cx + srcFixed.w / 2,
      topText,
      bottomText,
      edgeId: e.edgeId,
      invalid,
    })
  }
  return results
})

// ============================================================
// Fixed-stage layout computation (same algorithm as EnergyFlowConfigView)
// Used for route-point validation to match final-effect renderer positions
// ============================================================
function editorNodeStage(n: FlowNodeConfig): number {
  if (n.nodeType === 'energy_input') return 0
  if (n.nodeType === 'product_output') return 3
  if (n.nodeType === 'unit' && n.refId) {
    const u = units.value.find(uu => uu.id === n.refId)
    if (u) {
      if (u.unitType === 1) return 1
      if (u.unitType === 2) return 2
      if (u.unitType === 3) return 3
    }
  }
  return 2
}

interface FixedStageNode { nodeId: string; stage: number; cx: number; cy: number; w: number; h: number }
const fixedStageLayout = computed<Map<string, FixedStageNode>>(() => {
  const STAGE_MARGIN = 80
  const BASE_HEADER_Y = 55
  const BF_LANE_SP = 12
  const ROW_H = 90

  const visible = nodes.value.filter(n => (n.visible ?? 1) !== 0 && n.nodeType !== 'product_output')

  // Count backflow lanes for top channel height
  const bfGroups = new Set<string>()
  for (const e of edges.value.filter(ee => (ee.visible ?? 1) !== 0)) {
    const src = visible.find(n => n.nodeId === e.sourceNodeId)
    const tgt = visible.find(n => n.nodeId === e.targetNodeId)
    if (!src || !tgt) continue
    if (editorNodeStage(src) <= editorNodeStage(tgt)) continue
    const rec = resolveEdgeRecord(e)
    const iId = rec?.itemId ?? e.itemId
    bfGroups.add(iId ? `bf-${iId}-${e.sourceNodeId}-${e.targetNodeId}` : `bf-${e.edgeId}`)
  }
  const topChannelH = bfGroups.size > 0 ? bfGroups.size * BF_LANE_SP + 10 : 0
  const BODY_TOP = BASE_HEADER_Y + topChannelH + 20

  const sw = (canvasWidth.value - STAGE_MARGIN * 2) / 4
  const stageXArr = [0, 1, 2, 3].map(i => STAGE_MARGIN + i * sw)

  const buckets = new Map<number, FlowNodeConfig[]>()
  for (const n of visible) {
    const s = editorNodeStage(n)
    if (!buckets.has(s)) buckets.set(s, [])
    buckets.get(s)!.push(n)
  }

  const m = new Map<string, FixedStageNode>()
  for (const [stage, stageNodes] of buckets) {
    stageNodes.forEach((n, rowIdx) => {
      const isCircle = n.nodeType === 'energy_input'
      const w = isCircle ? 60 : 100
      const h = isCircle ? 60 : 50
      const cx = stageXArr[stage] + sw / 2
      const cy = BODY_TOP + rowIdx * ROW_H + ROW_H / 2
      m.set(n.nodeId, { nodeId: n.nodeId, stage, cx, cy, w, h })
    })
  }
  return m
})

function edgeLabelPos(edge: FlowEdgeConfig): { x: number; y: number } {
  // Use fixed-stage layout positions (same as final-effect renderer)
  const srcFixed = fixedStageLayout.value.get(edge.sourceNodeId)
  const dstFixed = fixedStageLayout.value.get(edge.targetNodeId)
  if (!srcFixed || !dstFixed) return { x: 0, y: 0 }

  const sx = srcFixed.cx + srcFixed.w / 2
  const sy = srcFixed.cy
  const tx = dstFixed.cx - dstFixed.w / 2
  const ty = dstFixed.cy

  return {
    x: (sx + tx) / 2,
    y: (sy + ty) / 2 - 8,
  }
}

// ============================================================
// Local export validation (runs against current unsaved page state)
// ============================================================
function computeLocalExportErrors(): string[] {
  const errors: string[] = []
  // Merge backend export errors (legacy 待确认, enterprise fields, etc.)
  if (validation.value.exportErrors && validation.value.exportErrors.length > 0) {
    errors.push(...validation.value.exportErrors)
  }
  if (!validation.value.enterpriseComplete) {
    errors.push('企业信息不完整（请先完善企业概况页面）')
  }
  if (units.value.length === 0) errors.push('至少需要一个用能单元')
  if (energies.value.length === 0) errors.push('至少需要一个能源品种')
  if (products.value.length === 0) errors.push('至少需要一个产品')
  for (const r of flowRecords.value) {
    if (r.itemType === 'energy' && r.itemId) {
      const energy = energies.value.find(e => e.id === r.itemId)
      if (!energy) {
        errors.push(`填报记录的能源品种(itemId=${r.itemId})在本企业中不存在或已删除（待确认）`)
      } else {
        if (energy.equivalentValue == null) {
          errors.push(`能源 [${energy.name}] 缺少当量值系数(equivalentValue)`)
        }
        if (energy.equalValue == null) {
          errors.push(`能源 [${energy.name}] 缺少等价值系数(equalValue)`)
        }
      }
    } else if (r.itemType === 'product' && r.itemId) {
      const product = products.value.find(p => p.id === r.itemId)
      if (!product) {
        errors.push(`填报记录的产品(itemId=${r.itemId})在本企业中不存在或已删除（待确认）`)
      } else if (product.unitPrice == null) {
        errors.push(`产品 [${product.name}] 缺少单价`)
      }
    } else if (r.itemType && !r.itemId) {
      errors.push(`填报记录品目类型为 [${r.itemType}] 但未选择品目(itemId为空)（待确认）`)
    } else if (!r.itemType && r.energyProduct) {
      errors.push(`填报记录 [${r.energyProduct}] 为旧数据（待确认），请编辑确认品目类型和品目`)
    }
  }
  // Terminal-use semantics: product-output records require:
  //   itemType=product + active product itemId + sourceType=unit + active sourceRefId with unitType=3
  // Block external_energy/system/custom/missing refs/missing unitType/non-terminal units/energy-to-product
  for (const r of flowRecords.value) {
    if (r.targetType !== 'product_output') continue
    // Must be itemType=product with valid product itemId
    if (r.itemType !== 'product') {
      errors.push(`产品输出记录必须设置itemType=product，当前itemType=${r.itemType ?? '空'}`)
    } else if (!r.itemId) {
      errors.push('产品输出记录必须关联有效的产品(itemId不能为空)')
    } else if (!products.value.find(p => p.id === r.itemId)) {
      errors.push(`产品输出记录关联的产品(itemId=${r.itemId})不存在`)
    }
    // Must be sourceType=unit with active terminal-use source
    if (r.sourceType !== 'unit') {
      errors.push(`产品输出记录的来源类型必须为unit(终端使用环节用能单元)，当前sourceType=${r.sourceType ?? '空'}`)
    } else if (!r.sourceRefId) {
      errors.push('产品输出记录必须关联来源单元(sourceRefId不能为空)')
    } else {
      const srcUnit = units.value.find(u => u.id === r.sourceRefId)
      if (!srcUnit) {
        errors.push(`产品输出记录的来源单元(sourceRefId=${r.sourceRefId})不存在`)
      } else if (srcUnit.unitType == null) {
        errors.push(`产品输出记录的来源单元 [${srcUnit.name}] 缺少unitType，无法确认为终端使用环节`)
      } else if (srcUnit.unitType !== 3) {
        errors.push(`产品输出记录的来源单元 [${srcUnit.name}] 不是终端使用环节(unitType=${srcUnit.unitType})`)
      }
    }
  }
  // Check visible edges for valid record bindings, endpoint nodes, and endpoint semantics
  const nodeIdSet = new Set(nodes.value.map(n => n.nodeId))
  const nodeByIdMap = new Map(nodes.value.map(n => [n.nodeId, n]))
  for (const e of edges.value) {
    if (e.visible === 0) continue
    // Find the bound record
    let boundRec: FlowRecord | undefined
    if (!e._flowRecordClientKey && !e.flowRecordId) {
      errors.push(`连线 [${e.edgeId}] 未绑定到有效的填报记录`)
    } else if (e._flowRecordClientKey) {
      boundRec = flowRecords.value.find(r => r._clientKey === e._flowRecordClientKey)
      if (!boundRec) errors.push(`连线 [${e.edgeId}] 绑定的填报记录已被删除`)
    } else if (e.flowRecordId) {
      boundRec = flowRecords.value.find(r => r.id === e.flowRecordId)
      if (!boundRec) errors.push(`连线 [${e.edgeId}] 绑定的填报记录(id=${e.flowRecordId})不存在`)
    }
    // Validate endpoint nodes: must be non-blank and exist
    if (!e.sourceNodeId) {
      errors.push(`连线 [${e.edgeId}] 的起点节点为空`)
    } else if (!nodeIdSet.has(e.sourceNodeId)) {
      errors.push(`连线 [${e.edgeId}] 的起点节点(${e.sourceNodeId})不存在`)
    }
    if (!e.targetNodeId) {
      errors.push(`连线 [${e.edgeId}] 的终点节点为空`)
    } else if (!nodeIdSet.has(e.targetNodeId)) {
      errors.push(`连线 [${e.edgeId}] 的终点节点(${e.targetNodeId})不存在`)
    }
    // Validate endpoint semantics against bound record (including null-ref detection)
    if (boundRec && e.sourceNodeId && nodeIdSet.has(e.sourceNodeId)) {
      const srcNode = nodeByIdMap.get(e.sourceNodeId)
      if (srcNode && boundRec.sourceType) {
        if (boundRec.sourceType === 'external_energy' && srcNode.nodeType !== 'energy_input') {
          errors.push(`连线 [${e.edgeId}] 的起点节点类型(${srcNode.nodeType})与填报记录来源类型(external_energy→energy_input)不一致`)
        } else if (boundRec.sourceType === 'external_energy' && boundRec.itemType === 'energy' && boundRec.itemId) {
          if (!srcNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的起点节点未关联能源(refId为空)，但填报记录要求能源品种(itemId=${boundRec.itemId})`)
          } else if (boundRec.itemId !== srcNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的起点节点引用能源(refId=${srcNode.refId})与填报记录能源品种(itemId=${boundRec.itemId})不一致`)
          }
        } else if (boundRec.sourceType === 'unit' && srcNode.nodeType !== 'unit') {
          errors.push(`连线 [${e.edgeId}] 的起点节点类型(${srcNode.nodeType})与填报记录来源类型(unit)不一致`)
        } else if (boundRec.sourceType === 'unit' && boundRec.sourceRefId) {
          if (!srcNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的起点节点未关联单元(refId为空)，但填报记录要求来源单元(sourceRefId=${boundRec.sourceRefId})`)
          } else if (boundRec.sourceRefId !== srcNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的起点节点引用单元(refId=${srcNode.refId})与填报记录来源单元(sourceRefId=${boundRec.sourceRefId})不一致`)
          }
        }
      }
    }
    if (boundRec && e.targetNodeId && nodeIdSet.has(e.targetNodeId)) {
      const tgtNode = nodeByIdMap.get(e.targetNodeId)
      if (tgtNode && boundRec.targetType) {
        if (boundRec.targetType === 'unit' && tgtNode.nodeType !== 'unit') {
          errors.push(`连线 [${e.edgeId}] 的终点节点类型(${tgtNode.nodeType})与填报记录目的类型(unit)不一致`)
        } else if (boundRec.targetType === 'unit' && boundRec.targetRefId) {
          if (!tgtNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的终点节点未关联单元(refId为空)，但填报记录要求目的单元(targetRefId=${boundRec.targetRefId})`)
          } else if (boundRec.targetRefId !== tgtNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的终点节点引用单元(refId=${tgtNode.refId})与填报记录目的单元(targetRefId=${boundRec.targetRefId})不一致`)
          }
        } else if (boundRec.targetType === 'product_output' && tgtNode.nodeType !== 'product_output') {
          errors.push(`连线 [${e.edgeId}] 的终点节点类型(${tgtNode.nodeType})与填报记录目的类型(product_output)不一致`)
        } else if (boundRec.targetType === 'product_output' && boundRec.itemType === 'product' && boundRec.itemId) {
          if (!tgtNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的终点节点未关联产品(refId为空)，但填报记录要求产品(itemId=${boundRec.itemId})`)
          } else if (boundRec.itemId !== tgtNode.refId) {
            errors.push(`连线 [${e.edgeId}] 的终点节点引用产品(refId=${tgtNode.refId})与填报记录产品(itemId=${boundRec.itemId})不一致`)
          }
        }
      }
    }
    // Validate route points against fixed-stage layout positions (same as final-effect renderer)
    if (e.routePoints && e.sourceNodeId && e.targetNodeId) {
      const srcFixed = fixedStageLayout.value.get(e.sourceNodeId)
      const dstFixed = fixedStageLayout.value.get(e.targetNodeId)
      if (srcFixed && dstFixed) {
        try {
          const rpts = JSON.parse(e.routePoints) as { x: number; y: number }[]
          if (Array.isArray(rpts) && rpts.length > 0) {
            const sx = srcFixed.cx + srcFixed.w / 2
            const sy = srcFixed.cy
            const tx = dstFixed.cx - dstFixed.w / 2
            const ty = dstFixed.cy
            const full = [{ x: sx, y: sy }, ...rpts, { x: tx, y: ty }]
            for (let i = 0; i < full.length - 1; i++) {
              const a = full[i], b = full[i + 1]
              if (Math.abs(a.x - b.x) > 1 && Math.abs(a.y - b.y) > 1) {
                errors.push(`连线 [${e.edgeId}] 的路由点不符合90°正交规则（第${i + 1}段为斜线），请重新编辑路由点`)
                break
              }
            }
            for (const p of rpts) {
              if (p.x < 0 || p.y < 0 || p.x > 5000 || p.y > 5000) {
                errors.push(`连线 [${e.edgeId}] 的路由点超出画布范围`)
                break
              }
            }
            // Check node-crossing against fixed-stage positions
            for (const [nid, fsn] of fixedStageLayout.value) {
              if (nid === e.sourceNodeId || nid === e.targetNodeId) continue
              const nx = fsn.cx - fsn.w / 2, ny = fsn.cy - fsn.h / 2
              const nw = fsn.w, nh = fsn.h
              for (const p of rpts) {
                if (p.x > nx && p.x < nx + nw && p.y > ny && p.y < ny + nh) {
                  errors.push(`连线 [${e.edgeId}] 的路由点穿过节点 [${nid}]，请调整路由点避免节点交叉`)
                  break
                }
              }
            }
            // Final-renderer equivalence: compare saved route points against
            // buildOrthoPath() output to guarantee saved = rendered consistency.
            if (previewViewRef.value) {
              const rendered = previewViewRef.value.getRenderedWaypoints(e.edgeId)
              if (rendered && rendered.length > 0) {
                if (rpts.length !== rendered.length) {
                  errors.push(`连线 [${e.edgeId}] 的路由点数量(${rpts.length})与最终渲染路径(${rendered.length})不一致，请重新添加折点`)
                } else {
                  for (let i = 0; i < rpts.length; i++) {
                    if (Math.abs(rpts[i].x - Math.round(rendered[i].x)) > 2 ||
                        Math.abs(rpts[i].y - Math.round(rendered[i].y)) > 2) {
                      errors.push(`连线 [${e.edgeId}] 的路由点[${i}](${rpts[i].x},${rpts[i].y})与最终渲染路径(${Math.round(rendered[i].x)},${Math.round(rendered[i].y)})不一致，将被忽略`)
                      break
                    }
                  }
                }
              }
            }
          }
        } catch {
              errors.push(`连线 [${e.edgeId}] 的routePoints为无效JSON格式，请重新编辑路由点`)
            }
      }
    }
  }
  return [...new Set(errors)]
}

// ============================================================
// PNG Export
// ============================================================
async function handleExportPng() {
  // Gate: block on validation.exportReady
  if (!validation.value.exportReady) {
    const reasons = validation.value.exportErrors?.length
      ? validation.value.exportErrors.join('\n')
      : '前置资料不完整'
    ElMessageBox.alert(reasons, '无法导出 PNG — 数据验证未通过', { type: 'error' })
    return
  }
  // Run local validation against current page state
  const localErrors = computeLocalExportErrors()
  if (localErrors.length > 0) {
    ElMessageBox.alert(
      localErrors.join('\n'),
      '无法导出 PNG — 以下问题必须解决',
      { type: 'error' }
    )
    return
  }
  if (nodes.value.length === 0) {
    ElMessage.warning('图表为空，无法导出')
    return
  }
  // Use off-screen final-effect renderer (EnergyFlowConfigView) for export
  if (!exportViewRef.value) {
    ElMessage.error('导出渲染器未就绪')
    return
  }
  try {
    const dataUri = await exportViewRef.value.exportPng()
    if (!dataUri) {
      ElMessage.warning('导出失败，图表为空')
      return
    }
    const link = document.createElement('a')
    link.download = `energy-flow-${enterpriseName.value}-${auditYear.value}.png`
    link.href = dataUri
    link.click()
    ElMessage.success('已导出 PNG')
  } catch {
    ElMessage.error('导出失败')
  }
}

// ============================================================
// Lifecycle
// ============================================================
onMounted(() => {
  loadData()
})

function formatNum(n: number | null | undefined): string {
  if (n == null) return '-'
  return Number(n).toFixed(2)
}
</script>

<template>
  <div class="energy-flow-config-page">
    <!-- Off-screen final-effect renderer for PNG export -->
    <div style="position: absolute; left: -9999px; top: -9999px; pointer-events: none;">
      <EnergyFlowConfigView
        ref="exportViewRef"
        :nodes="nodes"
        :edges="edges"
        :flow-records="flowRecords"
        :energies="energies"
        :units="units"
        :products="products"
        :energy-consumption="energyConsumption"
        :enterprise-name="enterpriseName"
        :audit-year="auditYear"
        :canvas-width="canvasWidth"
        :canvas-height="canvasHeight"
        :validation="validation"
      />
    </div>
    <!-- Top Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <h3>能源流程图配置</h3>
        <el-select v-model="auditYear" style="width: 110px; margin-left: 12px" @change="loadData">
          <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
        </el-select>
      </div>
      <div class="toolbar-actions">
        <el-button @click="undo" :disabled="undoStack.length <= 1" size="small">撤销</el-button>
        <el-button @click="redo" :disabled="redoStack.length === 0" size="small">重做</el-button>
        <el-button @click="autoLayout" size="small" type="info">自动布局</el-button>
        <el-button @click="startEdgeCreation" size="small">连线</el-button>
        <el-button @click="handleValidate" size="small">校验</el-button>
        <el-button @click="showFinalPreview = !showFinalPreview" size="small" :type="showFinalPreview ? 'warning' : 'info'">{{ showFinalPreview ? '返回编辑' : '预览效果' }}</el-button>
        <el-button @click="handleExportPng" size="small" type="success">导出 PNG</el-button>
        <el-button @click="handleSave" :loading="saving" type="primary" size="small">保存</el-button>
      </div>
    </div>

    <div class="main-layout">
      <!-- Left Sidebar -->
      <div class="sidebar-left">
        <div class="sidebar-section">
          <h4>{{ enterpriseName }}</h4>
          <div class="validation-badges">
            <el-tag :type="validation.enterpriseComplete ? 'success' : 'danger'" size="small">企业信息</el-tag>
            <el-tag :type="validation.hasUnits ? 'success' : 'danger'" size="small">用能单元</el-tag>
            <el-tag :type="validation.hasEnergies ? 'success' : 'danger'" size="small">能源品种</el-tag>
            <el-tag :type="validation.hasProducts ? 'success' : 'danger'" size="small">产品</el-tag>
          </div>
        </div>

        <div class="sidebar-section">
          <h4>用能单元</h4>
          <div
            v-for="u in units"
            :key="u.id"
            class="sidebar-item draggable"
            draggable="true"
            @dragstart="e => handleSidebarDragStart(e, 'unit', u)"
          >
            <span class="dot" style="background: #3498DB"></span>
            {{ u.name }}
            <span class="sub">{{ ['', '加工转换', '输送分配', '终端使用'][u.unitType] || '' }}</span>
          </div>
        </div>

        <div class="sidebar-section">
          <h4>能源品种</h4>
          <div
            v-for="e in energies"
            :key="e.id"
            class="sidebar-item draggable"
            draggable="true"
            @dragstart="ev => handleSidebarDragStart(ev, 'energy', e)"
          >
            <span class="dot" style="background: #E74C3C"></span>
            {{ e.name }}
          </div>
        </div>

        <div class="sidebar-section">
          <h4>产品</h4>
          <div
            v-for="p in products"
            :key="p.id"
            class="sidebar-item draggable"
            draggable="true"
            @dragstart="e => handleSidebarDragStart(e, 'product', p)"
          >
            <span class="dot" style="background: #27AE60"></span>
            {{ p.name }}
          </div>
        </div>
      </div>

      <!-- Center Canvas -->
      <div class="canvas-area" v-loading="loading">
        <!-- Final-effect renderer: always mounted so getRenderedWaypoints() is available
             in normal edit mode for route-point seeding, normalization, and validation.
             Hidden via v-show when the editor canvas is active. -->
        <EnergyFlowConfigView
          v-show="showFinalPreview"
          ref="previewViewRef"
          :nodes="nodes"
          :edges="edges"
          :flow-records="flowRecords"
          :energies="energies"
          :units="units"
          :products="products"
          :energy-consumption="energyConsumption"
          :enterprise-name="enterpriseName"
          :audit-year="auditYear"
          :canvas-width="canvasWidth"
          :canvas-height="canvasHeight"
          :validation="validation"
        />
        <!-- Editor canvas (visible when showFinalPreview is off) -->
        <svg
          v-show="!showFinalPreview"
          ref="svgRef"
          :width="canvasWidth"
          :height="canvasHeight"
          :viewBox="`0 0 ${canvasWidth} ${canvasHeight}`"
          xmlns="http://www.w3.org/2000/svg"
          class="flow-canvas"
          @mousemove="handleCanvasMouseMove"
          @mouseup="handleCanvasMouseUp"
          @click="handleCanvasClick"
          @drop="handleCanvasDrop"
          @dragover="handleCanvasDragOver"
        >
          <!-- Background -->
          <rect :width="canvasWidth" :height="canvasHeight" fill="#fff" />

          <!-- Title -->
          <text :x="canvasWidth / 2" y="30" text-anchor="middle" font-size="16" font-weight="bold" fill="#1f3a68">
            {{ enterpriseName }} {{ auditYear }}年 能源流程图
          </text>

          <!-- Defs: arrow markers -->
          <defs>
            <marker
              v-for="(c, ci) in EDGE_COLORS"
              :key="ci"
              :id="`arrow-cfg-${c.replace('#', '')}`"
              markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto" markerUnits="strokeWidth"
            >
              <path d="M0,0 L8,4 L0,8 z" :fill="c" />
            </marker>
          </defs>

          <!-- Edges (skip product edges — rendered as product output lines below) -->
          <g class="edges-layer">
            <template v-for="(e, ei) in edges" :key="e.edgeId">
              <path
                v-if="(e.visible ?? 1) !== 0 && !isEditorProductEdge(e)"
                :d="edgePath(e)"
                :stroke="edgeColor(e, ei)"
                :stroke-width="e.lineWidth || 2"
                fill="none"
                :marker-end="`url(#arrow-cfg-${edgeColor(e, ei).replace('#', '')})`"
                style="cursor: pointer"
                :class="{ 'edge-selected': selectedEdgeId === e.edgeId }"
                @click.stop="handleEdgeClick(e.edgeId, $event)"
              />
              <text
                v-if="(e.visible ?? 1) !== 0 && !isEditorProductEdge(e) && e.labelText"
                :x="edgeLabelPos(e).x"
                :y="edgeLabelPos(e).y"
                text-anchor="middle"
                font-size="11"
                :fill="edgeColor(e, ei)"
                stroke="#fff" stroke-width="3" stroke-linejoin="round" paint-order="stroke"
              >{{ e.labelText }}</text>
            </template>
          </g>

          <!-- Product output lines: black horizontal to right boundary with arrow (matches final-effect renderer) -->
          <g class="product-lines-layer">
            <template v-for="pl in editorProductLines" :key="pl.edgeId">
              <line :x1="pl.x1" :y1="pl.y" :x2="canvasWidth - 30" :y2="pl.y"
                :stroke="pl.invalid ? '#E74C3C' : '#000'" stroke-width="2"
                :stroke-dasharray="pl.invalid ? '6,3' : undefined"
                :marker-end="`url(#arrow-cfg-${pl.invalid ? 'E74C3C' : '000'})`" />
              <text
                v-if="pl.topText || pl.invalid"
                :x="(pl.x1 + canvasWidth - 30) / 2" :y="pl.y - 8"
                text-anchor="middle" font-size="10" :fill="pl.invalid ? '#E74C3C' : '#333'"
                stroke="#fff" stroke-width="3" stroke-linejoin="round" paint-order="stroke"
              >{{ pl.invalid ? '⚠ ' + (pl.topText || '非终端使用来源') : pl.topText }}</text>
              <text
                v-if="pl.bottomText"
                :x="(pl.x1 + canvasWidth - 30) / 2" :y="pl.y + 14"
                text-anchor="middle" font-size="9" :fill="pl.invalid ? '#E74C3C' : '#666'"
                stroke="#fff" stroke-width="2" stroke-linejoin="round" paint-order="stroke"
              >{{ pl.bottomText }}</text>
            </template>
          </g>

          <!-- Nodes (rendered at fixed-stage positions, same as final-effect renderer) -->
          <g class="nodes-layer">
            <template v-for="n in nodes" :key="n.nodeId">
              <g
                v-if="(n.visible ?? 1) !== 0 && n.nodeType !== 'product_output' && fixedStageLayout.get(n.nodeId)"
                :class="{ 'node-selected': selectedNodeId === n.nodeId, 'node-locked': n.locked }"
                style="cursor: pointer"
                @mousedown.stop="handleNodeMouseDown(n.nodeId, $event)"
              >
                <!-- Energy input: circle (fixed-stage position) -->
                <template v-if="n.nodeType === 'energy_input'">
                  <circle
                    :cx="fixedStageLayout.get(n.nodeId)!.cx"
                    :cy="fixedStageLayout.get(n.nodeId)!.cy"
                    :r="fixedStageLayout.get(n.nodeId)!.w / 2"
                    :stroke="nodeColor(n)"
                    stroke-width="2"
                    fill="#fff"
                  />
                  <text
                    :x="fixedStageLayout.get(n.nodeId)!.cx"
                    :y="fixedStageLayout.get(n.nodeId)!.cy + 4"
                    text-anchor="middle" font-size="11" :fill="nodeColor(n)"
                  >{{ n.label }}</text>
                </template>

                <!-- Unit / custom: rect (fixed-stage position) -->
                <template v-else>
                  <rect
                    :x="fixedStageLayout.get(n.nodeId)!.cx - fixedStageLayout.get(n.nodeId)!.w / 2"
                    :y="fixedStageLayout.get(n.nodeId)!.cy - fixedStageLayout.get(n.nodeId)!.h / 2"
                    :width="fixedStageLayout.get(n.nodeId)!.w"
                    :height="fixedStageLayout.get(n.nodeId)!.h"
                    :stroke="nodeColor(n)" stroke-width="2" fill="#fff" rx="3"
                  />
                  <text
                    :x="fixedStageLayout.get(n.nodeId)!.cx"
                    :y="fixedStageLayout.get(n.nodeId)!.cy + 4"
                    text-anchor="middle" font-size="11" fill="#222"
                  >{{ n.label }}</text>
                </template>
              </g>
            </template>
          </g>

          <!-- Legend -->
          <g v-if="nodes.length > 0" :transform="`translate(20, ${canvasHeight - 60})`">
            <rect width="200" height="50" fill="#f9f9f9" stroke="#ddd" rx="4" />
            <circle cx="20" cy="15" r="6" fill="#E74C3C" stroke="#E74C3C" />
            <text x="32" y="19" font-size="10" fill="#333">能源输入</text>
            <rect x="90" y="9" width="12" height="12" fill="#fff" stroke="#3498DB" rx="2" />
            <text x="108" y="19" font-size="10" fill="#333">用能单元</text>
            <line x1="8" y1="35" x2="28" y2="35" stroke="#000" stroke-width="2" />
            <text x="34" y="39" font-size="10" fill="#333">产品产出</text>
            <line x1="90" y1="35" x2="120" y2="35" stroke="#F39C12" stroke-width="2" />
            <text x="126" y="39" font-size="10" fill="#333">能源流向</text>
          </g>
        </svg>
      </div>

      <!-- Right Properties Panel -->
      <div class="sidebar-right">
        <template v-if="selectedNode">
          <h4>节点属性</h4>
          <div class="prop-row">
            <label>名称</label>
            <el-input :model-value="selectedNode.label" size="small" @update:model-value="v => updateNodeProp('label', v)" />
          </div>
          <div class="prop-row">
            <label>类型</label>
            <el-select :model-value="selectedNode.nodeType" size="small" @update:model-value="v => updateNodeProp('nodeType', v)">
              <el-option label="能源输入" value="energy_input" />
              <el-option label="用能单元" value="unit" />
              <el-option label="产品产出" value="product_output" />
              <el-option label="自定义" value="custom" />
            </el-select>
          </div>
          <div class="prop-row">
            <label>颜色</label>
            <el-color-picker :model-value="selectedNode.color || '#666'" size="small" @update:model-value="(v: string | null) => updateNodeProp('color', v)" />
          </div>
          <!-- Fixed-stage layout info and row reorder controls -->
          <div v-if="selectedNodeStageInfo" class="prop-row">
            <label>布局阶段</label>
            <el-tag size="small" type="info">{{ selectedNodeStageInfo.stageLabel }}（第{{ selectedNodeStageInfo.rowIdx + 1 }}行，共{{ selectedNodeStageInfo.stageSize }}行）</el-tag>
          </div>
          <div v-if="selectedNodeStageInfo" class="prop-row">
            <label>行顺序</label>
            <el-button size="small" :disabled="selectedNodeStageInfo.rowIdx <= 0" @click="moveNodeInStage(selectedNode.nodeId, 'up')">上移</el-button>
            <el-button size="small" :disabled="selectedNodeStageInfo.rowIdx >= selectedNodeStageInfo.stageSize - 1" @click="moveNodeInStage(selectedNode.nodeId, 'down')">下移</el-button>
          </div>
          <div class="prop-row">
            <el-checkbox :model-value="selectedNode.visible === 1" @update:model-value="v => updateNodeProp('visible', v ? 1 : 0)">可见</el-checkbox>
            <el-checkbox :model-value="selectedNode.locked === 1" @update:model-value="v => updateNodeProp('locked', v ? 1 : 0)">锁定</el-checkbox>
          </div>
          <el-button type="danger" size="small" @click="deleteSelectedNode" style="margin-top: 8px">删除节点</el-button>
        </template>

        <template v-else-if="selectedEdge">
          <h4>连线属性</h4>
          <div class="prop-row">
            <label>标签</label>
            <template v-if="selectedEdgeLabelDerived">
              <el-tag size="small" type="info">由填报记录自动生成</el-tag>
            </template>
            <template v-else>
              <el-input :model-value="selectedEdge.labelText || ''" size="small" @update:model-value="v => updateEdgeProp('labelText', v)" />
            </template>
          </div>
          <div class="prop-row">
            <label>颜色</label>
            <el-color-picker :model-value="selectedEdge.color || '#666'" size="small" @update:model-value="(v: string | null) => updateEdgeProp('color', v)" />
          </div>
          <div class="prop-row">
            <label>线宽</label>
            <el-input-number :model-value="selectedEdge.lineWidth || 2" size="small" :min="1" :max="10" @update:model-value="v => updateEdgeProp('lineWidth', v)" />
          </div>
          <div class="prop-row">
            <el-checkbox :model-value="(selectedEdge.visible ?? 1) === 1" @update:model-value="v => updateEdgeProp('visible', v ? 1 : 0)">可见</el-checkbox>
          </div>
          <!-- Route editing controls: constrained sliders for trunk/backflow/midpoint -->
          <div class="prop-row" style="margin-top: 8px">
            <label>路由编辑</label>
            <template v-if="selectedEdgeRouteInfo">
              <div class="route-edit-control">
                <span class="route-edit-label">{{ selectedEdgeRouteInfo.label }}</span>
                <div style="display: flex; align-items: center; gap: 6px; margin-top: 4px">
                  <el-slider
                    :model-value="selectedEdgeRouteInfo.value"
                    :min="selectedEdgeRouteInfo.min"
                    :max="selectedEdgeRouteInfo.max"
                    :step="1"
                    :show-tooltip="true"
                    style="flex: 1"
                    @update:model-value="(v: number) => setRouteHintValue(v)"
                  />
                  <el-input-number
                    :model-value="selectedEdgeRouteInfo.value"
                    :min="selectedEdgeRouteInfo.min"
                    :max="selectedEdgeRouteInfo.max"
                    :step="1"
                    size="small"
                    controls-position="right"
                    style="width: 90px"
                    @update:model-value="(v: number | undefined) => v != null && setRouteHintValue(v)"
                  />
                </div>
                <div style="font-size: 11px; color: #999; margin-top: 2px">
                  范围: {{ selectedEdgeRouteInfo.min }} – {{ selectedEdgeRouteInfo.max }}px
                  <template v-if="selectedEdgeRouteInfo.hasMultiTarget">
                    · 多目标分支共享干线
                  </template>
                </div>
              </div>
              <div style="display: flex; gap: 6px; margin-top: 6px">
                <el-button size="small" @click="addRoutePoint" v-if="!parsedRoutePoints.length">启用自定义路由</el-button>
                <el-button size="small" type="warning" @click="resetRouteToDefault" v-if="parsedRoutePoints.length > 0">恢复默认路由</el-button>
              </div>
              <div v-if="parsedRoutePoints.length > 0" style="margin-top: 4px">
                <details style="font-size: 11px; color: #888">
                  <summary>当前路由点 ({{ parsedRoutePoints.length }})</summary>
                  <div v-for="(pt, pi) in parsedRoutePoints" :key="pi" class="route-point-row">
                    <span class="route-point-idx">{{ pi + 1 }}.</span>
                    <span>({{ pt.x }}, {{ pt.y }})</span>
                  </div>
                </details>
              </div>
            </template>
            <template v-else>
              <span style="font-size: 12px; color: #999">该连线不支持路由编辑</span>
            </template>
          </div>
          <el-button type="danger" size="small" @click="deleteSelectedEdge" style="margin-top: 8px">删除连线</el-button>
        </template>

        <template v-else>
          <h4>画布属性</h4>
          <div class="prop-row">
            <label>宽度</label>
            <el-input-number v-model="canvasWidth" size="small" :min="600" :step="100" />
          </div>
          <div class="prop-row">
            <label>高度</label>
            <el-input-number v-model="canvasHeight" size="small" :min="400" :step="100" />
          </div>
          <p class="hint">点击节点或连线编辑属性</p>
        </template>
      </div>
    </div>

    <!-- Bottom Record Table -->
    <div class="record-table-area">
      <div class="record-header">
        <h4>填报记录</h4>
        <el-button type="primary" size="small" @click="addRecord">新增记录</el-button>
      </div>
      <el-table :data="flowRecords" size="small" max-height="220" border stripe>
        <el-table-column type="index" width="50" label="#" />
        <el-table-column prop="sourceUnit" label="来源" width="100" />
        <el-table-column prop="targetUnit" label="目的" width="100" />
        <el-table-column prop="energyProduct" label="能源/产品" width="100" />
        <el-table-column prop="itemType" label="类型" width="70">
          <template #default="{ row }">
            {{ row.itemType === 'energy' ? '能源' : row.itemType === 'product' ? '产品' : row.itemType || '待确认' }}
          </template>
        </el-table-column>
        <el-table-column prop="physicalQuantity" label="实物量" width="100" align="right">
          <template #default="{ row }">{{ formatNum(row.physicalQuantity) }}</template>
        </el-table-column>
        <el-table-column prop="calculatedValue" label="折标量/价格" width="110" align="right">
          <template #default="{ row }">{{ formatNum(row.calculatedValue) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row, $index }">
            <el-button link type="primary" size="small" @click="editRecord(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="deleteRecord($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Record Edit Dialog -->
    <el-dialog v-model="showRecordDialog" title="编辑填报记录" width="600px" :close-on-click-modal="false" @close="handleRecordDialogClose">
      <el-form v-if="editingRecord" ref="recordFormRef" :model="editingRecord" :rules="recordFormRules" label-width="100px" size="small">
        <el-form-item label="来源类型" prop="sourceType">
          <el-select v-model="editingRecord.sourceType" style="width: 100%">
            <el-option label="外购能源" value="external_energy" />
            <el-option label="用能单元" value="unit" />
            <el-option label="系统" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingRecord.sourceType === 'unit'" label="来源单元" prop="sourceRefId">
          <el-select v-model="editingRecord.sourceRefId" style="width: 100%" filterable>
            <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingRecord.sourceType === 'system'" label="系统名称" prop="sourceUnit">
          <el-input v-model="editingRecord.sourceUnit" placeholder="请输入系统名称" />
        </el-form-item>
        <el-form-item label="目的类型" prop="targetType">
          <el-select v-model="editingRecord.targetType" style="width: 100%">
            <el-option label="用能单元" value="unit" />
            <el-option label="生产系统" value="production_system" />
            <el-option label="产出节点" value="product_output" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingRecord.targetType === 'unit'" label="目的单元" prop="targetRefId">
          <el-select v-model="editingRecord.targetRefId" style="width: 100%" filterable>
            <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingRecord.targetType === 'production_system'" label="生产系统名称" prop="targetUnit">
          <el-input v-model="editingRecord.targetUnit" placeholder="请输入生产系统名称" />
        </el-form-item>
        <el-form-item label="品目类型" prop="itemType">
          <el-select v-model="editingRecord.itemType" style="width: 100%">
            <el-option label="能源" value="energy" />
            <el-option label="产品" value="product" />
          </el-select>
        </el-form-item>
        <el-form-item label="品目" prop="itemId">
          <el-select v-model="editingRecord.itemId" style="width: 100%" filterable>
            <template v-if="editingRecord.itemType === 'energy'">
              <el-option v-for="e in energies" :key="e.id" :label="e.name" :value="e.id" />
            </template>
            <template v-else>
              <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="实物量" prop="physicalQuantity">
          <el-input-number v-model="editingRecord.physicalQuantity" style="width: 100%" :precision="4" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editingRecord.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleRecordDialogCancel">取消</el-button>
        <el-button type="primary" @click="saveRecord">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.energy-flow-config-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-bottom: 1px solid #e6e6e6;
  background: #fafafa;
  flex-shrink: 0;

  .toolbar-left {
    display: flex;
    align-items: center;
    h3 { margin: 0; font-size: 16px; color: #303133; }
  }
  .toolbar-actions {
    display: flex;
    gap: 6px;
  }
}

.main-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.sidebar-left {
  width: 200px;
  border-right: 1px solid #e6e6e6;
  overflow-y: auto;
  padding: 8px;
  flex-shrink: 0;
  background: #fafbfc;

  .sidebar-section {
    margin-bottom: 12px;
    h4 { margin: 0 0 6px; font-size: 13px; color: #606266; }
  }

  .validation-badges {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    margin-bottom: 8px;
  }

  .sidebar-item {
    padding: 4px 8px;
    font-size: 12px;
    border-radius: 3px;
    margin-bottom: 2px;
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: grab;

    &:hover { background: #ecf5ff; }

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .sub {
      color: #999;
      font-size: 11px;
      margin-left: auto;
    }
  }
}

.canvas-area {
  flex: 1;
  overflow: auto;
  min-width: 0;
  background: #f5f5f5;

  .flow-canvas {
    display: block;
    background: #fff;
    box-shadow: 0 1px 4px rgba(0,0,0,0.1);
  }
}

.sidebar-right {
  width: 220px;
  border-left: 1px solid #e6e6e6;
  padding: 12px;
  overflow-y: auto;
  flex-shrink: 0;
  background: #fafbfc;

  h4 { margin: 0 0 10px; font-size: 13px; color: #606266; }

  .prop-row {
    margin-bottom: 8px;
    label {
      display: block;
      font-size: 12px;
      color: #909399;
      margin-bottom: 2px;
    }
  }
  .hint {
    color: #c0c4cc;
    font-size: 12px;
    text-align: center;
    margin-top: 20px;
  }
}

.record-table-area {
  border-top: 1px solid #e6e6e6;
  padding: 8px 16px;
  flex-shrink: 0;
  max-height: 260px;
  overflow-y: auto;

  .record-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
    h4 { margin: 0; font-size: 13px; }
  }
}

.node-selected circle,
.node-selected rect {
  stroke-dasharray: 4 2;
  stroke-width: 3;
}

.edge-selected {
  stroke-dasharray: 6 3;
  stroke-width: 3 !important;
}

.route-point-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
  font-size: 12px;

  .route-point-idx {
    color: #909399;
    width: 16px;
    flex-shrink: 0;
  }
}

.route-edit-control {
  margin-top: 4px;
}

.route-edit-label {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}
</style>
