package com.liveplatform.user.provider.auth;

import com.liveplatform.user.api.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.util.Locale;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AuthStore store;
    private final LoginAccountService accounts;
    private final AuthProperties properties;
    private final boolean local;

    public AuthService(AuthStore store, LoginAccountService accounts, AuthProperties properties, Environment environment) {
        this.store = store;
        this.accounts = accounts;
        this.properties = properties;
        this.local = environment.acceptsProfiles(Profiles.of("local"));
    }

    public LoginCodeDTO sendCode(String phone) {
        validatePhone(phone);
        if (!local || !properties.mockEnabled()) {
            throw new UserBusinessException("AUTH_CODE_UNAVAILABLE", "短信服务尚未启用");
        }
        String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        store.issueCode(phone, code);
        return new LoginCodeDTO(code, properties.codeTtlSeconds(), properties.resendSeconds());
    }

    public LoginResultDTO login(String phone, String code) {
        validatePhone(phone);
        if (code == null || !code.matches("[0-9]{6}")) {
            throw new UserBusinessException("INVALID_ARGUMENT", "验证码必须为6位数字字符串");
        }
        store.consumeCode(phone, code);
        UserDTO user = accounts.findOrRegister(phone);
        // 返回时数据库事务已经提交；会话写失败仍保留账户，下次登录复用同一用户。
        String token = store.createSession(user.getId());
        return new LoginResultDTO(token, properties.sessionTtlSeconds(), user);
    }

    public UserDTO currentUser(String token) {
        long id = store.requireUserId(token);
        try { return accounts.getActiveUser(id); }
        catch (UserBusinessException exception) {
            if ("USER_NOT_FOUND".equals(exception.getCode())) { throw AuthStore.unauthenticated(); }
            throw exception;
        }
    }

    public void logout(String token) { store.deleteSession(token); }

    private void validatePhone(String phone) {
        if (phone == null || !phone.matches("1[3-9][0-9]{9}")) {
            throw new UserBusinessException("INVALID_ARGUMENT", "请输入11位大陆手机号");
        }
    }
}
