# 能碳审计平台 — 完整开发计划

> 技术栈：Spring Boot 3 + MyBatis + Vue 3 + MySQL + Ehcache3 + SpreadJS + AntV X6 + OnlyOffice  
> 前端风格：方案B 绿色低碳风  
> 协作规范：每个 Sprint 完成后推一个 PR，合并后进入下一个 Sprint

---

## 总览

| Wave | 主题 | Sprint 数 | 预估周期 |
|------|------|-----------|----------|
| Wave 0 | 基础设施 & 认证 | 2 | 2 周 |
| Wave 1 | 管理端基础功能 | 2 | 2 周 |
| Wave 2 | 企业基础设置 | 2 | 2 周 |
| Wave 3 | SpreadJS 模板引擎 | 3 | 3 周 |
| Wave 4 | 数据录入（标准模块）| 4 | 4 周 |
| Wave 5 | 数据录入（复杂模块）| 3 | 3 周 |
| Wave 6 | 能源流程图（AntV X6）| 2 | 2 周 |
| Wave 7 | 图表输出 | 2 | 2 周 |
| Wave 8 | 审计报告（OnlyOffice）| 2 | 2 周 |
| Wave 9 | 审核工作流 | 2 | 2 周 |
| Wave 10 | 碳排放管理 & 平台对接 | 2 | 2 周 |
| Wave 11 | 收尾优化 & 测试 | 2 | 2 周 |
| **合计** | | **28 个 Sprint / PR** | **~28 周** |

---

## Wave 0 — 基础设施 & 认证
> 目标：项目可运行、可部署，认证主流程贯通。

### Sprint 0.1 — 基础设施
**PR 名称：** `feat: infrastructure - docker, db init, project baseline`

**后端：**
- [ ] `docker-compose.yml`：MySQL 8 + 后端服务 + 前端 Nginx
- [ ] `sql/init.sql`：建库建表脚本（schema.sql 拆分为 DDL + 初始数据）
- [ ] `application-dev.yml` / `application-prod.yml` 环境配置分离
- [ ] Maven 多模块打包验证（`mvn package -DskipTests`）

**前端：**
- [ ] `npm install` 依赖安装，`vite.config.ts` 代理配置
- [ ] 全局样式落地（方案B：variables.scss / sidebar.scss / index.scss / element.scss）
- [ ] Element Plus 主题色覆盖为 `#00897B`
- [ ] 三端 Layout 组件完成（EnterpriseLayout / AdminLayout / AuditorLayout）
- [ ] 路由守卫：未登录跳转 `/login`，登录后按角色跳转对应门户

**DoD：** `docker-compose up` 能启动所有服务，前端能渲染登录页，后端健康检查接口返回 200。

---

### Sprint 0.2 — 认证模块
**PR 名称：** `feat: auth - JWT login, role-based portal, force-change-password`

**后端：**
- [ ] `POST /api/auth/login`：用户名+密码登录，返回 JWT Token
- [ ] `POST /api/auth/logout`：Token 失效（加入本地黑名单 Ehcache）
- [ ] `GET /api/auth/info`：返回当前用户信息（含角色、企业ID、是否需要改密）
- [ ] `PUT /api/auth/password`：修改密码
- [ ] JWT 拦截器：校验 Token，注入 `SecurityUtils` 线程变量
- [ ] 白名单配置：`/api/auth/login` 和 `/api/auth/register` 不拦截
- [ ] 初始化 `admin` 管理员账号（`data.sql`）

**前端：**
- [ ] 登录页完整实现（方案B风格，三端Tab切换）
- [ ] 强制改密弹窗：登录后检测 `passwordChanged=0` 弹出
- [ ] `stores/user.ts`：`login()` / `logout()` / `getUserInfo()` Action 实现
- [ ] `utils/request.ts`：Token 注入、401 自动跳转登录、统一错误提示

**DoD：** 三端账号可正常登录/登出，初次登录强制改密流程可走通。

---

## Wave 1 — 管理端基础功能
> 目标：管理员能管理企业账号和系统字典。

### Sprint 1.1 — 企业管理 & 注册审核
**PR 名称：** `feat: admin - enterprise management and registration audit`

