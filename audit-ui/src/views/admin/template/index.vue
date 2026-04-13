<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Upload, View, CircleClose } from '@element-plus/icons-vue'
import SpreadDesigner from '@/components/SpreadDesigner/index.vue'
import ColumnMappingEditor from '@/components/ColumnMappingEditor.vue'
import { getTableOptions, getFieldOptions } from '@/config/schema-registry'
import {
  getTemplateList,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  publishTemplate,
  listVersions,
  publishVersion,
  deleteVersion,
  createDraftVersion,
  getVersionById,
  saveVersionJson,
  listTags,
  syncTagsFromJson,
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

const DATA_TYPE_OPTIONS = [
  { label: 'STRING', value: 'STRING' },
  { label: 'NUMBER', value: 'NUMBER' },
  { label: 'DECIMAL', value: 'DECIMAL' },
  { label: 'TEXT', value: 'TEXT' },
  { label: 'DATE', value: 'DATE' },
  { label: 'DICT', value: 'DICT' },
]

const MAPPING_TYPE_OPTIONS = [
  { label: '标量 (SCALAR)', value: 'SCALAR' },
  { label: '表格 (TABLE)', value: 'TABLE' },
]

const SOURCE_TYPE_LABELS: Record<string, string> = {
  NAMED_RANGE: 'Named Range',
  CELL_TAG: 'Cell Tag',
}

const tableOptions = getTableOptions()

// ── Template list ──────────────────────────────────────────────────────────────
const loading = ref(false)
const tableData = ref<TplTemplate[]>([])
const total = ref(0)
const query = reactive({ templateName: '', moduleType: '', status: undefined as number | undefined, pageNum: 1, pageSize: 20 })

// ── Template create / edit dialog ─────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = ref<Partial<TplTemplate>>({})
const rules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
}

// ── Version management drawer ─────────────────────────────────────────────────
const versionDrawer = ref(false)
const activeTemplate = ref<TplTemplate | null>(null)
const versions = ref<TplTemplateVersion[]>([])
const versionsLoading = ref(false)
const publishingVersion = ref<number | null>(null)

// ── Designer full-screen dialog ───────────────────────────────────────────────
const designerDialog = ref(false)
const designerVersion = ref<TplTemplateVersion | null>(null)
const designerLoading = ref(false)
const designerSaving = ref(false)
const designerTagsLoading = ref(false)
const designerTagsSaving = ref(false)
const designerTags = ref<TplTagMapping[]>([])
const designerRef = ref<InstanceType<typeof SpreadDesigner>>()

// ── Template list methods ──────────────────────────────────────────────────────
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

// ── Template CRUD methods ──────────────────────────────────────────────────────
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

// ── Version drawer methods ─────────────────────────────────────────────────────
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

async function handleDeleteVersion(v: TplTemplateVersion) {
  if (!activeTemplate.value) return
  await ElMessageBox.confirm(
    `确认删除版本 v${v.version}？删除后不可恢复。`,
    '删除版本',
    { type: 'warning' }
  )
  try {
    await deleteVersion(activeTemplate.value.id!, v.id!)
    ElMessage.success('版本已删除')
    versions.value = await listVersions(activeTemplate.value.id!)
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message ?? e?.message ?? '删除失败')
  }
}

// ── Designer dialog methods ────────────────────────────────────────────────────
async function openDesigner(v: TplTemplateVersion) {
  designerLoading.value = true
  designerDialog.value = true
  try {
    const full = await getVersionById(v.id!)
    designerVersion.value = full
    // Sync tags only for draft versions; published versions are read-only and
    // should not mutate tag rows on open.
    if (full.published !== 1) {
      await syncTagsFromJson(v.id!)
    }
  } finally {
    designerLoading.value = false
  }
  await refreshDesignerTags(v.id!)
}

async function refreshDesignerTags(versionId: number) {
  designerTagsLoading.value = true
  try {
    designerTags.value = await listTags(versionId)
  } finally {
    designerTagsLoading.value = false
  }
}

async function handleSaveDesign() {
  if (!designerVersion.value || !designerRef.value) return
  designerSaving.value = true
  try {
    const json = designerRef.value.getJson()
    await saveVersionJson(
      designerVersion.value.id!,
      json,
      undefined,
      designerVersion.value.protectionEnabled,
    )
    ElMessage.success('草稿已保存，Tag 映射已自动同步')
    await refreshDesignerTags(designerVersion.value.id!)
  } finally {
    designerSaving.value = false
  }
}

async function handleSaveDesignerTags() {
  if (!designerVersion.value) return
  designerTagsSaving.value = true
  try {
    await replaceTags(designerVersion.value.id!, designerTags.value)
    ElMessage.success('标签配置已保存')
  } finally {
    designerTagsSaving.value = false
  }
}

function handleDesignerClose() {
  designerDialog.value = false
  designerVersion.value = null
  designerTags.value = []
}

const isReadonly = (v: TplTemplateVersion | null) => v?.published === 1

