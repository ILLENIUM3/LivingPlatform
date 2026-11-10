package com.liveplatform.user.provider.rpc;

import com.liveplatform.user.api.*;
import com.liveplatform.user.provider.auth.AuthService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0.0", retries = 0)
public class AuthRpcImpl implements AuthRpc {
    private final AuthService service;
    public AuthRpcImpl(AuthService service) { this.service = service; }
    public LoginCodeDTO sendCode(String phone) { return service.sendCode(phone); }
    public LoginResultDTO login(String phone, String code) { return service.login(phone, code); }
    public UserDTO currentUser(String token) { return service.currentUser(token); }
    public void logout(String token) { service.logout(token); }
}
