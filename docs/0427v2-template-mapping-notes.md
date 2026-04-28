# 0427v2 新模板映射执行说明

## 已确认的执行策略

- 新模板文件：`0427v2.xlsx`，共 40 个 Sheet，其中 21 个为填报/映射 Sheet。
- 新模板版本：`tpl_template_version.id = 33`。
- 对标旧模板：`AUDIT_FULL_0412 / version 31`。
- SpreadJS Node 侧无法稳定把 xlsx 直接转为 templateJson，因此本次采用系统现有支持的 `CELL_RANGE`/`cell_range` TABLE 映射兜底，不依赖 Named Range 先存在。
- `BusinessTablePersister.ALLOWED_TABLES` 已包含本次需要的独立业务表：`de_saving_potential`、`de_management_suggestion`、`de_tech_reform_suggestion`、`de_rectification`、`de_carbon_peak_info` 等，无需新增 Java allow-list。

## 与用户方案相比的补充/修正

1. **Sheet 名以实际工作簿为准**  
   Excel/SpreadJS 中的实际 Sheet 名为中文短名/规范名（例如 `1.十四五已实施节能技改项目`、`15,温室气体排放排放汇总`），迁移按实际名称写入，避免抽取时按 sheet_name 找不到 Sheet。

2. **Sheet 1 实际有 11 列**  
   用户方案列到“完成时间”为止；实际模板还包含“实际节能量”“是否合同能源管理模式”“备注”，迁移已补齐并映射到 `de_tech_reform_history`。

3. **Sheet 8 实际有 12 列**  
   用户方案缺少“设备对标情况(能效等级)”列，迁移已补齐 `energy_efficiency_level`。

4. **Sheet 11.1 新增能源单价**  
   映射保留 `unit_price`，并补齐 `de_energy_consumption` 中 `transfer_out/gain_loss/unit_price` 兼容列。

5. **Sheet 13/16/17/18/19 使用独立业务表**  
   不是统一落 `de_submission_table`。迁移补齐新增字段（减碳量、项目类型、投资、整改经济效益等），避免业务表持久化时字段被跳过。

6. **Sheet 14 实际为审计年/基年两列**  
   不是单纯 B2:B6 五个固定值；迁移按 `de_saving_calculation` 的 current/base 字段配置 8 个 SCALAR tag。

7. **Sheet 15 表和标量并存**  
   化石燃料明细配置为 `A26:H33`，并按方案加入 `attribution=化石燃料` 的 CONFIG_PREFILL 过滤。`de_carbon_emission.emission_category` 历史为 NOT NULL，本迁移放宽为可空，避免模板没有类别列时业务表写入失败。

8. **Sheet 20/21 为多区域宽表映射**  
   Sheet 20 增加 2025 现状列；Sheet 21 产品单耗区域实际为 2025/2030 左右两组并排列。迁移补齐 `de_five_year_target` 宽表字段并配置两个 TABLE。

9. **Sheet 3 SCALAR 目标表拆分**  
   能源领导/部门等企业配置字段落 `ent_enterprise_setting`，专兼职人数/目标值等审计年度字段落 `de_company_overview`。这是为了匹配现有 `DataPersistenceServiceImpl` 对企业设置表的专门同步逻辑。

## 仍需要人工确认/后续测试的点

- 如果用户仍希望严格走 Named Range 自动发现，需要在 SpreadJS Designer 中定义 Named Range/Cell Tag；本迁移不阻塞该流程，只是先用 `CELL_RANGE` 完成可运行映射。
- SCALAR `CELL_TAG` 需要模板 JSON 内存在对应 tag 才能从单元格抽取；如果暂未打 tag，TABLE 映射可用，SCALAR 仍需 Designer 补 tag 或后续改造为单格 cellRange 抽取。
- Sheet 20/21 的 `section_type/year_label` 语义目前通过列值落库，没有做专门默认值后处理；如果报表生成需要强语义分区，可再加后处理或扩展 column_mappings 支持固定值。
