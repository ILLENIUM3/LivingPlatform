package com.liveplatform.user.api;

/** 第一阶段的只读连通性契约，用户业务在下一阶段扩展。 */
public interface UserProbeRpc {
    UserProbeResult probe();
}
