<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkPrerequisites } from '@/api/enterpriseSetting'
import { queryExtractedTable } from '@/api/extracted-data'
import { appendTableSheet, writeWorkbook, XLSX } from '@/utils/export'
import { standardChartDefinitions } from './definitions'

const route = useRoute()
const router = useRouter()

const prerequisitePassed = ref(false)
const checking = ref(true)
const exportingAll = ref(false)

async function runPrerequisiteCheck() {
  checking.value = true
  try {
    const result = await checkPrerequisites()
    if (!result.passed) {
      const lines = result.errors.map(e => `• ${e}`).join('\n')
      await ElMessageBox.alert(
        `请先完成以下前置配置：\n\n${lines}\n\n请前往「基础设置」完成配置后再导出规定图表。`,
        '前置校验未通过',
        {
          type: 'warning',
          confirmButtonText: '前往设置',
          dangerouslyUseHTMLString: false,
        }
      )
      router.push('/enterprise/settings/company')
      return
    }
    prerequisitePassed.value = true
  } catch (e: any) {
    // ElMessageBox.alert rejects if user presses Escape / close button — treat as redirect
    if (e === 'close' || e === 'cancel') {
      router.push('/enterprise/settings/company')
      return
    }
    ElMessage.error('前置校验失败：' + (e?.message ?? '未知错误'))
    router.push('/enterprise/settings/company')
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  runPrerequisiteCheck()
})

const navItems = [
  ...standardChartDefinitions.map((item) => ({ label: item.label, name: item.routeName })),
]

const currentName = computed(() => route.name as string)

function defaultColumns(rows: Record<string, unknown>[]) {
  const sample = rows[0] || {}
  return Object.keys(sample)
    .filter((key) => !['id', 'submission_id', 'enterprise_id', 'audit_year', 'deleted', 'create_by', 'create_time', 'update_by', 'update_time'].includes(key))
    .map((key) => ({ prop: key, label: key }))
}

async function exportAllCharts() {
  exportingAll.value = true
  try {
    const workbook = XLSX.utils.book_new()
    for (const item of standardChartDefinitions) {
      if (!item.tableName) {
        appendTableSheet(workbook, item.exportSheetName, [{ prop: 'note', label: item.exportSheetName }], [])
        continue
      }
      const data = await queryExtractedTable(item.tableName, { pageSize: 100 }).catch(() => ({ rows: [], total: 0 }))
      const rows = ((data.rows || []) as Record<string, unknown>[]).slice().reverse()
      appendTableSheet(workbook, item.exportSheetName, item.columns || defaultColumns(rows), rows)
    }
    writeWorkbook(workbook, '0515规定图表19表')
  } finally {
    exportingAll.value = false
  }
}
</script>

<template>
  <div v-if="checking" class="loading-area">
    <el-empty description="正在校验前置条件…" />
  </div>
  <div v-else-if="prerequisitePassed" class="standard-layout">
    <aside class="standard-sidebar">
      <div class="sidebar-title">
        <span>规定图表</span>
        <el-button type="primary" size="small" :loading="exportingAll" @click="exportAllCharts">
          导出19表
        </el-button>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="nav-item"
          :class="{ active: currentName === item.name }"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </aside>
    <main class="standard-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped lang="scss">
.standard-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.standard-sidebar {
  width: 280px;
  min-width: 280px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.nav-item {
  padding: 10px 16px;
  font-size: 13px;
  color: #606266;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;

  &:hover {
    background: #f0f7ff;
    color: #1890ff;
  }

  &.active,
  &.router-link-active {
    background: #e6f4ff;
    color: #1890ff;
    font-weight: 600;
    border-right: 3px solid #1890ff;
  }
}

.standard-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}

.loading-area {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
