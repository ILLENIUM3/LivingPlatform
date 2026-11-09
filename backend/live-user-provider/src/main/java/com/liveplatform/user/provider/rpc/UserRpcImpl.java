package com.liveplatform.user.provider.rpc;

import com.liveplatform.user.api.UserDTO;
import com.liveplatform.user.api.UserRpc;
import com.liveplatform.user.provider.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0", retries = 0)
public class UserRpcImpl implements UserRpc {
    private final UserService service;
    public UserRpcImpl(UserService service) { this.service = service; }
    public UserDTO create(String nickname, String avatarUrl) { return service.create(nickname, avatarUrl); }
    public UserDTO getById(Long id) { return service.getById(id); }
    public UserDTO updateProfile(Long id, String nickname, String avatarUrl, Long version) {
        return service.updateProfile(id, nickname, avatarUrl, version);
    }
}
