/**
 * Industry classification — cascading selector data (大类 → 中类)
 *
 * Source: customer attachment 企业信息（基本信息）0511.xlsx
 *   - 行业分类-大类: 34 majors (codes 13-46)
 *   - 行业分类-中类: 165 middles
 *
 * The cascader stores the middle code (numeric, no letter prefix) as
 * `industryCode`, the middle name as `industryName`, and the parent
 * major code as `industryCategory`. Legacy values that include a
 * GB/T 4754 门类 letter prefix (e.g. `C281`, `D441`) are normalized to
 * the bare numeric code on lookup so historical records still backfill.
 */

export interface IndustryNode {
  value: string
  label: string
  children?: IndustryNode[]
}

export const INDUSTRY_CLASSIFICATION: IndustryNode[] = [
  {
    "value": "13",
    "label": "13 农副食品加工业",
    "children": [
      {
        "value": "131",
        "label": "131 谷物磨制"
      },
      {
        "value": "132",
        "label": "132 饲料加工"
      },
      {
        "value": "133",
        "label": "133 植物油加工"
      },
      {
        "value": "135",
        "label": "135 屠宰及肉类加工"
      },
      {
        "value": "136",
        "label": "136 水产品加工"
      },
      {
        "value": "137",
        "label": "137 蔬菜、菌类、水果和坚果加工"
      },
      {
        "value": "139",
        "label": "139 其他农副食品加工"
      }
    ]
  },
  {
    "value": "14",
    "label": "14 食品制造业",
    "children": [
      {
        "value": "140",
        "label": "140 食品制造业"
      }
    ]
  },
  {
    "value": "15",
    "label": "15 酒、饮料和精制茶制造业",
    "children": [
      {
        "value": "151",
        "label": "151 酒的制造"
      },
      {
        "value": "152",
        "label": "152 饮料制造"
      },
      {
        "value": "153",
        "label": "153 精制茶加工"
      }
    ]
  },
  {
    "value": "16",
    "label": "16 烟草制品业",
    "children": [
      {
        "value": "160",
        "label": "160 烟草制品业"
      }
    ]
  },
  {
    "value": "17",
    "label": "17 纺织业",
    "children": [
      {
        "value": "171",
        "label": "171 棉纺织及印染精加工"
      },
      {
        "value": "172",
        "label": "172 毛纺织及染整精加工"
      },
      {
        "value": "173",
        "label": "173 麻纺织及染整精加工"
      },
      {
        "value": "174",
        "label": "174 丝绢纺织及印染精加工"
      },
      {
        "value": "175",
        "label": "175 化纤织造及印染精加工"
      },
      {
        "value": "176",
        "label": "176 针织或钩针编织物及其制品制造"
      },
      {
        "value": "177",
        "label": "177 家用纺织制成品制造"
      },
      {
        "value": "178",
        "label": "178 产业用纺织制成品制造"
      }
    ]
  },
  {
    "value": "18",
    "label": "18 纺织服装、服饰业",
    "children": [
      {
        "value": "181",
        "label": "181 机织服装制造"
      },
      {
        "value": "182",
        "label": "182 针织或钩针编织服装制造"
      },
      {
        "value": "183",
        "label": "183 服饰制造"
      }
    ]
  },
  {
    "value": "19",
    "label": "19 皮革、毛皮、羽毛及其制品和制鞋业",
    "children": [
      {
        "value": "191",
        "label": "191 皮革鞣制加工"
      },
      {
        "value": "192",
        "label": "192 皮革制品制造"
      },
      {
        "value": "193",
        "label": "193 毛皮鞣制及制品加工"
      },
      {
        "value": "194",
        "label": "194 羽毛（绒）加工及制品制造"
      },
      {
        "value": "195",
        "label": "195 制鞋业"
      }
    ]
  },
  {
    "value": "20",
    "label": "20 木材加工和木、竹、藤、棕、草制品业",
    "children": [
      {
        "value": "201",
        "label": "201 木材加工"
      },
      {
        "value": "202",
        "label": "202 人造板制造"
      },
      {
        "value": "203",
        "label": "203 木质制品制造"
      },
      {
        "value": "204",
        "label": "204 竹、藤、棕、草等制品制造"
      }
    ]
  },
  {
    "value": "21",
    "label": "21 家具制造业",
    "children": [
      {
        "value": "210",
        "label": "210 家具制造业"
      }
    ]
  },
  {
    "value": "22",
    "label": "22 造纸和纸制品业",
    "children": [
      {
        "value": "221",
        "label": "221 纸浆制造"
      },
      {
        "value": "222",
        "label": "222 造纸"
      },
      {
        "value": "223",
        "label": "223 纸制品制造"
      }
    ]
  },
  {
    "value": "23",
    "label": "23 印刷和记录媒介复制业",
    "children": [
      {
        "value": "231",
        "label": "231 印刷"
      }
    ]
  },
  {
    "value": "24",
    "label": "24 文教、工美、体育和娱乐用品制造业",
    "children": [
      {
        "value": "241",
        "label": "241 文教办公用品制造"
      },
      {
        "value": "242",
        "label": "242 乐器制造"
      },
      {
        "value": "243",
        "label": "243 工艺美术及礼仪用品制造"
      },
      {
        "value": "244",
        "label": "244 体育用品制造"
      },
      {
        "value": "245",
        "label": "245 玩具制造"
      },
      {
        "value": "246",
        "label": "246 游艺器材及娱乐用品制造"
      }
    ]
  },
  {
    "value": "25",
    "label": "25 石油、煤炭及其他燃料加工业",
    "children": [
      {
        "value": "251",
        "label": "251 精炼石油产品制造"
      },
      {
        "value": "252",
        "label": "252 煤炭加工"
      },
      {
        "value": "253",
        "label": "253 核燃料加工"
      },
      {
        "value": "254",
        "label": "254 生物质燃料加工"
      }
    ]
  },
  {
    "value": "26",
    "label": "26 化学原料和化学制品制造业",
    "children": [
      {
        "value": "261",
        "label": "261 基础化学原料制造"
      },
      {
        "value": "262",
        "label": "262 肥料制造"
      },
      {
        "value": "263",
        "label": "263 农药制造"
      },
      {
        "value": "264",
        "label": "264 涂料、油墨、颜料及类似产品制造"
      },
      {
        "value": "265",
        "label": "265 合成材料制造"
      },
      {
        "value": "266",
        "label": "266 专用化学产品制造"
      },
      {
        "value": "267",
        "label": "267 炸药、火工及焰火产品制造"
      },
      {
        "value": "268",
        "label": "268 日用化学产品制造"
      }
    ]
  },
  {
    "value": "27",
    "label": "27 医药制造业",
    "children": [
      {
        "value": "271",
        "label": "271 化学药品原料药制造"
      },
      {
        "value": "272",
        "label": "272 化学药品制剂制造"
      },
      {
        "value": "273",
        "label": "273 中药饮片加工"
      },
      {
        "value": "274",
        "label": "274 中成药生产"
      },
      {
        "value": "275",
        "label": "275 兽用药品制造"
      },
      {
        "value": "276",
        "label": "276 生物药品制品制造"
      },
      {
        "value": "277",
        "label": "277 卫生材料及医药用品制造"
      },
      {
        "value": "278",
        "label": "278 药用辅料及包装材料制造"
      }
    ]
  },
  {
    "value": "28",
    "label": "28 化学纤维制造业",
    "children": [
      {
        "value": "281",
        "label": "281 纤维素纤维原料及纤维制造"
      },
      {
        "value": "282",
        "label": "282 合成纤维制造"
      },
      {
        "value": "283",
        "label": "283 生物基材料制造"
      }
    ]
  },
  {
    "value": "29",
    "label": "29 橡胶和塑料制品业",
    "children": [
      {
        "value": "291",
        "label": "291 橡胶制品业"
      },
      {
        "value": "292",
        "label": "292 塑料制品业"
      }
    ]
  },
  {
    "value": "30",
    "label": "30 非金属矿物制品业",
    "children": [
      {
        "value": "301",
        "label": "301 水泥、石灰和石膏制造"
      },
      {
        "value": "302",
        "label": "302 石膏、水泥制品及类似制品制造"
      },
      {
        "value": "303",
        "label": "303 砖瓦、石材等建筑材料制造"
      },
      {
        "value": "304",
        "label": "304 玻璃制造"
      },
      {
        "value": "305",
        "label": "305 玻璃制品制造"
      },
      {
        "value": "306",
        "label": "306 玻璃纤维和玻璃纤维增强塑料制品制造"
      },
      {
        "value": "307",
        "label": "307 陶瓷制品制造"
      },
      {
        "value": "308",
        "label": "308 耐火材料制品制造"
      },
      {
        "value": "309",
        "label": "309 石墨及其他非金属矿物制品制造"
      }
    ]
  },
  {
    "value": "31",
    "label": "31 黑色金属冶炼和压延加工业",
    "children": [
      {
        "value": "310",
        "label": "310 黑色金属冶炼和压延加工业"
      }
    ]
  },
  {
    "value": "32",
    "label": "32 有色金属冶炼和压延加工业",
    "children": [
      {
        "value": "321",
        "label": "321 常用有色金属冶炼"
      },
      {
        "value": "322",
        "label": "322 贵金属冶炼"
      },
      {
        "value": "323",
        "label": "323 稀有稀土金属冶炼"
      },
      {
        "value": "325",
        "label": "325 有色金属压延加工"
      }
    ]
  },
  {
    "value": "33",
    "label": "33 金属制品业",
    "children": [
      {
        "value": "331",
        "label": "331 结构性金属制品制造"
      },
      {
        "value": "332",
        "label": "332 金属工具制造"
      },
      {
        "value": "333",
        "label": "333 集装箱及金属包装容器制造"
      },
      {
        "value": "334",
        "label": "334 金属丝绳及其制品制造"
      },
      {
        "value": "335",
        "label": "335 建筑、安全用金属制品制造"
      },
      {
        "value": "336",
        "label": "336 金属表面处理及热处理加工"
      },
      {
        "value": "337",
        "label": "337 搪瓷制品制造"
      },
      {
        "value": "338",
        "label": "338 金属制日用品制造"
      },
      {
        "value": "339",
        "label": "339 铸造及其他金属制品制造"
      }
    ]
  },
  {
    "value": "34",
    "label": "34 通用设备制造业",
    "children": [
      {
        "value": "341",
        "label": "341 锅炉及原动设备制造"
      },
      {
        "value": "342",
        "label": "342 金属加工机械制造"
      },
      {
        "value": "343",
        "label": "343 物料搬运设备制造"
      },
      {
        "value": "344",
        "label": "344 泵、阀门、压缩机及类似机械制造"
      },
      {
        "value": "345",
        "label": "345 轴承、齿轮和传动部件制造"
      },
      {
        "value": "346",
        "label": "346 烘炉、风机、包装等设备制造"
      },
      {
        "value": "347",
        "label": "347 文化、办公用机械制造"
      },
      {
        "value": "348",
        "label": "348 通用零部件制造"
      },
      {
        "value": "349",
        "label": "349 其他通用设备制造业"
      }
    ]
  },
  {
    "value": "35",
    "label": "35 专用设备制造业",
    "children": [
      {
        "value": "351",
        "label": "351 采矿、冶金、建筑专用设备制造"
      },
      {
        "value": "352",
        "label": "352 化工、木材、非金属加工专用设备制造"
      },
      {
        "value": "353",
        "label": "353 食品、饮料、烟草及饲料生产专用设备制造"
      },
      {
        "value": "354",
        "label": "354 印刷、制药、日化及日用品生产专用设备制造"
      },
      {
        "value": "355",
        "label": "355 纺织、服装和皮革加工专用设备制造"
      },
      {
        "value": "356",
        "label": "356 电子和电工机械专用设备制造"
      },
      {
        "value": "357",
        "label": "357 农、林、牧、渔专用机械制造"
      },
      {
        "value": "358",
        "label": "358 医疗仪器设备及器械制造"
      },
      {
        "value": "359",
        "label": "359 环保、邮政、社会公共服务及其他专用设备制造"
      }
    ]
  },
  {
    "value": "36",
    "label": "36 汽车制造业",
    "children": [
      {
        "value": "361",
        "label": "361 汽车整车制造"
      },
      {
        "value": "362",
        "label": "362 汽车用发动机制造"
      },
      {
        "value": "363",
        "label": "363 改装汽车制造"
      },
      {
        "value": "364",
        "label": "364 低速汽车制造"
      },
      {
        "value": "365",
        "label": "365 电车制造"
      },
      {
        "value": "366",
        "label": "366 汽车车身、挂车制造"
      },
      {
        "value": "367",
        "label": "367 汽车零部件及配件制造"
      }
    ]
  },
  {
    "value": "37",
    "label": "37 铁路、船舶、航空航天和其他运输设备制造业",
    "children": [
      {
        "value": "371",
        "label": "371 铁路运输设备制造"
      },
      {
        "value": "372",
        "label": "372 城市轨道交通设备制造"
      },
      {
        "value": "373",
        "label": "373 船舶及相关装置制造"
      },
      {
        "value": "374",
        "label": "374 航空、航天器及设备制造"
      },
      {
        "value": "375",
        "label": "375 摩托车制造"
      },
      {
        "value": "376",
        "label": "376 自行车和残疾人座车制造"
      },
      {
        "value": "377",
        "label": "377 助动车制造"
      },
      {
        "value": "378",
        "label": "378 非公路休闲车及零配件制造"
      },
      {
        "value": "379",
        "label": "379 潜水救捞及其他未列明运输设备制造"
      }
    ]
  },
  {
    "value": "38",
    "label": "38 电气机械和器材制造业",
    "children": [
      {
        "value": "381",
        "label": "381 电机制造"
      },
      {
        "value": "382",
        "label": "382 输配电及控制设备制造"
      },
      {
        "value": "383",
        "label": "383 电线、电缆、光缆及电工器材制造"
      },
      {
        "value": "384",
        "label": "384 电池制造"
      },
      {
        "value": "385",
        "label": "385 家用电力器具制造"
      },
      {
        "value": "386",
        "label": "386 非电力家用器具制造"
      },
      {
        "value": "387",
        "label": "387 照明器具制造"
      },
      {
        "value": "389",
        "label": "389 其他电气机械及器材制造"
      }
    ]
  },
  {
    "value": "39",
    "label": "39 计算机、通信和其他电子设备制造业",
    "children": [
      {
        "value": "391",
        "label": "391 计算机制造"
      },
      {
        "value": "392",
        "label": "392 通信设备制造"
      },
      {
        "value": "393",
        "label": "393 广播电视设备制造"
      },
      {
        "value": "394",
        "label": "394 雷达及配套设备制造"
      },
      {
        "value": "395",
        "label": "395 非专业视听设备制造"
      },
      {
        "value": "396",
        "label": "396 智能消费设备制造"
      },
      {
        "value": "397",
        "label": "397 电子器件制造"
      },
      {
        "value": "398",
        "label": "398 电子元件及电子专用材料制造"
      },
      {
        "value": "399",
        "label": "399 其他电子设备制造"
      }
    ]
  },
  {
    "value": "40",
    "label": "40 仪器仪表制造业",
    "children": [
      {
        "value": "401",
        "label": "401 通用仪器仪表制造"
      },
      {
        "value": "402",
        "label": "402 专用仪器仪表制造"
      },
      {
        "value": "403",
        "label": "403 钟表与计时仪器制造"
      },
      {
        "value": "404",
        "label": "404 光学仪器制造"
      },
      {
        "value": "405",
        "label": "405 衡器制造"
      },
      {
        "value": "409",
        "label": "409 其他仪器仪表制造业"
      }
    ]
  },
  {
    "value": "41",
    "label": "41 其他制造业",
    "children": [
      {
        "value": "411",
        "label": "411 日用杂品制造"
      },
      {
        "value": "412",
        "label": "412 核辐射加工"
      },
      {
        "value": "419",
        "label": "419 其他未列明制造业"
      }
    ]
  },
  {
    "value": "42",
    "label": "42 废弃资源综合利用业",
    "children": [
      {
        "value": "420",
        "label": "420 废弃资源综合利用业"
      }
    ]
  },
  {
    "value": "43",
    "label": "43 金属制品、机械和设备修理业",
    "children": [
      {
        "value": "431",
        "label": "431 金属制品修理"
      },
      {
        "value": "432",
        "label": "432 通用设备修理"
      },
      {
        "value": "433",
        "label": "433 专用设备修理"
      },
      {
        "value": "434",
        "label": "434 铁路、船舶、航空航天等运输设备修理"
      },
      {
        "value": "435",
        "label": "435 电气设备修理"
      },
      {
        "value": "436",
        "label": "436 仪器仪表修理"
      },
      {
        "value": "439",
        "label": "439 其他机械和设备修理业"
      }
    ]
  },
  {
    "value": "44",
    "label": "44 电力、热力生产和供应业",
    "children": [
      {
        "value": "441",
        "label": "441 电力生产"
      },
      {
        "value": "442",
        "label": "442 电力供应"
      },
      {
        "value": "443",
        "label": "443 热力生产和供应"
      }
    ]
  },
  {
    "value": "45",
    "label": "45 燃气生产和供应业",
    "children": [
      {
        "value": "451",
        "label": "451 燃气生产和供应业"
      },
      {
        "value": "452",
        "label": "452 生物质燃气生产和供应业"
      }
    ]
  },
  {
    "value": "46",
    "label": "46 水的生产和供应业",
    "children": [
      {
        "value": "460",
        "label": "460 水的生产和供应业"
      }
    ]
  }
]

