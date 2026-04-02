<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  getTemplateList,
  listSubmissions,
  getPublishedVersion,
  submitSubmission,
  type TplTemplate,
  type TplSubmission,
} from '@/api/template'

const router = useRouter()
const loading = ref(false)
const submissions = ref<TplSubmission[]>([])
const templates = ref<TplTemplate[]>([])
const submittingId = ref<number | null>(null)

const templateMap = computed<Map<number, TplTemplate>>(() => {
  const m = new Map<number, TplTemplate>()
  templates.value.forEach(t => { if (t.id) m.set(t.id, t) })
  return m
})

const STATUS_MAP: Record<number, { label: string; type: 'info' | 'warning' | 'success' }> = {
  0: { label: '草稿', type: 'warning' },
  1: { label: '已提交', type: 'success' },
}

async function loadData() {
  loading.value = true
  try {
    const [subs, tpls] = await Promise.all([
      listSubmissions(),
      getTemplateList({ status: 1, pageSize: 200 }).then(r => r.rows ?? []),
    ])
    submissions.value = subs
    templates.value = tpls
  } finally {
    loading.value = false
  }
}

async function handleSubmit(row: TplSubmission) {
  await ElMessageBox.confirm(
    `确认提交「${templateMap.value.get(row.templateId!)?.templateName ?? '模板'}」${row.auditYear} 年度数据？提交后将触发数据抽取并锁定编辑。`,
    '提交确认',
    { type: 'warning' }
  )
  submittingId.value = row.id!
  try {
    const publishedVer = await getPublishedVersion(row.templateId!)
    if (!publishedVer?.id) {
      ElMessage.error('模板尚未发布有效版本，无法提交')
      return
    }
    await submitSubmission(row.id!, publishedVer.id)
    ElMessage.success('提交成功，数据已抽取')
    loadData()
  } catch (e: any) {
    if (!String(e).includes('cancel')) {
      ElMessage.error('提交失败：' + (e?.message ?? '未知错误'))
    }
  } finally {
    submittingId.value = null
  }
}

function goToFill(row: TplSubmission) {
  router.push({
    path: '/enterprise/report/input',
    query: { templateId: row.templateId, year: row.auditYear },
  })
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
          <span class="card-title">填报进度概览</span>
          <el-button @click="loadData" :loading="loading" size="small">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="submissions" border stripe>
        <el-table-column label="模板名称" min-width="180">
          <template #default="{ row }">
            {{ templateMap.get(row.templateId)?.templateName ?? `模板 #${row.templateId}` }}
          </template>
        </el-table-column>
        <el-table-column prop="auditYear" label="审计年度" width="100" align="center">
          <template #default="{ row }">{{ row.auditYear }} 年</template>
        </el-table-column>
        <el-table-column prop="templateVersion" label="版本" width="80" align="center">
          <template #default="{ row }">v{{ row.templateVersion }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status ?? 0]?.type" size="small">
              {{ STATUS_MAP[row.status ?? 0]?.label ?? '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
        <el-table-column prop="updateTime" label="最后保存" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="row.status === 1"
              @click="goToFill(row)"
            >
              去填报
            </el-button>
            <el-button
              link
              type="success"
              :loading="submittingId === row.id"
              :disabled="row.status === 1"
              @click="handleSubmit(row)"
            >
              提交数据
            </el-button>
            <el-button link type="info" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && submissions.length === 0"
        description="暂无填报记录，请先在「报告录入」页保存草稿"
      />
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
