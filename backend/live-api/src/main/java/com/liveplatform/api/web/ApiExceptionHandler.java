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
            case "AUTH_UNAUTHENTICATED" -> HttpStatus.UNAUTHORIZED;
            case "AUTH_CODE_TOO_FREQUENT" -> HttpStatus.TOO_MANY_REQUESTS;
            case "AUTH_CODE_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
        var response = ResponseEntity.status(status).header("Cache-Control", "no-store");
        if (status == HttpStatus.UNAUTHORIZED) { response.header("WWW-Authenticate", "Bearer"); }
        return response.body(new ApiResponse<>(exception.getCode(),
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
        // RPC 异常消息可能携带调用参数；不把验证码或 Token 写入应用日志。
        LOG.warn("Dependency unavailable, requestId={}, causeType={}", requestId,
                exception.getCause() == null ? exception.getClass().getSimpleName()
                        : exception.getCause().getClass().getSimpleName());
        return ApiResponse.unavailable(requestId);
    }
}
