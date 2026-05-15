import { describe, expect, it } from 'vitest'
import { standardChartDefinitions } from './definitions'

describe('standardChartDefinitions 0515', () => {
  it('uses the official 19 regulated chart outputs in order', () => {
    expect(standardChartDefinitions.map((item) => item.exportSheetName)).toEqual([
      '企业基本信息表',
      '上一轮已实施的节能技改项目表',
      '企业概况及主要技术指标一览表',
      '重点用能设备汇总表',
      '能源流程图',
      '重点设备能耗和效率',
      '能碳计量器具汇总表',
      '能碳计量器具配备率表',
      '温室气体排放表',
      '能源消费平衡综合表',
      '淘汰产品、设备、装置、工艺和生产能力目录表',
      '企业产品能源成本表',
      '设备测试报告主要指标汇总表',
      '节能量计算',
      '节能降碳潜力明细表',
      '能碳管理改进建议表',
      '节能降碳技术改造建议汇总表',
      '节能降碳整改措施表',
      '整改-“十五五”期间节能降碳目标',
    ])
  })
})
