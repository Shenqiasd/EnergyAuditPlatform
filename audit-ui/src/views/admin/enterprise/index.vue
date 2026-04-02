<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import * as enterpriseApi from '@/api/enterprise'
import type { Enterprise } from '@/api/enterprise'

const loading = ref(false)
const tableData = ref<Enterprise[]>([])
const total = ref(0)

const query = reactive({
  enterpriseName: '',
  creditCode: '',
  isLocked: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新建企业')
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<Partial<Enterprise>>({
  enterpriseName: '',
  creditCode: '',
  contactPerson: '',
  contactEmail: '',
  contactPhone: '',
  remark: '',
})

const drawerVisible = ref(false)
const drawerData = ref<Enterprise | null>(null)

const expireDialogVisible = ref(false)
const expireTarget = ref<Enterprise | null>(null)
const expireDate = ref('')

const rules = {
  enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await enterpriseApi.getList(query)
    tableData.value = res.list
    total.value = res.total
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchData()
}

function handleReset() {
  query.enterpriseName = ''
  query.creditCode = ''
  query.isLocked = undefined
  query.pageNum = 1
  fetchData()
}

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新建企业'
  Object.assign(form, { id: undefined, enterpriseName: '', creditCode: '', contactPerson: '', contactEmail: '', contactPhone: '', remark: '' })
  dialogVisible.value = true
}

function handleEdit(row: Enterprise) {
  isEdit.value = true
  dialogTitle.value = '编辑企业'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  try {
    if (isEdit.value && form.id) {
      await enterpriseApi.update(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await enterpriseApi.create(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {}
}

function handleViewDetail(row: Enterprise) {
  drawerData.value = { ...row }
  drawerVisible.value = true
}

async function handleLock(row: Enterprise) {
  try {
    await ElMessageBox.confirm(`确认锁定企业「${row.enterpriseName}」吗？锁定后该企业将无法登录。`, '锁定确认', { type: 'warning' })
    await enterpriseApi.lock(row.id!)
    ElMessage.success('已锁定')
    fetchData()
  } catch {}
}

async function handleUnlock(row: Enterprise) {
  try {
    await ElMessageBox.confirm(`确认解锁企业「${row.enterpriseName}」吗？`, '解锁确认', { type: 'info' })
    await enterpriseApi.unlock(row.id!)
    ElMessage.success('已解锁')
    fetchData()
  } catch {}
}

function handleExpire(row: Enterprise) {
  expireTarget.value = row
  expireDate.value = row.expireDate || ''
  expireDialogVisible.value = true
}

async function handleExpireSubmit() {
  if (!expireTarget.value) return
  try {
    await enterpriseApi.updateExpireDate(expireTarget.value.id!, expireDate.value)
    ElMessage.success('已更新到期日期')
    expireDialogVisible.value = false
    fetchData()
  } catch {}
}

async function handleDelete(row: Enterprise) {
  try {
    await ElMessageBox.confirm(`确认删除企业「${row.enterpriseName}」吗？此操作不可恢复。`, '删除确认', { type: 'error' })
    await enterpriseApi.remove(row.id!)
    ElMessage.success('已删除')
    fetchData()
  } catch {}
}

function statusTag(row: Enterprise) {
  if (row.isLocked === 1) return { type: 'danger', label: '已锁定' }
  if (row.isActive === 0) return { type: 'info', label: '已停用' }
  return { type: 'success', label: '正常' }
}

onMounted(fetchData)
</script>

<template>
  <div class="enterprise-page">
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="企业名称">
          <el-input v-model="query.enterpriseName" placeholder="企业名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="信用代码">
          <el-input v-model="query.creditCode" placeholder="统一信用代码" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isLocked" placeholder="全部" clearable style="width:120px">
            <el-option label="正常" :value="0" />
            <el-option label="已锁定" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top:16px">
      <template #header>
        <div class="card-header">
          <span class="card-title">企业列表</span>
          <el-button type="primary" :icon="Plus" @click="handleCreate">新建企业</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="creditCode" label="统一信用代码" width="190" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="expireDate" label="到期日期" width="120">
          <template #default="{ row }">{{ row.expireDate || '未设置' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(statusTag(row).type as any)" size="small">{{ statusTag(row).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.isLocked !== 1" link type="warning" size="small" @click="handleLock(row)">锁定</el-button>
            <el-button v-else link type="success" size="small" @click="handleUnlock(row)">解锁</el-button>
            <el-button link type="info" size="small" @click="handleExpire(row)">到期日期</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;display:flex;justify-content:flex-end"
        @change="fetchData"
      />
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="企业名称" prop="enterpriseName">
          <el-input v-model="form.enterpriseName" placeholder="请输入企业全称" />
        </el-form-item>
        <el-form-item label="信用代码" prop="creditCode">
          <el-input v-model="form.creditCode" placeholder="统一社会信用代码（18位）" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.contactEmail" placeholder="联系邮箱" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- Detail Drawer -->
    <el-drawer v-model="drawerVisible" title="企业详情" size="480px" destroy-on-close>
      <el-descriptions v-if="drawerData" :column="1" border>
        <el-descriptions-item label="企业名称">{{ drawerData.enterpriseName }}</el-descriptions-item>
        <el-descriptions-item label="统一信用代码">{{ drawerData.creditCode }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ drawerData.contactPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ drawerData.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ drawerData.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ drawerData.expireDate || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="锁定状态">
          <el-tag :type="drawerData.isLocked === 1 ? 'danger' : 'success'" size="small">
            {{ drawerData.isLocked === 1 ? '已锁定' : '正常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ drawerData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="() => { handleEdit(drawerData!); drawerVisible = false }">编辑</el-button>
      </template>
    </el-drawer>

    <!-- Expire Date Dialog -->
    <el-dialog v-model="expireDialogVisible" title="设置到期日期" width="380px" destroy-on-close>
      <el-form label-width="80px" style="padding-right:20px">
        <el-form-item label="到期日期">
          <el-date-picker v-model="expireDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择到期日期" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="expireDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleExpireSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.enterprise-page { padding: 20px; }
.search-card :deep(.el-card__body) { padding-bottom: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; font-size: 15px; }
</style>
