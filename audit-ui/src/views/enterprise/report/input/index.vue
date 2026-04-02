<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTemplateList, getSubmission, saveDraft, type TplTemplate, type TplSubmission } from '@/api/template'

const templates = ref<TplTemplate[]>([])
const selectedTemplateId = ref<number | null>(null)
const selectedYear = ref<number>(new Date().getFullYear())
const yearOptions = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i)

const loadingDraft = ref(false)
const savingDraft = ref(false)
const currentDraft = ref<TplSubmission | null>(null)
const draftJson = ref('')

async function loadTemplates() {
  const res = await getTemplateList({ status: 1, pageSize: 200 })
  templates.value = res.rows ?? []
}

async function loadDraft() {
  if (!selectedTemplateId.value || !selectedYear.value) return
  loadingDraft.value = true
  currentDraft.value = null
  draftJson.value = ''
  try {
    const sub = await getSubmission(selectedTemplateId.value, selectedYear.value)
    if (sub) {
      currentDraft.value = sub
      draftJson.value = sub.submissionJson ?? ''
    }
  } finally {
    loadingDraft.value = false
  }
}

async function handleSaveDraft() {
  if (!selectedTemplateId.value || !selectedYear.value) {
    ElMessage.warning('请选择模板和审计年度')
    return
  }
  const tpl = templates.value.find(t => t.id === selectedTemplateId.value)
  if (!tpl) return
  savingDraft.value = true
  try {
    const saved = await saveDraft({
      templateId: selectedTemplateId.value,
      auditYear: selectedYear.value,
      submissionJson: draftJson.value || '{}',
      templateVersion: tpl.currentVersion ?? 1,
    })
    currentDraft.value = saved
    ElMessage.success('草稿已保存')
  } finally {
    savingDraft.value = false
  }
}

function onTemplateChange() {
  currentDraft.value = null
  draftJson.value = ''
  loadDraft()
}

onMounted(loadTemplates)
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-form-item label="审计模板" style="margin-bottom:0">
          <el-select
            v-model="selectedTemplateId"
            placeholder="请选择已发布的模板"
            filterable
            clearable
            style="width:280px"
            @change="onTemplateChange"
          >
            <el-option
              v-for="t in templates"
              :key="t.id"
              :label="`${t.templateName}（v${t.currentVersion ?? 1}）`"
              :value="t.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审计年度" style="margin-bottom:0">
          <el-select v-model="selectedYear" style="width:120px" @change="loadDraft">
            <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
        </el-form-item>
        <el-button type="primary" @click="loadDraft" :disabled="!selectedTemplateId">加载草稿</el-button>
      </div>
    </el-card>

    <el-card v-loading="loadingDraft" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">填报内容</span>
          <div>
            <el-tag v-if="currentDraft" :type="currentDraft.status === 1 ? 'success' : 'warning'" style="margin-right:12px">
              {{ currentDraft.status === 1 ? '已提交' : '草稿' }}
            </el-tag>
            <el-button
              type="primary"
              :loading="savingDraft"
              :disabled="!selectedTemplateId || currentDraft?.status === 1"
              @click="handleSaveDraft"
            >
              保存草稿
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="!selectedTemplateId" class="empty-tip">
        <el-empty description="请在上方选择模板和年度" />
      </div>

      <div v-else>
        <el-alert
          v-if="currentDraft?.status === 1"
          type="success"
          :closable="false"
          style="margin-bottom:16px"
          title="该年度数据已提交，无法继续编辑"
        />
        <el-form-item label="填报 JSON（submissionJson）" label-position="top">
          <el-input
            v-model="draftJson"
            type="textarea"
            :rows="16"
            placeholder="请输入 SpreadJS 工作簿 JSON，或直接粘贴采集数据"
            :disabled="currentDraft?.status === 1"
            style="font-family:monospace;font-size:12px"
          />
        </el-form-item>
        <div class="hint-text">
          提示：正式集成 SpreadJS 后，此处将展示在线电子表格编辑器。当前以 JSON 文本方式暂存填报数据。
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
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

.empty-tip {
  padding: 40px 0;
}

.hint-text {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}
</style>
