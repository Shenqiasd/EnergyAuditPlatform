<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete, Refresh } from '@element-plus/icons-vue'
import {
  listTemplates,
  uploadReportTemplate,
  activateReportTemplate,
  deactivateReportTemplate,
  deleteReportTemplate,
  type ArReportTemplate,
} from '@/api/report'

const loading = ref(false)
const tableData = ref<ArReportTemplate[]>([])

// Upload dialog
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadForm = ref<{ templateName: string; file: File | null }>({
  templateName: '',
  file: null,
})

const TEMPLATE_STATUS_MAP: Record<number, { label: string; type: 'success' | 'info' }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已激活', type: 'success' },
}

async function loadData() {
  loading.value = true
  try {
    const res = await listTemplates()
    tableData.value = (res as ArReportTemplate[]) ?? []
  } finally {
    loading.value = false
  }
}

function openUploadDialog() {
  uploadForm.value = { templateName: '', file: null }
  uploadDialogVisible.value = true
}

function handleFileChange(uploadFile: { raw: File }) {
  const file = uploadFile.raw
  if (!file.name.toLowerCase().endsWith('.docx') && !file.name.toLowerCase().endsWith('.doc')) {
    ElMessage.error('仅支持 .docx 或 .doc 格式的模板文件')
    return
  }
  uploadForm.value.file = file
}

async function handleUpload() {
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  uploading.value = true
  try {
    await uploadReportTemplate(uploadForm.value.file, uploadForm.value.templateName || undefined)
    ElMessage.success('模板上传成功')
    uploadDialogVisible.value = false
    loadData()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '上传失败'
    ElMessage.error(msg)
  } finally {
    uploading.value = false
  }
}

async function handleActivate(row: ArReportTemplate) {
  await ElMessageBox.confirm(
    `激活模板「${row.templateName}」将停用其他已激活的模板，确认？`,
    '激活确认',
    { type: 'warning' }
  )
  try {
    await activateReportTemplate(row.id)
    ElMessage.success('模板已激活')
    loadData()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '操作失败'
    ElMessage.error(msg)
  }
}

async function handleDeactivate(row: ArReportTemplate) {
  await ElMessageBox.confirm(`确认停用模板「${row.templateName}」？`, '停用确认', { type: 'warning' })
  try {
    await deactivateReportTemplate(row.id)
    ElMessage.success('模板已停用')
    loadData()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '操作失败'
    ElMessage.error(msg)
  }
}

async function handleDelete(row: ArReportTemplate) {
  await ElMessageBox.confirm(
    `确认删除模板「${row.templateName}」？此操作不可撤销。`,
    '删除确认',
    { type: 'warning', confirmButtonClass: 'el-button--danger' }
  )
  try {
    await deleteReportTemplate(row.id)
    ElMessage.success('模板已删除')
    loadData()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '删除失败'
    ElMessage.error(msg)
  }
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <h3 style="margin: 0">报告模板管理</h3>
      <div>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="Upload" @click="openUploadDialog">上传模板</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe empty-text="暂无报告模板">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="templateName" label="模板名称" min-width="200" />
      <el-table-column prop="version" label="版本" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="TEMPLATE_STATUS_MAP[row.status]?.type ?? 'info'" size="small">
            {{ TEMPLATE_STATUS_MAP[row.status]?.label ?? '未知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件路径" min-width="200">
        <template #default="{ row }">
          <span class="file-path" :title="row.templateFilePath">
            {{ row.templateFilePath?.split('/').pop() ?? '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="创建人" prop="createBy" width="100" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status !== 1"
            link
            type="success"
            @click="handleActivate(row)"
          >激活</el-button>
          <el-button
            v-if="row.status === 1"
            link
            type="warning"
            @click="handleDeactivate(row)"
          >停用</el-button>
          <el-button
            link
            type="danger"
            :icon="Delete"
            :disabled="row.status === 1"
            @click="handleDelete(row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Upload Dialog -->
    <el-dialog v-model="uploadDialogVisible" title="上传报告模板" width="500px">
      <el-form label-width="100px">
        <el-form-item label="模板名称">
          <el-input
            v-model="uploadForm.templateName"
            placeholder="可选，不填则使用文件名"
            clearable
          />
        </el-form-item>
        <el-form-item label="模板文件" required>
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept=".docx,.doc"
            :on-change="handleFileChange"
          >
            <el-button type="primary" :icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">仅支持 .docx 或 .doc 格式</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.file-path {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
}
</style>
