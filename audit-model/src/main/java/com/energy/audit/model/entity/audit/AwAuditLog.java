package com.energy.audit.model.entity.audit;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwAuditLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long operatorId;
    private String action;
    private String comment;
    private LocalDateTime operationTime;

    // transient join field
    private transient String operatorName;
}
