<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  getUnitList,
  createUnit,
  updateUnit,
  removeUnit,
  getUnitEnergies,
  addUnitEnergy,
  removeUnitEnergy,
  getEnergyList,
  type BsUnit,
  type BsUnitEnergy,
  type BsEnergy,
} from '@/api/setting'
import { getDataByType, type DictData } from '@/api/dict'

const TABS = [
  { label: '加工转换', unitType: 1 },
  { label: '分配输送', unitType: 2 },
  { label: '终端使用', unitType: 3 },
]

const activeTab = ref('1')
const loading = ref(false)
const tableData = ref<BsUnit[]>([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 20 })

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitting = ref(false)
const formRef = ref()
const form = ref<Partial<BsUnit>>({})
const rules = {
  name: [{ required: true, message: '请输入单元名称', trigger: 'blur' }],
}

const currentTabType = computed(() => Number(activeTab.value))
const isEndUse = computed(() => (dialogVisible.value ? form.value.unitType === 3 : currentTabType.value === 3))

const energyOptions = ref<BsEnergy[]>([])
const endUseCategoryOptions = ref<DictData[]>([])

const expandedUnitEnergies = ref<Record<number, BsUnitEnergy[]>>({})
const loadingEnergies = ref<Record<number, boolean>>({})
const selectedAddEnergy = ref<Record<number, number | null>>({})
const addingEnergy = ref<Record<number, boolean>>({})

async function loadData() {
  loading.value = true
  try {
    const res = await getUnitList({ ...query.value, unitType: currentTabType.value })
    tableData.value = res.rows ?? []
    total.value = Number(res.total ?? 0)
  } finally {
    loading.value = false
  }
}

async function loadEnergyOptions() {
  const res = await getEnergyList({ pageSize: 500 })
  energyOptions.value = res.rows ?? []
}

async function loadEndUseCategoryOptions() {
  try {
    endUseCategoryOptions.value = await getDataByType('unit_terminal_category')
  } catch {
    endUseCategoryOptions.value = []
  }
}

function onTabChange() {
  query.value.pageNum = 1
  expandedUnitEnergies.value = {}
  selectedAddEnergy.value = {}
  addingEnergy.value = {}
  loadingEnergies.value = {}
  loadData()
}

function openCreate() {
  form.value = { unitType: currentTabType.value }
  dialogTitle.value = '新增用能单元'
  dialogVisible.value = true
}

