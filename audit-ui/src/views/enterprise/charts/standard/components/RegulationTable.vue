<script setup lang="ts">
import { exportTableToExcel, flattenColumns, type ExportColumn } from '@/utils/export'

export interface RegColumn {
  prop: string
  label: string
  width?: number
  minWidth?: number
  children?: RegColumn[]
}

const props = withDefaults(
  defineProps<{
    columns: RegColumn[]
    data: Record<string, unknown>[]
    loading?: boolean
    exportFilename?: string
    title?: string
    /**
     * Opt in to multi-row grouped Excel headers (parent + child bands) for
     * pages whose template intentionally exports group labels like
     * “2025年实际 / 2030年目标”. Default `false` keeps the pre-existing
     * flat-header export behavior for shared regulated pages (表 7 / MeterRate
     * etc.) that already use `children` only for on-screen grouping.
     */
    groupedExport?: boolean
  }>(),
  { groupedExport: false },
)

function handleExport() {
  const cols: ExportColumn[] = props.groupedExport
    ? (props.columns as ExportColumn[])
    : flattenColumns(props.columns as ExportColumn[])
  exportTableToExcel(cols, props.data, props.exportFilename || props.title || '导出数据')
}
</script>

<template>
  <div class="regulation-table">
    <div class="table-header">
      <span class="table-title">{{ title }}</span>
      <el-button type="primary" size="small" @click="handleExport">
        导出 Excel
      </el-button>
    </div>
    <el-table
      v-loading="loading || false"
      :data="data"
      border
      stripe
      style="width: 100%"
      empty-text="暂无数据"
      header-cell-class-name="reg-header-cell"
    >
      <template v-for="col in columns" :key="col.prop || col.label">
        <el-table-column
          v-if="col.children?.length"
          :label="col.label"
          header-align="center"
          align="center"
          class-name="group-header"
        >
          <el-table-column
            v-for="child in col.children"
            :key="child.prop"
            :prop="child.prop"
            :label="child.label"
            :width="child.width"
            :min-width="child.minWidth || 100"
            align="center"
            show-overflow-tooltip
          />
        </el-table-column>
        <el-table-column
          v-else
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth || 100"
          align="center"
          show-overflow-tooltip
        />
      </template>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.regulation-table {
  margin-bottom: 20px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
</style>

<style lang="scss">
.reg-header-cell {
  background-color: #1890ff !important;
  color: #fff !important;
  font-weight: 600 !important;
}

.group-header > .cell {
  background-color: #0050b3 !important;
  color: #fff !important;
}
</style>
