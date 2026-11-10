package com.liveplatform.user.provider.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "live.auth")
public record AuthProperties(boolean mockEnabled, String keyPrefix, long codeTtlSeconds,
                             long resendSeconds, int maxAttempts, long sessionTtlSeconds) {
    public AuthProperties {
        if (keyPrefix == null || keyPrefix.isBlank() || codeTtlSeconds < 1 || codeTtlSeconds > 600
                || resendSeconds < 1 || resendSeconds > 600 || maxAttempts < 1 || maxAttempts > 10
                || sessionTtlSeconds < 1 || sessionTtlSeconds > 2592000) {
            throw new IllegalArgumentException("Invalid authentication configuration");
        }
    }
}
