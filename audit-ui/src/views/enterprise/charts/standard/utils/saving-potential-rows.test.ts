import { describe, expect, it } from 'vitest'
import { adaptRow, sortByTemplateOrder } from './saving-potential-rows'

describe('adaptRow (表13 节能潜力明细)', () => {
  it('maps snake_case API row to the camelCase RegulationTable shape', () => {
    const apiRow = {
      seq_no: 1,
      project_type: '电机系统节能',
      project_name: 'qa-codex-20260513-ea-cust-027',
      main_content: '表13端到端验证',
      saving_potential: 321312,
      carbon_reduction: 123123,
      investment: 12312,
      calc_description: '节能潜力计算说明-qa',
      remark: 'qa cleanup required',
    }
    expect(adaptRow(apiRow, 0)).toEqual({
      seqNo: 1,
      projectType: '电机系统节能',
      projectName: 'qa-codex-20260513-ea-cust-027',
      mainContent: '表13端到端验证',
      savingPotential: 321312,
      carbonReduction: 123123,
      investment: 12312,
      calculationNote: '节能潜力计算说明-qa',
      remark: 'qa cleanup required',
    })
  })

  it('accepts the legacy calculation_desc alias when calc_description is absent', () => {
    const apiRow = {
      seq_no: 2,
      project_name: '锅炉余热回收',
      calculation_desc: '锅炉余热-计算说明',
    }
    const out = adaptRow(apiRow, 1)
    expect(out.calculationNote).toBe('锅炉余热-计算说明')
    expect(out.projectName).toBe('锅炉余热回收')
  })

  it('prefers calc_description over calculation_desc when both exist', () => {
    const out = adaptRow(
      { calc_description: 'current', calculation_desc: 'legacy' },
      0,
    )
    expect(out.calculationNote).toBe('current')
  })

  it('preserves seq_no when present and falls back to display index when absent', () => {
    expect(adaptRow({ seq_no: 7, project_name: 'a' }, 0).seqNo).toBe(7)
    expect(adaptRow({ project_name: 'b' }, 0).seqNo).toBe(1)
    expect(adaptRow({ project_name: 'c' }, 4).seqNo).toBe(5)
  })

  it('defaults empty business fields to "" so the Excel export shows blank cells, not undefined', () => {
    const out = adaptRow({}, 0)
    expect(out.projectType).toBe('')
    expect(out.projectName).toBe('')
    expect(out.mainContent).toBe('')
    expect(out.savingPotential).toBe('')
    expect(out.carbonReduction).toBe('')
    expect(out.investment).toBe('')
    expect(out.calculationNote).toBe('')
    expect(out.remark).toBe('')
  })
})

describe('sortByTemplateOrder (表13 节能潜力明细)', () => {
  it('orders rows by seq_no ascending', () => {
    const out = sortByTemplateOrder([
      { seq_no: 3, project_name: 'c' },
      { seq_no: 1, project_name: 'a' },
      { seq_no: 2, project_name: 'b' },
    ])
    expect(out.map((r) => r.project_name)).toEqual(['a', 'b', 'c'])
  })

  it('breaks ties by id ascending then by original index', () => {
    const out = sortByTemplateOrder([
      { seq_no: 1, id: 20, project_name: 'b' },
      { seq_no: 1, id: 10, project_name: 'a' },
      { seq_no: 1, id: 10, project_name: 'a2' },
    ])
    expect(out.map((r) => r.project_name)).toEqual(['a', 'a2', 'b'])
  })

  it('places rows without seq_no at the end without dropping them', () => {
    const out = sortByTemplateOrder([
      { project_name: 'no-seq' },
      { seq_no: 1, project_name: 'first' },
    ])
    expect(out.map((r) => r.project_name)).toEqual(['first', 'no-seq'])
  })
})
