package com.energy.audit.service.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 能源流程图重构 v2 · PR #2 后端后处理器。
 *
 * <p>在 Audit11 模板提交后、{@code de_energy_flow} 落库完成之后被
 * {@link com.energy.audit.service.template.impl.DataPersistenceServiceImpl}
 * 调用，完成两件事：</p>
 *
 * <ol>
 *   <li><b>unit_id 回填</b>：通过 {@code bs_unit.name} 匹配已写入的
 *       {@code de_energy_flow.source_unit / target_unit} 字面值，把
 *       {@code source_unit_id / target_unit_id} 外键列填上。原字面列不动，
 *       便于保留 "外购" / "产出" 这类虚拟节点不映射到任何真实 bs_unit 的能力。</li>
 *   <li><b>de_energy_balance 派生</b>：按 v2 方案 X（彻底舍弃 Sheet 11.1）的
 *       逻辑，从 {@code de_energy_flow} 聚合 purchase_amount 与
 *       consumption_amount 两列，以 {@code energy_product} 分组写入
 *       {@code de_energy_balance}。其余字段保留 NULL。</li>
 * </ol>
 *
 * <p>之所以单独抽一个 Component 而不是塞进 DataPersistenceServiceImpl
 * 或 BusinessTablePersister 内部：后者是通用表写入器，不应感知某一具体业务；
 * 前者是流程编排者，不应直接写 SQL。这个类作为中间层，专管 Sheet 11 的语义
 * 派生，便于日后 PR #4 迭代和单测覆盖。</p>
 */
