/**
 * GB/T 4754-2017 国民经济行业分类 — 级联选择器数据
 * 三级结构：门类(Category Letter) → 大类(Major, 2-digit) → 中类(Medium, 3-4 digit)
 * 
 * Data source: 行业分类编码.xlsx (provided by user)
 * Standard: GB/T 4754-2017
 */

export interface IndustryNode {
  value: string
  label: string
  children?: IndustryNode[]
}

export const INDUSTRY_CLASSIFICATION: IndustryNode[] = [
  {
    "value": "B",
    "label": "B 采矿业",
    "children": [
      {
        "value": "B07",
        "label": "B07 石油和天然气开采业",
        "children": [
          {
            "value": "B071",
            "label": "B071 石油开采"
          },
          {
            "value": "B072",
            "label": "B072 天然气开采"
          }
        ]
      }
    ]
  },
  {
    "value": "C",
    "label": "C 制造业",
    "children": [
      {
        "value": "C13",
        "label": "C13 农副食品加工业",
        "children": [
          {
            "value": "C131",
            "label": "C131 谷物磨制"
          },
          {
            "value": "C132",
            "label": "C132 饲料加工"
          },
          {
            "value": "C133",
            "label": "C133 植物油加工"
          },
          {
            "value": "C135",
            "label": "C135 屠宰及肉类加工"
          },
          {
            "value": "C136",
            "label": "C136 水产品加工"
          },
          {
            "value": "C137",
            "label": "C137 蔬菜、菌类、水果和坚果加工"
          },
          {
            "value": "C139",
            "label": "C139 其他农副食品加工"
          }
        ]
      },
      {
        "value": "C14",
        "label": "C14 食品制造业",
        "children": [
          {
            "value": "C141",
            "label": "C141 焙烤食品制造"
          },
          {
            "value": "C142",
            "label": "C142 糖果、巧克力及蜜饯制造"
          },
          {
            "value": "C143",
            "label": "C143 方便食品制造"
          },
          {
            "value": "C144",
            "label": "C144 乳制品制造"
          },
          {
            "value": "C145",
            "label": "C145 罐头食品制造"
          },
          {
            "value": "C146",
            "label": "C146 调味品、发酵制品制造"
          },
          {
            "value": "C149",
            "label": "C149 其他食品制造"
          }
        ]
      },
      {
        "value": "C15",
        "label": "C15 酒、饮料和精制茶制造业",
        "children": [
          {
            "value": "C151",
            "label": "C151 酒的制造"
          },
          {
            "value": "C152",
            "label": "C152 饮料制造"
          }
        ]
      },
      {
        "value": "C16",
        "label": "C16 烟草制品业",
        "children": []
      },
      {
        "value": "C17",
        "label": "C17 纺织业",
        "children": [
          {
            "value": "C171",
            "label": "C171 棉纺织及印染精加工"
          },
          {
            "value": "C172",
            "label": "C172 毛纺织及染整精加工"
          },
          {
            "value": "C173",
            "label": "C173 麻纺织及染整精加工"
          },
          {
            "value": "C174",
            "label": "C174 丝绢纺织及印染精加工"
          },
          {
            "value": "C175",
            "label": "C175 化纤织造及印染精加工"
          },
          {
            "value": "C176",
            "label": "C176 针织或钩针编织物及其制品制造"
          },
          {
            "value": "C177",
            "label": "C177 家用纺织制成品制造"
          },
          {
            "value": "C178",
            "label": "C178 产业用纺织制成品制造"
          }
        ]
      },
      {
        "value": "C18",
        "label": "C18 纺织服装、服饰业",
        "children": [
          {
            "value": "C181",
            "label": "C181 机织服装制造"
          },
          {
            "value": "C182",
            "label": "C182 针织或钩针编织服装制造"
          }
        ]
      },
      {
        "value": "C19",
        "label": "C19 皮革、毛皮、羽毛及其制品和制鞋业",
        "children": [
          {
            "value": "C192",
            "label": "C192 皮革制品制造"
          },
          {
            "value": "C193",
            "label": "C193 毛皮鞣制及制品加工"
          },
          {
            "value": "C194",
            "label": "C194 羽毛（绒）加工及制品制造"
          },
          {
            "value": "C195",
            "label": "C195 制鞋业"
          }
        ]
      },
      {
        "value": "C20",
        "label": "C20 木材加工和木、竹、藤、棕、草制品业",
        "children": [
          {
            "value": "C201",
            "label": "C201 木材加工"
          },
          {
            "value": "C202",
            "label": "C202 人造板制造"
          },
          {
            "value": "C203",
            "label": "C203 木质制品制造"
          },
          {
            "value": "C204",
            "label": "C204 竹、藤、棕、草等制品制造"
          }
        ]
      },
      {
        "value": "C21",
        "label": "C21 家具制造业",
        "children": []
      },
      {
        "value": "C22",
        "label": "C22 造纸和纸制品业",
        "children": [
          {
            "value": "C221",
            "label": "C221 纸浆制造"
          },
          {
            "value": "C222",
            "label": "C222 造纸"
          },
          {
            "value": "C223",
            "label": "C223 纸制品制造"
          }
        ]
      },
      {
        "value": "C23",
        "label": "C23 印刷和记录媒介复制业",
        "children": [
          {
            "value": "C231",
            "label": "C231 印刷"
          }
        ]
      },
      {
        "value": "C24",
        "label": "C24 文教、工美、体育和娱乐用品制造业",
        "children": [
          {
            "value": "C241",
            "label": "C241 文教办公用品制造"
          },
          {
            "value": "C242",
            "label": "C242 乐器制造"
          },
          {
            "value": "C243",
            "label": "C243 工艺美术及礼仪用品制造"
          },
          {
            "value": "C244",
            "label": "C244 体育用品制造"
          },
          {
            "value": "C245",
            "label": "C245 玩具制造"
          },
          {
            "value": "C246",
            "label": "C246 游艺器材及娱乐用品制造"
          }
        ]
      },
      {
        "value": "C25",
        "label": "C25 石油、煤炭及其他燃料加工业",
        "children": [
          {
            "value": "C251",
            "label": "C251 精炼石油产品制造"
          },
          {
            "value": "C252",
            "label": "C252 煤炭加工"
          },
          {
            "value": "C254",
            "label": "C254 生物质燃料加工"
          }
        ]
      },
      {
        "value": "C26",
        "label": "C26 化学原料和化学制品制造业",
        "children": [
          {
            "value": "C261",
            "label": "C261 基础化学原料制造"
          },
          {
            "value": "C262",
            "label": "C262 肥料制造"
          },
          {
            "value": "C263",
            "label": "C263 农药制造"
          },
          {
            "value": "C264",
            "label": "C264 涂料、油墨、颜料及类似产品制造"
          },
          {
            "value": "C265",
            "label": "C265 合成材料制造"
          },
          {
            "value": "C266",
            "label": "C266 专用化学产品制造"
          },
          {
            "value": "C267",
            "label": "C267 炸药、火工及焰火产品制造"
          },
          {
            "value": "C268",
            "label": "C268 日用化学产品制造"
          }
        ]
      },
      {
        "value": "C27",
        "label": "C27 医药制造业",
        "children": [
          {
            "value": "C276",
            "label": "C276 生物药品制品制造"
          }
        ]
      },
      {
        "value": "C28",
        "label": "C28 化学纤维制造业",
        "children": [
          {
            "value": "C281",
            "label": "C281 纤维素纤维原料及纤维制造"
          },
          {
            "value": "C282",
            "label": "C282 合成纤维制造"
          },
          {
            "value": "C283",
            "label": "C283 生物基材料制造"
          }
        ]
      },
      {
        "value": "C29",
        "label": "C29 橡胶和塑料制品业",
        "children": [
          {
            "value": "C291",
            "label": "C291 橡胶制品业"
          },
          {
            "value": "C292",
            "label": "C292 塑料制品业"
          }
        ]
      },
      {
        "value": "C30",
        "label": "C30 非金属矿物制品业",
        "children": [
          {
            "value": "C301",
            "label": "C301 水泥、石灰和石膏制造"
          },
          {
            "value": "C302",
            "label": "C302 石膏、水泥制品及类似制品制造"
          },
          {
            "value": "C303",
            "label": "C303 砖瓦、石材等建筑材料制造"
          },
          {
            "value": "C304",
            "label": "C304 玻璃制造"
          },
          {
            "value": "C305",
            "label": "C305 玻璃制品制造"
          },
          {
            "value": "C306",
            "label": "C306 玻璃纤维和玻璃纤维增强塑料制品制造"
          },
          {
            "value": "C307",
            "label": "C307 陶瓷制品制造"
          },
          {
            "value": "C308",
            "label": "C308 耐火材料制品制造"
          },
          {
            "value": "C309",
            "label": "C309 石墨及其他非金属矿物制品制造"
          }
        ]
      },
      {
        "value": "C31",
        "label": "C31 黑色金属冶炼和压延加工业",
        "children": []
      },
      {
        "value": "C32",
        "label": "C32 有色金属冶炼和压延加工业",
        "children": [
          {
            "value": "C321",
            "label": "C321 常用有色金属冶炼"
          },
          {
            "value": "C322",
            "label": "C322 贵金属冶炼"
          },
          {
            "value": "C323",
            "label": "C323 稀有稀土金属冶炼"
          },
          {
            "value": "C325",
            "label": "C325 有色金属压延加工"
          }
        ]
      },
      {
        "value": "C33",
        "label": "C33 金属制品业",
        "children": [
          {
            "value": "C331",
            "label": "C331 结构性金属制品制造"
          },
          {
            "value": "C332",
            "label": "C332 金属工具制造"
          },
          {
            "value": "C333",
            "label": "C333 集装箱及金属包装容器制造"
          },
          {
            "value": "C335",
            "label": "C335 建筑、安全用金属制品制造"
          },
          {
            "value": "C337",
            "label": "C337 搪瓷制品制造"
          },
          {
            "value": "C338",
            "label": "C338 金属制日用品制造"
          },
          {
            "value": "C339",
            "label": "C339 铸造及其他金属制品制造"
          }
        ]
      },
      {
        "value": "C34",
        "label": "C34 通用设备制造业",
        "children": [
          {
            "value": "C341",
            "label": "C341 锅炉及原动设备制造"
          },
          {
            "value": "C342",
            "label": "C342 金属加工机械制造"
          },
          {
            "value": "C343",
            "label": "C343 物料搬运设备制造"
          },
          {
            "value": "C344",
            "label": "C344 泵、阀门、压缩机及类似机械制造"
          },
          {
            "value": "C345",
            "label": "C345 轴承、齿轮和传动部件制造"
          },
          {
            "value": "C346",
            "label": "C346 烘炉、风机、包装等设备制造"
          },
          {
            "value": "C347",
            "label": "C347 文化、办公用机械制造"
          },
          {
            "value": "C348",
            "label": "C348 通用零部件制造"
          },
          {
            "value": "C349",
            "label": "C349 其他通用设备制造业"
          }
        ]
      },
      {
        "value": "C35",
        "label": "C35 专用设备制造业",
        "children": [
          {
            "value": "C351",
            "label": "C351 采矿、冶金、建筑专用设备制造"
          },
          {
            "value": "C352",
            "label": "C352 化工、木材、非金属加工专用设备制造"
          },
          {
            "value": "C353",
            "label": "C353 食品、饮料、烟草及饲料生产专用设备制造"
          },
          {
            "value": "C354",
            "label": "C354 印刷、制药、日化及日用品生产专用设备制造"
          },
          {
            "value": "C355",
            "label": "C355 纺织、服装和皮革加工专用设备制造"
          },
          {
            "value": "C356",
            "label": "C356 电子和电工机械专用设备制造"
          },
          {
            "value": "C357",
            "label": "C357 农、林、牧、渔专用机械制造"
          },
          {
            "value": "C358",
            "label": "C358 医疗仪器设备及器械制造"
          },
          {
            "value": "C359",
            "label": "C359 环保、邮政、社会公共服务及其他专用设备制造"
          }
        ]
      },
      {
        "value": "C36",
        "label": "C36 汽车制造业",
        "children": [
          {
            "value": "C361",
            "label": "C361 汽车整车制造"
          }
        ]
      },
      {
        "value": "C37",
        "label": "C37 铁路、船舶、航空航天和其他运输设备制造业",
        "children": [
          {
            "value": "C371",
            "label": "C371 铁路运输设备制造"
          },
          {
            "value": "C373",
            "label": "C373 船舶及相关装置制造"
          },
          {
            "value": "C374",
            "label": "C374 航空、航天器及设备制造"
          },
          {
            "value": "C375",
            "label": "C375 摩托车制造"
          },
          {
            "value": "C376",
            "label": "C376 自行车和残疾人座车制造"
          },
          {
            "value": "C379",
            "label": "C379 潜水救捞及其他未列明运输设备制造"
          }
        ]
      },
      {
        "value": "C38",
        "label": "C38 电气机械和器材制造业",
        "children": [
          {
            "value": "C381",
            "label": "C381 电机制造"
          },
          {
            "value": "C382",
            "label": "C382 输配电及控制设备制造"
          },
          {
            "value": "C383",
            "label": "C383 电线、电缆、光缆及电工器材制造"
          },
          {
            "value": "C384",
            "label": "C384 电池制造"
          },
          {
            "value": "C385",
            "label": "C385 家用电力器具制造"
          },
          {
            "value": "C386",
            "label": "C386 非电力家用器具制造"
          },
          {
            "value": "C387",
            "label": "C387 照明器具制造"
          },
          {
            "value": "C389",
            "label": "C389 其他电气机械及器材制造"
          }
        ]
      },
      {
        "value": "C39",
        "label": "C39 计算机、通信和其他电子设备制造业",
        "children": [
          {
            "value": "C391",
            "label": "C391 计算机制造"
          },
          {
            "value": "C392",
            "label": "C392 通信设备制造"
          },
          {
            "value": "C393",
            "label": "C393 广播电视设备制造"
          },
          {
            "value": "C395",
            "label": "C395 非专业视听设备制造"
          },
          {
            "value": "C396",
            "label": "C396 智能消费设备制造"
          },
          {
            "value": "C397",
            "label": "C397 电子器件制造"
          },
          {
            "value": "C398",
            "label": "C398 电子元件及电子专用材料制造"
          }
        ]
      },
      {
        "value": "C40",
        "label": "C40 仪器仪表制造业",
        "children": [
          {
            "value": "C401",
            "label": "C401 通用仪器仪表制造"
          },
          {
            "value": "C402",
            "label": "C402 专用仪器仪表制造"
          }
        ]
      },
      {
        "value": "C41",
        "label": "C41 其他制造业",
        "children": [
          {
            "value": "C411",
            "label": "C411 日用杂品制造"
          }
        ]
      },
      {
        "value": "C42",
        "label": "C42 废弃资源综合利用业",
        "children": []
      },
      {
        "value": "C43",
        "label": "C43 金属制品、机械和设备修理业",
        "children": [
          {
            "value": "C434",
            "label": "C434 铁路、船舶、航空航天等运输设备修理"
          }
        ]
      }
    ]
  },
  {
    "value": "D",
    "label": "D 电力、热力、燃气及水生产和供应业",
    "children": [
      {
        "value": "D44",
        "label": "D44 电力、热力生产和供应业",
        "children": [
          {
            "value": "D441",
            "label": "D441 电力生产"
          }
        ]
      },
      {
        "value": "D45",
        "label": "D45 燃气生产和供应业",
        "children": [
          {
            "value": "D451",
            "label": "D451 燃气生产和供应业"
          }
        ]
      },
      {
        "value": "D46",
        "label": "D46 水的生产和供应业",
        "children": []
      }
    ]
  },
  {
    "value": "F",
    "label": "F 批发和零售业",
    "children": [
      {
        "value": "F51",
        "label": "F51 批发业",
        "children": [
          {
            "value": "F511",
            "label": "F511 农、林、牧、渔产品批发"
          },
          {
            "value": "F512",
            "label": "F512 食品、饮料及烟草制品批发"
          },
          {
            "value": "F513",
            "label": "F513 纺织、服装及家庭用品批发"
          },
          {
            "value": "F514",
            "label": "F514 文化、体育用品及器材批发"
          },
          {
            "value": "F515",
            "label": "F515 医药及医疗器材批发"
          },
          {
            "value": "F516",
            "label": "F516 矿产品、建材及化工产品批发"
          },
          {
            "value": "F517",
            "label": "F517 机械设备、五金产品及电子产品批发"
          },
          {
            "value": "F518",
            "label": "F518 贸易经纪与代理"
          },
          {
            "value": "F519",
            "label": "F519 其他批发业"
          }
        ]
      },
      {
        "value": "F52",
        "label": "F52 零售业",
        "children": [
          {
            "value": "F521",
            "label": "F521 综合零售"
          },
          {
            "value": "F522",
            "label": "F522 食品、饮料及烟草制品专门零售"
          },
          {
            "value": "F523",
            "label": "F523 纺织、服装及日用品专门零售"
          },
          {
            "value": "F524",
            "label": "F524 文化、体育用品及器材专门零售"
          },
          {
            "value": "F525",
            "label": "F525 医药及医疗器材专门零售"
          },
          {
            "value": "F526",
            "label": "F526 汽车、摩托车、零配件和燃料及其他动力销售"
          },
          {
            "value": "F527",
            "label": "F527 家用电器及电子产品专门零售"
          },
          {
            "value": "F528",
            "label": "F528 五金、家具及室内装饰材料专门零售"
          },
          {
            "value": "F529",
            "label": "F529 货摊、无店铺及其他零售业"
          }
        ]
      }
    ]
  },
  {
    "value": "G",
    "label": "G 交通运输、仓储和邮政业",
    "children": [
      {
        "value": "G56",
        "label": "G56 航空运输业",
        "children": [
          {
            "value": "G561",
            "label": "G561 航空客货运输"
          },
          {
            "value": "G562",
            "label": "G562 通用航空服务"
          },
          {
            "value": "G563",
            "label": "G563 航空运输辅助活动"
          }
        ]
      }
    ]
  },
  {
    "value": "I",
    "label": "I 信息传输、软件和信息技术服务业",
    "children": [
      {
        "value": "I63",
        "label": "I63 电信、广播电视和卫星传输服务",
        "children": [
          {
            "value": "I631",
            "label": "I631 电信"
          },
          {
            "value": "I632",
            "label": "I632 广播电视传输服务"
          },
          {
            "value": "I633",
            "label": "I633 卫星传输服务"
          }
        ]
      },
      {
        "value": "I64",
        "label": "I64 互联网和相关服务",
        "children": [
          {
            "value": "I642",
            "label": "I642 互联网信息服务"
          },
          {
            "value": "I643",
            "label": "I643 互联网平台"
          }
        ]
      },
      {
        "value": "I65",
        "label": "I65 软件和信息技术服务业",
        "children": [
          {
            "value": "I651",
            "label": "I651 软件开发"
          },
          {
            "value": "I653",
            "label": "I653 信息系统集成和物联网技术服务"
          },
          {
            "value": "I657",
            "label": "I657 数字内容服务"
          },
          {
            "value": "I659",
            "label": "I659 其他信息技术服务业"
          }
        ]
      }
    ]
  },
  {
    "value": "M",
    "label": "M 科学研究和技术服务业",
    "children": [
      {
        "value": "M73",
        "label": "M73 研究和试验发展",
        "children": []
      },
      {
        "value": "M74",
        "label": "M74 专业技术服务业",
        "children": [
          {
            "value": "M743",
            "label": "M743 海洋服务"
          },
          {
            "value": "M744",
            "label": "M744 测绘地理信息服务"
          },
          {
            "value": "M745",
            "label": "M745 质检技术服务"
          },
          {
            "value": "M746",
            "label": "M746 环境与生态监测检测服务"
          },
          {
            "value": "M747",
            "label": "M747 地质勘查"
          },
          {
            "value": "M748",
            "label": "M748 工程技术与设计服务"
          },
          {
            "value": "M749",
            "label": "M749 工业与专业设计及其他专业技术服务"
          }
        ]
      },
      {
        "value": "M75",
        "label": "M75 科技推广和应用服务业",
        "children": [
          {
            "value": "M751",
            "label": "M751 技术推广服务"
          }
        ]
      }
    ]
  },
  {
    "value": "N",
    "label": "N 水利、环境和公共设施管理业",
    "children": [
      {
        "value": "N77",
        "label": "N77 生态保护和环境治理业",
        "children": [
          {
            "value": "N771",
            "label": "N771 生态保护"
          },
          {
            "value": "N772",
            "label": "N772 环境治理业"
          }
        ]
      }
    ]
  }
]

/**
 * Flatten the tree to a lookup map: code → { code, name, fullPath }
 * Used to resolve stored code back to display labels.
 */
export function buildIndustryLookup(): Map<string, { code: string; name: string; fullPath: string[] }> {
  const map = new Map<string, { code: string; name: string; fullPath: string[] }>()
  for (const menl of INDUSTRY_CLASSIFICATION) {
    map.set(menl.value, { code: menl.value, name: menl.label, fullPath: [menl.value] })
    for (const dalei of menl.children ?? []) {
      map.set(dalei.value, { code: dalei.value, name: dalei.label, fullPath: [menl.value, dalei.value] })
      for (const zhonglei of dalei.children ?? []) {
        map.set(zhonglei.value, { code: zhonglei.value, name: zhonglei.label, fullPath: [menl.value, dalei.value, zhonglei.value] })
      }
    }
  }
  return map
}