async function handleDeleteTag(tag: TplTagMapping, index: number) {
  await ElMessageBox.confirm(
    `确认删除字段映射「${tag.tagName}」？删除后需点击「保存映射配置」才会生效。`,
    '删除确认',
    { type: 'warning' }
  )
  designerTags.value.splice(index, 1)
  ElMessage.success('已移除，请点击「保存映射配置」以持久化')
}

// ── Direct "设计" shortcut: open latest draft version for a template ──────────
async function openLatestDesigner(row: TplTemplate) {
  activeTemplate.value = row
  const vs = await listVersions(row.id!)
  let draft = vs.find(v => v.published !== 1)
  if (!draft && vs.length === 0) {
    // No versions at all — create one now
    await createDraftVersion(row.id!)
    const fresh = await listVersions(row.id!)
    draft = fresh.find(v => v.published !== 1)
    if (!draft) { ElMessage.error('创建草稿版本失败，请重试'); return }
  }
  if (!draft) {
    // All existing versions are published; ask before creating a new draft
    try {
      await ElMessageBox.confirm(
        '当前所有版本均已发布，是否自动创建新草稿版本进行设计？',
        '创建草稿',
        { type: 'info', confirmButtonText: '创建并设计', cancelButtonText: '取消' }
      )
    } catch { return }
    await createDraftVersion(row.id!)
    const fresh = await listVersions(row.id!)
    draft = fresh.find(v => v.published !== 1)
    if (!draft) { ElMessage.error('创建草稿版本失败，请重试'); return }
    loadData()
  }
  await openDesigner(draft)
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">

    <!-- ── Search ── -->
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

    <!-- ── Table ── -->
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
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" :icon="Upload" @click="handlePublish(row)" :disabled="row.status === 1">发布</el-button>
            <el-button link type="warning" @click="openLatestDesigner(row)">设计</el-button>
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

    <!-- ── Create / Edit Dialog ── -->
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

    <!-- ── Version Drawer ── -->
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
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button
              link type="success"
              @click="handlePublishVersion(row)"
              :loading="publishingVersion === row.id"
              :disabled="row.published === 1"
            >发布</el-button>
            <el-button link type="primary" @click="openDesigner(row)">设计</el-button>
            <el-button
              link type="danger"
              :icon="Delete"
              @click="handleDeleteVersion(row)"
              :disabled="row.published === 1"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- ── Designer Full-Screen Dialog ── -->
    <el-dialog
      v-model="designerDialog"
      :title="`模板设计器 — ${activeTemplate?.templateName ?? ''} ${designerVersion ? 'v' + designerVersion.version : ''}${isReadonly(designerVersion) ? ' （只读）' : ''}`"
      fullscreen
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      @close="handleDesignerClose"
      class="designer-dialog"
    >
      <div v-loading="designerLoading" class="designer-layout">
        <!-- Left: SpreadJS Designer -->
        <div class="designer-main">
          <SpreadDesigner
            v-if="!designerLoading && designerVersion"
            ref="designerRef"
            :template-json="designerVersion.templateJson"
            :readonly="isReadonly(designerVersion)"
          />
        </div>

        <!-- Right: Tag Config Panel -->
        <div class="designer-sidebar">
          <div class="sidebar-protection">
            <span class="protection-label">单元格保护</span>
            <el-switch
              v-if="designerVersion"
              v-model="designerVersion.protectionEnabled"
              :active-value="1"
              :inactive-value="0"
              :disabled="isReadonly(designerVersion)"
              active-text="开启"
              inactive-text="关闭"
              inline-prompt
              style="--el-switch-on-color: #67c23a"
            />
          </div>
          <div class="sidebar-header">
            <span class="sidebar-title">字段映射配置</span>
            <el-button
              type="primary"
              size="small"
              :loading="designerTagsSaving"
              :disabled="isReadonly(designerVersion)"
              @click="handleSaveDesignerTags"
            >保存映射配置</el-button>
          </div>

          <el-scrollbar class="sidebar-scroll">
            <div v-loading="designerTagsLoading" class="tags-panel">
              <el-empty
                v-if="!designerTagsLoading && designerTags.length === 0"
                description="暂无字段 — 先在设计器中给单元格设置 tag 属性或 Named Range，再点「保存草稿」自动发现"
                :image-size="80"
              />
              <div
                v-for="tag in designerTags"
                :key="tag.id ?? tag.tagName"
                class="tag-item"
              >
                <div class="tag-header">
                  <div class="tag-name-row">
                    <el-tag size="small" type="info">{{ tag.tagName }}</el-tag>
                    <el-button
                      v-if="!isReadonly(designerVersion)"
                      :icon="CircleClose"
                      type="danger"
                      size="small"
                      link
                      class="tag-delete-btn"
                      @click="handleDeleteTag(tag, designerTags.indexOf(tag))"
                    />
                  </div>
                  <div class="tag-badges">
                    <el-tag size="small" :type="tag.sourceType === 'NAMED_RANGE' ? 'success' : 'warning'" effect="plain">
                      {{ SOURCE_TYPE_LABELS[tag.sourceType ?? 'CELL_TAG'] ?? tag.sourceType }}
                    </el-tag>
                    <el-tag size="small" :type="tag.mappingType === 'TABLE' ? 'primary' : 'info'" effect="plain">
                      {{ tag.mappingType === 'TABLE' ? '表格' : '标量' }}
                    </el-tag>
                    <el-tag v-if="tag.sheetName" size="small" effect="plain">
                      {{ tag.sheetName }}
                    </el-tag>
                  </div>
                </div>
                <el-select
                  v-model="tag.targetTable"
                  placeholder="选择目标表 (targetTable)"
                  size="small"
                  filterable
                  clearable
                  :disabled="isReadonly(designerVersion)"
                  style="margin-top:6px;width:100%"
                  @change="() => { tag.fieldName = '' }"
                >
                  <el-option
                    v-for="opt in tableOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <el-select
                  v-model="tag.fieldName"
                  :placeholder="tag.targetTable ? '选择字段 (fieldName)' : '请先选择目标表'"
                  size="small"
                  filterable
                  clearable
                  :disabled="isReadonly(designerVersion) || !tag.targetTable"
                  style="margin-top:4px;width:100%"
                >
                  <el-option
                    v-for="opt in getFieldOptions(tag.targetTable ?? '')"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
                <div class="tag-row" style="margin-top:4px">
                  <el-select
                    v-model="tag.mappingType"
                    size="small"
                    style="flex:1"
                    :disabled="isReadonly(designerVersion) || tag.sourceType === 'NAMED_RANGE'"
                    placeholder="映射类型"
                  >
                    <el-option
                      v-for="opt in MAPPING_TYPE_OPTIONS"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                  <el-select
                    v-model="tag.dataType"
                    size="small"
                    style="flex:1;margin-left:6px"
                    :disabled="isReadonly(designerVersion)"
                  >
                    <el-option
                      v-for="opt in DATA_TYPE_OPTIONS"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                  <el-checkbox
                    v-model="tag.required"
                    :true-value="1"
                    :false-value="0"
                    :disabled="isReadonly(designerVersion)"
                    style="margin-left:8px"
                  >必填</el-checkbox>
                </div>
                <el-input
                  v-model="tag.dictType"
                  placeholder="字典类型 (dictType，数据类型为 DICT 时填写)"
                  size="small"
                  :disabled="isReadonly(designerVersion)"
                  style="margin-top:4px"
                  v-if="tag.dataType === 'DICT'"
                />
                <!-- TABLE-specific configuration -->
                <div v-if="tag.mappingType === 'TABLE'" class="table-config">
                  <div class="table-config-title">表格配置</div>
                  <div class="tag-row" style="margin-top:4px">
                    <el-input
                      v-model="tag.cellRange"
                      placeholder="单元格范围 (如 A2:F20)"
                      size="small"
                      :disabled="isReadonly(designerVersion) || tag.sourceType === 'NAMED_RANGE'"
                      style="flex:1"
                    />
                    <el-input-number
                      v-model="tag.headerRow"
                      placeholder="表头行"
                      size="small"
                      :min="0"
                      :disabled="isReadonly(designerVersion)"
                      style="width:110px;margin-left:6px"
                      controls-position="right"
                    />
                  </div>
                  <ColumnMappingEditor
                    v-model="tag.columnMappings"
                    :target-table="tag.targetTable ?? ''"
                    :disabled="isReadonly(designerVersion)"
                    style="margin-top:4px"
                  />
                  <el-input-number
                    v-model="tag.rowKeyColumn"
                    placeholder="行标识列"
                    size="small"
                    :min="0"
                    :disabled="isReadonly(designerVersion)"
                    controls-position="right"
                    style="width:100%;margin-top:4px"
                  />
                </div>
              </div>
            </div>
          </el-scrollbar>
        </div>
      </div>

      <template #footer>
        <el-button
          v-if="!isReadonly(designerVersion)"
          type="primary"
          :loading="designerSaving"
          @click="handleSaveDesign"
        >保存草稿</el-button>
        <el-button @click="handleDesignerClose">关闭</el-button>
      </template>
    </el-dialog>

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

/* Designer dialog */
:deep(.designer-dialog .el-dialog__body) {
  padding: 0;
  overflow: hidden;
}

:deep(.designer-dialog .el-dialog__header) {
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  margin-right: 0;
}

:deep(.designer-dialog .el-dialog__footer) {
  padding: 10px 20px;
  border-top: 1px solid #e4e7ed;
}

.designer-layout {
  display: flex;
  height: calc(100vh - 112px);
  overflow: hidden;
}

.designer-main {
  flex: 1;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.designer-sidebar {
  width: 320px;
  flex-shrink: 0;
  border-left: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.sidebar-protection {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #e4e7ed;
  background: #f0f9eb;
}

.protection-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.sidebar-scroll {
  flex: 1;
}

.tags-panel {
  padding: 12px;
}

.tag-item {
  padding: 10px;
  margin-bottom: 10px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.tag-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.tag-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tag-delete-btn {
  padding: 2px !important;
  font-size: 14px;
}

.tag-badges {
  display: flex;
  gap: 4px;
}

.tag-row {
  display: flex;
  align-items: center;
}

.table-config {
  margin-top: 8px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px dashed #d9ecff;
}

.table-config-title {
  font-size: 12px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 4px;
}
</style>
