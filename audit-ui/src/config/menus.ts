export interface MenuItem {
  key: string
  icon: string
  title: string
  path?: string
  children?: MenuItem[]
  disabled?: boolean
  tooltip?: string
  badge?: string | number
}

export interface MenuSection {
  section: string
  items: MenuItem[]
}

/* ── Enterprise portal: tree-based menu (GRA-32) ─────────────── */

export const enterpriseMenuItems: MenuItem[] = [
  { key: 'dashboard', icon: '🏠', title: '概览', path: '/enterprise/dashboard' },
  {
    key: 'settings', icon: '⚙️', title: '基本设置',
    children: [
      { key: 'company',  icon: '🏢', title: '企业信息', path: '/enterprise/settings/company' },
      { key: 'energy',   icon: '⚡', title: '能源品种', path: '/enterprise/settings/energy' },
      { key: 'unit',     icon: '🔧', title: '用能单元', path: '/enterprise/settings/unit' },
      { key: 'product',  icon: '📦', title: '产品设置', path: '/enterprise/settings/product' },
    ],
  },
  { key: 'data-entry', icon: '✏️', title: '数据录入', path: '/enterprise/data-entry' },
  {
    key: 'charts', icon: '📊', title: '图表输出',
    children: [
      { key: 'standard-charts', icon: '📉', title: '规定图表', path: '/enterprise/charts/standard' },
      { key: 'assist-charts',   icon: '📊', title: '辅助图表', path: '/enterprise/charts/report-assist' },
      { key: 'energy-flow-config', icon: '🔄', title: '能流图配置', path: '/enterprise/charts/energy-flow/config' },
    ],
  },
  {
    key: 'audit-report', icon: '📄', title: '审计报告',
    children: [
      { key: 'report-upload', icon: '📄', title: '我的报告', path: '/enterprise/audit-report/upload' },
    ],
  },
]

/* ── Legacy section-based menus (kept for backward compat with enterpriseMenus reference) ── */

export const enterpriseMenus: MenuSection[] = [
  {
    section: '工作台',
    items: [
      { key: 'dashboard', icon: '🏠', title: '概览', path: '/enterprise/dashboard' },
    ],
  },
  {
    section: '基本设置',
    items: [
      { key: 'company',  icon: '🏢', title: '企业信息', path: '/enterprise/settings/company' },
      { key: 'energy',   icon: '⚡', title: '能源品种', path: '/enterprise/settings/energy' },
      { key: 'unit',     icon: '🔧', title: '用能单元', path: '/enterprise/settings/unit' },
      { key: 'product',  icon: '📦', title: '产品设置', path: '/enterprise/settings/product' },
    ],
  },
  {
    section: '数据录入',
    items: [
      { key: 'data-entry', icon: '✏️', title: '数据录入', path: '/enterprise/data-entry' },
    ],
  },
  {
    section: '图表输出',
    items: [
      { key: 'standard-charts', icon: '📉', title: '规定图表', path: '/enterprise/charts/standard' },
      { key: 'assist-charts',   icon: '📊', title: '辅助图表', path: '/enterprise/charts/report-assist' },
      { key: 'energy-flow-config', icon: '🔄', title: '能流图配置', path: '/enterprise/charts/energy-flow/config' },
    ],
  },
  {
    section: '审计报告',
    items: [
      { key: 'report-upload', icon: '📄', title: '我的报告', path: '/enterprise/audit-report/upload' },
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
      { key: 'report-template', icon: '📄', title: '报告模板管理',   path: '/admin/report-template' },
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
  {
    section: '报告管理',
    items: [
      { key: 'report-review', icon: '📄', title: '报告审核', path: '/auditor/report-review' },
    ],
  },
]
