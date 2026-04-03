# 能碳审计平台 — 完整开发计划

> 技术栈：Spring Boot 3 + MyBatis + Vue 3 + MySQL + Ehcache3 + SpreadJS + AntV X6 + OnlyOffice  
> 前端风格：方案B 绿色低碳风  
> 协作规范：每个 Sprint 完成后推一个 PR，合并后进入下一个 Sprint  
> 核心理念：**SpreadJS 模板定义所有数据采集/计算逻辑，软件负责工作流管理**

---

## 总览

| Wave | 主题 | Sprint 数 | 预估周期 | 状态 |
|------|------|-----------|----------|------|
| Wave 0 | 基础设施 & 认证 | 2 | 2 周 | ✅ 已完成 |
| Wave 1 | 管理端基础功能 | 2 | 2 周 | ✅ 已完成 |
| Wave 2 | 企业基础设置 | 2 | 2 周 | ✅ 已完成 |
| Wave 3 | SpreadJS 模板引擎 | 3 | 3 周 | ✅ 已完成 |
| Wave 4 | 模板驱动数据抽取引擎 | 1 | 1 周 | ✅ 已完成 |
| Wave 5 | 菜单重组 & 抽取数据总览 | 2 | 2 周 | 待开发 |
| Wave 6 | 能源流程图（AntV X6）| 2 | 2 周 | 待开发 |
| Wave 7 | 图表输出 | 2 | 2 周 | 待开发 |
| Wave 8 | 审计报告（OnlyOffice）| 2 | 2 周 | 待开发 |
| Wave 9 | 审核工作流 | 2 | 2 周 | 待开发 |
| Wave 10 | 碳排放管理 & 平台对接 | 2 | 2 周 | 部分完成 |
| Wave 11 | 收尾优化 & 测试 | 2 | 2 周 | 待开发 |
| **合计** | | **22 个 Sprint / PR** | **~22 周** | |

---

## Wave 0 — 基础设施 & 认证 ✅

> 目标：项目可运行、可部署，认证主流程贯通。

### 已完成产出

**Sprint 0.1 — 基础设施** (`PR #1`)
- Docker Compose 生产环境配置（MySQL 8 + 后端 + 前端 Nginx）
- SQL 初始化脚本（`sql/00-schema.sql` + `sql/01-init-data.sql`，~55 张表）
- H2 内存数据库开发环境（`application-dev.yml` + `schema-h2.sql` + `data-h2.sql`）
- Maven 多模块结构（audit-common / audit-model / audit-dao / audit-service / audit-web）
- 全局样式方案B落地、Element Plus 主题色 `#00897B`
- 三端 Layout 组件 + 路由守卫（未登录跳转 `/login`，登录后按角色分流）

**Sprint 0.2 — 认证模块** (`PR #1`)
- JWT 认证完整实现：login / logout / info / password
- JWT 拦截器 + `SecurityUtils` 线程变量注入
- 登录页（三端 Tab 切换）、强制改密弹窗
- `stores/user.ts` + `utils/request.ts`（Token 注入、401 拦截）
- 6 个 MyBatis Mapper XML 文件

---

## Wave 1 — 管理端基础功能 ✅

> 目标：管理员能管理企业账号和系统字典。

### 已完成产出

**Sprint 1.1 — 企业管理 & 注册审核** (`PR #2`)
- 企业 CRUD + 锁定/解锁/到期管理
- 注册申请列表 + 审核通过/拒绝
- 管理端企业管理页面（搜索/分页/详情抽屉/操作弹窗）

**Sprint 1.2 — 用户管理 & 系统字典** (`PR #2`)
- 用户 CRUD（管理端/审核端账号）
- 字典类型 + 字典数据双层管理（含 Ehcache 缓存）
- 公共组件 `DictSelect.vue` / `DictTag.vue`
- 字典管理页面（类型树 + 数据表格二级联动）

---

