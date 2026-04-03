package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeCompanyOverview extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String energyLeaderName;
    private String energyLeaderPosition;
    private String energyDeptName;
    private String energyDeptLeader;
    private Integer fulltimeStaffCount;
    private Integer parttimeStaffCount;
    private BigDecimal fiveYearTargetValue;
    private String fiveYearTargetName;
    private String fiveYearTargetDept;
}