**后端：**
- [ ] `GET/POST/PUT/DELETE /api/enterprise`：企业 CRUD
- [ ] `PUT /api/enterprise/{id}/lock` / `unlock`：锁定/解锁
- [ ] `PUT /api/enterprise/{id}/expire`：设置到期日期
- [ ] `GET /api/registration`：注册申请列表（分页/状态筛选）
- [ ] `POST /api/registration`：企业提交注册申请
- [ ] `PUT /api/registration/{id}/approve` / `reject`：管理端审核
- [ ] 审核通过时自动创建企业账号（初始密码=信用代码后6位）
- [ ] 企业账号密码 BCrypt 加密
- [ ] `sys_operation_log` AOP 切面：记录关键操作

**前端：**
- [ ] 管理端 `/admin/enterprise`：企业列表（搜索/分页/状态筛选）
- [ ] 企业详情抽屉：查看/编辑企业信息
- [ ] 创建企业弹窗：手动创建 + 设置初始密码
- [ ] 锁定/解锁/到期操作确认弹窗
- [ ] 管理端 `/admin/registration`：注册申请列表 + 审核操作

**DoD：** 管理员可完整完成企业账号的创建、审核、锁定流程。

---

### Sprint 1.2 — 用户管理 & 系统字典
**PR 名称：** `feat: admin - user management and dictionary`

**后端：**
- [ ] `GET/POST/PUT/DELETE /api/system/users`：用户 CRUD（管理端和审核端账号）
- [ ] `GET/POST/PUT/DELETE /api/system/dicts/type`：字典类型 CRUD
- [ ] `GET/POST/PUT/DELETE /api/system/dicts/data`：字典数据 CRUD
- [ ] `GET /api/system/dicts/data/{dictType}`：按类型查询字典（接口加 Ehcache 缓存）
- [ ] 字典数据变更时清除对应缓存
- [ ] `GET /api/system/config`：系统配置读取

**前端：**
- [ ] 管理端用户管理页：列表/新增/编辑/禁用
- [ ] 字典管理页：字典类型树 + 字典数据表格（二级联动）
- [ ] 公共组件 `DictSelect.vue`：通用字典下拉选择组件（自动加载缓存字典项）
- [ ] 公共组件 `DictTag.vue`：字典值 → 标签展示

**DoD：** 字典数据可维护，`DictSelect` 组件在任意表单中可复用。

---

## Wave 2 — 企业基础设置
> 目标：企业完成 3.1-3.4 基础数据配置，为后续数据录入提供主数据。

### Sprint 2.1 — 企业信息设置 & 能源品种设置
**PR 名称：** `feat: enterprise-settings - company info (3.1) and energy types (3.2)`

**后端：**
- [ ] `GET/PUT /api/enterprise/setting`：企业设置（3.1）读取/更新（按当前用户企业ID隔离）
- [ ] `GET/POST/PUT/DELETE /api/setting/energy`：能源品种 CRUD（3.2）
- [ ] 能源品种列表接口加 Ehcache 缓存（企业级）
- [ ] 数据接口预留：大平台企业信息同步接口（TODO 占位）

**前端：**
- [ ] `3.1 企业信息`：分区域表单（企业信息/联系信息/编制单位/认证信息）
  - 行业分类/单位性质/用能企业类型使用 `DictSelect`
  - 保存时整体提交，不拆分字段
- [ ] `3.2 能源品种`：表格 + 行内/弹窗编辑
  - 列：名称/类别/计量单位/当量值/等价值/低位热值/含碳量/氧化率/颜色/状态
  - 颜色列使用 `<input type="color">` 选择器
  - 类别使用 `DictSelect`（能源类别字典）

**DoD：** 企业可保存基本信息，可维护自己的能源品种列表。

---

### Sprint 2.2 — 用能单元设置 & 产品设置
**PR 名称：** `feat: enterprise-settings - energy units (3.3) and products (3.4)`

**后端：**
- [ ] `GET/POST/PUT/DELETE /api/setting/unit`：用能单元 CRUD（3.3，含三种类型）
- [ ] `GET/POST/PUT/DELETE /api/setting/unit/{id}/energy`：单元-能源关联 CRUD（3.3.1/3.3.2）
- [ ] `GET/POST/PUT/DELETE /api/setting/product`：产品 CRUD（3.4）

**前端：**
- [ ] `3.3 用能单元`：三 Tab 页（加工转换/分配输送/终端使用）
  - 每个 Tab 为独立表格，支持增删改
  - 3.3.1/3.3.2 支持关联能源多选（从 3.2 能源品种中选）
  - 3.3.3 分类使用 `DictSelect`（终端使用分类字典）
