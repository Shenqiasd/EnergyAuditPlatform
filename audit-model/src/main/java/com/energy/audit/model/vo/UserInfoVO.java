package com.energy.audit.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class UserInfoVO implements Serializable {
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer userType;
    private Long enterpriseId;
    private String enterpriseName;
    private Integer auditYear;
    private Boolean passwordChanged;
}
