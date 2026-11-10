package com.liveplatform.user.api;

import java.io.Serializable;

/** mockCode 仅用于显式开启的本地模拟短信，不写入日志。 */
public class LoginCodeDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String mockCode;
    private long expiresInSeconds;
    private long resendAfterSeconds;

    public LoginCodeDTO() { }
    public LoginCodeDTO(String mockCode, long expiresInSeconds, long resendAfterSeconds) {
        this.mockCode = mockCode;
        this.expiresInSeconds = expiresInSeconds;
        this.resendAfterSeconds = resendAfterSeconds;
    }
    public String getMockCode() { return mockCode; }
    public void setMockCode(String mockCode) { this.mockCode = mockCode; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long value) { expiresInSeconds = value; }
    public long getResendAfterSeconds() { return resendAfterSeconds; }
    public void setResendAfterSeconds(long value) { resendAfterSeconds = value; }
}