- [ ] `3.4 产品设置`：简单 CRUD 表格（名称/计量单位/单价）
- [ ] 公共组件 `EnergySelect.vue`：能源品种多选/单选组件（供全局复用）
- [ ] 公共组件 `ProductSelect.vue`：产品选择组件（供全局复用）

**DoD：** 企业完成 3.1-3.4 全部基础设置后，数据录入模块可正常引用主数据。

---

## Wave 3 — SpreadJS 模板引擎
> 目标：建立 SpreadJS 模板的完整生命周期管理机制，作为数据录入的核心基础设施。

### Sprint 3.1 — 模板管理后端
**PR 名称：** `feat: template-engine - backend CRUD, versioning, tag mapping`

**后端：**
- [ ] `GET/POST/PUT/DELETE /api/template`：模板 CRUD
- [ ] `GET /api/template/{id}/versions`：版本列表
- [ ] `POST /api/template/{id}/publish`：发布指定版本
- [ ] `POST /api/template/import`：导入 Excel 文件 → 转换为 SpreadJS JSON
- [ ] `GET/POST/PUT/DELETE /api/template/{versionId}/tags`：标签映射 CRUD
- [ ] `GET /api/template/{code}/current`：按模板编码获取当前发布版本（Ehcache 缓存）
- [ ] `GET/POST /api/submission`：获取/保存企业填报数据
- [ ] `POST /api/submission/extract`：触发数据抽取（SpreadsheetDataExtractor）
- [ ] `POST/DELETE/GET /api/template/lock`：悲观锁 acquire/release/check

**DoD：** 管理员可通过 API 完整管理模板生命周期，企业可提交填报数据并触发抽取。

---

### Sprint 3.2 — 管理端模板设计器
**PR 名称：** `feat: template-engine - admin SpreadJS Designer UI`

**前端：**
- [ ] 安装 SpreadJS：`@grapecity/spread-sheets` + `@grapecity/spread-sheets-vue`
- [ ] 管理端 `/admin/template`：模板列表（编码/名称/当前版本/状态）
- [ ] 模板详情页：SpreadJS Designer 在线设计器挂载
  - 工具栏：保存草稿/发布版本/导入 Excel/导出 Excel
  - 版本历史侧栏：查看历史版本，支持回滚
- [ ] 标签映射配置面板：
  - 列出当前版本中所有已命名的 Tag 和 Named Range
  - 配置每个 Tag → 字段名 / 目标表 / 数据类型 / 是否必填 / 字典类型
- [ ] `SpreadDesigner.vue` 封装组件

**DoD：** 管理员可在线设计模板，配置 Tag 映射，发布版本。

---

### Sprint 3.3 — 企业端 SpreadJS 填报 & 编辑锁
**PR 名称：** `feat: template-engine - enterprise fill form with edit lock`

**前端：**
- [ ] `SpreadSheet.vue` 填报组件完整实现：
  - 挂载时调用 `acquireLock` API，获取编辑锁（失败时展示只读 + 锁定人提示）
  - 加载当前发布版本模板 JSON
  - 加载已有填报数据（如有）
  - 心跳续约：每5分钟自动 PUT 续约锁
  - 卸载时调用 `releaseLock`
- [ ] 数据保存流程：
  - 保存按钮 → 前端遍历 Named Range / Tag → 构建 `{tagName: value}` Map
  - 调用 `POST /api/submission`（携带 spreadjsJson + extractedData）
  - 后端触发数据抽取写入对应业务表
- [ ] 字典列校验：填报时对字典字段做下拉限制（`GC.Spread.Sheets.CellTypes.ComboBox`）
- [ ] 保存成功/失败提示

**DoD：** 企业端可打开任意 SpreadJS 模板填报、保存，悲观锁机制工作正常。

---

## Wave 4 — 数据录入（标准模块）
> 目标：完成 4.1-4.5、4.8-4.10、4.12-4.14、4.17-4.21 共 16 个标准表单/表格模块。
> 这些模块数据结构固定，使用传统 CRUD 表单实现，不依赖 SpreadJS。

### Sprint 4.1 — 表单类模块（4.1 / 4.2）
**PR 名称：** `feat: data-entry - company overview (4.1) and tech indicators (4.2)`

**后端：**
- [ ] `GET/PUT /api/entry/overview`：企业概况（4.1）读取/保存
- [ ] `GET/PUT /api/entry/indicators`：主要技术指标（4.2）读取/保存（含审计年/上年度两行）

