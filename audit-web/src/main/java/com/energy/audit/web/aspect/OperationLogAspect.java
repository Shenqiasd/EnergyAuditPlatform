package com.energy.audit.web.aspect;

import com.energy.audit.common.util.SecurityUtils;
import com.energy.audit.dao.mapper.system.SysOperationLogMapper;
import com.energy.audit.model.entity.system.SysOperationLog;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * AOP aspect for recording system operation logs into sys_operation_log
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private static final int MAX_PARAM_LENGTH = 2000;

    private final SysOperationLogMapper operationLogMapper;

    public OperationLogAspect(SysOperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Pointcut("execution(* com.energy.audit.web.controller..*(..)) && " +
              "!execution(* com.energy.audit.web.controller.system.AuthController.login(..)) && " +
              "!execution(* com.energy.audit.web.controller.enterprise.RegistrationController.submit(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog opLog = new SysOperationLog();
        opLog.setUserId(SecurityUtils.getCurrentUserId());
        opLog.setUsername(SecurityUtils.getCurrentUsername());
        opLog.setMethod(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
        opLog.setOperationTime(LocalDateTime.now());

        // Request info
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                opLog.setRequestUrl(request.getRequestURI());
                opLog.setIp(getClientIp(request));
            }
        } catch (Exception ignored) {}

        // Params (truncated)
        try {
            String params = Arrays.toString(joinPoint.getArgs());
            if (params.length() > MAX_PARAM_LENGTH) {
                params = params.substring(0, MAX_PARAM_LENGTH) + "...";
            }
            opLog.setRequestParams(params);
        } catch (Exception ignored) {}

        Object result = null;
        try {
            result = joinPoint.proceed();
            opLog.setStatus(1);
        } catch (Throwable ex) {
            opLog.setStatus(0);
            String errMsg = ex.getMessage();
            if (errMsg != null && errMsg.length() > 500) {
                errMsg = errMsg.substring(0, 500);
            }
            opLog.setErrorMsg(errMsg);
            throw ex;
        } finally {
            // Async-style: save log without affecting main flow
            try {
                operationLogMapper.insert(opLog);
            } catch (Exception e) {
                log.warn("Failed to save operation log: {}", e.getMessage());
            }
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
