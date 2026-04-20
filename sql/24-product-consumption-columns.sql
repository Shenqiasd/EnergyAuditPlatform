-- Sheet 12: 单位产品能耗数据 — 扩展 de_product_unit_consumption 表
-- 新增指标名称/指标单位/分子分母项单位 + 上年度数据列
-- 移除 year_type NOT NULL 约束（改为同行存本年+上年）

-- 1. 新增 4 个描述列
ALTER TABLE de_product_unit_consumption
    ADD COLUMN indicator_name   VARCHAR(128) DEFAULT NULL COMMENT '指标名称(如:XX单位产量综合能耗)' AFTER product_id,
    ADD COLUMN indicator_unit   VARCHAR(64)  DEFAULT NULL COMMENT '指标单位(如:千克标准煤/部)' AFTER indicator_name,
    ADD COLUMN numerator_unit   VARCHAR(32)  DEFAULT NULL COMMENT '分子项单位(千克标准煤)' AFTER indicator_unit,
    ADD COLUMN denominator_unit VARCHAR(32)  DEFAULT NULL COMMENT '分母项单位(如:部)' AFTER numerator_unit;

-- 2. 新增 3 个上年度数据列
ALTER TABLE de_product_unit_consumption
    ADD COLUMN prev_unit_consumption   DECIMAL(18,6) DEFAULT NULL COMMENT '上年度单耗(指标值)' AFTER unit_consumption,
    ADD COLUMN prev_energy_consumption DECIMAL(18,4) DEFAULT NULL COMMENT '上年度能源消耗量(分子项值)' AFTER prev_unit_consumption,
    ADD COLUMN prev_output             DECIMAL(18,4) DEFAULT NULL COMMENT '上年度产量(分母项值)' AFTER prev_energy_consumption;

-- 3. 放宽 year_type NOT NULL 约束（不再需要，本年+上年存同一行）
ALTER TABLE de_product_unit_consumption
    MODIFY COLUMN year_type VARCHAR(16) DEFAULT NULL COMMENT '年份(已弃用，本年+上年存同一行)';
