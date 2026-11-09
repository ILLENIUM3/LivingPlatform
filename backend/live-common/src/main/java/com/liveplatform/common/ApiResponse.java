package com.liveplatform.common;

import java.util.UUID;

/** HTTP 响应；RPC 契约单独定义，不把 HTTP 语义带到 Provider。 */
public record ApiResponse<T>(String code, String message, T data, String requestId) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data, UUID.randomUUID().toString());
    }

    public static ApiResponse<Void> unavailable(String requestId) {
        return new ApiResponse<>("DEPENDENCY_UNAVAILABLE", "用户服务或数据库暂不可用", null, requestId);
    }
}
