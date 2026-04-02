import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false },
  },
  // Enterprise portal
  {
    path: '/enterprise',
    component: () => import('@/layouts/EnterpriseLayout.vue'),
    redirect: '/enterprise/dashboard',
    meta: { requiresAuth: true, portal: 'enterprise', userType: 3 },
    children: [
      { path: 'dashboard', name: 'EnterpriseDashboard', component: () => import('@/views/enterprise/dashboard/index.vue'), meta: { title: '工作台' } },
      // Settings 3.x
      { path: 'settings/company', name: 'CompanySettings', component: () => import('@/views/enterprise/settings/company/index.vue'), meta: { title: '企业信息' } },
      { path: 'settings/energy', name: 'EnergySettings', component: () => import('@/views/enterprise/settings/energy/index.vue'), meta: { title: '能源品种' } },
      { path: 'settings/unit', name: 'UnitSettings', component: () => import('@/views/enterprise/settings/unit/index.vue'), meta: { title: '用能单元' } },
      { path: 'settings/product', name: 'ProductSettings', component: () => import('@/views/enterprise/settings/product/index.vue'), meta: { title: '产品设置' } },
      // Data entry 4.x
      { path: 'entry/overview', name: 'EntryOverview', component: () => import('@/views/enterprise/entry/overview/index.vue'), meta: { title: '企业概况' } },
      { path: 'entry/indicators', name: 'EntryIndicators', component: () => import('@/views/enterprise/entry/indicators/index.vue'), meta: { title: '主要技术指标' } },
      { path: 'entry/projects', name: 'EntryProjects', component: () => import('@/views/enterprise/entry/projects/index.vue'), meta: { title: '节能技改项目' } },
      { path: 'entry/meters', name: 'EntryMeters', component: () => import('@/views/enterprise/entry/meters/index.vue'), meta: { title: '计量器具汇总' } },
      { path: 'entry/meter-rate', name: 'EntryMeterRate', component: () => import('@/views/enterprise/entry/meter-rate/index.vue'), meta: { title: '计量器具配备率' } },
      { path: 'entry/benchmark', name: 'EntryBenchmark', component: () => import('@/views/enterprise/entry/benchmark/index.vue'), meta: { title: '能效对标' } },
      { path: 'entry/equipment-energy', name: 'EntryEquipmentEnergy', component: () => import('@/views/enterprise/entry/equipment-energy/index.vue'), meta: { title: '重点设备能耗' } },
      { path: 'entry/equipment-summary', name: 'EntryEquipmentSummary', component: () => import('@/views/enterprise/entry/equipment-summary/index.vue'), meta: { title: '主要用能设备' } },
      { path: 'entry/equipment-test', name: 'EntryEquipmentTest', component: () => import('@/views/enterprise/entry/equipment-test/index.vue'), meta: { title: '重点设备测试' } },
      { path: 'entry/obsolete', name: 'EntryObsolete', component: () => import('@/views/enterprise/entry/obsolete/index.vue'), meta: { title: '淘汰设备目录' } },
      { path: 'entry/energy-flow', name: 'EntryEnergyFlow', component: () => import('@/views/enterprise/entry/energy-flow/index.vue'), meta: { title: '能源流程图' } },
      { path: 'entry/product-consumption', name: 'EntryProductConsumption', component: () => import('@/views/enterprise/entry/product-consumption/index.vue'), meta: { title: '单位产品能耗' } },
      { path: 'entry/product-cost', name: 'EntryProductCost', component: () => import('@/views/enterprise/entry/product-cost/index.vue'), meta: { title: '产品能源成本' } },
      { path: 'entry/saving-calc', name: 'EntrySavingCalc', component: () => import('@/views/enterprise/entry/saving-calc/index.vue'), meta: { title: '节能量计算' } },
      { path: 'entry/ghg-emission', name: 'EntryGhgEmission', component: () => import('@/views/enterprise/entry/ghg-emission/index.vue'), meta: { title: '温室气体排放' } },
      { path: 'entry/waste-heat', name: 'EntryWasteHeat', component: () => import('@/views/enterprise/entry/waste-heat/index.vue'), meta: { title: '余热资源利用' } },
      { path: 'entry/saving-potential', name: 'EntrySavingPotential', component: () => import('@/views/enterprise/entry/saving-potential/index.vue'), meta: { title: '节能潜力明细' } },
      { path: 'entry/management-policy', name: 'EntryManagementPolicy', component: () => import('@/views/enterprise/entry/management-policy/index.vue'), meta: { title: '能源管理制度' } },
      { path: 'entry/improvement', name: 'EntryImprovement', component: () => import('@/views/enterprise/entry/improvement/index.vue'), meta: { title: '管理改进建议' } },
      { path: 'entry/tech-reform', name: 'EntryTechReform', component: () => import('@/views/enterprise/entry/tech-reform/index.vue'), meta: { title: '技改建议汇总' } },
      { path: 'entry/rectification', name: 'EntryRectification', component: () => import('@/views/enterprise/entry/rectification/index.vue'), meta: { title: '节能整改措施' } },
      { path: 'entry/five-year-target', name: 'EntryFiveYearTarget', component: () => import('@/views/enterprise/entry/five-year-target/index.vue'), meta: { title: '"十四五"节能目标' } },
      { path: 'entry/energy-ghg-source', name: 'EntryEnergyGhgSource', component: () => import('@/views/enterprise/entry/energy-ghg-source/index.vue'), meta: { title: '能源与温室气体源' } },
      { path: 'entry/energy-data-query', name: 'EntryEnergyDataQuery', component: () => import('@/views/enterprise/entry/energy-data-query/index.vue'), meta: { title: '能耗数据查询' } },
      // Charts 5.x
      { path: 'charts/standard', name: 'ChartsStandard', component: () => import('@/views/enterprise/charts/standard/index.vue'), meta: { title: '规定图表' } },
      { path: 'charts/report-assist', name: 'ChartsReportAssist', component: () => import('@/views/enterprise/charts/report-assist/index.vue'), meta: { title: '报告辅助图表' } },
      // Report 6.x
      { path: 'report/input', name: 'ReportInput', component: () => import('@/views/enterprise/report/input/index.vue'), meta: { title: '报告信息录入' } },
      { path: 'report/generate', name: 'ReportGenerate', component: () => import('@/views/enterprise/report/generate/index.vue'), meta: { title: '在线生成报告' } },
      { path: 'report/upload', name: 'ReportUpload', component: () => import('@/views/enterprise/report/upload/index.vue'), meta: { title: '上传最终报告' } },
      { path: 'report/detail', name: 'ReportDetail', component: () => import('@/views/enterprise/report/detail/index.vue'), meta: { title: '报告详情' } },
    ],
  },
  // Admin portal
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, portal: 'admin', userType: 1 },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/dashboard/index.vue'), meta: { title: '管理首页' } },
      { path: 'enterprise', name: 'AdminEnterprise', component: () => import('@/views/admin/enterprise/index.vue'), meta: { title: '企业管理' } },
      { path: 'registration', name: 'AdminRegistration', component: () => import('@/views/admin/registration/index.vue'), meta: { title: '注册审核' } },
      { path: 'template', name: 'AdminTemplate', component: () => import('@/views/admin/template/index.vue'), meta: { title: '模板管理' } },
      { path: 'energy-category', name: 'AdminEnergyCategory', component: () => import('@/views/admin/energy-category/index.vue'), meta: { title: '能源品类管理' } },
      { path: 'emission-factor', name: 'AdminEmissionFactor', component: () => import('@/views/admin/emission-factor/index.vue'), meta: { title: '碳排放因子管理' } },
      { path: 'audit-manage', name: 'AdminAuditManage', component: () => import('@/views/admin/audit-manage/index.vue'), meta: { title: '审计管理' } },
      // Sprint 1.2 — System management
      { path: 'system/users', name: 'AdminSystemUsers', component: () => import('@/views/admin/system/users/index.vue'), meta: { title: '用户管理' } },
      { path: 'system/dict', name: 'AdminSystemDict', component: () => import('@/views/admin/system/dict/index.vue'), meta: { title: '字典管理' } },
    ],
  },
  // Auditor portal
  {
    path: '/auditor',
    component: () => import('@/layouts/AuditorLayout.vue'),
    redirect: '/auditor/dashboard',
    meta: { requiresAuth: true, portal: 'auditor', userType: 2 },
    children: [
      { path: 'dashboard', name: 'AuditorDashboard', component: () => import('@/views/auditor/dashboard/index.vue'), meta: { title: '审计首页' } },
      { path: 'tasks', name: 'AuditorTasks', component: () => import('@/views/auditor/tasks/index.vue'), meta: { title: '审计任务' } },
      { path: 'review', name: 'AuditorReview', component: () => import('@/views/auditor/review/index.vue'), meta: { title: '审核详情' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function getPortalHome(userType: number | null): string {
  switch (userType) {
    case 1: return '/admin/dashboard'
    case 2: return '/auditor/dashboard'
    case 3: return '/enterprise/dashboard'
    default: return '/login'
  }
}

router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  const requiresAuth = to.meta.requiresAuth !== false

  // Already logged in, redirect away from login page to portal home
  if (to.path === '/login') {
    if (token) {
      const userType = Number(localStorage.getItem('userType')) || null
      next(getPortalHome(userType))
    } else {
      next()
    }
    return
  }

  // Not authenticated, redirect to login
  if (requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // Portal access control: check userType matches the route's required portal
  if (to.matched.some(r => r.meta.userType)) {
    const requiredType = to.matched.find(r => r.meta.userType)?.meta.userType as number
    const currentType = Number(localStorage.getItem('userType')) || null
    if (currentType && requiredType && currentType !== requiredType) {
      next(getPortalHome(currentType))
      return
    }
  }

  next()
})

export default router
