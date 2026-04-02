<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Upload, View } from '@element-plus/icons-vue'
import {
  getTemplateList,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  publishTemplate,
  listVersions,
  publishVersion,
  createDraftVersion,
  listTags,
  replaceTags,
  type TplTemplate,
  type TplTemplateVersion,
  type TplTagMapping,
} from '@/api/template'

const STATUS_MAP: Record<number, { label: string; type: 'info' | 'warning' | 'success' | 'danger' }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已发布', type: 'success' },
  2: { label: '已归档', type: 'warning' },
}

const MODULE_OPTIONS = [
  { label: '能源审计', value: 'energy_audit' },
  { label: '碳排放', value: 'carbon_emission' },
  { label: '能效评估', value: 'efficiency' },
]

const loading = ref(false)
const tableData = ref<TplTemplate[]>([])
const total = ref(0)
const query = reactive({ templateName: '', moduleType: '', status: undefined as number | undefined, pageNum: 1, pageSize: 20 })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = ref<Partial<TplTemplate>>({})
const rules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
}

const versionDrawer = ref(false)
const activeTemplate = ref<TplTemplate | null>(null)
const versions = ref<TplTemplateVersion[]>([])
const versionsLoading = ref(false)
const publishingVersion = ref<number | null>(null)

const tagDrawer = ref(false)
const activeVersion = ref<TplTemplateVersion | null>(null)
const tags = ref<TplTagMapping[]>([])
const tagsLoading = ref(false)
const savingTags = ref(false)

