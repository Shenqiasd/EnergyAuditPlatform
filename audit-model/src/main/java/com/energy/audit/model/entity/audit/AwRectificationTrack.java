package com.energy.audit.model.entity.audit;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwRectificationTrack extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long enterpriseId;
    private Integer auditYear;
    private String itemName;
    private String requirement;
    private Integer status;
    private LocalDateTime deadline;
    private LocalDateTime completeTime;
    private String result;

    private transient String enterpriseName;
    private transient String taskTitle;
}
