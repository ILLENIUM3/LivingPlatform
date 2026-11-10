package com.liveplatform.user.provider.auth;

import com.liveplatform.user.api.UserBusinessException;
import com.liveplatform.user.api.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.data.redis.RedisConnectionFailureException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final AuthStore store = mock(AuthStore.class);
    private final LoginAccountService accounts = mock(LoginAccountService.class);

    private AuthService service(boolean enabled, String... profiles) {
        var env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        return new AuthService(store, accounts, new AuthProperties(enabled, "test:", 300, 60, 5, 604800), env);
    }

    @Test
    void mockRequiresBothLocalProfileAndExplicitSwitch() {
        assertThatThrownBy(() -> service(true).sendCode("13800138000"))
                .isInstanceOf(UserBusinessException.class).hasMessage("短信服务尚未启用");
        assertThatThrownBy(() -> service(false, "local").sendCode("13800138000"))
                .isInstanceOf(UserBusinessException.class);
        verifyNoInteractions(store);
        var result = service(true, "local").sendCode("13800138000");
        assertThat(result.getMockCode()).matches("[0-9]{6}");
        verify(store).issueCode("13800138000", result.getMockCode());
    }

    @Test
    void consumesCodeBeforeRegisteringAndOnlyThenIssuesSession() {
        var user = new UserDTO();
        user.setId(12L);
        when(accounts.findOrRegister("13800138000")).thenReturn(user);
        when(store.createSession(12)).thenReturn("token");
        var result = service(true, "local").login("13800138000", "012345");
        var order = inOrder(store, accounts);
        order.verify(store).consumeCode("13800138000", "012345");
        order.verify(accounts).findOrRegister("13800138000");
        order.verify(store).createSession(12);
        assertThat(result.getToken()).isEqualTo("token");
        assertThat(result.getExpiresInSeconds()).isEqualTo(604800);
    }

    @Test
    void rejectedCodeNeverCreatesAnAccountOrSession() {
        doThrow(new UserBusinessException("AUTH_CODE_INVALID", "invalid"))
                .when(store).consumeCode("13800138000", "000000");
        assertThatThrownBy(() -> service(true, "local").login("13800138000", "000000"))
                .isInstanceOf(UserBusinessException.class);
        verifyNoInteractions(accounts);
        verify(store, never()).createSession(anyLong());
    }

    @Test
    void malformedPhoneAndCodeNeverReachRedis() {
        var service = service(true, "local");
        assertThatThrownBy(() -> service.sendCode("13800138000 ")).isInstanceOf(UserBusinessException.class);
        assertThatThrownBy(() -> service.login("13800138000", null)).isInstanceOf(UserBusinessException.class);
        assertThatThrownBy(() -> service.login("13800138000", "12345")).isInstanceOf(UserBusinessException.class);
        verifyNoInteractions(store, accounts);
    }

    @Test
    void redisOutageDoesNotAuthenticateAndMissingAccountBecomesUnauthorized() {
        when(store.requireUserId("token")).thenThrow(new RedisConnectionFailureException("offline"));
        assertThatThrownBy(() -> service(false).currentUser("token"))
                .isInstanceOf(RedisConnectionFailureException.class);
        verifyNoInteractions(accounts);
        when(store.requireUserId("valid")).thenReturn(12L);
        when(accounts.getActiveUser(12)).thenThrow(new UserBusinessException("USER_NOT_FOUND", "missing"));
        assertThatThrownBy(() -> service(false).currentUser("valid"))
                .isInstanceOfSatisfying(UserBusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo("AUTH_UNAUTHENTICATED"));
    }
}
