<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { queryExtractedTable } from '@/api/extracted-data'
import SectionTitle from '../components/SectionTitle.vue'
import RegulationTable from '../components/RegulationTable.vue'
import type { RegColumn } from '../components/RegulationTable.vue'

const loading = ref(false)
const rows = ref<Record<string, unknown>[]>([])
const tableError = ref('')

const columns: RegColumn[] = [
  { prop: 'energyType', label: '能源种类', minWidth: 120 },
  {
    prop: '_inOut', label: '进出用能单位', children: [
      { prop: 'inOutStandard', label: '配备率标准（%）' },
      { prop: 'inOutRequired', label: '需要配置数' },
      { prop: 'inOutActual', label: '实际配置数' },
      { prop: 'inOutRate', label: '配备率（%）' },
    ],
  },
  {
    prop: '_secondary', label: '进出主要次级用能单位', children: [
      { prop: 'secondaryStandard', label: '配备率标准（%）' },
      { prop: 'secondaryRequired', label: '需要配置数' },
      { prop: 'secondaryActual', label: '实际配置数' },
      { prop: 'secondaryRate', label: '配备率（%）' },
    ],
  },
  {
    prop: '_equipment', label: '主要用能设备', children: [
      { prop: 'equipmentStandard', label: '配备率标准（%）' },
      { prop: 'equipmentRequired', label: '需要配置数' },
      { prop: 'equipmentActual', label: '实际配置数' },
      { prop: 'equipmentRate', label: '配备率（%）' },
    ],
  },
]

const LEVEL_PREFIX = ['inOut', 'secondary', 'equipment'] as const
type LevelPrefix = typeof LEVEL_PREFIX[number]

function toNumber(v: unknown): number | null {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

function formatNumeric(v: number | null): number | string {
  return v === null ? '' : v
}

function computeRate(actual: number | null, required: number | null): string {
  if (actual === null || required === null || required === 0) return ''
  return ((actual / required) * 100).toFixed(2)
}

function toIdOrInf(v: unknown): number {
  if (v === null || v === undefined || v === '') return Infinity
  const n = Number(v)
  return Number.isFinite(n) ? n : Infinity
}

function combineEnergyKey(energyType: string, energySubType: string): string {
  return energySubType ? `${energyType}-${energySubType}` : energyType
}

function adaptWideRow(r: Record<string, unknown>): Record<string, unknown> | null {
  const energyType = String(r.energy_type ?? '').trim()
  if (!energyType) return null
  const energySubType = String(r.energy_sub_type ?? '').trim()
  const key = combineEnergyKey(energyType, energySubType)
  const out: Record<string, unknown> = { energyType: key }
  for (let i = 0; i < LEVEL_PREFIX.length; i++) {
    const prefix: LevelPrefix = LEVEL_PREFIX[i]
    const level = i + 1
    const standard = toNumber(r[`l${level}_standard_rate`])
    const required = toNumber(r[`l${level}_required_count`])
    const actual = toNumber(r[`l${level}_actual_count`])
    const dbRate = toNumber(r[`l${level}_actual_rate`])
    if (standard !== null) out[`${prefix}Standard`] = standard
    out[`${prefix}Required`] = formatNumeric(required)
    out[`${prefix}Actual`] = formatNumeric(actual)
    out[`${prefix}Rate`] = dbRate !== null ? dbRate.toFixed(2) : computeRate(actual, required)
  }
  return out
}

function buildRows(dbRows: Record<string, unknown>[]): Record<string, unknown>[] {
  const defaults = getDefaultRows()
  if (!dbRows.length) return defaults

  // Backend returns ORDER BY id DESC; sort by id ascending so unmatched
  // energy_type rows render in insertion order rather than visibly reversed.
  const sorted = [...dbRows]
    .map((r, i) => ({ r, i }))
    .sort((a, b) => {
      const idDiff = toIdOrInf(a.r.id) - toIdOrInf(b.r.id)
      if (idDiff !== 0) return idDiff
      return a.i - b.i
    })
    .map(({ r }) => r)

  // Wide-table rows: one row per (energy_type, energy_sub_type) with l1_*/l2_*/l3_*.
  const adapted = new Map<string, Record<string, unknown>>()
  for (const r of sorted) {
    const row = adaptWideRow(r)
    if (!row) continue
    const key = row.energyType as string
    // If duplicate composite keys exist, prefer the latest by id (sorted asc).
    adapted.set(key, row)
  }

  // Merge with default rows so default standards and template row order are preserved.
  const result: Record<string, unknown>[] = []
  const used = new Set<string>()
  for (const def of defaults) {
    const key = def.energyType as string
    const merged = { ...def }
    const extracted = adapted.get(key)
    if (extracted) {
      Object.assign(merged, extracted)
      used.add(key)
    }
    result.push(merged)
  }
  for (const [key, extracted] of adapted.entries()) {
    if (!used.has(key)) {
      result.push(extracted)
    }
  }
  return result
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await queryExtractedTable('de_meter_config_rate', { pageSize: 200 }).catch((e: Error) => {
      tableError.value = e.message?.includes('404') ? '数据表尚未对接' : ''
      return { rows: [], total: 0 }
    })
    rows.value = buildRows(data.rows || [])
  } finally {
    loading.value = false
  }
})

function getDefaultRows(): Record<string, unknown>[] {
  return [
    { energyType: '电力', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 95 },
    { energyType: '固态能源-煤', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '固态能源-焦炭', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '固态能源-原煤', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '液态能源-成品油', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 95 },
    { energyType: '液态能源-重油', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '液态能源-渣油', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '气态能源-天然气', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '气态能源-液化气', inOutStandard: 100, secondaryStandard: 100, equipmentStandard: 90 },
    { energyType: '气态能源-煤气', inOutStandard: 100, secondaryStandard: 90, equipmentStandard: 80 },
    { energyType: '载热工质-蒸汽', inOutStandard: 100, secondaryStandard: 80, equipmentStandard: 70 },
    { energyType: '载热工质-热水', inOutStandard: 100, secondaryStandard: 95, equipmentStandard: 80 },
    { energyType: '可回收余能', inOutStandard: 100, secondaryStandard: 80, equipmentStandard: 60 },
    { energyType: '其他', inOutStandard: 100, secondaryStandard: 90, equipmentStandard: 80 },
  ]
}
</script>

<template>
  <div>
    <SectionTitle title="表7：能源计量器具配备率表" />
    <el-alert v-if="tableError" :title="tableError" type="warning" show-icon :closable="false" style="margin-bottom: 12px" />
    <RegulationTable
      :columns="columns"
      :data="rows"
      :loading="loading"
      export-filename="能源计量器具配备率表"
      title="能源计量器具配备率表"
    />
  </div>
</template>
