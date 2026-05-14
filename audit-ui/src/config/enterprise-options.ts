export const REGION_OPTIONS = [
  '黄浦区','徐汇区','长宁区','静安区','普陀区','虹口区','杨浦区',
  '闵行区','宝山区','嘉定区','浦东新区','金山区','松江区','青浦区',
  '奉贤区','崇明区','上海化学工业区','临港新片区'
]

export const FIELD_OPTIONS = ['工业','通信业','其他']

export const UNIT_TYPE_OPTIONS = [
  '国有企业(独资企业)','国有企业(联营企业)',
  '有限责任公司(国内合资)','有限责任公司(外商投资企业与内资合资)',
  '有限责任公司(台港澳与境内合资)','有限责任公司(自然人投资或控股)',
  '有限责任公司(法人/自然人独资)','有限责任公司(外商投资企业法人独资)',
  '有限责任公司(国有控股，上市)','有限责任公司(国有控股，非上市)',
  '有限责任公司(中外合资，上市)','有限责任公司(中外合资，非上市)',
  '有限责任公司(台港澳与境内合资，上市)','有限责任公司(台港澳与境内合资，非上市)',
  '有限责任公司(国内合资，上市)','有限责任公司(国内合资，非上市)',
  '有限责任公司(社会团体)'
]

export const ENERGY_USAGE_TYPE_OPTIONS = [
  '电力生产企业','其他能源加工转换企业','非能源加工转换企业'
]

/**
 * Legacy energy-usage-type strings rewritten to the current option set.
 * Used to forward-migrate persisted values transparently on load and before save
 * (GRA-68 — the option label `非能源加工转换行业` was renamed to
 * `非能源加工转换企业`).
 */
export const ENERGY_USAGE_TYPE_LEGACY_MAP: Record<string, string> = {
  '非能源加工转换行业': '非能源加工转换企业',
}

/** Normalize a stored energy-usage-type value to the current option label. */
export function normalizeEnergyUsageType(
  value: string | null | undefined
): string | null | undefined {
  if (!value) return value
  return ENERGY_USAGE_TYPE_LEGACY_MAP[value] ?? value
}

export const SUPERIOR_DEPT_GROUPS = [
  {
    label: '集团企业',
    options: [
      '中国宝武钢铁集团有限公司','中国石化上海石油化工股份有限公司',
      '国网上海市电力公司','中国石化上海高桥石油化工公司',
      '上海汽车工业集团股份有限公司','上海华谊（集团）公司',
      '上海电力股份有限公司','申能（集团）有限公司',
      '上海华虹（集团）有限公司','华能电力上海分公司',
      '上海船舶工业公司','上海电气（集团）总公司',
      '上海城投（集团）有限公司','中海石油（中国）有限公司上海分公司',
      '上海光明食品（集团）有限公司','上海医药（集团）有限公司',
      '上海烟草集团有限责任公司','中国商用飞机有限责任公司',
      '上海建筑材料（集团）总公司','上海仪电（集团）公司',
      '东方国际集团（原上海纺织）','上海航天局',
      '上海电信','上海移动','上海联通','铁塔股份','东方有线'
    ]
  },
  {
    label: '区政府/管委会',
    options: [
      '浦东新区','闵行区','嘉定区','金山区','松江区','奉贤区',
      '宝山区','青浦区','崇明区','杨浦区','长宁区','徐汇区',
      '黄浦区','普陀区','静安区','虹口区',
      '上海化学工业区管理委员会','临港新片区'
    ]
  }
]
