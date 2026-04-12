package com.energy.audit.common.exception;

import com.energy.audit.common.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.error("Business exception: {}", e.getMessage(), e);
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        log.error("Validation exception: {}", message);
        return R.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("Unexpected exception: {}", e.getMessage(), e);
        // Include root-cause chain for diagnostics (no secrets in stack traces)
        StringBuilder detail = new StringBuilder(e.getClass().getSimpleName());
        if (e.getMessage() != null) detail.append(": ").append(e.getMessage());
        Throwable cause = e.getCause();
        while (cause != null) {
            detail.append(" → ").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null) detail.append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }
        return R.fail(500, detail.toString());
    }
}
