package com.energy.audit.web.interceptor;

import com.energy.audit.common.util.JwtUtils;
import com.energy.audit.common.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthInterceptorTest {

    private static final String SECRET = "energy-audit-test-secret-key-at-least-256-bits";

    private JwtAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache("tokenBlacklist")).thenReturn(null);
        interceptor = new JwtAuthInterceptor(cacheManager);
        ReflectionTestUtils.setField(interceptor, "jwtSecret", SECRET);
    }

    @AfterEach
    void tearDown() {
        SecurityUtils.clear();
    }

    @Test
    void preHandleRejectsMissingBearerToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(new MockHttpServletRequest("GET", "/api/auth/info"), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("未提供认证令牌");
    }

    @Test
    void preHandleAcceptsValidTokenAndSetsSecurityContext() throws Exception {
        String token = JwtUtils.generateToken(12L, "auditor", 2, null, SECRET, 60_000L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/info");
        request.addHeader("Authorization", "Bearer " + token);

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(12L);
        assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("auditor");
        assertThat(SecurityUtils.getCurrentUserType()).isEqualTo(2);
    }
}
