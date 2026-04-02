<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import * as dictApi from '@/api/dict'
import type { DictType, DictData } from '@/api/dict'

// === Dict Type (left panel) ===
const typeLoading = ref(false)
const typeList = ref<DictType[]>([])
const typeTotal = ref(0)
const typeQuery = reactive({ dictName: '', dictType: '', pageNum: 1, pageSize: 20 })
const selectedType = ref<DictType | null>(null)

const typeDialogVisible = ref(false)
const typeDialogTitle = ref('新建字典类型')
const typeFormRef = ref<FormInstance>()
const isTypeEdit = ref(false)
const typeForm = reactive<Partial<DictType>>({ dictName: '', dictType: '', status: 1, remark: '' })

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型标识', trigger: 'blur' }],
}

// === Dict Data (right panel) ===
const dataLoading = ref(false)
const dataList = ref<DictData[]>([])
const dataTotal = ref(0)
const dataQuery = reactive({ dictLabel: '', pageNum: 1, pageSize: 20 })

const dataDialogVisible = ref(false)
const dataDialogTitle = ref('新建字典数据')
const dataFormRef = ref<FormInstance>()
const isDataEdit = ref(false)
const dataForm = reactive<Partial<DictData>>({ dictType: '', dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', status: 1, remark: '' })

const dataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典值', trigger: 'blur' }],
}

// === Type methods ===
async function fetchTypes() {
  typeLoading.value = true
  try {
    const res = await dictApi.listTypes(typeQuery)
    typeList.value = res.list
    typeTotal.value = res.total
  } catch {} finally {
    typeLoading.value = false
  }
}

function handleSelectType(row: DictType) {
  selectedType.value = row
  dataQuery.pageNum = 1
  fetchData()
}

function handleCreateType() {
  isTypeEdit.value = false
  typeDialogTitle.value = '新建字典类型'
  Object.assign(typeForm, { id: undefined, dictName: '', dictType: '', status: 1, remark: '' })
  typeDialogVisible.value = true
}

function handleEditType(row: DictType) {
  isTypeEdit.value = true
  typeDialogTitle.value = '编辑字典类型'
  Object.assign(typeForm, { ...row })
  typeDialogVisible.value = true
}

async function handleTypeSubmit() {
  await typeFormRef.value?.validate()
  try {
    if (isTypeEdit.value && typeForm.id) {
      await dictApi.updateType(typeForm.id, typeForm)
      ElMessage.success('更新成功')
    } else {
      await dictApi.createType(typeForm)
      ElMessage.success('创建成功')
    }
    typeDialogVisible.value = false
    fetchTypes()
  } catch {}
}

async function handleDeleteType(row: DictType) {
  try {
    await ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」吗？关联的字典数据也会一并删除。`, '删除确认', { type: 'error' })
    await dictApi.deleteType(row.id!)
    ElMessage.success('已删除')
    if (selectedType.value?.id === row.id) {
      selectedType.value = null
      dataList.value = []
    }
    fetchTypes()
  } catch {}
}

// === Data methods ===
async function fetchData() {
  if (!selectedType.value) return
  dataLoading.value = true
  try {
    const res = await dictApi.listData({ ...dataQuery, dictType: selectedType.value.dictType })
    dataList.value = res.list
    dataTotal.value = res.total
  } catch {} finally {
    dataLoading.value = false
  }
}

function handleCreateData() {
  if (!selectedType.value) { ElMessage.warning('请先选择一个字典类型'); return }
  isDataEdit.value = false
  dataDialogTitle.value = '新建字典数据'
  Object.assign(dataForm, { id: undefined, dictType: selectedType.value.dictType, dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', status: 1, remark: '' })
  dataDialogVisible.value = true
}

function handleEditData(row: DictData) {
  isDataEdit.value = true
  dataDialogTitle.value = '编辑字典数据'
  Object.assign(dataForm, { ...row })
  dataDialogVisible.value = true
}

async function handleDataSubmit() {
  await dataFormRef.value?.validate()
  try {
    if (isDataEdit.value && dataForm.id) {
      await dictApi.updateData(dataForm.id, dataForm)
      ElMessage.success('更新成功')
    } else {
      await dictApi.createData(dataForm)
      ElMessage.success('创建成功')
    }
    dataDialogVisible.value = false
    fetchData()
  } catch {}
}

async function handleDeleteData(row: DictData) {
  try {
    await ElMessageBox.confirm(`确认删除字典数据「${row.dictLabel}」吗？`, '删除确认', { type: 'error' })
    await dictApi.deleteData(row.id!)
    ElMessage.success('已删除')
    fetchData()
  } catch {}
}

const cssClassOptions = [
  { label: '默认', value: '' },
  { label: '成功 (success)', value: 'success' },
  { label: '警告 (warning)', value: 'warning' },
  { label: '危险 (danger)', value: 'danger' },
  { label: '信息 (info)', value: 'info' },
]

onMounted(fetchTypes)
</script>

<template>
  <div class="dict-page">
    <el-row :gutter="16">
      <!-- Left: Dict Types -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">字典类型</span>
              <el-button type="primary" size="small" :icon="Plus" @click="handleCreateType">新建</el-button>
            </div>
          </template>
          <el-input v-model="typeQuery.dictName" placeholder="搜索字典名称" clearable @change="fetchTypes" style="margin-bottom:12px" />
          <el-table
            v-loading="typeLoading"
            :data="typeList"
            size="small"
            highlight-current-row
            @row-click="handleSelectType"
            style="cursor:pointer"
          >
            <el-table-column prop="dictName" label="字典名称" show-overflow-tooltip />
            <el-table-column prop="dictType" label="类型标识" show-overflow-tooltip />
            <el-table-column label="" width="80">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="handleEditType(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click.stop="handleDeleteType(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="typeQuery.pageNum"
            v-model:page-size="typeQuery.pageSize"
            :total="typeTotal"
            layout="prev, pager, next"
            small
            style="margin-top:8px;justify-content:center"
            @change="fetchTypes"
          />
        </el-card>
      </el-col>

      <!-- Right: Dict Data -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                {{ selectedType ? `字典数据 — ${selectedType.dictName} (${selectedType.dictType})` : '字典数据（请先选择类型）' }}
              </span>
              <el-button type="primary" size="small" :icon="Plus" :disabled="!selectedType" @click="handleCreateData">新建</el-button>
            </div>
          </template>

          <el-empty v-if="!selectedType" description="请从左侧选择一个字典类型" />

          <template v-else>
            <el-table v-loading="dataLoading" :data="dataList" border size="small">
              <el-table-column prop="dictLabel" label="字典标签" min-width="120" />
              <el-table-column prop="dictValue" label="字典值" width="120" />
              <el-table-column prop="dictSort" label="排序" width="70" align="center" />
              <el-table-column label="样式" width="100" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.cssClass" :type="(row.cssClass as any)" size="small">{{ row.dictLabel }}</el-tag>
                  <span v-else>{{ row.dictLabel }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
              <el-table-column label="操作" width="110" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleEditData(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteData(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="dataQuery.pageNum"
              v-model:page-size="dataQuery.pageSize"
              :total="dataTotal"
              layout="total, prev, pager, next"
              small
              style="margin-top:8px;display:flex;justify-content:flex-end"
              @change="fetchData"
            />
          </template>
        </el-card>
      </el-col>
    </el-row>

    <!-- Type Dialog -->
    <el-dialog v-model="typeDialogVisible" :title="typeDialogTitle" width="440px" destroy-on-close>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="字典显示名称" />
        </el-form-item>
        <el-form-item label="类型标识" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="唯一标识，如 sys_gender" :disabled="isTypeEdit" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTypeSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- Data Dialog -->
    <el-dialog v-model="dataDialogVisible" :title="dataDialogTitle" width="480px" destroy-on-close>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="字典值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="存储值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dataForm.dictSort" :min="0" :max="999" style="width:120px" />
        </el-form-item>
        <el-form-item label="标签样式">
          <el-select v-model="dataForm.cssClass" placeholder="选择样式" style="width:100%">
            <el-option v-for="o in cssClassOptions" :key="o.value" :label="o.label" :value="o.value">
              <el-tag v-if="o.value" :type="(o.value as any)" size="small">{{ o.label }}</el-tag>
              <span v-else>{{ o.label }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="dataForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDataSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.dict-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; font-size: 14px; }
</style>
