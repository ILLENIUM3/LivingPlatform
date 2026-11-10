package com.liveplatform.user.provider.auth;

import com.liveplatform.user.api.UserBusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthStoreTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final AuthStore store = new AuthStore(redis, new AuthProperties(false, "test:", 300, 60, 5, 60));

    @Test
    @SuppressWarnings("unchecked")
    void tokenIsRandomStoredAsDigestAndReadDoesNotExtendTtl() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("12"), eq(Duration.ofSeconds(60)))).thenReturn(true);
        String token = store.createSession(12);
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        var key = ArgumentCaptor.forClass(String.class);
        verify(values).setIfAbsent(key.capture(), eq("12"), eq(Duration.ofSeconds(60)));
        assertThat(key.getValue()).matches("test:session:[0-9a-f]{64}").doesNotContain(token);
        when(values.get(key.getValue())).thenReturn("12");
        assertThat(store.requireUserId(token)).isEqualTo(12);
        store.deleteSession(token);
        store.deleteSession(token);
        verify(redis, times(2)).delete(key.getValue());
        verify(values).get(key.getValue());
        verifyNoMoreInteractions(values);
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingOrMalformedTokenCannotAuthenticate() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        assertThatThrownBy(() -> store.requireUserId("x".repeat(43)))
                .isInstanceOfSatisfying(UserBusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo("AUTH_UNAUTHENTICATED"));
        assertThatThrownBy(() -> store.requireUserId("bad")).isInstanceOf(UserBusinessException.class);
        assertThatThrownBy(() -> store.requireUserId(null)).isInstanceOf(UserBusinessException.class);
        verify(values, times(1)).get(anyString());
    }
}