## Wave 2 — 企业基础设置 ✅

> 目标：企业完成基础数据配置，为后续数据录入提供主数据。

### 已完成产出

**Sprint 2.1 — 企业信息 & 能源品种** (`PR #3`)
- 企业设置 CRUD（按企业 ID 隔离）
- 能源品种 CRUD（含"从全局目录导入"功能）
- 企业信息分区表单 + 能源品种表格管理

**Sprint 2.2 — 用能单元 & 产品设置** (`PR #4`)
- 用能单元 CRUD（三种类型：加工转换/分配输送/终端使用）
- 单元-能源关联管理
- 产品 CRUD
- 三 Tab 用能单元页面 + 产品设置页面

---

## Wave 3 — SpreadJS 模板引擎 ✅

> 目标：建立 SpreadJS 模板的完整生命周期管理机制，作为数据录入的核心基础设施。

### 已完成产出

**Sprint 3.1 — 模板管理后端** (`PR #5`)
- 模板 CRUD + 版本管理（草稿/发布）
- 标签映射 CRUD（Tag → 字段名/目标表/数据类型/是否必填）
- 企业填报数据 CRUD + 数据抽取触发
- 悲观锁（acquire / release / check）

**Sprint 3.2 — 管理端模板设计器** (`PR #6`)
- SpreadJS V18.2.5 升级（V17 → V18，含 API 兼容适配）
- `SpreadDesigner.vue` 组件（在线模板设计器）
- 模板列表页 + Designer 全屏设计
- 标签发现（Named Range + Cell Tag 自动识别）
- 字段映射配置面板（侧边栏）
- SpreadJS 授权配置（`VITE_SPREADJS_LICENSE` 环境变量 + 集中初始化模块）

**Sprint 3.3 — 企业端填报 & 编辑锁** (`PR #7`)
- `SpreadSheet.vue` 填报组件完整实现
- 编辑锁机制（获取锁/心跳续约/释放锁）
- 数据保存流程（遍历 Tag → 构建数据 Map → 提交）
- 填报进度总览页面（模板列表 + 完成状态）
- 报告详情页面（查看抽取数据）

---

## Wave 4 — 模板驱动数据抽取引擎 ✅

> **⚠️ 架构重大变更**：原计划为 24 个独立 CRUD 数据录入页面（旧 Wave 4 标准模块 + 旧 Wave 5 复杂模块），
> 实际采用了**模板驱动的数据抽取引擎**方案。所有审计数据通过 SpreadJS 模板填报 + SCALAR/TABLE
> 双映射引擎自动抽取到 `de_*` 业务表，不再需要独立的 CRUD 页面和 `/api/entry/*` 接口。

### 已完成产出 (`PR #10`)

**SCALAR + TABLE 双映射引擎**
- `tpl_tag_mapping` 表扩展：`mapping_type`（SCALAR/TABLE）、`source_type`（NAMED_RANGE/CELL_TAG）、`row_key_column`、`column_mappings`（JSON）、`header_row`
- `DiscoveredField` DTO：自动检测 Named Range（单格→SCALAR，多格→TABLE）vs Cell Tag（默认 SCALAR，手动可配 TABLE）
- `SpreadsheetDataExtractor`：支持 SCALAR 提取（单元格值）和 TABLE 提取（逐行提取 + column_mappings 列映射）
- `DataPersistenceService`：按 `target_table` 路由到业务表或通用表
- `BusinessTablePersister`：动态 SQL 插入/更新，列名白名单校验（`^[a-z][a-z0-9_]{0,63}$`）

**12 张 de_* 业务表**
- 10 张核心业务表：`de_company_overview` / `de_tech_indicator` / `de_energy_consumption` / `de_energy_conversion` / `de_product_unit_consumption` / `de_equipment_detail` / `de_carbon_emission` / `de_energy_balance` / `de_energy_flow` / `de_five_year_target`
- 2 张通用存储表：`de_submission_field`（SCALAR 兜底）/ `de_submission_table`（TABLE 兜底）
- MySQL 生产迁移脚本：`sql/02-wave4-data-extraction.sql`
- H2 开发 schema 同步更新

