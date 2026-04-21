-- ============================================================================
-- P0 数据完整性修复 — 阻塞级 (影响数据完整性)
-- 执行顺序: 在生产数据库上按顺序执行
-- ============================================================================

-- ─── P0-1: de_ghg_emission 补充 submission_id 列 ───
CALL ensure_column('de_ghg_emission', 'submission_id',
    'BIGINT NOT NULL DEFAULT 0 COMMENT ''关联提交ID'' AFTER id');

-- ─── P0-2: 新建 de_carbon_peak_info 表 (Sheet 21 碳达峰信息) ───
CREATE TABLE IF NOT EXISTS `de_carbon_peak_info` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `submission_id`        BIGINT        NOT NULL DEFAULT 0     COMMENT '关联提交ID',
    `enterprise_id`        BIGINT        NOT NULL                COMMENT '企业ID -> ent_enterprise.id',
    `audit_year`           INT           NOT NULL                COMMENT '审计年度',
    `peak_year`            INT           DEFAULT NULL            COMMENT '碳达峰年份',
    `peak_emission`        DECIMAL(18,4) DEFAULT NULL            COMMENT '峰值排放量(tCO2)',
    `current_emission`     DECIMAL(18,4) DEFAULT NULL            COMMENT '当前排放量(tCO2)',
    `reduction_target`     DECIMAL(8,4)  DEFAULT NULL            COMMENT '减排目标(%)',
    `reduction_measures`   TEXT          DEFAULT NULL            COMMENT '减排措施',
    `peak_status`          VARCHAR(32)   DEFAULT NULL            COMMENT '达峰状态',
    `verification_method`  VARCHAR(128)  DEFAULT NULL            COMMENT '核查方法',
    `remark`               VARCHAR(512)  DEFAULT NULL            COMMENT '备注',
    `create_by`            VARCHAR(64)   DEFAULT NULL            COMMENT '创建人',
    `create_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`            VARCHAR(64)   DEFAULT NULL            COMMENT '更新人',
    `update_time`          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`              TINYINT       DEFAULT 0               COMMENT '逻辑删除(0未删除 1已删除)',
    PRIMARY KEY (`id`),
    INDEX `idx_enterprise_year` (`enterprise_id`, `audit_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='碳达峰信息';

-- ─── P0-4: de_equipment_energy 补充 submission_id 列 ───
CALL ensure_column('de_equipment_energy', 'submission_id',
    'BIGINT NOT NULL DEFAULT 0 COMMENT ''关联提交ID'' AFTER id');

-- ─── P0-5: de_product_energy_cost 补充 cost_ratio + energy_total_ratio 列 ───
CALL ensure_column('de_product_energy_cost', 'cost_ratio',
    'DECIMAL(8,4) DEFAULT NULL COMMENT ''能源成本占比(%)'' AFTER production_cost');
CALL ensure_column('de_product_energy_cost', 'energy_total_ratio',
    'DECIMAL(8,4) DEFAULT NULL COMMENT ''能源费用占总能源费用比(%)'' AFTER cost_ratio');
