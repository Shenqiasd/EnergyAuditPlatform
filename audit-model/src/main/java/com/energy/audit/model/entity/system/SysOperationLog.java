package com.energy.audit.model.entity.system;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * System operation log entity — matches sys_operation_log production schema
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperationLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Operator user ID */
    private Long userId;

    /** Operator username */
    private String username;

    /** Operation description */
    private String operation;

    /** Request method */
    private String method;

    /** Request URL */
    private String requestUrl;

    /** Request parameters (JSON) */
    private String requestParams;

    /** Response result (truncated JSON) */
    private String responseResult;

    /** Client IP address */
    private String ip;

    /** Status (0=fail, 1=success) */
    private Integer status;

    /** Error message */
    private String errorMsg;

    /** Operation time */
    private LocalDateTime operationTime;
}