**前端字段映射面板增强**
- 管理端模板设计器侧边栏重命名为"字段映射配置"
- 显示源类型标签（Named Range / Cell Tag）
- 映射类型选择器（SCALAR / TABLE）
- TABLE 专属配置面板（cellRange、headerRow、columnMappings JSON、rowKeyColumn）
- 映射同步保存逻辑（replaceAll API）
- TABLE 校验：Cell Tag 类型需要手动配置 cellRange + headerRow

---

## Wave 5 — 菜单重组 & 抽取数据总览

> 目标：删除 24 个空壳数据录入页面，重组企业端菜单结构，新增结构化数据查看功能。
> 让企业用户能确认通过模板填报后自动抽取到 `de_*` 业务表的数据是否正确。

### Sprint 5.1 — 企业端菜单重组 & 路由清理

**PR 名称：** `feat: menu-restructure - remove 24 placeholders, reorganize enterprise menu`

**前端：**
- [ ] 删除 24 个空壳 entry 页面组件（`audit-ui/src/views/enterprise/entry/*/index.vue`）
- [ ] 清理对应的 24 条路由定义（`router/index.ts`）
- [ ] 重组 `menus.ts` 企业端菜单结构：
  - **工作台**：概览（不变）
  - **基础设置**：企业信息 / 能源品种 / 用能单元 / 产品设置（不变）
  - **数据填报**：模板填报（原 report/input）/ 填报进度（原 report/generate）/ 抽取数据总览（新增）
  - **图表分析**：规定图表 / 报告辅助图表（保留，后续迭代实现）
  - **报告管理**：上传最终报告 / 报告详情（精简）
- [ ] 调整路由路径以匹配新菜单结构

**DoD：** 企业端菜单精简为 ~12 项（原 ~32 项），无空壳页面，所有路由可正常访问。

---

### Sprint 5.2 — 抽取数据总览页面

**PR 名称：** `feat: extracted-data-overview - structured table view for de_* business data`

**后端：**
- [ ] `GET /api/extracted-data/{tableName}`：按业务表名查询抽取数据（分页，按 enterprise_id 隔离）
- [ ] `GET /api/extracted-data/tables`：返回当前企业有数据的业务表列表及记录数
- [ ] 支持按 audit_year / submission_id 筛选
- [ ] 返回结构化列定义（列名 → 中文标签映射）

**前端：**
- [ ] 新建"抽取数据总览"页面（路径 `/enterprise/data/overview`）
- [ ] 按业务维度分 Tab 展示（企业概况 / 技术指标 / 能源消费 / 能源转换 / 产品单耗 / 设备明细 / 碳排放 / 能源平衡 / 能源流向 / 十四五目标）
- [ ] 每个 Tab 为结构化 el-table（列名中文化、数值格式化、日期格式化）
- [ ] 只读模式，不可编辑
- [ ] 支持按审计年度筛选
- [ ] 支持导出 Excel

**DoD：** 企业可按维度查看所有已抽取的业务数据，数据与 SpreadJS 填报内容一致。

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
- [ ] `FlowEditor.vue` 完整实现（当前为骨架占位）：
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
- [ ] `4.11.3 单元图式`：调用 `FlowEditor.vue`，节点从用能单元设置中选择
  - 节点属性面板：关联对应单元的能源列表
- [ ] `4.11.4 二维表式`：源单元×目的单元的矩阵表格输入
  - 行：源单元，列：目的单元 + 能源品种
  - 单元格：实物量输入
  - 与图式数据联动：矩阵数据和图式连线数据保持一致

**DoD：** 三种输入方式（4.11.2/3/4）均可正常使用，数据互通。

