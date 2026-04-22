package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeEnergyBalance extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;

    /** v2 有效字段: 能源品种名（对应 bs_energy.name）。 */
    private String energyName;
    /** v2 有效字段: 计量单位（冗余）。 */
    private String measurementUnit;
    /** v2 有效字段: 购入量（从 de_energy_flow where flow_stage='purchased' 派生）。 */
    private BigDecimal purchaseAmount;
    /** v2 有效字段: 消耗量（从 de_energy_flow where 终端单元 or target_unit='产出' 派生）。 */
    private BigDecimal consumptionAmount;
}
