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
    section: '数据录入',
    items: [
      { key: 'overview',            icon: '📋', title: '企业概况',         path: '/enterprise/entry/overview' },
      { key: 'indicators',          icon: '📈', title: '主要技术指标',     path: '/enterprise/entry/indicators' },
      { key: 'projects',            icon: '🏗️', title: '节能技改项目',     path: '/enterprise/entry/projects' },
      { key: 'meters',              icon: '🔌', title: '计量器具汇总',     path: '/enterprise/entry/meters' },
      { key: 'meter-rate',          icon: '📐', title: '计量器具配备率',   path: '/enterprise/entry/meter-rate' },
      { key: 'benchmark',           icon: '📊', title: '能效对标',         path: '/enterprise/entry/benchmark' },
      { key: 'equipment-energy',    icon: '⚙️', title: '重点设备能耗',     path: '/enterprise/entry/equipment-energy' },
      { key: 'equipment-summary',   icon: '🏭', title: '主要用能设备',     path: '/enterprise/entry/equipment-summary' },
      { key: 'equipment-test',      icon: '🧪', title: '重点设备测试',     path: '/enterprise/entry/equipment-test' },
      { key: 'obsolete',            icon: '🗑️', title: '淘汰设备目录',     path: '/enterprise/entry/obsolete' },
      { key: 'energy-flow',         icon: '🌊', title: '能源流程图',       path: '/enterprise/entry/energy-flow' },
      { key: 'product-consumption', icon: '📦', title: '单位产品能耗',     path: '/enterprise/entry/product-consumption' },
      { key: 'product-cost',        icon: '💰', title: '产品能源成本',     path: '/enterprise/entry/product-cost' },
      { key: 'saving-calc',         icon: '💡', title: '节能量计算',       path: '/enterprise/entry/saving-calc' },
      { key: 'ghg-emission',        icon: '🌡️', title: '温室气体排放',     path: '/enterprise/entry/ghg-emission' },
      { key: 'waste-heat',          icon: '♨️', title: '余热资源利用',     path: '/enterprise/entry/waste-heat' },
      { key: 'saving-potential',    icon: '🔋', title: '节能潜力明细',     path: '/enterprise/entry/saving-potential' },
      { key: 'management-policy',   icon: '📜', title: '能源管理制度',     path: '/enterprise/entry/management-policy' },
      { key: 'improvement',         icon: '🔄', title: '管理改进建议',     path: '/enterprise/entry/improvement' },
      { key: 'tech-reform',         icon: '🔩', title: '技改建议汇总',     path: '/enterprise/entry/tech-reform' },
      { key: 'rectification',       icon: '✅', title: '节能整改措施',     path: '/enterprise/entry/rectification' },
      { key: 'five-year-target',    icon: '🎯', title: '"十四五"节能目标', path: '/enterprise/entry/five-year-target' },
      { key: 'energy-ghg-source',   icon: '🌿', title: '能源与温室气体源', path: '/enterprise/entry/energy-ghg-source' },
      { key: 'energy-data-query',   icon: '🔍', title: '能耗数据查询',     path: '/enterprise/entry/energy-data-query' },
    ],
  },
  {
    section: '图表输出',
    items: [
      { key: 'standard-charts',    icon: '📉', title: '规定图表', path: '/enterprise/charts/standard' },
      { key: 'report-assist',      icon: '📊', title: '报告辅助图表', path: '/enterprise/charts/report-assist' },
    ],
  },
  {
    section: '审计报告',
    items: [
      { key: 'report-input',    icon: '✏️', title: '信息录入',   path: '/enterprise/report/input' },
      { key: 'report-generate', icon: '📄', title: '在线生成报告', path: '/enterprise/report/generate' },
      { key: 'report-upload',   icon: '📤', title: '上传最终报告', path: '/enterprise/report/upload' },
      { key: 'report-detail',   icon: '👁️', title: '报告详情',   path: '/enterprise/report/detail' },
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
