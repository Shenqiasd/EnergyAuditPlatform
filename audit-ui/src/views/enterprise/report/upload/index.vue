<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { listSubmissions, type TplSubmission } from '@/api/template'

const loading = ref(false)
const submissions = ref<TplSubmission[]>([])
const submittedList = ref<TplSubmission[]>([])

async function loadData() {
  loading.value = true
  try {
    const all = await listSubmissions()
    submissions.value = all
    submittedList.value = all.filter(s => s.status === 1)
  } finally {
    loading.value = false
  }
}

function handleBeforeUpload(_file: File) {
  ElMessage.info('文件上传功能将在下一迭代接入 OSS 存储，敬请期待')
  return false
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <el-alert
      type="info"
      :closable="false"
      style="margin-bottom:16px"
      title="说明：仅已提交（状态=已提交）的填报记录支持上传最终审计报告附件。文件存储功能将在后续迭代完成对接。"
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">上传最终报告</span>
          <el-button @click="loadData" :loading="loading">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="submissions" border stripe>
        <el-table-column prop="templateId" label="模板ID" width="90" align="center" />
        <el-table-column prop="auditYear" label="审计年度" width="100" align="center">
          <template #default="{ row }">{{ row.auditYear }} 年</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
              {{ row.status === 1 ? '已提交' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-upload
              v-if="row.status === 1"
              :before-upload="handleBeforeUpload"
              :show-file-list="false"
              accept=".pdf,.doc,.docx"
            >
              <el-button link type="primary" :icon="UploadFilled">上传报告</el-button>
            </el-upload>
            <el-button v-else link type="info" disabled>请先提交数据</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && submissions.length === 0" description="暂无填报记录" />
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
