package com.energy.audit.service.system;

import com.energy.audit.model.dto.ChangePasswordDTO;
import com.energy.audit.model.dto.LoginDTO;
import com.energy.audit.model.vo.LoginVO;
import com.energy.audit.model.vo.UserInfoVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
    void logout(String token);
    UserInfoVO getUserInfo();
    void changePassword(ChangePasswordDTO dto);
}
