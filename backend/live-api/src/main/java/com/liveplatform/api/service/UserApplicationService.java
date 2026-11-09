package com.liveplatform.api.service;

import com.liveplatform.user.api.UserBusinessException;
import com.liveplatform.user.api.UserDTO;
import com.liveplatform.user.api.UserRpc;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class UserApplicationService {
    @DubboReference(version = "1.0.0", retries = 0, timeout = 5000, check = false)
    private UserRpc rpc;

    public UserDTO create(String name, String avatar) { return invoke(() -> rpc.create(name, avatar)); }
    public UserDTO getById(Long id) { return invoke(() -> rpc.getById(id)); }
    public UserDTO update(Long id, String name, String avatar, Long version) {
        return invoke(() -> rpc.updateProfile(id, name, avatar, version));
    }

    private UserDTO invoke(Supplier<UserDTO> operation) {
        try { return operation.get(); }
        catch (UserBusinessException exception) { throw exception; }
        catch (RuntimeException exception) { throw new DependencyUnavailableException(exception); }
    }
}