function openEdit(row: BsUnit) {
  form.value = { ...row }
  dialogTitle.value = '编辑用能单元'
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
      await updateUnit(form.value.id, form.value)
    } else {
      await createUnit(form.value)
    }
    ElMessage.success('保存成功')
    handleClose()
    loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: BsUnit) {
  await ElMessageBox.confirm(`确认删除用能单元「${row.name}」？`, '删除确认', { type: 'warning' })
  await removeUnit(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

async function onExpandChange(row: BsUnit, expandedRows: BsUnit[]) {
  const expanding = expandedRows.some(r => r.id === row.id)
  if (!expanding || !row.id) return
  if (expandedUnitEnergies.value[row.id] !== undefined) return
  loadingEnergies.value[row.id] = true
  try {
    expandedUnitEnergies.value[row.id] = await getUnitEnergies(row.id)
  } finally {
    loadingEnergies.value[row.id] = false
  }
}

async function handleAddEnergy(unitId: number) {
  const energyId = selectedAddEnergy.value[unitId]
  if (!energyId) return
  addingEnergy.value[unitId] = true
  try {
    await addUnitEnergy(unitId, energyId)
    ElMessage.success('关联成功')
    selectedAddEnergy.value[unitId] = null
    expandedUnitEnergies.value[unitId] = await getUnitEnergies(unitId)
  } finally {
    addingEnergy.value[unitId] = false
  }
}

async function handleRemoveEnergy(unitId: number, energyId: number) {
  await removeUnitEnergy(unitId, energyId)
  ElMessage.success('已移除')
  expandedUnitEnergies.value[unitId] = await getUnitEnergies(unitId)
}

function availableEnergies(unitId: number) {
  const linked = expandedUnitEnergies.value[unitId] ?? []
  const linkedIds = new Set(linked.map(ue => ue.energyId))
  return energyOptions.value.filter(e => !linkedIds.has(e.id!))
}

onMounted(async () => {
  await Promise.all([loadData(), loadEnergyOptions(), loadEndUseCategoryOptions()])
})
</script>

<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" @tab-change="onTabChange" class="unit-tabs">
      <el-tab-pane
        v-for="tab in TABS"
        :key="tab.unitType"
        :label="tab.label"
        :name="String(tab.unitType)"
      >
        <div class="tab-toolbar">
          <el-button type="primary" :icon="Plus" @click="openCreate">新增</el-button>
        </div>

        <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          row-key="id"
          @expand-change="onExpandChange"
        >
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="energy-panel" v-if="row.id">
                <div class="energy-add-row">
                  <el-select
                    v-model="selectedAddEnergy[row.id]"
                    placeholder="选择能源品种"
                    filterable
                    clearable
                    style="width:260px"
                  >
                    <el-option
                      v-for="e in availableEnergies(row.id)"
                      :key="e.id"
                      :label="`${e.name}（${e.measurementUnit ?? '-'}）`"
                      :value="e.id!"
                    />
                  </el-select>
                  <el-button
                    type="primary"
                    size="small"
                    :loading="addingEnergy[row.id]"
                    @click="handleAddEnergy(row.id)"
                  >
                    关联
                  </el-button>
                </div>

                <div class="energy-tags" v-if="!loadingEnergies[row.id]">
                  <template v-if="expandedUnitEnergies[row.id]?.length">
                    <el-tag
                      v-for="ue in expandedUnitEnergies[row.id]"
                      :key="ue.energyId"
                      closable
                      size="default"
                      type="info"
                      @close="handleRemoveEnergy(row.id, ue.energyId)"
                      style="margin:4px"
                    >
                      {{ ue.energyName ?? `能源#${ue.energyId}` }}
                      <span v-if="ue.measurementUnit" class="unit-label">（{{ ue.measurementUnit }}）</span>
                    </el-tag>
                  </template>
                  <span v-else class="no-energy">暂未关联能源品种，请在上方选择后点击「关联」</span>
                </div>
                <el-skeleton v-else :rows="1" animated style="margin-top:8px" />
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="name" label="单元名称" min-width="160" />
          <el-table-column prop="subCategory" label="子类别" width="140" />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="操作" width="160" fixed="right">
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
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          style="margin-top:16px"
          @change="loadData"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="handleClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="单元名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入用能单元名称" />
        </el-form-item>

        <el-form-item label="子类别">
          <template v-if="isEndUse && endUseCategoryOptions.length">
            <el-select
              v-model="form.subCategory"
              placeholder="请选择终端使用子类别"
              clearable
              style="width:100%"
            >
              <el-option
                v-for="item in endUseCategoryOptions"
                :key="item.dictValue"
                :label="item.dictLabel"
                :value="item.dictValue"
              />
            </el-select>
          </template>
          <template v-else>
            <el-input
              v-model="form.subCategory"
              :placeholder="isEndUse ? '请输入终端使用子类别（如：照明、动力、工艺等）' : '请输入子类别（选填）'"
            />
          </template>
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

.unit-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}

.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.energy-panel {
  padding: 12px 24px 12px 48px;
  background: #fafafa;
  border-top: 1px solid #ebeef5;
}

.energy-add-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.energy-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  min-height: 32px;
}

.no-energy {
  color: #909399;
  font-size: 13px;
}

.unit-label {
  font-size: 11px;
  color: #606266;
}
</style>