async function loadData() {
  loading.value = true
  try {
    const res = await getTemplateList(query)
    tableData.value = res.rows ?? []
    total.value = Number(res.total ?? 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.templateName = ''
  query.moduleType = ''
  query.status = undefined
  query.pageNum = 1
  loadData()
}

function openCreate() {
  form.value = {}
  dialogTitle.value = '新建模板'
  dialogVisible.value = true
}

function openEdit(row: TplTemplate) {
  form.value = { ...row }
  dialogTitle.value = '编辑模板'
  dialogVisible.value = true
}

function handleClose() {
  formRef.value?.resetFields()
  dialogVisible.value = false
}

async function handleSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.value.id) {
      await updateTemplate(form.value.id, form.value)
    } else {
      await createTemplate(form.value)
    }
    ElMessage.success('保存成功')
    handleClose()
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: TplTemplate) {
  await ElMessageBox.confirm(`确认删除模板「${row.templateName}」？`, '删除确认', { type: 'warning' })
  await deleteTemplate(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

async function handlePublish(row: TplTemplate) {
  await ElMessageBox.confirm(`将发布模板「${row.templateName}」的最新草稿版本，是否继续？`, '发布确认', { type: 'warning' })
  await publishTemplate(row.id!)
  ElMessage.success('发布成功')
  loadData()
}

async function openVersions(row: TplTemplate) {
  activeTemplate.value = row
  versionDrawer.value = true
  versionsLoading.value = true
  try {
    versions.value = await listVersions(row.id!)
  } finally {
    versionsLoading.value = false
  }
}

async function handlePublishVersion(v: TplTemplateVersion) {
  if (!activeTemplate.value) return
  await ElMessageBox.confirm(`确认发布版本 v${v.version}？`, '发布版本', { type: 'warning' })
  publishingVersion.value = v.id!
  try {
    await publishVersion(activeTemplate.value.id!, v.id!)
    ElMessage.success('版本已发布')
    versions.value = await listVersions(activeTemplate.value.id!)
    loadData()
  } finally {
    publishingVersion.value = null
  }
}

async function handleCreateDraft() {
  if (!activeTemplate.value) return
  await createDraftVersion(activeTemplate.value.id!)
  ElMessage.success('已创建新草稿版本')
  versions.value = await listVersions(activeTemplate.value.id!)
}

async function openTags(v: TplTemplateVersion) {
  activeVersion.value = v
  tagDrawer.value = true
  tagsLoading.value = true
  try {
    tags.value = await listTags(v.id!)
  } finally {
    tagsLoading.value = false
  }
}

function addTagRow() {
  tags.value.push({
    templateVersionId: activeVersion.value?.id,
    tagName: '',
    fieldName: '',
    targetTable: '',
    dataType: 'STRING',
    required: 0,
    sheetIndex: 0,
  })
}

function removeTagRow(idx: number) {
  tags.value.splice(idx, 1)
}

async function saveTags() {
  if (!activeVersion.value) return
  savingTags.value = true
  try {
    await replaceTags(activeVersion.value.id!, tags.value)
    ElMessage.success('标签映射已保存')
  } finally {
    savingTags.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="模板名称">
          <el-input v-model="query.templateName" placeholder="请输入模板名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="模块类型">
          <el-select v-model="query.moduleType" placeholder="全部" clearable style="width:160px">
            <el-option v-for="m in MODULE_OPTIONS" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option v-for="(v, k) in STATUS_MAP" :key="k" :label="v.label" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">模板列表</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建模板</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="templateCode" label="模板编码" width="160" />
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="moduleType" label="模块类型" width="130">
          <template #default="{ row }">
            {{ MODULE_OPTIONS.find(m => m.value === row.moduleType)?.label ?? row.moduleType ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="currentVersion" label="当前版本" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">v{{ row.currentVersion ?? 1 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_MAP[row.status ?? 0]?.type" size="small">
              {{ STATUS_MAP[row.status ?? 0]?.label ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" :icon="Upload" @click="handlePublish(row)" :disabled="row.status === 1">发布</el-button>
            <el-button link type="info" :icon="View" @click="openVersions(row)">版本</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px"
        @change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="如：ENERGY_AUDIT_2024" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模块类型">
          <el-select v-model="form.moduleType" clearable placeholder="请选择模块类型" style="width:100%">
            <el-option v-for="m in MODULE_OPTIONS" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="versionDrawer" :title="`版本管理 — ${activeTemplate?.templateName ?? ''}`" size="680px">
      <div class="drawer-toolbar">
        <el-button type="primary" size="small" :icon="Plus" @click="handleCreateDraft">创建新草稿版本</el-button>
      </div>
      <el-table v-loading="versionsLoading" :data="versions" border>
        <el-table-column prop="version" label="版本号" width="80" align="center">
          <template #default="{ row }">v{{ row.version }}</template>
        </el-table-column>
        <el-table-column prop="published" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.published === 1 ? 'success' : 'info'" size="small">
              {{ row.published === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="changeLog" label="更新说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="publishTime" label="发布时间" width="150" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button
              link type="success"
              @click="handlePublishVersion(row)"
              :loading="publishingVersion === row.id"
              :disabled="row.published === 1"
            >发布</el-button>
            <el-button link type="primary" @click="openTags(row)">标签映射</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-drawer
      v-model="tagDrawer"
      :title="`标签映射 — v${activeVersion?.version ?? ''}`"
      size="880px"
      direction="rtl"
    >
      <div class="drawer-toolbar">
        <el-button type="primary" size="small" :icon="Plus" @click="addTagRow">添加映射行</el-button>
        <el-button type="success" size="small" :loading="savingTags" @click="saveTags">保存全部</el-button>
      </div>
      <el-table v-loading="tagsLoading" :data="tags" border size="small" style="margin-top:12px">
        <el-table-column label="Tag名称" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.tagName" placeholder="tagName" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="字段名" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.fieldName" placeholder="fieldName" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="目标表" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.targetTable" placeholder="targetTable" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="数据类型" width="120">
          <template #default="{ row }">
            <el-select v-model="row.dataType" size="small" style="width:100%">
              <el-option label="STRING" value="STRING" />
              <el-option label="NUMBER" value="NUMBER" />
              <el-option label="DATE" value="DATE" />
              <el-option label="DICT" value="DICT" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.required" :true-value="1" :false-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="100">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="备注" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="" width="60" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" :icon="Delete" @click="removeTagRow($index)" />
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.search-card {
  :deep(.el-card__body) {
    padding-bottom: 4px;
  }
}

.table-card {}

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

.drawer-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
