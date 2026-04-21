-- =====================================================================
-- 25-fix-sheet12-prefill-filter.sql
--
-- 修复 Sheet 12「单位产品能耗数据」在企业填报页面不按产品自动生成行的问题。
--
-- 根因：PR #129 插入的 CONFIG_PREFILL tag (tag_name='product_unit_consumption_prefill')
--       `column_mappings` 里带了 `"filter": {"isActive": 1}`。这个 filter 是从
--       bs_energy 的 PREFILL 配置复制过来的，但 `bs_product` 表并没有 `is_active`
--       字段（建表时就不存在，后端 DTO 也不返回 isActive）。前端
--       `applyOneConfigPrefill` 执行 `records.filter(r => r.isActive === 1)` 时，
--       undefined !== 1 使得所有 bs_product 记录都被过滤掉，`records.length` 为 0，
--       触发第 323 行 `if (!records.length && !isDropdownOnly) return` 提前返回，
--       Sheet 12 不会生成任何产品行。
--
-- 其它同样指向 bs_product 的 CONFIG_PREFILL tag（3696 PREFILL_13_PRODUCT /
-- 3698 PREFILL_20_CARBON_PEAK / 3699 PREFILL_21_ENERGY_TARGET）本来就没有带
-- isActive filter，运行正常，不受影响。
--
-- 本迁移只做一件事：把 tag_name='product_unit_consumption_prefill' 的
-- column_mappings JSON 里的 `filter` 键移除。
--
-- 说明：使用 JSON_REMOVE 直接原地修改 JSON，不改 source / columns / locked 等
-- 其它字段；幂等（再次执行找不到 $.filter 就是 no-op）。
--
-- 两个环境都需要执行：
--   Railway:   `mysql --host=$HOST --port=$PORT --user=$U -p$P railway < sql/25-...`
--   腾讯云:   在腾讯云 MySQL 实例上手动执行一遍
-- =====================================================================

UPDATE tpl_tag_mapping
SET column_mappings = JSON_REMOVE(column_mappings, '$.filter'),
    update_time = NOW()
WHERE tag_name = 'product_unit_consumption_prefill'
  AND mapping_type = 'CONFIG_PREFILL'
  AND JSON_EXTRACT(column_mappings, '$.filter') IS NOT NULL;

-- 校验：期望 $.filter 字段被移除，其它字段（source/columns）保留。
SELECT id,
       tag_name,
       target_table,
       sheet_index,
       JSON_EXTRACT(column_mappings, '$.filter')   AS filter_after,
       JSON_EXTRACT(column_mappings, '$.source')   AS source_after,
       JSON_LENGTH(JSON_EXTRACT(column_mappings, '$.columns')) AS column_count
FROM tpl_tag_mapping
WHERE tag_name = 'product_unit_consumption_prefill'
  AND mapping_type = 'CONFIG_PREFILL';
