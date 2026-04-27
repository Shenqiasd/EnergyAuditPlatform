<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'
import {
  listEnergyCatalog,
  createEnergyCatalog,
  updateEnergyCatalog,
  deleteEnergyCatalog,
  type EnergyCatalog,
} from '@/api/admin'

const loading = ref(false)
const tableData = ref<EnergyCatalog[]>([])
const total = ref(0)

const query = ref({ name: '', category: '', isActive: undefined as number | undefined, pageNum: 1, pageSize: 15 })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)

const formRef = ref()
const form = ref<Partial<EnergyCatalog>>({})
const rules = {
  name: [{ required: true, message: '请输入品种名称', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await listEnergyCatalog(query.value)
    tableData.value = res.rows ?? []
    total.value = Number(res.total ?? 0)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.value.pageNum = 1
  loadData()
}

function handleReset() {
  query.value = { name: '', category: '', isActive: undefined, pageNum: 1, pageSize: 15 }
  loadData()
}

function openCreate() {
  form.value = { isActive: 1, sortOrder: 0 }
  dialogTitle.value = '新增能源品种'
  dialogVisible.value = true
}

function openEdit(row: EnergyCatalog) {
  form.value = { ...row }
  dialogTitle.value = '编辑能源品种'
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
      await updateEnergyCatalog(form.value.id, form.value)
    } else {
      await createEnergyCatalog(form.value)
    }
    ElMessage.success('保存成功')
    handleClose()
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: EnergyCatalog) {
  await ElMessageBox.confirm(`确认删除能源品种「${row.name}」？`, '删除确认', { type: 'warning' })
  await deleteEnergyCatalog(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <el-form :model="query" inline>
        <el-form-item label="品种名称">
          <el-input v-model="query.name" placeholder="请输入名称" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="品类">
          <el-input v-model="query.category" placeholder="请输入品类" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isActive" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="tableData" border stripe>
      <el-table-column prop="name" label="品种名称" min-width="140" />
      <el-table-column prop="category" label="品类" width="120" />
      <el-table-column prop="attribution" label="归属" width="110">
        <template #default="{ row }">
          <el-tag
            :type="row.attribution === '化石燃料' ? 'warning' : row.attribution === '非化石燃料' ? 'success' : 'info'"
            size="small"
          >
            {{ row.attribution ?? '—' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="measurementUnit" label="计量单位" width="100" />
      <el-table-column prop="equivalentValue" label="折标系数(当量值)" width="140" />
      <el-table-column prop="equalValue" label="折标系数(等价值)" width="140" />
      <el-table-column prop="carbonContent" label="含碳量" width="100" />
      <el-table-column prop="oxidationRate" label="氧化率" width="90" />
      <el-table-column prop="isActive" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.isActive === 1 ? 'success' : 'info'">
            {{ row.isActive === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 15, 30, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top:16px"
      @change="loadData"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="品种名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入品种名称" />
        </el-form-item>
        <el-form-item label="品类">
          <el-input v-model="form.category" placeholder="如：煤炭、油品、天然气" />
        </el-form-item>
        <el-form-item label="归属">
          <el-radio-group v-model="form.attribution">
            <el-radio value="化石燃料">化石燃料</el-radio>
            <el-radio value="非化石燃料">非化石燃料</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="form.measurementUnit" placeholder="如：吨、立方米" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="折标系数(当量值)">
              <el-input-number v-model="form.equivalentValue" :precision="4" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="折标系数(等价值)">
              <el-input-number v-model="form.equalValue" :precision="4" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="低位热值">
              <el-input-number v-model="form.lowHeatValue" :precision="4" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="含碳量">
              <el-input-number v-model="form.carbonContent" :precision="4" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="氧化率">
              <el-input-number v-model="form.oxidationRate" :precision="4" :min="0" :max="1" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="form.isActive">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width:120px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
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
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
</style>
