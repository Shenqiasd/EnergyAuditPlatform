export interface MenuItem {
  key: string
  icon: string
  title: string
  path: string
  badge?: string | number
}

export interface MenuSection {
  section: string
  items: MenuItem[]
}

export const enterpriseMenus: MenuSection[] = [
  {
    section: '工作台',
    items: [
      { key: 'dashboard', icon: '🏠', title: '概览', path: '/enterprise/dashboard' },
    ],
  },
  {
    section: '基础设置',
    items: [
      { key: 'company',  icon: '🏢', title: '企业信息', path: '/enterprise/settings/company' },
      { key: 'energy',   icon: '⚡', title: '能源品种', path: '/enterprise/settings/energy' },
      { key: 'unit',     icon: '🔧', title: '用能单元', path: '/enterprise/settings/unit' },
      { key: 'product',  icon: '📦', title: '产品设置', path: '/enterprise/settings/product' },
    ],
  },
  {
    section: '数据填报',
    items: [
      { key: 'report-input',    icon: '✏️', title: '模板填报',     path: '/enterprise/report/input' },
      { key: 'report-generate', icon: '📄', title: '填报进度',     path: '/enterprise/report/generate' },
      { key: 'data-overview',   icon: '📊', title: '抽取数据总览', path: '/enterprise/data/overview' },
    ],
  },
  {
    section: '图表分析',
    items: [
      { key: 'standard-charts', icon: '📉', title: '规定图表',     path: '/enterprise/charts/standard' },
      { key: 'report-assist',   icon: '📊', title: '报告辅助图表', path: '/enterprise/charts/report-assist' },
    ],
  },
  {
    section: '报告管理',
    items: [
      { key: 'report-upload', icon: '📤', title: '上传最终报告', path: '/enterprise/report/upload' },
      { key: 'report-detail', icon: '👁️', title: '报告详情',     path: '/enterprise/report/detail' },
    ],
  },
]

export const adminMenus: MenuSection[] = [
  {
    section: '工作台',
    items: [
      { key: 'dashboard', icon: '🏠', title: '管理首页', path: '/admin/dashboard' },
    ],
  },
  {
    section: '企业管理',
    items: [
      { key: 'enterprise',    icon: '🏢', title: '企业管理',   path: '/admin/enterprise' },
      { key: 'registration',  icon: '📝', title: '注册审核',   path: '/admin/registration' },
    ],
  },
  {
    section: '系统管理',
    items: [
      { key: 'system-users', icon: '👤', title: '用户管理', path: '/admin/system/users' },
      { key: 'system-dict',  icon: '📖', title: '字典管理', path: '/admin/system/dict' },
    ],
  },
  {
    section: '系统配置',
    items: [
      { key: 'template',        icon: '📋', title: '模板管理',       path: '/admin/template' },
      { key: 'energy-category', icon: '⚡', title: '能源品类管理',   path: '/admin/energy-category' },
      { key: 'emission-factor', icon: '🌡️', title: '碳排放因子管理', path: '/admin/emission-factor' },
    ],
  },
  {
    section: '审核管理',
    items: [
      { key: 'audit-manage', icon: '🔍', title: '审计管理', path: '/admin/audit-manage' },
    ],
  },
]

export const auditorMenus: MenuSection[] = [
  {
    section: '工作台',
    items: [
      { key: 'dashboard', icon: '🏠', title: '审计首页', path: '/auditor/dashboard' },
    ],
  },
  {
    section: '审核任务',
    items: [
      { key: 'tasks',  icon: '📋', title: '任务列表', path: '/auditor/tasks' },
      { key: 'review', icon: '🔍', title: '审核详情', path: '/auditor/review' },
    ],
  },
]
