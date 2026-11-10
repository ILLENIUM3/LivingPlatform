package com.liveplatform.user.provider.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "live.user-cache.rebuild-lock")
public record CacheRebuildLockProperties(long leaseMillis, long waitMillis, long pollMillis) {
    public CacheRebuildLockProperties {
        if (leaseMillis < 100 || leaseMillis > 60000 || waitMillis < 0 || waitMillis > 1000
                || pollMillis < 1 || pollMillis > 1000) {
            throw new IllegalArgumentException("Invalid cache rebuild lease/wait/poll configuration");
        }
    }
}