/** Strip an optional GB/T 4754 门类 letter prefix from a stored code. */
export function normalizeIndustryCode(code: string | undefined | null): string {
  if (!code) return ''
  return code.replace(/^[A-Za-z]+/, '').trim()
}

/**
 * Flatten the tree to a lookup map: code → { code, name, fullPath }.
 * Used to resolve a stored industry code back to a cascader path / label.
 */
export function buildIndustryLookup(): Map<string, { code: string; name: string; fullPath: string[] }> {
  const map = new Map<string, { code: string; name: string; fullPath: string[] }>()
  for (const major of INDUSTRY_CLASSIFICATION) {
    map.set(major.value, { code: major.value, name: major.label, fullPath: [major.value] })
    for (const middle of major.children ?? []) {
      map.set(middle.value, { code: middle.value, name: middle.label, fullPath: [major.value, middle.value] })
    }
  }
  return map
}

/**
 * Resolve a stored industry code (possibly with a legacy 门类 letter prefix)
 * back to a cascader path `[majorCode, middleCode]`. When the stored code
 * matches a major that has exactly one middle child — which covers the five
 * old single-leaf majors `C16/C21/C31/C42/D46` that used to be stored as a
 * major-level code — the path is auto-promoted to that middle so the leaf
 * cascader can render and persist a valid 中类 code.
 */
export function resolveIndustryPath(
  code: string | undefined | null,
  lookup?: Map<string, { code: string; name: string; fullPath: string[] }>,
): string[] {
  const normalized = normalizeIndustryCode(code)
  if (!normalized) return []
  const map = lookup ?? buildIndustryLookup()
  const entry = map.get(normalized)
  if (!entry) return []
  const path = [...entry.fullPath]
  if (path.length === 1) {
    const major = INDUSTRY_CLASSIFICATION.find((m) => m.value === path[0])
    if (major?.children?.length === 1) {
      path.push(major.children[0].value)
    }
  }
  return path
}
