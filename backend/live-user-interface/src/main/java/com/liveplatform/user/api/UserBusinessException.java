package com.liveplatform.user.api;

/** RPC 双方共享的业务异常；与网络/数据库故障区分。 */
public class UserBusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String code;

    public UserBusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
