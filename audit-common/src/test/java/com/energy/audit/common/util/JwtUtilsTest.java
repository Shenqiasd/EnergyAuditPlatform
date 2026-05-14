package com.energy.audit.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private static final String SECRET = "energy-audit-test-secret-key-at-least-256-bits";

    @Test
    void generatedTokenRoundTripsUserClaims() {
        String token = JwtUtils.generateToken(7L, "enterprise_user", 3, 99L, SECRET, 60_000L);

        Claims claims = JwtUtils.parseToken(token, SECRET);

        assertThat(claims.getSubject()).isEqualTo("enterprise_user");
        assertThat(claims.get("userId", Long.class)).isEqualTo(7L);
        assertThat(claims.get("username", String.class)).isEqualTo("enterprise_user");
        assertThat(claims.get("userType", Integer.class)).isEqualTo(3);
        assertThat(claims.get("enterpriseId", Long.class)).isEqualTo(99L);
    }

    @Test
    void expiredOrInvalidTokenIsReportedAsExpired() {
        assertThat(JwtUtils.isTokenExpired("not-a-valid-token", SECRET)).isTrue();
    }
}
