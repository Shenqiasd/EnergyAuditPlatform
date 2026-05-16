-- EA-CUST-048 v2: Energy Flow Config & PNG Export
-- Evolve de_energy_flow with v2 typed columns; extend diagram/node/edge tables.
-- Uses ensure_column() helper from 01a-migration-helpers.sql for MySQL 8.0 compat.

-- ============================================================
-- 1. de_energy_flow — add v2 columns for typed source/target/item
-- ============================================================
CALL ensure_column('de_energy_flow', 'source_type',      'VARCHAR(32)    DEFAULT NULL COMMENT ''来源类型: external_energy / unit / system''');
CALL ensure_column('de_energy_flow', 'source_ref_id',    'BIGINT         DEFAULT NULL COMMENT ''来源引用ID (bs_unit.id when source is unit)''');
CALL ensure_column('de_energy_flow', 'target_type',      'VARCHAR(32)    DEFAULT NULL COMMENT ''目的类型: unit / production_system / product_output''');
CALL ensure_column('de_energy_flow', 'target_ref_id',    'BIGINT         DEFAULT NULL COMMENT ''目的引用ID (bs_unit.id when target is unit)''');
CALL ensure_column('de_energy_flow', 'item_type',        'VARCHAR(16)    DEFAULT NULL COMMENT ''品目类型: energy / product''');
CALL ensure_column('de_energy_flow', 'item_id',          'BIGINT         DEFAULT NULL COMMENT ''品目ID (bs_energy.id or bs_product.id)''');
CALL ensure_column('de_energy_flow', 'calculated_value', 'DECIMAL(18,4)  DEFAULT NULL COMMENT ''计算值(折标量或价格/产值)''');

-- ============================================================
-- 2. de_energy_flow_diagram — add v2 columns for canvas config
-- ============================================================
CALL ensure_column('de_energy_flow_diagram', 'name',             'VARCHAR(128) DEFAULT NULL COMMENT ''图表名称''');
CALL ensure_column('de_energy_flow_diagram', 'canvas_width',     'INT          DEFAULT NULL COMMENT ''画布宽度''');
CALL ensure_column('de_energy_flow_diagram', 'canvas_height',    'INT          DEFAULT NULL COMMENT ''画布高度''');
CALL ensure_column('de_energy_flow_diagram', 'background_color', 'VARCHAR(32)  DEFAULT NULL COMMENT ''背景色''');

-- ============================================================
-- 3. de_energy_flow_node — add v2 visual columns
-- ============================================================
CALL ensure_column('de_energy_flow_node', 'width',   'DOUBLE      DEFAULT NULL COMMENT ''节点宽度''');
CALL ensure_column('de_energy_flow_node', 'height',  'DOUBLE      DEFAULT NULL COMMENT ''节点高度''');
CALL ensure_column('de_energy_flow_node', 'color',   'VARCHAR(32) DEFAULT NULL COMMENT ''节点颜色''');
CALL ensure_column('de_energy_flow_node', 'visible', 'TINYINT     DEFAULT 1    COMMENT ''是否可见(0隐藏 1可见)''');
CALL ensure_column('de_energy_flow_node', 'locked',  'TINYINT     DEFAULT 0    COMMENT ''是否锁定(0未锁定 1已锁定)''');

-- ============================================================
-- 4. de_energy_flow_edge — add v2 binding & style columns
-- ============================================================
CALL ensure_column('de_energy_flow_edge', 'flow_record_id',    'BIGINT         DEFAULT NULL COMMENT ''关联填报记录ID -> de_energy_flow.id''');
CALL ensure_column('de_energy_flow_edge', 'item_type',         'VARCHAR(16)    DEFAULT NULL COMMENT ''品目类型: energy / product''');
CALL ensure_column('de_energy_flow_edge', 'item_id',           'BIGINT         DEFAULT NULL COMMENT ''品目ID (bs_energy.id or bs_product.id)''');
CALL ensure_column('de_energy_flow_edge', 'physical_quantity', 'DECIMAL(18,4)  DEFAULT NULL COMMENT ''实物量''');
CALL ensure_column('de_energy_flow_edge', 'calculated_value',  'DECIMAL(18,4)  DEFAULT NULL COMMENT ''计算值(折标量或价格/产值)''');
CALL ensure_column('de_energy_flow_edge', 'label_text',        'VARCHAR(256)   DEFAULT NULL COMMENT ''标签文本''');
CALL ensure_column('de_energy_flow_edge', 'color',             'VARCHAR(32)    DEFAULT NULL COMMENT ''连线颜色''');
CALL ensure_column('de_energy_flow_edge', 'line_width',        'DOUBLE         DEFAULT NULL COMMENT ''线宽''');
CALL ensure_column('de_energy_flow_edge', 'route_points',      'JSON           DEFAULT NULL COMMENT ''折线路由点''');
CALL ensure_column('de_energy_flow_edge', 'visible',           'TINYINT        DEFAULT 1    COMMENT ''是否可见(0隐藏 1可见)''');
