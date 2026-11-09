package com.liveplatform.user.provider.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "live.user-cache")
public record UserCacheProperties(boolean enabled, String keyPrefix, long ttlSeconds,
                                  long jitterSeconds, long nullTtlSeconds, long nullJitterSeconds) {
    public UserCacheProperties {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            throw new IllegalArgumentException("User cache key prefix is required");
        }
        if (ttlSeconds < 1 || ttlSeconds > 86400 || nullTtlSeconds < 1 || nullTtlSeconds > 86400
                || jitterSeconds < 0 || jitterSeconds > 86400
                || nullJitterSeconds < 0 || nullJitterSeconds > 86400) {
            throw new IllegalArgumentException("Cache TTL must be 1..86400 seconds, jitter 0..86400 seconds");
        }
    }
}
