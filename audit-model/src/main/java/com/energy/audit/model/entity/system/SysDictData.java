package com.energy.audit.model.entity.system;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictData extends BaseEntity {
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
