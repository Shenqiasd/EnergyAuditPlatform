<script setup lang="ts">
import { computed } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getFieldOptions } from '@/config/schema-registry'

interface ColumnMapping {
  col: number
  field: string
  type: string
}

const props = defineProps<{
  modelValue: string
  targetTable: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const COL_TYPE_OPTIONS = [
  { label: 'STRING', value: 'STRING' },
  { label: 'NUMBER', value: 'NUMBER' },
  { label: 'DATE', value: 'DATE' },
  { label: 'DICT', value: 'DICT' },
]

const mappings = computed<ColumnMapping[]>({
  get() {
    if (!props.modelValue) return []
    try {
      const parsed = JSON.parse(props.modelValue)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  },
  set(val: ColumnMapping[]) {
    emit('update:modelValue', JSON.stringify(val))
  },
})

const fieldOptions = computed(() => getFieldOptions(props.targetTable))

function updateCol(index: number, value: number) {
  const copy = mappings.value.map((m) => ({ ...m }))
  copy[index].col = value
  mappings.value = copy
}

function updateField(index: number, value: string) {
  const copy = mappings.value.map((m) => ({ ...m }))
  copy[index].field = value
  mappings.value = copy
}

function updateType(index: number, value: string) {
  const copy = mappings.value.map((m) => ({ ...m }))
  copy[index].type = value
  mappings.value = copy
}

function addRow() {
  const next = mappings.value.length > 0
    ? Math.max(...mappings.value.map((m) => m.col)) + 1
    : 0
  mappings.value = [...mappings.value, { col: next, field: '', type: 'STRING' }]
}

function removeRow(index: number) {
  mappings.value = mappings.value.filter((_, i) => i !== index)
}
</script>

<template>
  <div class="col-mapping-editor">
    <div v-if="mappings.length > 0" class="mapping-header">
      <span class="hdr-col">列号</span>
      <span class="hdr-field">字段</span>
      <span class="hdr-type">类型</span>
      <span class="hdr-action"></span>
    </div>
    <div
      v-for="(row, idx) in mappings"
      :key="idx"
      class="mapping-row"
    >
      <el-input-number
        :model-value="row.col"
        :min="0"
        size="small"
        controls-position="right"
        class="col-num"
        :disabled="disabled"
        @update:model-value="(v: number | undefined) => updateCol(idx, v ?? 0)"
      />
      <el-select
        :model-value="row.field"
        size="small"
        filterable
        clearable
        placeholder="选择字段"
        class="col-field"
        :disabled="disabled"
        @update:model-value="(v: string) => updateField(idx, v)"
      >
        <el-option
          v-for="opt in fieldOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select
        :model-value="row.type"
        size="small"
        filterable
        class="col-type"
        :disabled="disabled"
        @update:model-value="(v: string) => updateType(idx, v)"
      >
        <el-option
          v-for="opt in COL_TYPE_OPTIONS"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-button
        :icon="Delete"
        size="small"
        type="danger"
        text
        class="col-del"
        :disabled="disabled"
        @click="removeRow(idx)"
      />
    </div>
    <el-button
      :icon="Plus"
      size="small"
      type="primary"
      text
      :disabled="disabled"
      @click="addRow"
      style="margin-top:4px;width:100%"
    >添加列映射</el-button>
  </div>
</template>

<style scoped>
.col-mapping-editor {
  width: 100%;
}
.mapping-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 0 4px;
  font-size: 11px;
  color: #909399;
  font-weight: 600;
}
.hdr-col { width: 64px; }
.hdr-field { flex: 1; }
.hdr-type { width: 80px; }
.hdr-action { width: 28px; }
.mapping-row {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}
.col-num { width: 64px; }
.col-field { flex: 1; }
.col-type { width: 80px; }
.col-del { width: 28px; flex-shrink: 0; }
</style>
