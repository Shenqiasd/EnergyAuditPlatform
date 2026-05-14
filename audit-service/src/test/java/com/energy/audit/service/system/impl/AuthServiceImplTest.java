package com.energy.audit.service.system.impl;

import com.energy.audit.common.exception.BusinessException;
import com.energy.audit.dao.mapper.enterprise.EntEnterpriseMapper;
import com.energy.audit.dao.mapper.system.SysUserMapper;
import com.energy.audit.model.dto.LoginDTO;
import com.energy.audit.model.entity.enterprise.EntEnterprise;
import com.energy.audit.model.entity.system.SysUser;
import com.energy.audit.model.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private static final String SECRET = "energy-audit-test-secret-key-at-least-256-bits";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private SysUserMapper userMapper;
    private EntEnterpriseMapper enterpriseMapper;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        enterpriseMapper = mock(EntEnterpriseMapper.class);
        authService = new AuthServiceImpl(userMapper, enterpriseMapper, mock(CacheManager.class));
        ReflectionTestUtils.setField(authService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(authService, "jwtExpiration", 60_000L);
    }

    @Test
    void loginRejectsAccountFromWrongPortal() {
        LoginDTO dto = loginDto("admin", "password", "enterprise");
        when(userMapper.selectByUsername("admin")).thenReturn(user(1L, "admin", 1, null));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("该账号无此门户的登录权限");
    }

    @Test
    void loginReturnsEnterpriseNameAndUpdatesLastLogin() {
        LoginDTO dto = loginDto("enterprise", "password", "enterprise");
        when(userMapper.selectByUsername("enterprise")).thenReturn(user(7L, "enterprise", 3, 88L));

        EntEnterprise enterprise = new EntEnterprise();
        enterprise.setEnterpriseName("测试企业");
        when(enterpriseMapper.selectById(88L)).thenReturn(enterprise);

        LoginVO result = authService.login(dto);

        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getUserId()).isEqualTo(7L);
        assertThat(result.getUsername()).isEqualTo("enterprise");
        assertThat(result.getUserType()).isEqualTo(3);
        assertThat(result.getEnterpriseId()).isEqualTo(88L);
        assertThat(result.getEnterpriseName()).isEqualTo("测试企业");
        assertThat(result.getPasswordChanged()).isTrue();
        verify(userMapper).updateLastLoginTime(7L);
    }

    private static LoginDTO loginDto(String username, String password, String portal) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setPortal(portal);
        return dto;
    }

    private static SysUser user(Long id, String username, Integer userType, Long enterpriseId) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(ENCODER.encode("password"));
        user.setRealName(username + " real");
        user.setUserType(userType);
        user.setEnterpriseId(enterpriseId);
        user.setStatus(1);
        user.setPasswordChanged(1);
        return user;
    }
}
