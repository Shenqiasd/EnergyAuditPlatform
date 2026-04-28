<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { checkPrerequisites } from '@/api/enterpriseSetting'

const route = useRoute()
const router = useRouter()

const prerequisitePassed = ref(false)
const checking = ref(true)

async function runPrerequisiteCheck() {
  checking.value = true
  try {
    const result = await checkPrerequisites()
    if (!result.passed) {
      const lines = result.errors.map(e => `• ${e}`).join('\n')
      await ElMessageBox.alert(
        `请先完成以下前置配置：\n\n${lines}\n\n请前往「基础设置」完成配置后再导出规定图表。`,
        '前置校验未通过',
        {
          type: 'warning',
          confirmButtonText: '前往设置',
          dangerouslyUseHTMLString: false,
        }
      )
      router.push('/enterprise/settings/company')
      return
    }
    prerequisitePassed.value = true
  } catch (e: any) {
    ElMessage.error('前置校验失败：' + (e?.message ?? '未知错误'))
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  runPrerequisiteCheck()
})

const navItems = [
  { label: '1. 用能单位基本情况', name: 'StandardBasicInfo' },
  { label: '2. 企业概况及主要技术指标一览表', name: 'StandardEnterpriseOverview' },
  { label: '3. 上一轮已实施的节能技改项目表', name: 'StandardRetrofitProjects' },
  { label: '4. 主要用能设备汇总表', name: 'StandardMajorEquipment' },
  { label: '5. 能源流程图', name: 'StandardEnergyFlow' },
  { label: '6. 能源计量器具汇总表', name: 'StandardMeterSummary' },
  { label: '7. 能源计量器具配备率表', name: 'StandardMeterRate' },
  { label: '8. 温室气体排放表', name: 'StandardGhgEmission' },
  { label: '9. 能源消费平衡综合表', name: 'StandardEnergyBalance' },
  { label: '10. 重点用能设备能效对标表', name: 'StandardEquipmentBenchmark' },
  { label: '11. 淘汰产品、设备、装置、工艺和生产能力目录表', name: 'StandardObsoleteEquipment' },
  { label: '12. 企业产品能源成本表', name: 'StandardProductEnergyCost' },
  { label: '13. 设备测试报告主要指标汇总表', name: 'StandardTestIndicators' },
  { label: '14. 节能潜力明细表', name: 'StandardSavingPotential' },
  { label: '15. 能源管理改进建议表', name: 'StandardMgmtSuggestions' },
  { label: '16. 节能技术改造建议汇总表', name: 'StandardRetrofitSuggestions' },
  { label: '17. 节能整改措施表', name: 'StandardRectification' },
  { label: '18. 十五五期间节能目标', name: 'StandardFiveYearTarget' },
]

const currentName = computed(() => route.name as string)
</script>

<template>
  <div v-if="checking" class="loading-area">
    <el-empty description="正在校验前置条件…" />
  </div>
  <div v-else-if="prerequisitePassed" class="standard-layout">
    <aside class="standard-sidebar">
      <div class="sidebar-title">规定图表</div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="nav-item"
          :class="{ active: currentName === item.name }"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </aside>
    <main class="standard-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped lang="scss">
.standard-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

.standard-sidebar {
  width: 280px;
  min-width: 280px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.nav-item {
  padding: 10px 16px;
  font-size: 13px;
  color: #606266;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
  line-height: 1.4;

  &:hover {
    background: #f0f7ff;
    color: #1890ff;
  }

  &.active,
  &.router-link-active {
    background: #e6f4ff;
    color: #1890ff;
    font-weight: 600;
    border-right: 3px solid #1890ff;
  }
}

.standard-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}
</style>
