package com.liveplatform.api.web;

import com.liveplatform.api.service.SystemProbeService;
import com.liveplatform.user.api.UserProbeRpc;
import com.liveplatform.user.api.UserProbeResult;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class SystemControllerTest {
    private UserProbeRpc rpc;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        rpc = mock(UserProbeRpc.class);
        var service = new SystemProbeService();
        ReflectionTestUtils.setField(service, "userProbeRpc", rpc);
        mvc = MockMvcBuilders.standaloneSetup(new SystemController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void returnsTheRemoteDatabaseResultInThePublicEnvelope() throws Exception {
        when(rpc.probe()).thenReturn(new UserProbeResult("live-user-provider", "live_platform",
                "MySQL", "8.4.7", "stage01-ready"));
        mvc.perform(get("/api/system/user-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.database").value("live_platform"))
                .andExpect(jsonPath("$.data.probeValue").value("stage01-ready"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        verify(rpc, times(1)).probe();
    }

    @Test
    void rpcFailureReturns503WithoutExposingInternalDetails() throws Exception {
        when(rpc.probe()).thenThrow(new RpcException("private-host:20880 connection refused"));
        assertUnavailable();
    }

    @Test
    void remoteDatabaseFailureAlsoReturns503() throws Exception {
        when(rpc.probe()).thenThrow(new IllegalStateException("SELECT secret FROM private_table"));
        assertUnavailable();
    }

    private void assertUnavailable() throws Exception {
        mvc.perform(get("/api/system/user-service"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(content().string(not(containsString("private"))));
    }
}
