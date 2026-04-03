package com.energy.audit.model.entity.extraction;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeSubmissionTable extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long submissionId;
    private Long enterpriseId;
    private Integer auditYear;
    private String tagName;
    private Integer rowIndex;
    private String rowKey;
    private String columnValues;
}
