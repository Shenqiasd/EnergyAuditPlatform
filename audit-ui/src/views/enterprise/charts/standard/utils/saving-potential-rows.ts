/**
 * Row helpers for regulated chart 表13 (节能潜力明细表).
 *
 * The `/extracted-data/de_saving_potential` API returns rows with snake_case
 * keys (project_type / project_name / main_content / saving_potential /
 * carbon_reduction / investment / calc_description / remark, plus
 * `calculation_desc` as a legacy/wave-6 alias). The regulated chart and its
 * Excel export both consume the same column definitions, so we normalize
 * each row to camelCase props here and let the page and export share the
 * single mapping.
 */
export type Row = Record<string, unknown>

function toNumOrInf(v: unknown): number {
  if (v === null || v === undefined || v === '') return Infinity
  const n = Number(v)
  return Number.isFinite(n) ? n : Infinity
}

/**
 * Stable template-order sort: ascending seq_no (then id) and then by the
 * row's original index. Rows with missing seq_no fall to the end so that
 * legitimate business rows without a sequence number still render
 * (`adaptRow` will fall back to a display index for the 序号 column).
 */
export function sortByTemplateOrder(items: Row[]): Row[] {
  return items
    .map((r, i) => ({ r, i }))
    .sort((a, b) => {
      const seqA = toNumOrInf(a.r.seq_no)
      const seqB = toNumOrInf(b.r.seq_no)
      if (seqA !== seqB) return seqA < seqB ? -1 : 1
      const idA = toNumOrInf(a.r.id)
      const idB = toNumOrInf(b.r.id)
      if (idA !== idB) return idA < idB ? -1 : 1
      return a.i - b.i
    })
    .map(({ r }) => r)
}

/**
 * Normalize a snake_case extracted row into the camelCase shape consumed
 * by the regulated-chart table and the shared Excel export. Supports both
 * `calc_description` (current schema) and `calculation_desc` (legacy /
 * Wave-6 column name) so the page works regardless of which column the
 * extraction populated. Falls back to a display index for 序号 when
 * `seq_no` is missing.
 */
export function adaptRow(r: Row, index: number): Row {
  return {
    seqNo: r.seq_no ?? index + 1,
    projectType: r.project_type ?? '',
    projectName: r.project_name ?? '',
    mainContent: r.main_content ?? '',
    savingPotential: r.saving_potential ?? '',
    carbonReduction: r.carbon_reduction ?? '',
    investment: r.investment ?? '',
    calculationNote: r.calc_description ?? r.calculation_desc ?? '',
    remark: r.remark ?? '',
  }
}
