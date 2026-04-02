<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import * as userApi from '@/api/user'
import type { SysUser } from '@/api/user'

const loading = ref(false)
const tableData = ref<SysUser[]>([])
const total = ref(0)

const query = reactive({
  username: '',
  realName: '',
  userType: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const dialogTitle = ref('新建用户')
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const form = reactive<Partial<SysUser>>({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  userType: 1,
  status: 1,
})

const resetPwdDialogVisible = ref(false)
const resetPwdTarget = ref<SysUser | null>(null)
const newPassword = ref('')

const userTypeMap: Record<number, string> = { 1: '管理员', 2: '审核员' }

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: !isEdit.value, message: '请输入初始密码', trigger: 'blur' }],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res = await userApi.listUsers(query)
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
  query.username = ''
  query.realName = ''
  query.userType = undefined
  query.status = undefined
  query.pageNum = 1
  fetchData()
}

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新建用户'
  Object.assign(form, { id: undefined, username: '', password: '', realName: '', phone: '', email: '', userType: 1, status: 1 })
  dialogVisible.value = true
}

function handleEdit(row: SysUser) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  try {
    const data = { ...form }
    if (isEdit.value) {
      delete data.password
      await userApi.updateUser(data.id!, data)
      ElMessage.success('更新成功')
    } else {
      await userApi.createUser(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {}
}

async function handleToggleStatus(row: SysUser) {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${label}用户「${row.username}」吗？`, `${label}确认`, { type: 'warning' })
    await userApi.updateStatus(row.id!, newStatus)
    ElMessage.success(`已${label}`)
    fetchData()
  } catch {}
}

function handleResetPwd(row: SysUser) {
  resetPwdTarget.value = row
  newPassword.value = ''
  resetPwdDialogVisible.value = true
}

async function handleResetPwdSubmit() {
  if (!resetPwdTarget.value) return
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  try {
    await userApi.resetPassword(resetPwdTarget.value.id!, newPassword.value)
    ElMessage.success('密码已重置')
    resetPwdDialogVisible.value = false
  } catch {}
}

async function handleDelete(row: SysUser) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」吗？`, '删除确认', { type: 'error' })
    await userApi.deleteUser(row.id!)
    ElMessage.success('已删除')
    fetchData()
  } catch {}
}

onMounted(fetchData)
</script>

<template>
  <div class="user-page">
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" placeholder="真实姓名" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-select v-model="query.userType" placeholder="全部" clearable style="width:120px">
            <el-option label="管理员" :value="1" />
            <el-option label="审核员" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
          <span class="card-title">用户列表</span>
          <el-button type="primary" :icon="Plus" @click="handleCreate">新建用户</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="用户类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="primary" size="small">{{ userTypeMap[row.userType] || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="lastLoginTime" label="最后登录" width="170" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'info' : 'success'" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请设置初始密码" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="form.userType" style="width:100%">
            <el-option label="管理员" :value="1" />
            <el-option label="审核员" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- Reset Password Dialog -->
    <el-dialog v-model="resetPwdDialogVisible" title="重置密码" width="380px" destroy-on-close>
      <el-form label-width="80px" style="padding-right:20px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwdSubmit">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.user-page { padding: 20px; }
.search-card :deep(.el-card__body) { padding-bottom: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; font-size: 15px; }
</style>
