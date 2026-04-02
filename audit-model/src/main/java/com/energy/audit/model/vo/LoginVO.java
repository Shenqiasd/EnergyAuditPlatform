package com.energy.audit.model.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class LoginVO implements Serializable {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Integer userType;
    private Long enterpriseId;
    private String enterpriseName;
    private Boolean passwordChanged;
}
