<script setup lang="ts">
export interface SheetFillStatus {
  sheetIndex: number
  sheetName: string
  totalRequired: number
  filledRequired: number
  status: 'completed' | 'in_progress' | 'not_started' | 'no_required'
}

defineProps<{
  sheets: SheetFillStatus[]
  activeIndex: number
  collapsed: boolean
}>()

const emit = defineEmits<{
  select: [index: number]
  'update:collapsed': [value: boolean]
}>()

function statusIcon(status: SheetFillStatus['status']): string {
  switch (status) {
    case 'completed': return '✓'
    case 'in_progress': return '●'
    case 'not_started': return '○'
    case 'no_required': return '—'
  }
}

function statusClass(status: SheetFillStatus['status']): string {
  switch (status) {
    case 'completed': return 'status-completed'
    case 'in_progress': return 'status-progress'
    case 'not_started': return 'status-empty'
    case 'no_required': return 'status-none'
  }
}

function progressText(sheet: SheetFillStatus): string {
  if (sheet.totalRequired === 0) return '无必填'
  return `${sheet.filledRequired}/${sheet.totalRequired}`
}
</script>

<template>
  <div :class="['sheet-nav', { 'sheet-nav--collapsed': collapsed }]">
    <div class="sheet-nav__header">
      <span v-if="!collapsed" class="sheet-nav__title">工作表导航</span>
      <button
        class="sheet-nav__toggle"
        :title="collapsed ? '展开导航' : '收起导航'"
        @click="emit('update:collapsed', !collapsed)"
      >
        {{ collapsed ? '»' : '«' }}
      </button>
    </div>

    <div class="sheet-nav__list">
      <div
        v-for="sheet in sheets"
        :key="sheet.sheetIndex"
        :class="[
          'sheet-nav__item',
          { 'sheet-nav__item--active': sheet.sheetIndex === activeIndex },
          statusClass(sheet.status),
        ]"
        :title="collapsed ? `${sheet.sheetName} (${progressText(sheet)})` : undefined"
        @click="emit('select', sheet.sheetIndex)"
      >
        <span :class="['sheet-nav__icon', statusClass(sheet.status)]">
          {{ statusIcon(sheet.status) }}
        </span>
        <template v-if="!collapsed">
          <div class="sheet-nav__info">
            <span class="sheet-nav__name">{{ sheet.sheetName }}</span>
            <span class="sheet-nav__progress">{{ progressText(sheet) }}</span>
          </div>
        </template>
      </div>
    </div>

    <div v-if="!collapsed" class="sheet-nav__summary">
      <span class="sheet-nav__summary-text">
        共 {{ sheets.length }} 个工作表
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.sheet-nav {
  width: 200px;
  min-width: 200px;
  background: #fafbfc;
  border-right: 1px solid #e4e7ed;
  border-radius: 4px 0 0 4px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.2s ease, min-width 0.2s ease;

  &--collapsed {
    width: 44px;
    min-width: 44px;
  }
}

.sheet-nav__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #e4e7ed;
  background: #f0f2f5;
  min-height: 20px;
}

.sheet-nav__title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.sheet-nav__toggle {
  background: none;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
  color: #909399;
  padding: 2px 6px;
  line-height: 1;
  transition: color 0.2s, border-color 0.2s;

  &:hover {
    color: #409eff;
    border-color: #409eff;
  }
}

.sheet-nav__list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.sheet-nav__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.15s;
  border-left: 3px solid transparent;

  &:hover {
    background: #ecf5ff;
  }

  &--active {
    background: #e6f0ff;
    border-left-color: #409eff;
  }
}

.sheet-nav__icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;

  &.status-completed {
    background: #e8f5e9;
    color: #4caf50;
  }

  &.status-progress {
    background: #fff3e0;
    color: #ff9800;
  }

  &.status-empty {
    background: #f5f5f5;
    color: #bdbdbd;
  }

  &.status-none {
    background: #f5f5f5;
    color: #9e9e9e;
  }
}

.sheet-nav__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sheet-nav__name {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sheet-nav__progress {
  font-size: 11px;
  color: #909399;
}

.sheet-nav__summary {
  padding: 8px 12px;
  border-top: 1px solid #e4e7ed;
  background: #f0f2f5;
}

.sheet-nav__summary-text {
  font-size: 11px;
  color: #909399;
}
</style>
