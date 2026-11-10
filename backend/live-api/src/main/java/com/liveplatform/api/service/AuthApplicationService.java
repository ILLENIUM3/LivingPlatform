package com.liveplatform.api.service;

import com.liveplatform.user.api.*;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import java.util.function.Supplier;

@Service
public class AuthApplicationService {
    // 验证码消费和会话创建有副作用，超时后不能由 RPC 自动重试。
    @DubboReference(version = "1.0.0", retries = 0, timeout = 5000, check = false)
    private AuthRpc rpc;

    public LoginCodeDTO sendCode(String phone) { return invoke(() -> rpc.sendCode(phone)); }
    public LoginResultDTO login(String phone, String code) { return invoke(() -> rpc.login(phone, code)); }
    public UserDTO currentUser(String token) { return invoke(() -> rpc.currentUser(token)); }
    public void logout(String token) { invoke(() -> { rpc.logout(token); return null; }); }

    private <T> T invoke(Supplier<T> operation) {
        try { return operation.get(); }
        catch (UserBusinessException exception) { throw exception; }
        catch (RuntimeException exception) { throw new DependencyUnavailableException(exception); }
    }
}
