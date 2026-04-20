<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getEnterpriseSetting, upsertEnterpriseSetting, type EnterpriseSetting } from '@/api/enterpriseSetting'
import { INDUSTRY_CLASSIFICATION, buildIndustryLookup, type IndustryNode } from '@/config/industry-classification'

const saving = ref(false)
const loading = ref(false)

const formRef = ref()
const form = ref<Partial<EnterpriseSetting>>({})

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

/** Display label for the selected industry (code + name) */
const industryDisplayLabel = computed(() => {
  const path = industryCascaderValue.value
  if (!path || path.length === 0) return ''
  const lastCode = path[path.length - 1]
  const entry = industryLookup.get(lastCode)
  return entry ? entry.name : lastCode
})

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

    <el-form ref="formRef" :model="form" label-width="160px" class="setting-form">

      <!-- ── 地区 / 行业 ── -->
      <el-divider content-position="left">地区与行业</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="所属地区">
            <el-input v-model="form.region" placeholder="如：浦东新区" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属领域">
            <el-input v-model="form.industryField" placeholder="如：工业" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="行业分类">
            <el-cascader
              v-model="industryCascaderValue"
              :options="industryOptions"
              :props="{ expandTrigger: 'hover', emitPath: true }"
              filterable
              clearable
              placeholder="请选择行业分类"
              style="width: 100%"
              @change="onIndustryChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位类型">
            <el-input v-model="form.unitNature" placeholder="如：港、澳、台商投资企业" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ── 工商注册 ── -->
      <el-divider content-position="left">工商注册</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="单位注册日期">
            <el-date-picker v-model="form.registeredDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="注册资本（万元）">
            <el-input-number v-model="form.registeredCapital" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ── 法人代表 ── -->
      <el-divider content-position="left">法人代表</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="法定代表人姓名">
            <el-input v-model="form.legalRepresentative" placeholder="请输入法定代表人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系电话（区号）">
            <el-input v-model="form.legalPhone" placeholder="如：021-57501888-2679" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否央企">
            <el-radio-group v-model="form.isCentralEnterprise">
              <el-radio :value="1">是</el-radio>
              <el-radio :value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属集团名称">
            <el-input v-model="form.groupName" placeholder="请输入集团名称" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ── 地址 / 通讯 ── -->
      <el-divider content-position="left">地址与通讯</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="单位地址">
            <el-input v-model="form.enterpriseAddress" placeholder="请输入单位地址" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邮政编码">
            <el-input v-model="form.postalCode" placeholder="请输入邮政编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="行政区划代码">
            <el-input v-model="form.adminDivisionCode" placeholder="如：310115" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="电子邮箱">
            <el-input v-model="form.enterpriseEmail" placeholder="请输入邮箱" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="传真（区号）">
            <el-input v-model="form.fax" placeholder="如：021-57503220" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ── 能源管理 ── -->
      <el-divider content-position="left">能源管理</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="能源管理机构名称">
            <el-input v-model="form.energyMgmtOrg" placeholder="如：节能降耗管理委员会" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="节能领导姓名">
            <el-input v-model="form.energyLeaderName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="节能领导电话">
            <el-input v-model="form.energyLeaderPhone" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源管理负责人">
            <el-input v-model="form.energyManagerName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="负责人手机">
            <el-input v-model="form.energyManagerMobile" placeholder="请输入手机号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源管理师证号">
            <el-input v-model="form.energyManagerCert" placeholder="请输入证号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="能源部门负责人电话">
            <el-input v-model="form.energyDeptLeaderPhone" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ── 能源认证 ── -->
      <el-divider content-position="left">能源认证</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="是否通过能源管理体系认证">
            <el-radio-group v-model="form.energyCert">
              <el-radio :value="1">是</el-radio>
              <el-radio :value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="认证通过日期">
            <el-date-picker v-model="form.certPassDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="认证机构">
            <el-input v-model="form.certAuthority" placeholder="请输入认证机构名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="是否建设能源管理中心">
            <el-radio-group v-model="form.hasEnergyCenter">
              <el-radio :value="1">是</el-radio>
              <el-radio :value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <!-- ── 其他信息 ── -->
      <el-divider content-position="left">其他信息</el-divider>
      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="企业联系人">
            <el-input v-model="form.enterpriseContact" placeholder="请输入联系人姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="企业联系手机">
            <el-input v-model="form.enterpriseMobile" placeholder="请输入手机号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="编制人联系人">
            <el-input v-model="form.compilerContact" placeholder="请输入联系人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="编制人姓名">
            <el-input v-model="form.compilerName" placeholder="请输入姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="编制人手机">
            <el-input v-model="form.compilerMobile" placeholder="请输入手机号" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="编制人邮箱">
            <el-input v-model="form.compilerEmail" placeholder="请输入邮箱" />
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
