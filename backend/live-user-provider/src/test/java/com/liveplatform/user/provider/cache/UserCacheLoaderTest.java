package com.liveplatform.user.provider.cache;

import com.liveplatform.user.api.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.function.Supplier;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserCacheLoaderTest {
    private UserCache cache;
    private CacheRebuildLock lock;
    private Supplier<UserDTO> database;
    private UserCacheLoader loader;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        cache = mock(UserCache.class);
        lock = mock(CacheRebuildLock.class);
        database = mock(Supplier.class);
        loader = loader(true);
    }

    private UserCacheLoader loader(boolean enabled) {
        return new UserCacheLoader(cache, lock,
                new UserCacheProperties(enabled, "test:user:", 1800, 300, 30, 10),
                new CacheRebuildLockProperties(5000, 0, 40));
    }

    @Test
    void secondCheckUsesAnotherRequestsFillWithoutQueryingDatabase() {
        UserDTO user = new UserDTO();
        when(cache.get(1L)).thenReturn(new UserCache.Lookup(false, null), new UserCache.Lookup(true, user));
        when(lock.tryAcquire(1L)).thenReturn(new CacheRebuildLock.Attempt(CacheRebuildLock.State.ACQUIRED, "owner"));
        assertThat(loader.get(1L, database)).isSameAs(user);
        verifyNoInteractions(database);
        verify(lock).release(1L, "owner");
    }

    @Test
    void busyTimeoutQueriesDatabaseWithoutFillingOrReleasingOthersLock() {
        when(cache.get(1L)).thenReturn(new UserCache.Lookup(false, null));
        when(lock.tryAcquire(1L)).thenReturn(new CacheRebuildLock.Attempt(CacheRebuildLock.State.BUSY, null));
        loader.get(1L, database);
        verify(database, times(1)).get();
        verify(cache, never()).putIfOwned(anyLong(), any(), anyString());
        verify(lock, never()).release(anyLong(), anyString());
    }

    @Test
    void databaseFailureStillReleasesLockAndDoesNotCacheNotFound() {
        when(cache.get(1L)).thenReturn(new UserCache.Lookup(false, null));
        when(lock.tryAcquire(1L)).thenReturn(new CacheRebuildLock.Attempt(CacheRebuildLock.State.ACQUIRED, "owner"));
        RuntimeException failure = new IllegalStateException("database unavailable");
        when(database.get()).thenThrow(failure);
        assertThatThrownBy(() -> loader.get(1L, database)).isSameAs(failure);
        verify(lock).release(1L, "owner");
        verify(cache, never()).putIfOwned(anyLong(), any(), anyString());
    }

    @Test
    void disabledCacheBypassesBothCacheAndLock() {
        loader(false).get(1L, database);
        verify(database).get();
        verifyNoInteractions(cache, lock);
    }

    @Test
    void lockOutageQueriesDatabaseWithoutFilling() {
        when(cache.get(1L)).thenReturn(new UserCache.Lookup(false, null));
        when(lock.tryAcquire(1L)).thenReturn(new CacheRebuildLock.Attempt(CacheRebuildLock.State.UNAVAILABLE, null));
        loader.get(1L, database);
        verify(database).get();
        verify(cache, never()).putIfOwned(anyLong(), any(), anyString());
    }

    @Test
    void absentUserIsFilledOnlyUnderOwnedLease() {
        when(cache.get(1L)).thenReturn(new UserCache.Lookup(false, null));
        when(lock.tryAcquire(1L)).thenReturn(new CacheRebuildLock.Attempt(CacheRebuildLock.State.ACQUIRED, "owner"));
        assertThat(loader.get(1L, database)).isNull();
        verify(cache).putIfOwned(1L, null, "owner");
        verify(lock).release(1L, "owner");
    }
}
