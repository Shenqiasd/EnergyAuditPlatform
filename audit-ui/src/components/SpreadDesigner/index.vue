<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import type { GCSpreadDesigner, GCSpreadWorkbook } from '@/types/spreadjs'

/**
 * SpreadDesigner — wraps GrapeCity SpreadJS Designer (loaded via CDN as window.GC).
 *
 * Props:
 *   templateJson — SpreadJS workbook JSON string to load on mount.
 *   readonly     — when true, all sheets are protected and editing options disabled.
 *
 * Exposed:
 *   getJson(): string — serialises current workbook state to JSON.
 */

const props = defineProps<{
  templateJson?: string
  readonly?: boolean
}>()

const containerRef = ref<HTMLDivElement>()
const gcError = ref(false)

let designer: GCSpreadDesigner | null = null
let workbook: GCSpreadWorkbook | null = null

onMounted(() => {
  if (!window.GC?.Spread?.Sheets?.Designer?.Designer) {
    gcError.value = true
    console.error('SpreadDesigner: GC.Spread.Sheets.Designer not available — check CDN scripts in index.html')
    return
  }
  initDesigner()
})

onBeforeUnmount(() => {
  designer?.destroy()
  designer = null
  workbook = null
})

function initDesigner() {
  if (!containerRef.value) return

  const GCDesigner = window.GC.Spread.Sheets.Designer
  designer = new GCDesigner.Designer(containerRef.value, GCDesigner.DefaultConfig, null)
  workbook = designer.getWorkbook()

  if (props.templateJson && props.templateJson !== '{}') {
    try {
      workbook.fromJSON(JSON.parse(props.templateJson))
    } catch (e) {
      console.error('SpreadDesigner: failed to load templateJson — workbook starts empty', e)
    }
  }

  if (props.readonly) {
    applyReadonly()
  }
}

const MUTATING_COMMANDS = [
  'clear', 'clearContents', 'clearFormat', 'clearAll',
  'delete', 'insertRows', 'insertColumns', 'deleteRows', 'deleteColumns',
  'insertSheet', 'deleteSheet', 'editCell', 'commitEdit',
  'paste', 'cut', 'redo', 'undo', 'sort', 'filter',
]

function applyReadonly() {
  if (!workbook) return

  // Disable formula editing at workbook level
  workbook.options.allowUserEditFormula = false

  // Protect every sheet
  const count = workbook.getSheetCount()
  for (let i = 0; i < count; i++) {
    workbook.getSheet(i).options.isProtected = true
  }

  // Override mutating commands so keyboard shortcuts are also blocked
  const cmdManager = workbook.commandManager()
  if (cmdManager) {
    const noOp = { execute: () => false, canUndo: false }
    MUTATING_COMMANDS.forEach((name) => cmdManager.register(name, noOp))
  }
}

function getJson(): string {
  if (!workbook) return '{}'
  return JSON.stringify(workbook.toJSON())
}

defineExpose({ getJson })
</script>

<template>
  <div class="spread-designer-wrapper">
    <el-alert
      v-if="gcError"
      type="error"
      title="SpreadJS 设计器加载失败"
      description="无法连接 CDN 加载 SpreadJS，请检查网络连接或刷新页面重试。"
      :closable="false"
      style="margin-bottom: 8px"
    />
    <div ref="containerRef" class="spread-designer-host" />
  </div>
</template>

<style scoped>
.spread-designer-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.spread-designer-host {
  flex: 1;
  min-height: 0;
  width: 100%;
}
</style>
