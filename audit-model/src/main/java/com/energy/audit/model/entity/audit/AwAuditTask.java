package com.energy.audit.model.entity.audit;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwAuditTask extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long enterpriseId;
    private Integer auditYear;
    private Integer taskType;
    private String taskTitle;
    private Integer status;
    private Long assigneeId;
    private LocalDateTime assignTime;
    private LocalDateTime deadline;
    private LocalDateTime completeTime;
    private String result;

    // transient join fields (not persisted)
    private transient String enterpriseName;
    private transient String assigneeName;
}
