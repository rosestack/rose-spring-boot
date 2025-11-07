package io.github.rosestack.spring.boot.web.exception;

import io.github.rosestack.core.exception.BusinessException;
import io.github.rosestack.core.util.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResponse<Void> handleValidation(Exception ex) {
        log.warn("Validation failed", ex);
        String message = "validation failed";
        if (ex instanceof MethodArgumentNotValidException manv) {
            FieldError fe = manv.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .orElse(null);
            if (fe != null) {
                message = fe.getField() + ": " + fe.getDefaultMessage();
            }
        } else if (ex instanceof BindException be) {
            FieldError fe =
                    be.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
            if (fe != null) {
                message = fe.getField() + ": " + fe.getDefaultMessage();
            }
        }
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ConstraintViolationException.class})
    public ApiResponse<Void> handleConstraint(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .orElse("constraint violation");
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ApiResponse<Void> handleBadBody(HttpMessageNotReadableException ex) {
        log.warn("Bad request body", ex);
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "request body error");
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({OptimisticLockException.class})
    public ApiResponse<Void> handleOptimisticLock(OptimisticLockException ex) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.CONFLICT.value(), "conflict");
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NotFoundException.class})
    public ApiResponse<Void> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "not found");
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BusinessException.class)
    public ApiResponse handleBusiness(BusinessException ex) {
        log.error("Business error", ex);
        return ApiResponse.ok(ex.getMessageArgs());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleOthers(Exception ex) {
        log.error("Internal server error", ex);
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "internal error");
    }

    public static class OptimisticLockException extends RuntimeException {
        public OptimisticLockException(String message) {
            super(message);
        }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}