**前端（通用规范）：**
- [ ] 封装 `EntryPageLayout.vue`：统一数据录入页布局（标题/描述/操作栏/表格区）
- [ ] 封装 `SaveBar.vue`：固定底部操作栏（保存草稿/提交）

**前端（模块）：**
- [ ] `4.1 企业概况`：分区表单（节能管理组织/十四五目标）
- [ ] `4.2 主要技术指标`：双列对比表单（审计年 vs 上年度）
  - 经济指标/能耗指标/节能项目指标/年度目标四分组
  - 增减(%) 列前端自动计算展示

**DoD：** 两个模块可独立保存，数据正确写入对应数据库表。

---

### Sprint 4.2 — 表格类模块（4.3 / 4.4 / 4.5）
**PR 名称：** `feat: data-entry - saving projects (4.3), meters (4.4), meter rate (4.5)`

**后端（CRUD pattern，以下每个模块均含 list/create/update/delete）：**
- [ ] `/api/entry/saving-projects`：已实施节能技改项目（4.3）
- [ ] `/api/entry/meters`：能源计量器具汇总（4.4）
  - 关联能源品种（外键 energy_id）
- [ ] `/api/entry/meter-config-rate`：能源计量器具配备率（4.5）
  - 按能源种类 × 层级（3层）存储

**前端（通用组件封装）：**
- [ ] `DataTable.vue`：通用数据表格（含分页/搜索/批量导入/导出Excel）
- [ ] `ImportDialog.vue`：Excel 批量导入对话框（上传→预览→确认导入）

**前端（模块）：**
- [ ] `4.3 节能技改项目`：表格 CRUD，是否合同能源管理 Toggle
- [ ] `4.4 计量器具汇总`：表格 CRUD，能源品种关联 `EnergySelect`，状态字典
- [ ] `4.5 计量器具配备率`：固定3层级×N能源品种的矩阵表格，行内编辑

**DoD：** 三个模块支持增删改查和 Excel 导入导出。

---

### Sprint 4.3 — 表格类模块（4.8 / 4.9 / 4.10）
**PR 名称：** `feat: data-entry - equipment summary (4.8), test (4.9), obsolete (4.10)`

**后端：**
- [ ] `/api/entry/equipment-summary`：主要用能设备汇总（4.8）
- [ ] `/api/entry/equipment-test`：重点设备测试数据（4.9）
- [ ] `/api/entry/obsolete-equipment`：淘汰设备目录（4.10）

**前端：**
- [ ] `4.8 主要用能设备汇总`：表格 CRUD，分类使用 `DictSelect`
- [ ] `4.9 重点设备测试数据`：表格 CRUD，区域/判别字典，实测值/合格值颜色标注
- [ ] `4.10 淘汰设备目录`：表格 CRUD，日期选择器

**DoD：** 三个设备相关模块可完整录入。

---

### Sprint 4.4 — 表格类模块（4.12 / 4.13 / 4.14 / 4.17-4.21）
**PR 名称：** `feat: data-entry - product energy, saving calc, management modules (4.12-4.21)`

**后端：**
- [ ] `/api/entry/product-unit-consumption`：单位产品能耗（4.12，关联产品）
- [ ] `/api/entry/product-energy-cost`：产品能源成本（4.13，关联产品）
- [ ] `/api/entry/saving-calc`：节能量计算（4.14，按年份双行）
- [ ] `/api/entry/saving-potential`：节能潜力明细（4.17）
- [ ] `/api/entry/management-policy`：能源管理制度（4.18）
- [ ] `/api/entry/improvement`：管理改进建议（4.19）
- [ ] `/api/entry/tech-reform`：技改建议汇总（4.20）
- [ ] `/api/entry/rectification`：整改措施（4.21）

**前端：**
- [ ] 8个模块的标准 CRUD 表格，均使用 `DataTable.vue` + `ProductSelect.vue`
- [ ] 产品相关模块与 3.4 产品设置数据联动

**DoD：** 8个管理类模块可完整录入。

---

## Wave 5 — 数据录入（复杂模块）
> 目标：完成依赖计算/多维度/跨表关联的复杂录入模块。

### Sprint 5.1 — SpreadJS 驱动模块（4.6 / 4.7）
**PR 名称：** `feat: data-entry - equipment benchmark (4.6) and equipment efficiency (4.7) via SpreadJS`

