<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import * as registrationApi from '@/api/registration'
import type { Registration } from '@/api/registration'

const loading = ref(false)
const tableData = ref<Registration[]>([])
const total = ref(0)

const query = reactive({
  enterpriseName: '',
  creditCode: '',
  auditStatus: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10,
})

const detailDrawerVisible = ref(false)
const detailData = ref<Registration | null>(null)

const auditDialogVisible = ref(false)
const auditTarget = ref<Registration | null>(null)
const auditAction = ref<'approve' | 'reject'>('approve')
const auditFormRef = ref<FormInstance>()
const auditForm = reactive({ auditRemark: '' })

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '待审核', type: 'warning' },
  1: { label: '已通过', type: 'success' },
  2: { label: '已驳回', type: 'danger' },
}

async function fetchData() {
  loading.value = true
  try {
    const res = await registrationApi.getList(query)
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
  query.auditStatus = undefined
  query.pageNum = 1
  fetchData()
}

function handleViewDetail(row: Registration) {
  detailData.value = { ...row }
  detailDrawerVisible.value = true
}

function handleAudit(row: Registration, action: 'approve' | 'reject') {
  auditTarget.value = row
  auditAction.value = action
  auditForm.auditRemark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  if (!auditTarget.value) return
  try {
    if (auditAction.value === 'approve') {
      await registrationApi.approve(auditTarget.value.id!, auditForm.auditRemark)
      ElMessage.success('已通过审核，企业账号已自动创建')
    } else {
      if (!auditForm.auditRemark) {
        ElMessage.warning('驳回时请填写驳回原因')
        return
      }
      await registrationApi.reject(auditTarget.value.id!, auditForm.auditRemark)
      ElMessage.success('已驳回')
    }
    auditDialogVisible.value = false
    fetchData()
  } catch {}
}

onMounted(fetchData)
</script>

<template>
  <div class="registration-page">
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="企业名称">
          <el-input v-model="query.enterpriseName" placeholder="企业名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="信用代码">
          <el-input v-model="query.creditCode" placeholder="统一信用代码" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width:120px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
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
          <span class="card-title">注册申请列表</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="applyNo" label="申请编号" width="190" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="creditCode" label="统一信用代码" width="190" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="applyTime" label="申请时间" width="170" />
        <el-table-column label="审核状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(statusMap[row.auditStatus]?.type as any) || 'info'" size="small">
              {{ statusMap[row.auditStatus]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
            <template v-if="row.auditStatus === 0">
              <el-button link type="success" size="small" @click="handleAudit(row, 'approve')">通过</el-button>
              <el-button link type="danger" size="small" @click="handleAudit(row, 'reject')">驳回</el-button>
            </template>
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

    <!-- Detail Drawer -->
    <el-drawer v-model="detailDrawerVisible" title="申请详情" size="520px" destroy-on-close>
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="申请编号">{{ detailData.applyNo }}</el-descriptions-item>
        <el-descriptions-item label="企业名称">{{ detailData.enterpriseName }}</el-descriptions-item>
        <el-descriptions-item label="统一信用代码">{{ detailData.creditCode }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detailData.contactPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailData.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ detailData.contactEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detailData.applyTime }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="(statusMap[detailData.auditStatus!]?.type as any)" size="small">
            {{ statusMap[detailData.auditStatus!]?.label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.auditTime" label="审核时间">{{ detailData.auditTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.auditRemark" label="审核备注">{{ detailData.auditRemark }}</el-descriptions-item>
      </el-descriptions>
      <template v-if="detailData?.auditStatus === 0" #footer>
        <el-button type="success" @click="() => { handleAudit(detailData!, 'approve'); detailDrawerVisible = false }">通过</el-button>
        <el-button type="danger" @click="() => { handleAudit(detailData!, 'reject'); detailDrawerVisible = false }">驳回</el-button>
      </template>
    </el-drawer>

    <!-- Audit Dialog -->
    <el-dialog
      v-model="auditDialogVisible"
      :title="auditAction === 'approve' ? '审核通过确认' : '驳回申请'"
      width="440px"
      destroy-on-close
    >
      <div v-if="auditAction === 'approve'" class="approve-info">
        <el-alert
          title="审核通过后，系统将自动为该企业创建账号。初始密码为统一信用代码后6位。"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom:16px"
        />
      </div>
      <el-form ref="auditFormRef" :model="auditForm" label-width="80px">
        <el-form-item label="备注">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="3"
            :placeholder="auditAction === 'reject' ? '请填写驳回原因（必填）' : '备注（选填）'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button :type="auditAction === 'approve' ? 'success' : 'danger'" @click="handleAuditSubmit">
          {{ auditAction === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.registration-page { padding: 20px; }
.search-card :deep(.el-card__body) { padding-bottom: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; font-size: 15px; }
</style>
