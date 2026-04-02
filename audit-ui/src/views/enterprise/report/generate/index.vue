<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { listSubmissions, getPublishedVersion, submitSubmission, type TplSubmission } from '@/api/template'

const router = useRouter()
const loading = ref(false)
const submissions = ref<TplSubmission[]>([])
const submittingId = ref<number | null>(null)

const STATUS_MAP: Record<number, { label: string; type: 'warning' | 'success' }> = {
  0: { label: '草稿', type: 'warning' },
  1: { label: '已提交', type: 'success' },
}

async function loadData() {
  loading.value = true
  try {
    submissions.value = await listSubmissions()
  } finally {
    loading.value = false
  }
}

async function handleSubmit(row: TplSubmission) {
  await ElMessageBox.confirm(
    `确认提交 ${row.auditYear} 年度数据？提交后将抽取数据并锁定编辑。`,
    '提交确认',
    { type: 'warning' }
  )
  submittingId.value = row.id!
  try {
    const publishedVer = await getPublishedVersion(row.templateId!)
    if (!publishedVer || !publishedVer.id) {
      ElMessage.error('模板尚未发布有效版本，无法提交')
      return
    }
    await submitSubmission(row.id!, publishedVer.id)
    ElMessage.success('提交成功，数据已抽取')
    loadData()
  } finally {
    submittingId.value = null
  }
}

function viewDetail(row: TplSubmission) {
  router.push({ path: '/enterprise/report/detail', query: { id: row.id } })
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">填报记录 — 在线生成报告</span>
          <el-button @click="loadData" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="submissions" border stripe>
        <el-table-column prop="templateId" label="模板ID" width="90" align="center" />
        <el-table-column prop="auditYear" label="审计年度" width="100" align="center">
          <template #default="{ row }">{{ row.auditYear }} 年</template>
        </el-table-column>
        <el-table-column prop="templateVersion" label="模板版本" width="100" align="center">
          <template #default="{ row }">v{{ row.templateVersion }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status ?? 0]?.type" size="small">
              {{ STATUS_MAP[row.status ?? 0]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              link type="success"
              :loading="submittingId === row.id"
              :disabled="row.status === 1"
              @click="handleSubmit(row)"
            >
              生成报告
            </el-button>
            <el-button link type="primary" @click="viewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && submissions.length === 0" description="暂无填报记录，请先在「报告录入」中保存草稿" />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
</style>