---

## Wave 7 — 图表输出

> 目标：基于 `de_*` 业务表中的抽取数据生成规定图表和报告辅助图表。

### Sprint 7.1 — 规定图表（5.1）

**PR 名称：** `feat: charts - standard charts (5.1)`

**后端：**
- [ ] `GET /api/chart/standard/{code}`：按图表编码返回图表数据（查询 `de_*` 表聚合）
- [ ] 实现以下规定图表的数据查询接口：
  - 能源消费结构（饼图，数据源：`de_energy_consumption`）
  - 综合能耗趋势（折线图，数据源：`de_tech_indicator`）
  - 各单元能耗分布（柱状图，数据源：`de_energy_balance`）
  - 温室气体排放构成（饼图，数据源：`de_carbon_emission`）
  - 单位产品能耗对比（柱状图，数据源：`de_product_unit_consumption`）

**前端：**
- [ ] 安装 `echarts` + `vue-echarts`
- [ ] 封装 `ChartCard.vue`：图表卡片（含标题/导出/全屏）
- [ ] `5.1 规定图表`：Grid 布局展示5类规定图表 + 支持导出图片/导出Excel数据

**DoD：** 5类规定图表基于 `de_*` 表真实填报数据正确渲染。

---

### Sprint 7.2 — 报告辅助图表（5.2）

**PR 名称：** `feat: charts - report assistant charts (5.2)`

**后端：**
- [ ] `GET /api/chart/assist/list`：辅助图表列表
- [ ] `GET /api/chart/assist/{code}`：辅助图表数据
- [ ] 实现以下辅助图表：
  - 能流图 Sankey（数据源：`de_energy_flow`）
  - 节能项目投资效益对比（数据源：模板抽取数据）
  - 产品产量与能耗关联趋势（数据源：`de_product_unit_consumption`）
  - 设备能效分布（数据源：`de_equipment_detail`）

**前端：**
- [ ] `5.2 报告辅助图表`：可配置图表面板
  - 支持图表显示/隐藏切换
  - 支持导出选定图表为 Word 插图
- [ ] Sankey 图组件（ECharts sankey series）

**DoD：** 辅助图表可展示并导出。

---

## Wave 8 — 审计报告（OnlyOffice）

> 目标：完成报告自动生成与在线编辑全流程。数据源为 `de_*` 业务表。

### Sprint 8.1 — 报告生成引擎

**PR 名称：** `feat: report - auto generation engine and info input (6.1, 6.2)`

**后端：**
- [ ] 安装 `Apache POI` (docx4j 或 poi-ooxml)：Word 报告生成
- [ ] `POST /api/report/generate`：触发报告自动生成
  - 读取企业全部 `de_*` 数据 → 填充 Word 模板 → 生成初始报告
  - 存储到本地 `upload/report/` 目录
  - 更新 `ar_report` 状态为 `GENERATED`
- [ ] `GET /api/report`：报告列表
- [ ] `GET /api/report/{id}/download`：下载报告文件

**前端：**
- [ ] `6.1 信息录入`：补充报告元信息（审计周期/审计机构/审计人员等）
- [ ] `6.2 在线生成报告`：
  - 数据完整度检查展示（哪些模板未填）
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
- [ ] `DocEditor.vue` 完整实现（当前为骨架占位）：嵌入 OnlyOffice 在线编辑器
- [ ] `6.2 在线生成报告`（增强）：生成后可直接在线编辑（OnlyOffice 嵌入）
- [ ] `6.3 上传最终报告`：文件上传（支持 .docx/.pdf），上传成功后展示预览
  - 注：当前页面 UI 已存在，但文件上传功能待接入 OSS 存储
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
  - 注：当前为简单欢迎语占位，需完整实现
- [ ] `/auditor/tasks`：任务列表（状态筛选/分配操作）
  - 注：当前为"功能开发中"占位
