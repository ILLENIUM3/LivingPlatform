package com.liveplatform.api.web;

import com.liveplatform.api.service.DependencyUnavailableException;
import com.liveplatform.common.ApiResponse;
import com.liveplatform.user.api.UserBusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(UserBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> business(UserBusinessException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "USER_DISABLED" -> HttpStatus.FORBIDDEN;
            case "USER_VERSION_CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(new ApiResponse<>(exception.getCode(),
                exception.getMessage(), null, UUID.randomUUID().toString()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> invalidRequest(Exception exception) {
        return new ApiResponse<>("INVALID_ARGUMENT", "请求格式或参数类型错误", null, UUID.randomUUID().toString());
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> unavailable(DependencyUnavailableException exception) {
        String requestId = UUID.randomUUID().toString();
        LOG.warn("Dependency probe failed, requestId={}", requestId, exception);
        return ApiResponse.unavailable(requestId);
    }
}
