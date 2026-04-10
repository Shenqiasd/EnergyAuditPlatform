-- Audit04: 能源计量器具汇总 — 新增 energy_attribute 列
-- Excel H列"能源属性"为文本（如"电力""天然气"），DB 现有 energy_id 为 BIGINT 外键，无法直接存文本
-- 新增 energy_attribute VARCHAR(128) 存储 Excel 中的能源属性文本

ALTER TABLE de_meter_instrument
  ADD COLUMN energy_attribute VARCHAR(128) DEFAULT NULL COMMENT '能源属性(文本)'
  AFTER grade;
