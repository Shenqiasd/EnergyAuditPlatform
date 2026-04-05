package com.energy.audit.model.entity.system;

import com.energy.audit.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictType extends BaseEntity {
    private String dictType;
    private String dictName;
    private Integer status;
    private String remark;
}
