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
const EDGE_COLORS = ['#E74C3C', '#3498DB', '#27AE60', '#F39C12', '#9B59B6', '#1ABC9C', '#E67E22']

function nodeColor(n: FlowNodeConfig): string {
  return n.color || NODE_COLORS[n.nodeType] || '#666'
}

function edgeColor(e: FlowEdgeConfig, idx: number): string {
  return e.color || EDGE_COLORS[idx % EDGE_COLORS.length]
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

  dragging.value = true
  dragNodeId.value = nodeId
  dragOffset.x = event.clientX - node.positionX
  dragOffset.y = event.clientY - node.positionY
}

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
  const rect = (event.target as Element)?.closest('svg')?.getBoundingClientRect()
  if (!rect) return

  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

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
    positionX: x,
    positionY: y,
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
  const src = nodes.value.find(n => n.nodeId === selectedEdge.value!.sourceNodeId)
  const dst = nodes.value.find(n => n.nodeId === selectedEdge.value!.targetNodeId)
  const mx = ((src?.positionX ?? 0) + (dst?.positionX ?? 400)) / 2
  const my = ((src?.positionY ?? 0) + (dst?.positionY ?? 200)) / 2
  const pts = [...parsedRoutePoints.value, { x: Math.round(mx), y: Math.round(my) }]
  setRoutePointsJson(pts)
}

function updateRoutePoint(index: number, x: number, y: number) {
  const pts = [...parsedRoutePoints.value]
  pts[index] = { x, y }
  setRoutePointsJson(pts)
}

function removeRoutePoint(index: number) {
  const pts = parsedRoutePoints.value.filter((_: { x: number; y: number }, i: number) => i !== index)
  setRoutePointsJson(pts)
}

// ============================================================
// SVG edge path generation
// ============================================================
function edgePath(edge: FlowEdgeConfig): string {
  const src = nodes.value.find(n => n.nodeId === edge.sourceNodeId)
  const dst = nodes.value.find(n => n.nodeId === edge.targetNodeId)
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
  const src = nodes.value.find(n => n.nodeId === edge.sourceNodeId)
  const dst = nodes.value.find(n => n.nodeId === edge.targetNodeId)
  if (!src || !dst) return { x: 0, y: 0 }

  const srcW = src.width || 100
  const srcH = src.height || 50
  const dstH = dst.height || 50

  return {
    x: (src.positionX + srcW + dst.positionX) / 2,
    y: (src.positionY + srcH / 2 + dst.positionY + dstH / 2) / 2 - 8,
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
      } else if (energy.equivalentValue == null) {
        errors.push(`能源 [${energy.name}] 缺少折标系数`)
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
  // Terminal-use semantics: product-output records must originate from terminal-use sources
  // Validates ALL source types (unit, system, custom), not just unit
  for (const r of flowRecords.value) {
    if (r.targetType !== 'product_output') continue
    if (r.sourceType === 'unit' && r.sourceRefId) {
      const srcUnit = units.value.find(u => u.id === r.sourceRefId)
      if (srcUnit && srcUnit.unitType !== 3) {
        errors.push(`产品输出记录的来源单元 [${srcUnit.name}] 不是终端使用环节(unitType=${srcUnit.unitType})，产品输出必须从终端使用环节产出`)
      }
    } else if (r.sourceType === 'system' && !r.sourceUnit) {
      errors.push('产品输出记录的来源系统名称不能为空')
    } else if (r.sourceType === 'custom' && !r.sourceUnit) {
      errors.push('产品输出记录的自定义来源名称不能为空')
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
        <svg
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

          <!-- Edges -->
          <g class="edges-layer">
            <template v-for="(e, ei) in edges" :key="e.edgeId">
              <path
                v-if="(e.visible ?? 1) !== 0"
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
                v-if="(e.visible ?? 1) !== 0 && e.labelText"
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
            <template v-for="n in nodes" :key="n.nodeId">
              <g
                v-if="(n.visible ?? 1) !== 0"
                :class="{ 'node-selected': selectedNodeId === n.nodeId, 'node-locked': n.locked }"
                style="cursor: move"
                @mousedown.stop="handleNodeMouseDown(n.nodeId, $event)"
              >
                <!-- Energy input: circle -->
                <template v-if="n.nodeType === 'energy_input'">
                  <circle
                    :cx="n.positionX + (n.width || 60) / 2"
                    :cy="n.positionY + (n.height || 60) / 2"
                    :r="(n.width || 60) / 2"
                    :stroke="nodeColor(n)"
                    stroke-width="2"
                    fill="#fff"
                  />
                  <text
                    :x="n.positionX + (n.width || 60) / 2"
                    :y="n.positionY + (n.height || 60) / 2 + 4"
                    text-anchor="middle" font-size="11" :fill="nodeColor(n)"
                  >{{ n.label }}</text>
                </template>

                <!-- Product output: rounded rect -->
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

                <!-- Unit / custom: rect -->
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
          <g v-if="nodes.length > 0" :transform="`translate(20, ${canvasHeight - 60})`">
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
          <div class="prop-row">
            <label>宽度</label>
            <el-input-number :model-value="selectedNode.width || 100" size="small" :min="30" @update:model-value="v => updateNodeProp('width', v)" />
          </div>
          <div class="prop-row">
            <label>高度</label>
            <el-input-number :model-value="selectedNode.height || 50" size="small" :min="20" @update:model-value="v => updateNodeProp('height', v)" />
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
            <el-input :model-value="selectedEdge.labelText || ''" size="small" @update:model-value="v => updateEdgeProp('labelText', v)" />
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
          <!-- Fix #4: Route point editing UI -->
          <div class="prop-row" style="margin-top: 8px">
            <label>折点 (Route Points)</label>
            <div v-for="(pt, pi) in parsedRoutePoints" :key="pi" class="route-point-row">
              <span class="route-point-idx">{{ pi + 1 }}.</span>
              <el-input-number :model-value="pt.x" size="small" :controls="false" style="width: 70px" @update:model-value="v => updateRoutePoint(pi, v ?? 0, pt.y)" />
              <span>,</span>
              <el-input-number :model-value="pt.y" size="small" :controls="false" style="width: 70px" @update:model-value="v => updateRoutePoint(pi, pt.x, v ?? 0)" />
              <el-button link type="danger" size="small" @click="removeRoutePoint(pi)">×</el-button>
            </div>
            <el-button size="small" @click="addRoutePoint" style="margin-top: 4px">+ 添加折点</el-button>
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
</style>
