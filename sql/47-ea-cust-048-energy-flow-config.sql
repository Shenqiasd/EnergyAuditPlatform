-- EA-CUST-048 v2: Energy Flow Config & PNG Export
-- Evolve de_energy_flow with v2 typed columns; extend diagram/node/edge tables.

-- ============================================================
-- 1. de_energy_flow — add v2 columns for typed source/target/item
-- ============================================================
ALTER TABLE de_energy_flow
  ADD COLUMN IF NOT EXISTS source_type   VARCHAR(32)    DEFAULT NULL COMMENT '来源类型: external_energy / unit / system',
  ADD COLUMN IF NOT EXISTS source_ref_id BIGINT         DEFAULT NULL COMMENT '来源引用ID (bs_unit.id when source is unit)',
  ADD COLUMN IF NOT EXISTS target_type   VARCHAR(32)    DEFAULT NULL COMMENT '目的类型: unit / production_system / product_output',
  ADD COLUMN IF NOT EXISTS target_ref_id BIGINT         DEFAULT NULL COMMENT '目的引用ID (bs_unit.id when target is unit)',
  ADD COLUMN IF NOT EXISTS item_type     VARCHAR(16)    DEFAULT NULL COMMENT '品目类型: energy / product',
  ADD COLUMN IF NOT EXISTS item_id       BIGINT         DEFAULT NULL COMMENT '品目ID (bs_energy.id or bs_product.id)',
  ADD COLUMN IF NOT EXISTS calculated_value DECIMAL(18,4) DEFAULT NULL COMMENT '计算值(折标量或价格/产值)';

-- ============================================================
-- 2. de_energy_flow_diagram — add v2 columns for canvas config
-- ============================================================
ALTER TABLE de_energy_flow_diagram
  ADD COLUMN IF NOT EXISTS name             VARCHAR(128) DEFAULT NULL COMMENT '图表名称',
  ADD COLUMN IF NOT EXISTS canvas_width     INT          DEFAULT NULL COMMENT '画布宽度',
  ADD COLUMN IF NOT EXISTS canvas_height    INT          DEFAULT NULL COMMENT '画布高度',
  ADD COLUMN IF NOT EXISTS background_color VARCHAR(32)  DEFAULT NULL COMMENT '背景色';

-- ============================================================
-- 3. de_energy_flow_node — add v2 visual columns
-- ============================================================
ALTER TABLE de_energy_flow_node
  ADD COLUMN IF NOT EXISTS width   DOUBLE   DEFAULT NULL COMMENT '节点宽度',
  ADD COLUMN IF NOT EXISTS height  DOUBLE   DEFAULT NULL COMMENT '节点高度',
  ADD COLUMN IF NOT EXISTS color   VARCHAR(32) DEFAULT NULL COMMENT '节点颜色',
  ADD COLUMN IF NOT EXISTS visible TINYINT  DEFAULT 1    COMMENT '是否可见(0隐藏 1可见)',
  ADD COLUMN IF NOT EXISTS locked  TINYINT  DEFAULT 0    COMMENT '是否锁定(0未锁定 1已锁定)';

-- ============================================================
-- 4. de_energy_flow_edge — add v2 binding & style columns
-- ============================================================
ALTER TABLE de_energy_flow_edge
  ADD COLUMN IF NOT EXISTS flow_record_id    BIGINT         DEFAULT NULL COMMENT '关联填报记录ID -> de_energy_flow.id',
  ADD COLUMN IF NOT EXISTS item_type         VARCHAR(16)    DEFAULT NULL COMMENT '品目类型: energy / product',
  ADD COLUMN IF NOT EXISTS item_id           BIGINT         DEFAULT NULL COMMENT '品目ID (bs_energy.id or bs_product.id)',
  ADD COLUMN IF NOT EXISTS physical_quantity DECIMAL(18,4)  DEFAULT NULL COMMENT '实物量',
  ADD COLUMN IF NOT EXISTS calculated_value  DECIMAL(18,4)  DEFAULT NULL COMMENT '计算值(折标量或价格/产值)',
  ADD COLUMN IF NOT EXISTS label_text        VARCHAR(256)   DEFAULT NULL COMMENT '标签文本',
  ADD COLUMN IF NOT EXISTS color             VARCHAR(32)    DEFAULT NULL COMMENT '连线颜色',
  ADD COLUMN IF NOT EXISTS line_width        DOUBLE         DEFAULT NULL COMMENT '线宽',
  ADD COLUMN IF NOT EXISTS route_points      JSON           DEFAULT NULL COMMENT '折线路由点',
  ADD COLUMN IF NOT EXISTS visible           TINYINT        DEFAULT 1   COMMENT '是否可见(0隐藏 1可见)';
