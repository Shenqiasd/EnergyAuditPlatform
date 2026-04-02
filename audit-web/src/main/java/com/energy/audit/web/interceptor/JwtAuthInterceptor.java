package com.energy.audit.web.interceptor;

import com.energy.audit.common.result.R;
import com.energy.audit.common.util.JwtUtils;
import com.energy.audit.common.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final CacheManager cacheManager;

    public JwtAuthInterceptor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendError(response, 401, "未提供认证令牌");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        // Check token blacklist
        if (cacheManager.getCache("tokenBlacklist") != null
                && cacheManager.getCache("tokenBlacklist").get(token) != null) {
            sendError(response, 401, "令牌已失效");
            return false;
        }

        try {
            Claims claims = JwtUtils.parseToken(token, jwtSecret);

            Long userId = claims.get("userId", Long.class);
            // Handle potential Integer/Long type issue from JSON deserialization
            if (userId == null) {
                Object raw = claims.get("userId");
                if (raw instanceof Integer) {
                    userId = ((Integer) raw).longValue();
                } else if (raw instanceof Number) {
                    userId = ((Number) raw).longValue();
                }
            }
            String username = claims.get("username", String.class);
            Integer userType = claims.get("userType", Integer.class);

            Long enterpriseId = null;
            Object rawEnt = claims.get("enterpriseId");
            if (rawEnt instanceof Number) {
                enterpriseId = ((Number) rawEnt).longValue();
            }

            SecurityUtils.setContext(userId, username, userType, enterpriseId);
            return true;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            sendError(response, 401, "令牌无效或已过期");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityUtils.clear();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(R.fail(status, message)));
    }
}