@Component
public class EnergyFlowPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(EnergyFlowPostProcessor.class);

    /** "外购" 虚拟源节点的 sentinel 字符串（不映射到 bs_unit）。 */
    public static final String EXTERNAL_PURCHASE_SENTINEL = "外购";
    /** "产出" 虚拟目的节点的 sentinel 字符串（不映射到 bs_unit）。 */
    public static final String OUTPUT_SENTINEL = "产出";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate nestedTxTemplate;

    public EnergyFlowPostProcessor(NamedParameterJdbcTemplate jdbcTemplate,
                                   PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        // NESTED 传播 → JDBC Savepoint。用于把 de_energy_balance 的
        // "软删旧行 + 插入新聚合" 包成原子子事务：失败时只回滚这两步，不影响
        // 外层 de_energy_flow 的入库提交。
        this.nestedTxTemplate = new TransactionTemplate(transactionManager);
        this.nestedTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
    }

    /**
     * 单次提交 de_energy_flow 后调用，完成 unit_id 回填与 de_energy_balance 派生。
     *
     * <p>本方法本身不开事务 —— 调用方 {@code DataPersistenceServiceImpl.persistExtractedData}
     * 已经被 {@code @Transactional} 包裹，这里所有 SQL 都会跟随外层事务一起提交或回滚。</p>
     *
     * @param submissionId 当前提交 id
     * @param enterpriseId 企业 id
     * @param auditYear    审计年度
     * @param operator     操作人，用于落库 create_by/update_by
     */
    public void afterEnergyFlowPersist(Long submissionId, Long enterpriseId,
                                       Integer auditYear, String operator) {
        if (submissionId == null || enterpriseId == null || auditYear == null) {
            log.warn("afterEnergyFlowPersist skipped: missing submissionId/enterpriseId/auditYear");
            return;
        }
        try {
            int translated = translateFlowStages(submissionId);
            int backfilled = backfillUnitIds(submissionId, enterpriseId);
            int derived = deriveEnergyBalance(submissionId, enterpriseId, auditYear, operator);
            log.info("EnergyFlowPostProcessor: submission {} — translated {} flow_stage labels, backfilled {} unit_ids, derived {} de_energy_balance rows",
                    submissionId, translated, backfilled, derived);
        } catch (Exception e) {
            // 派生失败不应阻塞主流程（Sheet 11 本体已入库），但必须让运维看到。
            log.error("EnergyFlowPostProcessor failed for submission {}: {}", submissionId, e.getMessage(), e);
        }
    }

    /**
     * 把 {@code de_energy_flow.flow_stage} 从模板中的中文标签标准化成英文枚举，
     * 便于前端 4 环节图布局 ({@code purchased / conversion / distribution / terminal})
     * 以及 {@link #deriveEnergyBalance} 的 SQL 条件命中。
     *
     * <p>Sheet 11 "能源流程图" 的"环节"列下拉值为：</p>
     * <ul>
     *   <li>购入储存 → {@code purchased}</li>
     *   <li>加工转换 → {@code conversion}</li>
     *   <li>分配输送 → {@code distribution}</li>
     *   <li>终端使用 → {@code terminal}</li>
     * </ul>
     *
     * <p>已是英文值或空值的行不受影响（IN 匹配严格等于中文标签）。</p>
     *
     * <p><b>不走 {@link #safeUpdate}</b>：本步失败会让下游 {@link #deriveEnergyBalance}
     * 读到中文标签，聚合 {@code flow_stage = 'purchased'} 的 SQL 就全部落空，
     * {@code purchase_amount} 会被错误地派生为 0。异常直接抛给
     * {@link #afterEnergyFlowPersist} 顶层 try/catch —— 它会记录错误并短路 derive，
     * 保证不会产生错误派生数据（老行保持不动，由下次成功的提交覆盖）。</p>
     *
     * @return 标准化过的行数
     */
    int translateFlowStages(Long submissionId) {
        String sql =
                "UPDATE de_energy_flow "
                        + "SET flow_stage = CASE flow_stage "
                        + "  WHEN '购入储存' THEN 'purchased' "
                        + "  WHEN '加工转换' THEN 'conversion' "
                        + "  WHEN '分配输送' THEN 'distribution' "
                        + "  WHEN '终端使用' THEN 'terminal' "
                        + "  ELSE flow_stage "
                        + "END "
                        + "WHERE submission_id = :submissionId "
                        + "  AND deleted = 0 "
                        + "  AND flow_stage IN ('购入储存','加工转换','分配输送','终端使用')";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("submissionId", submissionId);
        // NOTE: intentionally NOT using safeUpdate — see javadoc.
        return jdbcTemplate.update(sql, params);
    }

    /**
     * 用 {@code bs_unit.name} 反查，填补 {@code de_energy_flow.source_unit_id / target_unit_id}。
     *
     * <p>两条 UPDATE ... JOIN 语句，只针对当前 submission 的未填充行。虚拟节点
     * "外购" / "产出" 不会命中 bs_unit，unit_id 保持 NULL 是预期行为。</p>
     *
     * @return 受影响行数（两条 UPDATE 之和；同一行被填 source 和 target 会被计两次）
     */
    int backfillUnitIds(Long submissionId, Long enterpriseId) {
        String srcSql =
                "UPDATE de_energy_flow f "
                        + "JOIN bs_unit u "
                        + "  ON u.name = f.source_unit "
                        + " AND u.enterprise_id = f.enterprise_id "
                        + " AND u.deleted = 0 "
                        + "SET f.source_unit_id = u.id "
                        + "WHERE f.submission_id = :submissionId "
                        + "  AND f.enterprise_id = :enterpriseId "
                        + "  AND f.deleted = 0 "
                        + "  AND f.source_unit_id IS NULL "
                        + "  AND f.source_unit IS NOT NULL";

        String dstSql =
                "UPDATE de_energy_flow f "
                        + "JOIN bs_unit u "
                        + "  ON u.name = f.target_unit "
                        + " AND u.enterprise_id = f.enterprise_id "
                        + " AND u.deleted = 0 "
                        + "SET f.target_unit_id = u.id "
                        + "WHERE f.submission_id = :submissionId "
                        + "  AND f.enterprise_id = :enterpriseId "
                        + "  AND f.deleted = 0 "
                        + "  AND f.target_unit_id IS NULL "
                        + "  AND f.target_unit IS NOT NULL";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("enterpriseId", enterpriseId);
        int srcUpdated = safeUpdate(srcSql, params);
        int dstUpdated = safeUpdate(dstSql, params);
        return srcUpdated + dstUpdated;
    }

    /**
     * 按 v2 方案派生 {@code de_energy_balance}。
     *
     * <p>语义：</p>
     * <pre>
     *   purchase_amount    = Σ physical_quantity WHERE flow_stage = 'purchased'
     *   consumption_amount = Σ physical_quantity WHERE
     *                          (target_unit = '产出')
     *                       OR (target_unit_id 指向 bs_unit.unit_type = 3 的终端单元)
     *   GROUP BY energy_product
     * </pre>
     *
     * <p>流程：先软删当前 submission 下已有的 de_energy_balance 行（归属权
     *   于本次 Audit11 提交），再 INSERT ... SELECT 一次性写入聚合结果。</p>
     *
     * @return 新插入的聚合行数
     */
    int deriveEnergyBalance(Long submissionId, Long enterpriseId, Integer auditYear, String operator) {
        // 1) 软删旧派生行（仅属于本 submission 的）
        String deleteSql =
                "UPDATE de_energy_balance "
                        + "SET deleted = 1, update_by = :operator, update_time = NOW() "
                        + "WHERE submission_id = :submissionId "
                        + "  AND deleted = 0";
        MapSqlParameterSource deleteParams = new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("operator", operator);

        // 2) 一次性聚合：UNION ALL + 外层 GROUP BY 同一 energy_product 合并购入/消耗
        String insertSql =
                "INSERT INTO de_energy_balance ("
                        + "  submission_id, enterprise_id, audit_year, "
                        + "  energy_name, measurement_unit, "
                        + "  purchase_amount, consumption_amount, "
                        + "  create_by, update_by, deleted"
                        + ") "
                        + "SELECT "
                        + "  :submissionId, :enterpriseId, :auditYear, "
                        + "  agg.energy_product, NULL, "
                        + "  SUM(agg.purchase), SUM(agg.consumption), "
                        + "  :operator, :operator, 0 "
                        + "FROM ( "
                        + "  SELECT f.energy_product, "
                        + "         CASE WHEN f.flow_stage = 'purchased' THEN COALESCE(f.physical_quantity, 0) ELSE 0 END AS purchase, "
                        + "         0 AS consumption "
                        + "  FROM de_energy_flow f "
                        + "  WHERE f.submission_id = :submissionId "
                        + "    AND f.deleted = 0 "
                        + "    AND f.energy_product IS NOT NULL "
                        + "    AND f.energy_product <> '' "
                        + "  UNION ALL "
                        + "  SELECT f.energy_product, "
                        + "         0 AS purchase, "
                        + "         CASE "
                        + "           WHEN f.target_unit = :outputSentinel THEN COALESCE(f.physical_quantity, 0) "
                        + "           WHEN u.unit_type = 3 THEN COALESCE(f.physical_quantity, 0) "
                        + "           ELSE 0 "
                        + "         END AS consumption "
                        + "  FROM de_energy_flow f "
                        + "  LEFT JOIN bs_unit u ON u.id = f.target_unit_id AND u.deleted = 0 "
                        + "  WHERE f.submission_id = :submissionId "
                        + "    AND f.deleted = 0 "
                        + "    AND f.energy_product IS NOT NULL "
                        + "    AND f.energy_product <> '' "
                        + ") agg "
                        + "GROUP BY agg.energy_product "
                        + "HAVING SUM(agg.purchase) > 0 OR SUM(agg.consumption) > 0";

        MapSqlParameterSource insertParams = new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("enterpriseId", enterpriseId)
                .addValue("auditYear", auditYear)
                .addValue("operator", operator)
                .addValue("outputSentinel", OUTPUT_SENTINEL);

        // 关键：delete + insert 必须在同一个 savepoint 子事务内执行。
        // 如果 insert 失败（比如列缺失、类型不匹配），savepoint 会把 delete
        // 一起回滚，老的 de_energy_balance 行不会丢失；外层 de_energy_flow 入库
        // 也不会被波及。过去这里用 safeUpdate 各吞异常，delete 成功而 insert 失败
        // 会造成派生数据永久丢失 (Devin Review 指出)。
        try {
            Integer inserted = nestedTxTemplate.execute(status -> {
                jdbcTemplate.update(deleteSql, deleteParams);
                return jdbcTemplate.update(insertSql, insertParams);
            });
            return inserted == null ? 0 : inserted;
        } catch (Exception e) {
            // savepoint 已回滚 → 老行仍在 deleted=0 状态，新行未写入；对主流程无影响。
            log.warn("deriveEnergyBalance rolled back ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            return 0;
        }
    }

    /**
     * 包一层异常处理：若表不存在（例如 H2 分片测试未建 bs_unit）或列缺失，
     * 退回到 0 影响行数，不抛异常阻塞提交主路径。仅用于 backfillUnitIds 这类
     * 无相互依赖的 UPDATE —— 派生 de_energy_balance 的 delete/insert 对必须
     * 走 {@link #nestedTxTemplate} 保证原子性。
     */
    private int safeUpdate(String sql, MapSqlParameterSource params) {
        try {
            return jdbcTemplate.update(sql, params);
        } catch (Exception e) {
            log.warn("EnergyFlowPostProcessor SQL skipped ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            return 0;
        }
    }
}
