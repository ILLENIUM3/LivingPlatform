package com.liveplatform.api.service;

import com.liveplatform.user.api.UserProbeResult;
import com.liveplatform.user.api.UserProbeRpc;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

@Service
public class SystemProbeService {
    @DubboReference(version = "1.0.0", retries = 0, timeout = 5000, check = false)
    private UserProbeRpc userProbeRpc;

    public UserProbeResult probe() {
        try {
            return userProbeRpc.probe();
        } catch (RuntimeException exception) {
            // 包含 RPC 不可达与 Provider 数据库异常，不将内部地址或 SQL 返回给调用者。
            throw new DependencyUnavailableException(exception);
        }
    }
}
