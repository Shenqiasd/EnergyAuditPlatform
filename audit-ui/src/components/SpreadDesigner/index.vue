<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'

/**
 * SpreadDesigner — wraps GrapeCity SpreadJS Designer (loaded via CDN as window.GC).
 *
 * Props:
 *   templateJson — SpreadJS workbook JSON string to load on mount.
 *   readonly     — when true:
 *                  • all sheets are protected (no cell editing)
 *                  • the Designer ribbon is hidden and replaced by the view-only ribbon
 *                  • command handling is overridden to block mutating commands
 *
 * Exposed methods:
 *   getJson(): string — serialises the current workbook state to JSON.
 */

const props = defineProps<{
  templateJson?: string
  readonly?: boolean
}>()

const containerRef = ref<HTMLDivElement>()
const gcError = ref(false)

let designer: any = null
let workbook: any = null

onMounted(() => {
  const gc = (window as any).GC
  if (!gc?.Spread?.Sheets?.Designer?.Designer) {
    gcError.value = true
    console.error('SpreadDesigner: GC.Spread.Sheets.Designer not found — check CDN scripts in index.html')
    return
  }
  initDesigner(gc)
})

onBeforeUnmount(() => {
  try {
    designer?.destroy?.()
  } catch (_) {}
  designer = null
  workbook = null
})

function initDesigner(gc: any) {
  if (!containerRef.value) return

  const GCDesigner = gc.Spread.Sheets.Designer

  // Choose config based on readonly mode
  const config: any = props.readonly
    ? buildReadonlyConfig(gc)
    : GCDesigner.DefaultConfig

  designer = new GCDesigner.Designer(containerRef.value, config, null)
  workbook = designer.getWorkbook()

  // Load JSON
  if (props.templateJson && props.templateJson !== '{}') {
    try {
      workbook.fromJSON(JSON.parse(props.templateJson))
    } catch (e) {
      console.warn('SpreadDesigner: failed to parse templateJson —', e)
    }
  }

  if (props.readonly) {
    applySheetProtection()
    blockMutatingCommands(gc)
  }
}

/**
 * Build a stripped-down designer config for readonly mode:
 * remove all ribbon tabs that contain editing commands, keep only the Home view tab.
 */
function buildReadonlyConfig(gc: any): any {
  const GCDesigner = gc.Spread.Sheets.Designer
  // Deep-clone default config to avoid mutating the shared constant
  const base = JSON.parse(JSON.stringify(GCDesigner.DefaultConfig ?? {}))

  // Keep only view-safe ribbon tabs (remove Insert, Formulas, Data, etc.)
  const viewOnlyTabs = ['home', 'view']
  if (Array.isArray(base?.ribbon)) {
    base.ribbon = base.ribbon.filter((tab: any) =>
      viewOnlyTabs.includes((tab.id ?? '').toLowerCase())
    )
  }

  // Disable the file menu entries that would mutate data
  if (Array.isArray(base?.fileMenu?.menuItems)) {
    const allowedFileItems = ['open', 'close']
    base.fileMenu.menuItems = base.fileMenu.menuItems.filter((item: any) =>
      allowedFileItems.includes((item.commandName ?? '').toLowerCase())
    )
  }

  return base
}

/**
 * Protect every sheet so cells cannot be edited.
 */
function applySheetProtection() {
  if (!workbook) return
  const count = workbook.getSheetCount()
  for (let i = 0; i < count; i++) {
    const sheet = workbook.getSheet(i)
    sheet.options.isProtected = true
    // Also lock all cells in the used range
    sheet.protect({ allowSelectLockedCells: true, allowSelectUnlockedCells: false })
  }
}

/**
 * Override the Designer's command infrastructure to swallow any mutating commands,
 * giving a second layer of defence beyond sheet protection.
 */
function blockMutatingCommands(gc: any) {
  if (!designer || !workbook) return
  try {
    const commandManager = workbook.commandManager()
    if (!commandManager) return

    // List of mutating command names to block in readonly mode
    const BLOCKED = new Set([
      'clear', 'clearContents', 'clearFormat', 'clearAll',
      'delete', 'insertRows', 'insertColumns', 'deleteRows', 'deleteColumns',
      'insertSheet', 'deleteSheet',
      'editCell', 'commitEdit',
      'paste', 'cut', 'redo', 'undo',
      'sort', 'filter',
    ])

    BLOCKED.forEach((name) => {
      try {
        commandManager.register(name, {
          execute: () => false,
          canUndo: false,
        })
      } catch (_) {}
    })
  } catch (e) {
    console.warn('SpreadDesigner: could not override command manager —', e)
  }
}

function getJson(): string {
  if (!workbook) return '{}'
  try {
    return JSON.stringify(workbook.toJSON())
  } catch (e) {
    console.error('SpreadDesigner.getJson failed', e)
    return '{}'
  }
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