**后端：**
- [ ] 为 4.6、4.7 创建对应模板记录（template_code: `EQUIP_BENCHMARK`, `EQUIP_EFFICIENCY`）
- [ ] 管理端初始化并发布这两个模板（包含 Tag 映射）
- [ ] 填报数据存入 `tpl_submission` + 结构化字段存入 `de_equipment_benchmark` / `de_equipment_energy`

**前端：**
- [ ] `4.6 能效对标`：使用 `SpreadSheet.vue` 组件，加载模板进行填报
- [ ] `4.7 重点设备能耗和效率`：使用 `SpreadSheet.vue`，多位置×设备类型的多维表格

**DoD：** 两个复杂模块可通过 SpreadJS 模板完成填报。

---

### Sprint 5.2 — 能源平衡 & 温室气体排放（4.11.1 / 4.15）
**PR 名称：** `feat: data-entry - energy balance (4.11.1) and GHG emission (4.15)`

**后端：**
- [ ] `GET/POST/PUT/DELETE /api/entry/energy-balance`：能源平衡表（4.11.1，关联能源）
- [ ] `GET/POST/PUT/DELETE /api/entry/ghg-emission`：温室气体排放（4.15）
- [ ] 排放量自动计算：`排放量 = 活动数据 × 碳排放因子`（调用 cm_emission_factor）
- [ ] `GET /api/carbon/factor`：碳排放因子查询（供 4.15 计算用）

**前端：**
- [ ] `4.11.1 能源平衡表`：基于能源品种列表的动态行表格（每行=一种能源，列固定）
  - 期末库存 = 期初 + 购入 - 消耗 - 转出（前端实时计算展示）
- [ ] `4.15 温室气体排放`：
  - 排放源明细表格（活动数据填入）
  - 排放量列：从接口获取自动计算结果（不可直接编辑）
  - 排放类型使用 `DictSelect`

**DoD：** 能源平衡表和温室气体排放表可完整录入，排放量自动计算正确。

---

### Sprint 5.3 — 十四五目标 & 能源数据源 & 查询（4.22 / 4.23 / 4.24）
**PR 名称：** `feat: data-entry - five-year targets (4.22), energy sources (4.23), query (4.24)`

**后端：**
- [ ] `GET/POST/PUT /api/entry/five-year-target`：十四五目标（4.22）
- [ ] `GET/POST/PUT/DELETE /api/entry/energy-ghg-source`：能源与温室气体排放源（4.23）
- [ ] `GET /api/entry/energy-data-query`：能耗相关数据聚合查询（4.24，只读，跨表统计）
  - 聚合：各能源消耗量/产值/增加值/单位能耗

**前端：**
- [ ] `4.22 十四五目标`：复杂分组表单（2020实际/2025目标/2020-2025三列×多行指标）+ 按年度目标子表格
- [ ] `4.23 能源数据和温室气体排放源`：动态行表格（关联能源品种）
- [ ] `4.24 能耗数据查询`：只读统计表格 + 导出 Excel 功能

**DoD：** 十四五目标可录入，4.24 能展示跨模块聚合数据。

---

## Wave 6 — 能源流程图（AntV X6）
> 目标：完成交互式能源流程图的绘制与数据持久化。

### Sprint 6.1 — AntV X6 集成 & 分层图式（4.11.2）
**PR 名称：** `feat: energy-flow - AntV X6 integration and layer diagram (4.11.2)`

**后端：**
- [ ] `GET/PUT /api/flow-diagram/{enterpriseId}/{auditYear}/{type}`：流程图读写
- [ ] `GET/POST/PUT/DELETE /api/flow-diagram/node`：节点 CRUD
- [ ] `GET/POST/PUT/DELETE /api/flow-diagram/edge`：连线 CRUD

**前端：**
- [ ] 安装 `@antv/x6` + `@antv/x6-vue-shape`
- [ ] `FlowEditor.vue` 完整实现：
  - 左侧节点面板：可拖拽节点类型（外购能源/加工转换/分配输送/终端使用/产品/固定节点）
  - 画布：节点拖拽/连线拖拽/删除/缩放/全屏
  - 右侧属性面板：点击节点/连线后显示属性编辑（关联能源/产品/实物量）
  - 工具栏：保存/撤销/重做/导出图片
- [ ] `4.11.2 分层图式`：调用 `FlowEditor.vue`，节点类型为四大用能环节

