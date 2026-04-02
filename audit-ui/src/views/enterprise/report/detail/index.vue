<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSubmissions, type TplSubmission } from '@/api/template'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submission = ref<TplSubmission | null>(null)
const allSubmissions = ref<TplSubmission[]>([])
const selectedId = ref<number | null>(null)

const extractedPairs = computed<Array<{ key: string; value: string }>>(() => {
  if (!submission.value?.extractedData) return []
  try {
    const obj = JSON.parse(submission.value.extractedData)
    return Object.entries(obj).map(([key, value]) => ({ key, value: String(value ?? '') }))
  } catch {
    return []
  }
})

async function loadList() {
  const list = await listSubmissions()
  allSubmissions.value = list
  const qid = route.query.id ? Number(route.query.id) : null
  if (qid) {
    selectedId.value = qid
    submission.value = list.find(s => s.id === qid) ?? null
  }
}

async function selectSubmission(id: number) {
  selectedId.value = id
  submission.value = allSubmissions.value.find(s => s.id === id) ?? null
  router.replace({ query: { id } })
}

onMounted(async () => {
  loading.value = true
  try {
    await loadList()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-container">
    <div class="layout">
      <el-card class="list-card" shadow="never">
        <template #header><span class="card-title">填报记录</span></template>
        <el-scrollbar>
          <div
            v-for="s in allSubmissions"
            :key="s.id"
            class="list-item"
            :class="{ active: s.id === selectedId }"
            @click="selectSubmission(s.id!)"
          >
            <div class="list-item-main">
              <span class="list-item-year">{{ s.auditYear }} 年</span>
              <el-tag :type="s.status === 1 ? 'success' : 'warning'" size="small">
                {{ s.status === 1 ? '已提交' : '草稿' }}
              </el-tag>
            </div>
            <div class="list-item-sub">模板 #{{ s.templateId }} · v{{ s.templateVersion }}</div>
          </div>
          <el-empty v-if="!loading && allSubmissions.length === 0" description="暂无记录" :image-size="60" />
        </el-scrollbar>
      </el-card>

      <el-card class="detail-card" v-loading="loading" shadow="never">
        <template #header><span class="card-title">填报详情</span></template>

        <el-empty v-if="!submission" description="请在左侧选择一条填报记录" />

        <template v-else>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="填报 ID">{{ submission.id }}</el-descriptions-item>
            <el-descriptions-item label="审计年度">{{ submission.auditYear }} 年</el-descriptions-item>
            <el-descriptions-item label="模板 ID">{{ submission.templateId }}</el-descriptions-item>
            <el-descriptions-item label="模板版本">v{{ submission.templateVersion }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="submission.status === 1 ? 'success' : 'warning'" size="small">
                {{ submission.status === 1 ? '已提交' : '草稿' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ submission.submitTime ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ submission.createTime }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ submission.updateTime }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">抽取数据（extractedData）</el-divider>

          <div v-if="extractedPairs.length">
            <el-table :data="extractedPairs" border size="small">
              <el-table-column prop="key" label="字段名" width="220" />
              <el-table-column prop="value" label="值" show-overflow-tooltip />
            </el-table>
          </div>
          <el-empty v-else-if="submission.status === 0" description="草稿尚未提交，无抽取数据" :image-size="60" />
          <el-empty v-else description="暂无抽取数据" :image-size="60" />

          <el-divider content-position="left">原始 JSON（submissionJson）</el-divider>
          <el-input
            :model-value="submission.submissionJson ?? ''"
            type="textarea"
            :rows="8"
            readonly
            style="font-family:monospace;font-size:12px"
          />
        </template>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
  height: calc(100vh - 120px);
}

.layout {
  display: flex;
  gap: 16px;
  height: 100%;
}

.list-card {
  width: 260px;
  flex-shrink: 0;
  height: 100%;
  display: flex;
  flex-direction: column;

  :deep(.el-card__body) {
    flex: 1;
    overflow: hidden;
    padding: 8px 0;
  }
}

.detail-card {
  flex: 1;
  overflow: auto;
  height: 100%;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.list-item {
  padding: 10px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;

  &:hover {
    background: #f5f7fa;
  }

  &.active {
    background: #ecf5ff;
    border-left: 3px solid #409eff;
  }
}

.list-item-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.list-item-year {
  font-weight: 600;
  font-size: 14px;
}

.list-item-sub {
  font-size: 12px;
  color: #909399;
}
</style>
