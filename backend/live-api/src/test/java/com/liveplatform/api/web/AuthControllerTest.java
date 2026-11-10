package com.liveplatform.api.web;

import com.liveplatform.api.service.AuthApplicationService;
import com.liveplatform.user.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private AuthRpc rpc;
    private MockMvc mvc;
    private final String token = "x".repeat(43);

    @BeforeEach
    void setUp() {
        rpc = mock(AuthRpc.class);
        var service = new AuthApplicationService();
        ReflectionTestUtils.setField(service, "rpc", rpc);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void missingTokenAndForgedUserIdCannotSupplyIdentity() throws Exception {
        mvc.perform(get("/api/users/me").header("X-User-Id", "12").param("userId", "12"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(rpc);
    }

    @Test
    void loginReturnsStringUserIdAndDoesNotCacheCredentials() throws Exception {
        var user = new UserDTO();
        user.setId(9007199254740993L);
        user.setStatus(1);
        user.setVersion(0L);
        when(rpc.login("13800138000", "012345")).thenReturn(new LoginResultDTO(token, 60, user));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800138000\",\"code\":\"012345\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.user.id").value("9007199254740993"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.token").value(token));
    }

    @Test
    void mapsRateLimitAndDisabledMock() throws Exception {
        when(rpc.sendCode("13800138000"))
                .thenThrow(new UserBusinessException("AUTH_CODE_TOO_FREQUENT", "wait"))
                .thenThrow(new UserBusinessException("AUTH_CODE_UNAVAILABLE", "disabled"));
        mvc.perform(post("/api/auth/code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}")).andExpect(status().isTooManyRequests());
        mvc.perform(post("/api/auth/code").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}")).andExpect(status().isServiceUnavailable());
    }

    @Test
    void expiredAndDisabledSessionsHaveDistinctStatusAndOutageNeverPasses() throws Exception {
        when(rpc.currentUser(token))
                .thenThrow(new UserBusinessException("AUTH_UNAUTHENTICATED", "expired"))
                .thenThrow(new UserBusinessException("USER_DISABLED", "disabled"))
                .thenThrow(new IllegalStateException("private redis endpoint"));
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void logoutIsRepeatableAndOnlyUsesBearer() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
        verify(rpc, times(2)).logout(token);
    }
}
