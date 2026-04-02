package com.energy.audit.web.controller.system;

import com.energy.audit.common.result.R;
import com.energy.audit.model.dto.ChangePasswordDTO;
import com.energy.audit.model.dto.LoginDTO;
import com.energy.audit.model.vo.LoginVO;
import com.energy.audit.model.vo.UserInfoVO;
import com.energy.audit.service.system.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication endpoints")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public R<LoginVO> login(@jakarta.validation.Valid @RequestBody LoginDTO dto) {
        return R.ok(authService.login(dto));
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        authService.logout(token);
        return R.ok();
    }

    @Operation(summary = "Get current user info")
    @GetMapping("/info")
    public R<UserInfoVO> info() {
        return R.ok(authService.getUserInfo());
    }

    @Operation(summary = "Change password")
    @PutMapping("/password")
    public R<Void> changePassword(@jakarta.validation.Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return R.ok();
    }
}
