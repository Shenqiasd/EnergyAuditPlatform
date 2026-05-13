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
            // 执行顺序必须是 backfillUnitIds → deriveFlowStage：后者依赖 target_unit_id
            // 反查 bs_unit.unit_type 决定环节归属；若颠倒顺序则大多数行会落到 ELSE 分支
            // (unit_type NULL) 导致 flow_stage 保持空，前端图会漏掉加工/分配/终端环节的行。
            int stageDerived = deriveFlowStage(submissionId);
            // standard_quantity 派生独立于 flow_stage，仅依赖 physical_quantity 与
            // bs_energy / bs_product 匹配；顺序无所谓，放在最后让 de_energy_balance
            // 聚合能看到最新 standard_quantity（虽然当前 derive 用的是 physical_quantity）。
            int quantityDerived = deriveStandardQuantity(submissionId, enterpriseId);
            int derived = deriveEnergyBalance(submissionId, enterpriseId, auditYear, operator);
            log.info("EnergyFlowPostProcessor: submission {} — translated {} flow_stage labels, backfilled {} unit_ids, derived {} stages, {} standard_quantity, {} de_energy_balance rows",
                    submissionId, translated, backfilled, stageDerived, quantityDerived, derived);
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
     *   <li>输送分配 / 分配输送 → {@code distribution}</li>
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
                        + "  WHEN '输送分配' THEN 'distribution' "
                        + "  WHEN '分配输送' THEN 'distribution' "
                        + "  WHEN '终端使用' THEN 'terminal' "
                        + "  ELSE flow_stage "
                        + "END "
                        + "WHERE submission_id = :submissionId "
                        + "  AND deleted = 0 "
                        + "  AND flow_stage IN ('购入储存','加工转换','输送分配','分配输送','终端使用')";
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
     * <p><b>不走 {@link #safeUpdate}</b>：本步的 {@code target_unit_id} 是
     * {@link #deriveFlowStage} 反查 {@code bs_unit.unit_type} 推导环节的唯一前置。
     * 若此处静默失败（列缺失/表不存在等），target_unit_id 全部保持 NULL，
     * deriveFlowStage 的 CASE 只能命中虚拟节点分支（外购→purchased / 产出→terminal），
     * 其余所有真实单元流向的 flow_stage 保持为空，前端图会漏掉加工/分配/终端绝大多数节点；
     * 同时下游 {@link #deriveEnergyBalance} 的聚合也会错，purchase_amount 全部派生为 0。
     * 异常直接上抛给 {@link #afterEnergyFlowPersist} 的外层 try/catch，短路 derive，
     * 宁可不派生也不产生错误数据（Devin Review PR #157 指出）。</p>
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
        // NOTE: intentionally NOT using safeUpdate — see javadoc.
        int srcUpdated = jdbcTemplate.update(srcSql, params);
        int dstUpdated = jdbcTemplate.update(dstSql, params);
        return srcUpdated + dstUpdated;
    }

    /**
     * 按照业务规则推导 {@code de_energy_flow.flow_stage}。
     *
     * <p>Sheet 11 "11.能流图（二维表）" 没有"环节"列，用户只填源/目的单元和能源/产品，
     * 所以 flow_stage 必须由服务端从源/目的单元的语义反推：</p>
     * <ul>
     *   <li>{@code source_unit = "外购"} → {@code purchased}（购入储存，虚拟源节点）</li>
     *   <li>{@code target_unit = "产出"} → {@code terminal}（终端使用，虚拟汇节点）</li>
     *   <li>否则按 {@code target_unit_id} 指向的 bs_unit.unit_type 映射：
     *       {@code 1=conversion / 2=distribution / 3=terminal}</li>
     *   <li>都命不中则 {@code flow_stage} 保持原值（通常为空），前端图会跳过该行</li>
     * </ul>
     *
     * <p><b>前置依赖</b>：必须在 {@link #backfillUnitIds} 之后调用，否则 target_unit_id
     * 为 NULL，unit_type 反查不到，绝大多数非虚拟流向会落到 ELSE 分支无法分层。</p>
     *
     * <p><b>幂等条件</b>：只对 flow_stage 为 NULL 或空字符串的行做 UPDATE。已有值
     * （例如历史数据、用户手工通过 API 补录过）不覆盖，避免把 {@link #translateFlowStages}
     * 标准化完的英文值再次改写。</p>
     *
     * <p><b>不走 {@link #safeUpdate}</b>：新布局 Sheet 11 抽取时 flow_stage 恒为空，
     * 本方法是 {@code flow_stage = 'purchased'} 的唯一来源，也是下游
     * {@link #deriveEnergyBalance} 聚合 purchase_amount 所依赖的前置条件。若因
     * 列缺失/表不存在等原因静默失败（safeUpdate 吞异常），deriveEnergyBalance 会
     * 继续把老的 de_energy_balance 软删再写入 purchase_amount=0 的错误派生数据。
     * 异常直接上抛给 {@link #afterEnergyFlowPersist} 的外层 try/catch，短路
     * deriveEnergyBalance，保证宁可不派生也不产生错误数据（Devin Review PR #157 指出）。</p>
     *
     * @return 更新的行数
     */
    int deriveFlowStage(Long submissionId) {
        String sql =
                "UPDATE de_energy_flow f "
                        + "LEFT JOIN bs_unit tu "
                        + "  ON tu.id = f.target_unit_id "
                        + " AND tu.deleted = 0 "
                        + "SET f.flow_stage = CASE "
                        + "  WHEN f.source_unit = :purchaseSentinel THEN 'purchased' "
                        + "  WHEN f.target_unit = :outputSentinel THEN 'terminal' "
                        + "  WHEN tu.unit_type = 1 THEN 'conversion' "
                        + "  WHEN tu.unit_type = 2 THEN 'distribution' "
                        + "  WHEN tu.unit_type = 3 THEN 'terminal' "
                        + "  ELSE f.flow_stage "
                        + "END "
                        + "WHERE f.submission_id = :submissionId "
                        + "  AND f.deleted = 0 "
                        + "  AND (f.flow_stage IS NULL OR f.flow_stage = '')";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("purchaseSentinel", EXTERNAL_PURCHASE_SENTINEL)
                .addValue("outputSentinel", OUTPUT_SENTINEL);
        // NOTE: intentionally NOT using safeUpdate — see javadoc.
        return jdbcTemplate.update(sql, params);
    }

    /**
     * 推导列 E {@code standard_quantity}（折标量/价格（万元））。
     *
     * <p>业务规则（用户确认）：列 E 是计算列，由列 C "能源/产品" 的性质决定计算方式：</p>
     * <ul>
     *   <li>C 命中 {@code bs_energy.name}（能源）→ {@code physical_quantity × bs_energy.equivalent_value}
     *       （实物量 × 折标系数/当量值）</li>
     *   <li>C 命中 {@code bs_product.name}（产品，bs_energy 未命中时）→
     *       {@code physical_quantity × bs_product.unit_price}（产量 × 单价）</li>
     *   <li>两表都不命中 → 保持现值（通常 NULL）</li>
     * </ul>
     *
     * <p>因为用户明确"这是计算值"，对已有值也覆盖；如果命中不到任何一边的系数则
     * 不动原值，保留用户意图（例如手动填写的价格）。{@code physical_quantity} 为
     * NULL 时跳过（无法乘）。</p>
     *
     * <p><b>匹配优先级</b>：bs_energy &gt; bs_product。同名时能源表胜出，与用户
     * 描述 "如果是能源就是...；如果是产品就是..." 的顺序一致。</p>
     *
     * @return 更新的行数
     */
    int deriveStandardQuantity(Long submissionId, Long enterpriseId) {
        String sql =
                "UPDATE de_energy_flow f "
                        + "LEFT JOIN bs_energy e "
                        + "  ON e.name = f.energy_product "
                        + " AND e.enterprise_id = f.enterprise_id "
                        + " AND e.deleted = 0 "
                        + "LEFT JOIN bs_product p "
                        + "  ON p.name = f.energy_product "
                        + " AND p.enterprise_id = f.enterprise_id "
                        + " AND p.deleted = 0 "
                        + "SET f.standard_quantity = CASE "
                        + "  WHEN f.physical_quantity IS NULL THEN f.standard_quantity "
                        + "  WHEN e.equivalent_value IS NOT NULL THEN f.physical_quantity * e.equivalent_value "
                        + "  WHEN p.unit_price IS NOT NULL THEN f.physical_quantity * p.unit_price "
                        + "  ELSE f.standard_quantity "
                        + "END "
                        + "WHERE f.submission_id = :submissionId "
                        + "  AND f.enterprise_id = :enterpriseId "
                        + "  AND f.deleted = 0 "
                        + "  AND f.energy_product IS NOT NULL "
                        + "  AND f.energy_product <> ''";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("enterpriseId", enterpriseId);
        return safeUpdate(sql, params);
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
     * 包一层异常处理：若表不存在（例如 H2 分片测试未建 bs_energy / bs_product）或列缺失，
     * 退回到 0 影响行数，不抛异常阻塞提交主路径。
     *
     * <p><b>仅限用于没有任何下游依赖、失败也不会污染其他派生数据的 UPDATE</b>。
     * 当前仅 {@link #deriveStandardQuantity} 使用：该步失败只会导致
     * {@code standard_quantity} 留空，下游 {@link #deriveEnergyBalance} 聚合用的
     * 是 {@code physical_quantity}，不受影响。</p>
     *
     * <p><b>反例</b>：{@link #backfillUnitIds} 和 {@link #translateFlowStages} / {@link #deriveFlowStage}
     * 不能走 safeUpdate —— 它们都是 {@link #deriveEnergyBalance} 的前置条件，
     * 静默失败会让派生行写入错误数据。派生 de_energy_balance 的 delete/insert 对
     * 必须走 {@link #nestedTxTemplate} 保证原子性。</p>
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
