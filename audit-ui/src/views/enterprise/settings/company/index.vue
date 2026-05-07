<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getEnterpriseSetting, upsertEnterpriseSetting, type EnterpriseSetting } from '@/api/enterpriseSetting'
import { INDUSTRY_CLASSIFICATION, buildIndustryLookup, type IndustryNode } from '@/config/industry-classification'
import { notifyEnterpriseSettingUpdated } from '@/utils/enterprise-setting-events'
import {
  REGION_OPTIONS,
  FIELD_OPTIONS,
  UNIT_TYPE_OPTIONS,
  ENERGY_USAGE_TYPE_OPTIONS,
  SUPERIOR_DEPT_GROUPS
} from '@/config/enterprise-options'

const saving = ref(false)
const loading = ref(false)

const formRef = ref()
const form = ref<Partial<EnterpriseSetting>>({})

// ── Validation rules ──
const rules = computed(() => ({
  region:                 [{ required: true, message: '请选择所属地区', trigger: 'change' }],
  industryField:          [{ required: true, message: '请选择所属领域', trigger: 'change' }],
  unitNature:             [{ required: true, message: '请选择单位类型', trigger: 'change' }],
  energyUsageType:        [{ required: true, message: '请选择用能企业类型', trigger: 'change' }],
  registeredDate:         [{ required: true, message: '请选择单位注册日期', trigger: 'change' }],
  registeredCapital:      [{ required: true, message: '请输入注册资本', trigger: 'blur' }],
  legalRepresentative:    [{ required: true, message: '请输入法定代表人姓名', trigger: 'blur' }],
  legalPhone:             [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  superiorDepartment:     [{ required: true, message: '请选择上级主管部门', trigger: 'change' }],
  enterpriseAddress:      [{ required: true, message: '请输入单位地址', trigger: 'blur' }],
  postalCode:             [{ required: true, message: '请输入邮政编码', trigger: 'blur' }],
  enterpriseEmail:        [{ required: true, message: '请输入电子邮箱', trigger: 'blur' }],
  energyMgmtOrg:          [{ required: true, message: '请输入能源管理机构名称', trigger: 'blur' }],
  energyLeaderName:       [{ required: true, message: '请输入单位主管节能领导姓名', trigger: 'blur' }],
  energyLeaderPhone:      [{ required: true, message: '请输入单位主管节能领导电话', trigger: 'blur' }],
  energyLeaderTitle:      [{ required: true, message: '请输入单位主管节能领导职务', trigger: 'blur' }],
  energyDeptName:         [{ required: true, message: '请输入节能主管部门名称', trigger: 'blur' }],
  energyManagerName:      [{ required: true, message: '请输入能源管理负责人姓名', trigger: 'blur' }],
  energyManagerMobile:    [{ required: true, message: '请输入能源管理负责人电话', trigger: 'blur' }],
  energyAuditContactName: [{ required: true, message: '请输入能源审计联系人姓名', trigger: 'blur' }],
  energyAuditContactPhone:[{ required: true, message: '请输入能源审计联系人电话', trigger: 'blur' }],
  compilerContact:        [{ required: true, message: '请输入能源审计报告编制单位', trigger: 'blur' }],
  compilerName:           [{ required: true, message: '请输入编制单位联系人姓名', trigger: 'blur' }],
  compilerMobile:         [{ required: true, message: '请输入编制单位联系人电话', trigger: 'blur' }],
  compilerEmail:          [{ required: true, message: '请输入编制单位联系人邮箱', trigger: 'blur' }],
  energyCert:             [{ required: true, message: '请选择是否通过认证', trigger: 'change' }],
  certPassDate:           form.value.energyCert === 1 ? [{ required: true, message: '请选择通过日期', trigger: 'change' }] : [],
  certAuthority:          form.value.energyCert === 1 ? [{ required: true, message: '请输入认证机构', trigger: 'blur' }] : [],
  hasEnergyCenter:        [{ required: true, message: '请选择是否建设能源管理中心', trigger: 'change' }],
}))

// ── Industry cascading selector ──
const industryLookup = buildIndustryLookup()
const industryOptions: IndustryNode[] = INDUSTRY_CLASSIFICATION

/** Current cascader selection path, e.g. ['C', 'C28', 'C281'] */
const industryCascaderValue = ref<string[]>([])

/** Resolve stored industryCode back to cascader path on load */
function resolveIndustryPath(code: string | undefined | null): string[] {
  if (!code) return []
  const entry = industryLookup.get(code)
  return entry ? [...entry.fullPath] : []
}

/** Handle cascader selection change */
function onIndustryChange(value: string[] | null) {
  if (!value || value.length === 0) {
    form.value.industryCode = undefined
    form.value.industryName = undefined
    form.value.industryCategory = undefined
    return
  }
  const selectedCode = value[value.length - 1]
  const entry = industryLookup.get(selectedCode)
  if (entry) {
    // Extract just the name part (after the code prefix)
    const nameOnly = entry.name.replace(/^\S+\s+/, '')
    form.value.industryCode = selectedCode
    form.value.industryName = nameOnly
    // Store the 大类 code (second level) for category
    form.value.industryCategory = value.length >= 2 ? value[1] : value[0]
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getEnterpriseSetting()
    form.value = res ?? {}
    // Restore cascader path from stored industry code
    industryCascaderValue.value = resolveIndustryPath(form.value.industryCode)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await upsertEnterpriseSetting(form.value)
    notifyEnterpriseSettingUpdated()
    ElMessage.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <span class="title">企业概况</span>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="180px" class="setting-form">

      <!-- ══════ 大类-基本信息 ══════ -->
      <el-divider content-position="left">基本信息</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="所属地区" prop="region">
            <el-select v-model="form.region" placeholder="请选择所属地区" style="width:100%" filterable>
              <el-option v-for="item in REGION_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属领域" prop="industryField">
            <el-select v-model="form.industryField" placeholder="请选择所属领域" style="width:100%">
              <el-option v-for="item in FIELD_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="行业分类">
            <el-cascader
              v-model="industryCascaderValue"
              :options="(industryOptions as unknown as import('element-plus').CascaderOption[])"
              :props="{ expandTrigger: 'hover', emitPath: true }"
              filterable
              clearable
              placeholder="请选择行业分类"
              style="width: 100%"
              @change="(value: unknown) => onIndustryChange(value as string[] | null)"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位类型" prop="unitNature">
            <el-select v-model="form.unitNature" placeholder="请选择单位类型" style="width:100%" filterable>
              <el-option v-for="item in UNIT_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="用能企业类型" prop="energyUsageType">
            <el-select v-model="form.energyUsageType" placeholder="请选择用能企业类型" style="width:100%">
              <el-option v-for="item in ENERGY_USAGE_TYPE_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位注册日期" prop="registeredDate">
            <el-date-picker v-model="form.registeredDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="注册资本（万元）" prop="registeredCapital">
            <el-input-number v-model="form.registeredCapital" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="法定代表人姓名" prop="legalRepresentative">
            <el-input v-model="form.legalRepresentative" placeholder="请输入法定代表人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话（区号）" prop="legalPhone">
            <el-input v-model="form.legalPhone" placeholder="如：021-57501888-2679" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="上级主管部门" prop="superiorDepartment">
            <el-select v-model="form.superiorDepartment" placeholder="请选择上级主管部门" style="width:100%" filterable>
              <el-option-group v-for="group in SUPERIOR_DEPT_GROUPS" :key="group.label" :label="group.label">
                <el-option v-for="item in group.options" :key="item" :label="item" :value="item" />
              </el-option-group>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位地址" prop="enterpriseAddress">
            <el-input v-model="form.enterpriseAddress" placeholder="请输入单位地址" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮政编码" prop="postalCode">
            <el-input v-model="form.postalCode" placeholder="请输入邮政编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="电子邮箱" prop="enterpriseEmail">
            <el-input v-model="form.enterpriseEmail" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="传真（区号）">
            <el-input v-model="form.fax" placeholder="如：021-57503220" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ══════ 大类-能源管理 ══════ -->
      <el-divider content-position="left">能源管理</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="能源管理机构名称" prop="energyMgmtOrg">
            <el-input v-model="form.energyMgmtOrg" placeholder="如：节能降耗管理委员会" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位主管节能领导姓名" prop="energyLeaderName">
            <el-input v-model="form.energyLeaderName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位主管节能领导电话" prop="energyLeaderPhone">
            <el-input v-model="form.energyLeaderPhone" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位主管节能领导职务" prop="energyLeaderTitle">
            <el-input v-model="form.energyLeaderTitle" placeholder="请输入职务" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="节能主管部门名称" prop="energyDeptName">
            <el-input v-model="form.energyDeptName" placeholder="请输入部门名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源管理负责人姓名" prop="energyManagerName">
            <el-input v-model="form.energyManagerName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源管理负责人电话" prop="energyManagerMobile">
            <el-input v-model="form.energyManagerMobile" placeholder="请输入电话" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源审计联系人姓名" prop="energyAuditContactName">
            <el-input v-model="form.energyAuditContactName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源审计联系人电话" prop="energyAuditContactPhone">
            <el-input v-model="form.energyAuditContactPhone" placeholder="请输入电话" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源审计报告编制单位" prop="compilerContact">
            <el-input v-model="form.compilerContact" placeholder="请输入编制单位名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="编制单位联系人姓名" prop="compilerName">
            <el-input v-model="form.compilerName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="编制单位联系人电话" prop="compilerMobile">
            <el-input v-model="form.compilerMobile" placeholder="请输入电话" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="编制单位联系人邮箱" prop="compilerEmail">
            <el-input v-model="form.compilerEmail" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否通过能源管理体系认证" prop="energyCert">
            <el-radio-group v-model="form.energyCert">
              <el-radio :value="1">是</el-radio>
              <el-radio :value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="通过日期" prop="certPassDate">
            <el-date-picker v-model="form.certPassDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="认证机构" prop="certAuthority">
            <el-input v-model="form.certAuthority" placeholder="请输入认证机构名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否建设能源管理中心" prop="hasEnergyCenter">
            <el-radio-group v-model="form.hasEnergyCenter">
              <el-radio :value="1">是</el-radio>
              <el-radio :value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
  max-width: 1000px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  .title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
  }
}
.setting-form {
  background: #fff;
  border-radius: 8px;
  padding: 24px 32px 8px;
}
</style>
