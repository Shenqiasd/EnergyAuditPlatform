package com.energy.audit.common.util;

import com.energy.audit.common.exception.BusinessException;

public class SecurityUtils {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Integer> USER_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<Long> ENTERPRISE_ID = new ThreadLocal<>();

    private SecurityUtils() {}

    public static void setContext(Long userId, String username, Integer userType, Long enterpriseId) {
        USER_ID.set(userId);
        USERNAME.set(username);
        USER_TYPE.set(userType);
        ENTERPRISE_ID.set(enterpriseId);
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        USER_TYPE.remove();
        ENTERPRISE_ID.remove();
    }

    public static Long getCurrentUserId() {
        return USER_ID.get();
    }

    public static Long getRequiredCurrentUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BusinessException(401, "用户未认证");
        }
        return userId;
    }

    public static String getCurrentUsername() {
        return USERNAME.get();
    }

    public static Integer getCurrentUserType() {
        return USER_TYPE.get();
    }

    public static Long getCurrentEnterpriseId() {
        return ENTERPRISE_ID.get();
    }

    public static Long getRequiredCurrentEnterpriseId() {
        Long enterpriseId = ENTERPRISE_ID.get();
        if (enterpriseId == null) {
            throw new BusinessException(403, "当前用户不属于任何企业");
        }
        return enterpriseId;
    }
}
