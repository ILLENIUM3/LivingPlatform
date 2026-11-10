package com.liveplatform.user.provider.cache;

import com.liveplatform.user.api.UserDTO;
import org.springframework.stereotype.Component;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 组织缓存读取与重建；loader 只查数据库，null 表示用户不存在，异常不能缓存成空值。 */
@Component
public class UserCacheLoader {
    private final UserCache cache;
    private final CacheRebuildLock lock;
    private final UserCacheProperties cacheProperties;
    private final CacheRebuildLockProperties lockProperties;

    public UserCacheLoader(UserCache cache, CacheRebuildLock lock, UserCacheProperties cacheProperties,
                           CacheRebuildLockProperties lockProperties) {
        this.cache = cache;
        this.lock = lock;
        this.cacheProperties = cacheProperties;
        this.lockProperties = lockProperties;
    }

    public UserDTO get(long userId, Supplier<UserDTO> loader) {
        if (!cacheProperties.enabled()) { return loader.get(); }
        UserCache.Lookup cached = cache.get(userId);
        if (cached.hit()) { return cached.user(); }
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(lockProperties.waitMillis());
        while (true) {
            if (Thread.currentThread().isInterrupted()) { throw new CancellationException("Cache wait interrupted"); }
            CacheRebuildLock.Attempt attempt = lock.tryAcquire(userId);
            if (attempt.state() == CacheRebuildLock.State.UNAVAILABLE) { return loader.get(); }
            if (attempt.state() == CacheRebuildLock.State.ACQUIRED) {
                try {
                    // 首次读缓存到拿到锁之间，其他请求可能已完成回填，因此必须二次检查。
                    cached = cache.get(userId);
                    //二次判断，防止重复写入
                    if (cached.hit()) { return cached.user(); }
                    UserDTO user = loader.get();
                    cache.putIfOwned(userId, user, attempt.token());
                    //这里看
                    return user;
                } finally {
                    lock.release(userId, attempt.token());
                }
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) { break; }
            // 有限循环，不递归；随机等待减少同一批请求同步抢锁。
            long sleepNanos = Math.min(remaining, TimeUnit.MILLISECONDS.toNanos(
                    ThreadLocalRandom.current().nextLong(1, lockProperties.pollMillis() + 1)));
            try { TimeUnit.NANOSECONDS.sleep(sleepNanos); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Cache wait interrupted");
            }
            cached = cache.get(userId);
            if (cached.hit()) { return cached.user(); }
            if (System.nanoTime() >= deadline) { break; }
        }
        // 超时选择可用性：回源但不回填。故障或长查询下仍可能并发查库，并非严格单飞。
        return loader.get();
    }
}