- [ ] `/auditor/review`：审核详情页
  - 企业信息 + 填报数据汇总（只读，从 `de_*` 表读取）
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
- [ ] `POST /api/audit/rectification`：创建整改跟踪项（审核通过时从填报数据同步）
- [ ] `GET/PUT /api/audit/rectification`：整改项列表/更新进度
- [ ] `POST /api/audit/task/{taskId}/complete`：任务整体完结
- [ ] 定时任务：整改超期自动更新状态为 OVERDUE，写入告警日志
- [ ] `GET /api/audit/warning`：告警列表（管理端可查）

**前端（企业端）：**
- [ ] 企业工作台：整改任务待办展示（与 Dashboard 联动）
- [ ] 整改跟踪列表：每项显示整改要求/状态/截止日期/实际完成日期

**前端（管理端）：**
- [ ] `/admin/audit-manage`：审计管理总览（企业审计状态/超期预警红标）
  - 注：当前为"功能开发中"占位，需完整实现

**DoD：** 整改任务可追踪，超期项自动标红预警。

---

## Wave 10 — 碳排放管理 & 平台对接

> 目标：完善碳排放因子管理，对接上海产业绿色发展综合服务大平台。
> 注：碳排放因子管理的管理端 CRUD 页面（`/admin/emission-factor`）已在 Wave 1 中实现，
> 支持因子 CRUD、生效年份、数据来源。本 Wave 聚焦自动计算优化和大平台对接。

### Sprint 10.1 — GHG 自动计算优化

**PR 名称：** `feat: carbon - GHG auto-calculation enhancement`

**后端：**
- [ ] 温室气体排放量自动计算优化（基于 `de_carbon_emission` 表数据）：
  - 保存活动数据时触发计算：`排放量 = 活动数据 × 对应因子`
  - 支持"1+N"通则：固定排放源 + N 个移动/间接排放源
  - 计算结果回写 `de_carbon_emission` 表
- [ ] `GET /api/carbon/factor/{energyType}`：按能源类型查询因子（供计算用）

**前端（管理端）：**
- [x] `/admin/emission-factor`：碳排放因子管理表格（已实现 — 支持 CRUD、生效年份、数据来源）

**DoD：** 碳排放因子可配置，温室气体排放量基于因子自动计算准确。

---

### Sprint 10.2 — 大平台接口对接

**PR 名称：** `feat: integration - Shanghai green platform API integration`

**后端（待甲方提供接口文档后实现）：**
- [ ] `integration` 包：封装大平台 HTTP Client（`RestTemplate` + 超时/重试配置）
- [ ] 企业信息同步：`GET /api/integration/enterprise/sync`
  - 从大平台拉取企业基本信息，更新本地 `ent_enterprise_setting`
  - 支持反向维护：本地修改后推送到大平台
- [ ] 数据推送：`POST /api/integration/push`
  - 将企业填报数据（能耗/碳排放汇总，来自 `de_*` 表）推送到大平台
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
  - 注意 `BusinessTablePersister` 动态 SQL 的列名白名单校验已实现
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
- [ ] 数据完整性验证脚本：检查 `de_*` 表数据一致性

**前端：**
- [ ] 主流程 E2E 冒烟测试：
  - 登录 → 基础设置 → 模板填报 → 数据抽取 → 查看抽取数据 → 生成报告 → 审核通过
- [ ] 响应式适配：1366×768 / 1920×1080 分辨率验证
- [ ] 浏览器兼容性：Chrome / Edge 最新版

**运维：**
- [ ] `DEPLOY.md`：生产环境部署指南（Docker Compose / 环境变量说明）
- [ ] 数据备份脚本：MySQL 每日自动备份
- [ ] 日志配置：按天滚动，保留30天

**DoD：** 主流程可完整演示，部署文档齐全，系统可在新环境一键启动。

---

## 附录 A：各端页面实现状态

### 管理端（Admin Portal）

