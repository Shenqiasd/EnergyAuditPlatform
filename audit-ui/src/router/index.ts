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
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { requiresAuth: false },
  },
  // Enterprise portal
  {
    path: '/enterprise',
    component: () => import('@/layouts/EnterpriseLayout.vue'),
    redirect: '/enterprise/dashboard',
    meta: { requiresAuth: true, portal: 'enterprise', userType: 3 },
    children: [
      { path: 'dashboard', name: 'EnterpriseDashboard', component: () => import('@/views/enterprise/dashboard/index.vue'), meta: { title: '概览' } },
      // Settings
      { path: 'settings/company', name: 'CompanySettings', component: () => import('@/views/enterprise/settings/company/index.vue'), meta: { title: '企业信息' } },
      { path: 'settings/energy', name: 'EnergySettings', component: () => import('@/views/enterprise/settings/energy/index.vue'), meta: { title: '能源品种' } },
      { path: 'settings/unit', name: 'UnitSettings', component: () => import('@/views/enterprise/settings/unit/index.vue'), meta: { title: '用能单元' } },
      { path: 'settings/product', name: 'ProductSettings', component: () => import('@/views/enterprise/settings/product/index.vue'), meta: { title: '产品设置' } },
      // Data entry (renamed from report/input)
      { path: 'data-entry', name: 'DataEntry', component: () => import('@/views/enterprise/report/input/index.vue'), meta: { title: '数据录入' } },
      // Legacy redirects → data-entry
      { path: 'report/input', redirect: '/enterprise/data-entry' },
      { path: 'template-filling', redirect: '/enterprise/data-entry' },
      { path: 'report-filling', redirect: '/enterprise/data-entry' },
      // Data overview (kept, accessible via direct URL)
      { path: 'data/overview', name: 'DataOverview', component: () => import('@/views/enterprise/data/overview/index.vue'), meta: { title: '抽取数据总览' } },
      // Charts
      {
        path: 'charts/standard',
        name: 'ChartsStandard',
        component: () => import('@/views/enterprise/charts/standard/index.vue'),
        redirect: { name: 'StandardBasicInfo' },
        meta: { title: '规定图表' },
        children: [
          { path: 'basic-info', name: 'StandardBasicInfo', component: () => import('@/views/enterprise/charts/standard/pages/BasicInfo.vue') },
          { path: 'retrofit-projects', name: 'StandardRetrofitProjects', component: () => import('@/views/enterprise/charts/standard/pages/RetrofitProjects.vue') },
          { path: 'enterprise-overview', name: 'StandardEnterpriseOverview', component: () => import('@/views/enterprise/charts/standard/pages/EnterpriseOverview.vue') },
          { path: 'major-equipment', name: 'StandardMajorEquipment', component: () => import('@/views/enterprise/charts/standard/pages/MajorEquipment.vue') },
          { path: 'energy-flow', name: 'StandardEnergyFlow', component: () => import('@/views/enterprise/charts/standard/pages/EnergyFlowChart.vue') },
          { path: 'equipment-energy', name: 'StandardEquipmentEnergy', component: () => import('@/views/enterprise/charts/standard/pages/EquipmentEnergy.vue') },
          { path: 'meter-summary', name: 'StandardMeterSummary', component: () => import('@/views/enterprise/charts/standard/pages/MeterSummary.vue') },
          { path: 'meter-rate', name: 'StandardMeterRate', component: () => import('@/views/enterprise/charts/standard/pages/MeterRate.vue') },
          { path: 'ghg-emission', name: 'StandardGhgEmission', component: () => import('@/views/enterprise/charts/standard/pages/GhgEmission.vue') },
          { path: 'energy-balance', name: 'StandardEnergyBalance', component: () => import('@/views/enterprise/charts/standard/pages/EnergyBalance.vue') },
          { path: 'obsolete-equipment', name: 'StandardObsoleteEquipment', component: () => import('@/views/enterprise/charts/standard/pages/ObsoleteEquipment.vue') },
          { path: 'product-energy-cost', name: 'StandardProductEnergyCost', component: () => import('@/views/enterprise/charts/standard/pages/ProductEnergyCost.vue') },
          { path: 'test-indicators', name: 'StandardTestIndicators', component: () => import('@/views/enterprise/charts/standard/pages/TestIndicators.vue') },
          { path: 'saving-calculation', name: 'StandardSavingCalculation', component: () => import('@/views/enterprise/charts/standard/pages/SavingCalculation.vue') },
          { path: 'saving-potential', name: 'StandardSavingPotential', component: () => import('@/views/enterprise/charts/standard/pages/SavingPotential.vue') },
          { path: 'mgmt-suggestions', name: 'StandardMgmtSuggestions', component: () => import('@/views/enterprise/charts/standard/pages/MgmtSuggestions.vue') },
          { path: 'retrofit-suggestions', name: 'StandardRetrofitSuggestions', component: () => import('@/views/enterprise/charts/standard/pages/RetrofitSuggestions.vue') },
          { path: 'rectification', name: 'StandardRectification', component: () => import('@/views/enterprise/charts/standard/pages/Rectification.vue') },
          { path: 'five-year-target', name: 'StandardFiveYearTarget', component: () => import('@/views/enterprise/charts/standard/pages/FiveYearTarget.vue') },
        ],
      },
      { path: 'charts/energy-flow', name: 'ChartsEnergyFlow', component: () => import('@/views/enterprise/charts/energy-flow/index.vue'), meta: { title: '能源流向图' } },
      { path: 'charts/energy-flow/config', name: 'ChartsEnergyFlowConfig', component: () => import('@/views/enterprise/charts/energy-flow/config.vue'), meta: { title: '能流图配置' } },
      { path: 'charts/report-assist', name: 'ChartsReportAssist', component: () => import('@/views/enterprise/charts/report-assist/index.vue'), meta: { title: '辅助图表' } },
      // Audit report (unified download-template + upload-filled-report page)
      { path: 'audit-report/upload', name: 'AuditReportUpload', component: () => import('@/views/enterprise/audit-report/upload/index.vue'), meta: { title: '审计报告' } },
      // Legacy report routes (kept for backward compat)
      { path: 'report/generate', name: 'ReportGenerate', component: () => import('@/views/enterprise/report/generate/index.vue'), meta: { title: '填报进度' } },
      { path: 'report/edit', name: 'ReportEdit', component: () => import('@/views/enterprise/report/edit/index.vue'), meta: { title: '在线编辑报告' } },
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
      { path: 'report-template', name: 'AdminReportTemplate', component: () => import('@/views/admin/report-template/index.vue'), meta: { title: '报告模板管理' } },
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
      { path: 'report-review', name: 'AuditorReportReview', component: () => import('@/views/auditor/report-review/index.vue'), meta: { title: '报告审核' } },
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
