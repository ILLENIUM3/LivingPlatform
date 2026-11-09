package com.liveplatform.user.provider.rpc;

import com.liveplatform.user.api.UserProbeResult;
import com.liveplatform.user.api.UserProbeRpc;
import com.liveplatform.user.provider.service.DatabaseProbeService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0")
public class UserProbeRpcImpl implements UserProbeRpc {
    private final DatabaseProbeService databaseProbeService;

    public UserProbeRpcImpl(DatabaseProbeService databaseProbeService) {
        this.databaseProbeService = databaseProbeService;
    }

    @Override
    public UserProbeResult probe() {
        return databaseProbeService.probe();
    }
}
