package com.liveplatform.user.provider.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** 用户缓存重建专用租约。只减少重复查询，不承担数据库写入的正确性保证。 */
@Component
@EnableConfigurationProperties(CacheRebuildLockProperties.class)
public class CacheRebuildLock {
    private static final Logger LOG = LoggerFactory.getLogger(CacheRebuildLock.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>();
    static {
        RELEASE.setLocation(new ClassPathResource("redis/release-rebuild-lock.lua"));
        RELEASE.setResultType(Long.class);
    }
    private final StringRedisTemplate redis;
    private final UserCacheProperties cacheProperties;
    private final CacheRebuildLockProperties properties;

    public CacheRebuildLock(StringRedisTemplate redis, UserCacheProperties cacheProperties,
                            CacheRebuildLockProperties properties) {
        this.redis = redis;
        this.cacheProperties = cacheProperties;
        this.properties = properties;
    }

    public enum State { ACQUIRED, BUSY, UNAVAILABLE }
    public record Attempt(State state, String token) { }

    public Attempt tryAcquire(long userId) {
        // 每次尝试生成新令牌，线程重用或同一请求再次尝试也不会沿用旧所有权。
        String token = UUID.randomUUID().toString();
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(key(userId), token,
                    Duration.ofMillis(properties.leaseMillis()));
            if (Boolean.TRUE.equals(acquired)) { return new Attempt(State.ACQUIRED, token); }
            return new Attempt(acquired == null ? State.UNAVAILABLE : State.BUSY, null);
        } catch (DataAccessException exception) {
            // 连接超时可能是命令已执行但响应丢失；不宣称持锁，也不回填，让租约自行过期。
            LOG.warn("Cache rebuild lock unavailable, userId={}, reason={}", userId,
                    exception.getClass().getSimpleName());
            return new Attempt(State.UNAVAILABLE, null);
        }
    }

    public void release(long userId, String token) {
        try {
            redis.execute(RELEASE, List.of(key(userId)), token);
        } catch (DataAccessException exception) {
            // 解锁失败不覆盖原查询结果或异常，过期时间负责最终释放。
            LOG.warn("Cache rebuild unlock failed, userId={}; waiting for lease expiry", userId);
        }
    }

    String key(long userId) { return cacheProperties.keyPrefix() + userId + ":rebuild-lock"; }
}