**DoD：** 可在画布上拖拽创建能流图，保存后可重新加载恢复状态。

---

### Sprint 6.2 — 单元图式 & 二维表式（4.11.3 / 4.11.4）
**PR 名称：** `feat: energy-flow - unit diagram (4.11.3) and matrix table (4.11.4)`

**前端：**
- [ ] `4.11.3 单元图式`：调用 `FlowEditor.vue`，节点从 3.3 用能单元中选择
  - 节点属性面板：关联对应单元的能源列表
- [ ] `4.11.4 二维表式`：源单元×目的单元的矩阵表格输入
  - 行：源单元（3.3），列：目的单元（3.3）+ 能源品种
  - 单元格：实物量输入
  - 与图式数据联动：矩阵数据和图式连线数据保持一致

**DoD：** 三种输入方式（4.11.2/3/4）均可正常使用，数据互通。

---

## Wave 7 — 图表输出
> 目标：基于录入数据生成规定图表和报告辅助图表。

### Sprint 7.1 — 规定图表（5.1）
**PR 名称：** `feat: charts - standard charts (5.1)`

**后端：**
- [ ] `GET /api/chart/standard/{code}`：按图表编码返回图表数据（查询 de_* 表聚合）
- [ ] 实现以下规定图表的数据查询接口：
  - 能源消费结构（饼图）
  - 综合能耗趋势（折线图）
  - 各单元能耗分布（柱状图）
  - 温室气体排放构成（饼图）
  - 单位产品能耗对比（柱状图）

**前端：**
- [ ] 安装 `echarts` + `vue-echarts`
- [ ] 封装 `ChartCard.vue`：图表卡片（含标题/导出/全屏）
- [ ] `5.1 规定图表`：Grid 布局展示5类规定图表 + 支持导出图片/导出Excel数据

**DoD：** 5类规定图表基于真实填报数据正确渲染。

---

### Sprint 7.2 — 报告辅助图表（5.2）
**PR 名称：** `feat: charts - report assistant charts (5.2)`

**后端：**
- [ ] `GET /api/chart/assist/list`：辅助图表列表
- [ ] `GET /api/chart/assist/{code}`：辅助图表数据
- [ ] 实现以下辅助图表：
  - 能流图 Sankey（基于 4.11 数据）
  - 节能项目投资效益对比
  - 产品产量与能耗关联趋势
  - 设备能效分布

**前端：**
- [ ] `5.2 报告辅助图表`：可配置图表面板
  - 支持图表显示/隐藏切换
  - 支持导出选定图表为 Word 插图
- [ ] Sankey 图组件（ECharts sankey series）

**DoD：** 辅助图表可展示并导出。

---

## Wave 8 — 审计报告（OnlyOffice）
> 目标：完成报告自动生成与在线编辑全流程。

### Sprint 8.1 — 报告生成引擎
**PR 名称：** `feat: report - auto generation engine and info input (6.1, 6.2)`

**后端：**
- [ ] 安装 `Apache POI` (docx4j 或 poi-ooxml)：Word 报告生成
- [ ] `POST /api/report/generate`：触发报告自动生成
  - 读取企业全部 de_* 数据 → 填充 Word 模板 → 生成初始报告
  - 存储到本地 `upload/report/` 目录
  - 更新 `ar_report` 状态为 `GENERATED`
- [ ] `GET /api/report`：报告列表
- [ ] `GET /api/report/{id}/download`：下载报告文件

**前端：**
- [ ] `6.1 信息录入`：补充报告元信息（审计周期/审计机构/审计人员等）
- [ ] `6.2 在线生成报告`：
  - 数据完整度检查展示（哪些模块未填）
  - "生成初始报告"按钮 + 生成进度展示
  - 报告预览（Word 文件链接/在线查看）

**DoD：** 点击生成按钮后能产出包含真实数据的 Word 报告。

---

### Sprint 8.2 — OnlyOffice 在线编辑 & 报告上传（6.3 / 6.4）
**PR 名称：** `feat: report - OnlyOffice online editor and report management (6.3, 6.4)`

**后端：**
- [ ] 部署 OnlyOffice Document Server（Docker 服务追加到 docker-compose.yml）
- [ ] `POST /api/report/{id}/edit-token`：生成 OnlyOffice 编辑 Token + DocumentKey
- [ ] OnlyOffice 回调接口：`POST /api/report/onlyoffice/callback`（接收文档保存回调）
- [ ] `POST /api/report/{id}/upload`：企业上传最终报告文件（multipart）
- [ ] `PUT /api/report/{id}/submit`：企业提交报告进入审核流程