| 页面 | 路径 | 状态 | 所属 Wave |
|------|------|------|-----------|
| 管理首页 | `/admin/dashboard` | ⚠️ 占位（欢迎语） | Wave 9+ |
| 企业管理 | `/admin/enterprise` | ✅ 已实现 | Wave 1 |
| 注册审核 | `/admin/registration` | ✅ 已实现 | Wave 1 |
| 用户管理 | `/admin/system/users` | ✅ 已实现 | Wave 1 |
| 字典管理 | `/admin/system/dict` | ✅ 已实现 | Wave 1 |
| 模板管理 | `/admin/template` | ✅ 已实现 | Wave 3 |
| 能源品类管理 | `/admin/energy-category` | ✅ 已实现 | Wave 2 |
| 碳排放因子管理 | `/admin/emission-factor` | ✅ 已实现 | Wave 1 |
| 审计管理 | `/admin/audit-manage` | ⚠️ 占位 | Wave 9 |

### 企业端（Enterprise Portal）

| 页面 | 路径 | 状态 | 所属 Wave |
|------|------|------|-----------|
| 概览 | `/enterprise/dashboard` | ✅ 已实现（Mock 数据） | Wave 0 |
| 企业信息 | `/enterprise/settings/company` | ✅ 已实现 | Wave 2 |
| 能源品种 | `/enterprise/settings/energy` | ✅ 已实现 | Wave 2 |
| 用能单元 | `/enterprise/settings/unit` | ✅ 已实现 | Wave 2 |
| 产品设置 | `/enterprise/settings/product` | ✅ 已实现 | Wave 2 |
| 24 个数据录入页面 | `/enterprise/entry/*` | ❌ 全部空壳 | Wave 5 删除 |
| 规定图表 | `/enterprise/charts/standard` | ⚠️ 占位 | Wave 7 |
| 报告辅助图表 | `/enterprise/charts/report-assist` | ⚠️ 占位 | Wave 7 |
| 信息录入（SpreadJS） | `/enterprise/report/input` | ✅ 已实现 | Wave 3 |
| 在线生成报告 | `/enterprise/report/generate` | ✅ 已实现 | Wave 3 |
| 上传最终报告 | `/enterprise/report/upload` | ⚠️ 半实现（待 OSS） | Wave 8 |
| 报告详情 | `/enterprise/report/detail` | ✅ 已实现 | Wave 3 |

### 审核端（Auditor Portal）

| 页面 | 路径 | 状态 | 所属 Wave |
|------|------|------|-----------|
| 审计首页 | `/auditor/dashboard` | ⚠️ 占位（欢迎语） | Wave 9 |
| 任务列表 | `/auditor/tasks` | ⚠️ 占位 | Wave 9 |
| 审核详情 | `/auditor/review` | ⚠️ 占位 | Wave 9 |

### 公共组件

| 组件 | 状态 | 说明 |
|------|------|------|
| `SpreadDesigner.vue` | ✅ 已实现 | SpreadJS V18 模板设计器 |
| `SpreadSheet.vue` | ✅ 已实现 | SpreadJS 填报组件（含编辑锁） |
| `FlowEditor.vue` | ⚠️ 骨架占位 | AntV X6 能源流程图（Wave 6） |
| `DocEditor.vue` | ⚠️ 骨架占位 | OnlyOffice 文档编辑器（Wave 8） |
| `ChartCard.vue` | ❌ 未创建 | ECharts 图表卡片（Wave 7） |
| `DictSelect.vue` | ✅ 已实现 | 字典下拉选择 |
| `DictTag.vue` | ✅ 已实现 | 字典标签展示 |
| `ChangePasswordDialog.vue` | ✅ 已实现 | 强制改密弹窗 |

---

## 附录 B：PR 规范

### Branch 命名
```
feat/wave{N}-sprint{M}-{short-desc}
例：feat/wave5-sprint1-menu-restructure
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
