package com.liveplatform.user.provider.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveplatform.user.api.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Component
@EnableConfigurationProperties(UserCacheProperties.class)
public class UserCache {
    private static final Logger LOG = LoggerFactory.getLogger(UserCache.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final UserCacheProperties properties;

    public UserCache(StringRedisTemplate redis, ObjectMapper mapper, UserCacheProperties properties) {
        this.redis = redis;
        this.mapper = mapper;
        this.properties = properties;
    }

    /** hit=false 是未命中；hit=true、user=null 是命中“用户不存在”。 */
    public record Lookup(boolean hit, UserDTO user) {
        static Lookup miss() { return new Lookup(false, null); }
    }

    // 显式 JSON 结构，不使用 Java 原生序列化或带类名的多态反序列化。
    public record Entry(int schemaVersion, boolean missing, UserDTO user) { }

    public Lookup get(long userId) {
        if (!properties.enabled()) { return Lookup.miss(); }
        try {
            String json = redis.opsForValue().get(key(userId));
            if (json == null) {
                LOG.debug("User cache miss, userId={}", userId);
                return Lookup.miss();
            }
            Entry entry = mapper.readValue(json, Entry.class);
            if (!valid(entry, userId)) {
                LOG.warn("Invalid user cache entry, userId={}; falling back to database", userId);
                return Lookup.miss();
            }
            LOG.debug("User cache hit, userId={}, missing={}", userId, entry.missing());
            return new Lookup(true, entry.user());
        } catch (DataAccessException | JsonProcessingException exception) {
            LOG.warn("User cache read failed, userId={}, reason={}; falling back to database",
                    userId, exception.getClass().getSimpleName());
            return Lookup.miss();
        }
    }

    public void put(long userId, UserDTO user) {
        if (!properties.enabled()) { return; }
        boolean missing = user == null;
        //空值写入
        long base = missing ? properties.nullTtlSeconds() : properties.ttlSeconds();
        long jitter = missing ? properties.nullJitterSeconds() : properties.jitterSeconds();
        Duration ttl = Duration.ofSeconds(base + ThreadLocalRandom.current().nextLong(jitter + 1));
        //过期时间
        try {
            redis.opsForValue().set(key(userId), mapper.writeValueAsString(new Entry(1, missing, user)), ttl);
        } catch (DataAccessException | JsonProcessingException exception) {
            // 缓存写入失败不改变数据库查询结果。
            LOG.warn("User cache fill failed, userId={}, reason={}", userId, exception.getClass().getSimpleName());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    //事务提交时才更新数据key,回滚时不删除,phase = TransactionPhase.AFTER_COMMIT
    //定义删除缓存方法，用户数据改变
    public void onUserChanged(UserChangedEvent event) {
        if (!properties.enabled()) { return; }
        try {
            redis.delete(key(event.userId()));
            LOG.debug("User cache invalidated after commit, userId={}", event.userId());
        } catch (DataAccessException exception) {
            // 数据库已经提交：不能把删除缓存失败伪装成业务回滚，暂由 TTL 收敛。
            LOG.warn("User cache invalidation failed AFTER COMMIT, userId={}; stale data may remain until TTL",
                    event.userId(), exception);
        }
    }

    private String key(long userId) { return properties.keyPrefix() + userId; }

    //JSON是否符合规范
    private boolean valid(Entry entry, long userId) {
        if (entry == null || entry.schemaVersion() != 1) { return false; }
        if (entry.missing()) { return entry.user() == null; }
        UserDTO user = entry.user();
        return user != null && Long.valueOf(userId).equals(user.getId())
                && user.getNickname() != null && user.getAvatarUrl() != null
                && user.getStatus() != null && (user.getStatus() == 0 || user.getStatus() == 1)
                && user.getVersion() != null && user.getVersion() >= 0
                && user.getCreatedAt() != null && user.getUpdatedAt() != null;
    }
}