**前端：**
- [ ] `DocEditor.vue` 完整实现：嵌入 OnlyOffice 在线编辑器
- [ ] `6.2 在线生成报告`（增强）：生成后可直接在线编辑（OnlyOffice 嵌入）
- [ ] `6.3 上传最终报告`：文件上传（支持 .docx/.pdf），上传成功后展示预览
- [ ] `6.4 报告详情`：报告信息/版本历史/下载/提交审核按钮

**DoD：** 企业可在线编辑初始报告并上传最终版，完整报告管理流程走通。

---

## Wave 9 — 审核工作流
> 目标：打通审核端工作流，实现报告单级审核和整改跟踪。

### Sprint 9.1 — 审核任务管理
**PR 名称：** `feat: audit-workflow - task management and auditor portal`

**后端：**
- [ ] 企业提交报告时，自动创建 `aw_audit_task`（状态：PENDING）
- [ ] `GET /api/audit/task`：任务列表（支持按状态/企业/年度筛选）
- [ ] `POST /api/audit/task/{id}/assign`：分配审核员
- [ ] `POST /api/audit/task/{id}/approve`：审核通过
- [ ] `POST /api/audit/task/{id}/reject`：退回（含退回意见）
- [ ] `POST /api/audit/task/{id}/comment`：添加审核意见
- [ ] `GET /api/audit/task/{id}/logs`：审核日志
- [ ] 定时任务（`@Scheduled`）：检查超期未处理任务，状态标记/日志告警

**前端（审核端）：**
- [ ] 审核端 Dashboard：任务统计（待审/进行中/已完成/超期）
- [ ] `/auditor/tasks`：任务列表（状态筛选/分配操作）
- [ ] `/auditor/review`：审核详情页
  - 企业信息 + 填报数据汇总（只读）
  - 嵌入报告预览
  - 审核意见输入 + 通过/退回操作
  - 审核日志时间线

**前端（企业端）：**
- [ ] 审核状态展示：报告被退回时显示退回意见，支持修改后重新提交

**DoD：** 企业提交→审核分配→通过/退回→企业重提的完整流程可走通。

---

### Sprint 9.2 — 整改跟踪 & 预警
**PR 名称：** `feat: audit-workflow - rectification tracking and overdue warning`

**后端：**
- [ ] `POST /api/audit/rectification`：创建整改跟踪项（审核通过时从 4.21 数据同步）
- [ ] `GET/PUT /api/audit/rectification`：整改项列表/更新进度
- [ ] `POST /api/audit/task/{taskId}/complete`：任务整体完结
- [ ] 定时任务：整改超期自动更新状态为 OVERDUE，写入告警日志
- [ ] `GET /api/audit/warning`：告警列表（管理端可查）

**前端（企业端）：**
- [ ] 企业工作台：整改任务待办展示（与 Dashboard 联动）
- [ ] 整改跟踪列表：每项显示整改要求/状态/截止日期/实际完成日期

**前端（管理端）：**
- [ ] `/admin/audit-manage`：审计管理总览（企业审计状态/超期预警红标）

**DoD：** 整改任务可追踪，超期项自动标红预警。

---

## Wave 10 — 碳排放管理 & 平台对接
> 目标：完善碳排放因子管理，对接上海产业绿色发展综合服务大平台。

### Sprint 10.1 — 碳排放因子管理 & GHG 自动计算优化
**PR 名称：** `feat: carbon - emission factor management and GHG auto-calculation`

**后端：**
- [ ] `GET/POST/PUT/DELETE /api/carbon/factor`：碳排放因子 CRUD（管理端）
- [ ] 因子表支持：能源类型/因子值/计量单位/数据来源/生效年份
- [ ] 4.15 温室气体排放量自动计算优化：
  - 保存活动数据时触发计算：`排放量 = 活动数据 × 对应因子`
  - 支持"1+N"通则：固定排放源 + N 个移动/间接排放源
  - 计算结果写入 `total_emission` 字段

**前端（管理端）：**
- [ ] `/admin/emission-factor`：碳排放因子管理表格（分能源类型分组展示）

**前端（企业端）：**
- [ ] `4.15 温室气体排放`：活动数据填入后自动展示计算排放量（实时预览）

**DoD：** 碳排放因子可配置，4.15 排放量自动计算准确。

---

### Sprint 10.2 — 大平台接口对接
**PR 名称：** `feat: integration - Shanghai green platform API integration`

**后端（待甲方提供接口文档后实现）：**
- [ ] `integration` 包：封装大平台 HTTP Client（`RestTemplate` + 超时/重试配置）
- [ ] 企业信息同步：`GET /api/integration/enterprise/sync`
  - 从大平台拉取企业基本信息，更新本地 `ent_enterprise_setting`
  - 支持反向维护：本地修改后推送到大平台
- [ ] 数据推送：`POST /api/integration/push`
  - 将企业填报数据（能耗/碳排放汇总）推送到大平台
  - 批量推送 + 单条补推
- [ ] 对接日志：记录每次同步结果（成功/失败/数据快照）

**前端（管理端）：**
- [ ] 对接状态监控页：最近同步时间/同步状态/错误信息
- [ ] 手动触发同步按钮

**DoD：** 大平台接口可完成双向数据同步（具体依赖甲方接口文档）。

---

## Wave 11 — 收尾优化 & 测试
> 目标：系统稳定、安全、性能达标，可交付演示。

### Sprint 11.1 — 性能优化 & 安全加固
**PR 名称：** `feat: optimization - cache strategy, security hardening, file management`

**后端：**
- [ ] Ehcache 缓存策略全面梳理：
  - `dictCache`：字典数据（TTL 1h，变更时清除）
  - `energyCache`：企业能源品种（TTL 30min，按 enterpriseId 分组）
  - `templateCache`：模板元数据（TTL 1h，发布时清除）
- [ ] 文件上传安全：类型白名单（.xlsx/.docx/.pdf）+ 文件名随机化 + 大小限制
- [ ] SQL 注入防护：MyBatis 参数绑定审查（禁止 `${}` 拼接）
- [ ] 接口限流：基于 IP 对登录接口限流（Ehcache 计数器）
- [ ] 数据隔离全面审查：所有 Mapper 的 SQL 必须携带 `enterprise_id = #{enterpriseId}` 条件

**前端：**
- [ ] 路由权限精细化：企业端/管理端/审核端路由严格按角色隔离
- [ ] 大文件加载优化：SpreadJS / AntV X6 / ECharts 按需异步加载（动态 import）
- [ ] 404/403/500 错误页面

**DoD：** 安全审查无高危漏洞，关键接口有缓存和限流保护。

---

### Sprint 11.2 — 测试 & Bug 修复 & 部署文档
**PR 名称：** `feat: testing - E2E test, bug fixes, deployment guide`

**后端：**
- [ ] 单元测试：Service 层关键方法测试（SpreadsheetDataExtractor / EditLockService / 报告生成）
- [ ] 接口测试：用 Postman Collection 覆盖全部 API（含认证/边界条件）
- [ ] 数据完整性验证脚本：检查 de_* 表数据一致性

**前端：**
- [ ] 主流程 E2E 冒烟测试：
  - 注册 → 登录 → 基础设置 → 数据录入 → 生成报告 → 审核通过
- [ ] 响应式适配：1366×768 / 1920×1080 分辨率验证
- [ ] 浏览器兼容性：Chrome / Edge 最新版

**运维：**
- [ ] `DEPLOY.md`：生产环境部署指南（Docker Compose / 环境变量说明）
- [ ] 数据备份脚本：MySQL 每日自动备份
- [ ] 日志配置：按天滚动，保留30天

**DoD：** 主流程可完整演示，部署文档齐全，系统可在新环境一键启动。

---

## 附录：PR 规范

### Branch 命名
```
feat/wave{N}-sprint{M}-{short-desc}
例：feat/wave2-sprint1-company-settings
```

### PR Checklist（每个 PR 合并前必须完成）
- [ ] 后端：`mvn compile` 无报错
- [ ] 前端：`npm run build` 无报错
- [ ] 新增接口有 Swagger `@Tag` / `@Operation` 注解
- [ ] 新增实体类与数据库字段一致
- [ ] 业务数据查询均携带 `enterprise_id` 过滤（数据隔离）
- [ ] 无 hardcode 密钥/密码
- [ ] PR Description 包含：变更说明 / 截图（前端）/ 测试步骤

### Commit Message 规范
```
feat(module): 功能描述
fix(module): 修复描述
refactor(module): 重构描述
```
