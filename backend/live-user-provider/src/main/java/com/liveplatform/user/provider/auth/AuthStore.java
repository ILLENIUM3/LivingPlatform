package com.liveplatform.user.provider.auth;

import com.liveplatform.user.api.UserBusinessException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/** Redis 是验证码及会话的事实来源，任何读写故障都向上抛出，不能降级放行。 */
@Component
@EnableConfigurationProperties(AuthProperties.class)
public class AuthStore {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DefaultRedisScript<Long> ISSUE = script("issue-login-code.lua");
    private static final DefaultRedisScript<Long> CONSUME = script("consume-login-code.lua");
    private final StringRedisTemplate redis;
    private final AuthProperties properties;

    public AuthStore(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void issueCode(String phone, String code) {
        Long result = redis.execute(ISSUE, List.of(codeKey(phone)), code,
                Long.toString(properties.codeTtlSeconds()), Long.toString(properties.resendSeconds()));
        if (Long.valueOf(0).equals(result)) {
            throw new UserBusinessException("AUTH_CODE_TOO_FREQUENT", "验证码发送过于频繁，请稍后重试");
        }
        requireScriptResult(result);
    }

    public void consumeCode(String phone, String code) {
        Long result = redis.execute(CONSUME, List.of(codeKey(phone)), code,
                Integer.toString(properties.maxAttempts()));
        if (Long.valueOf(0).equals(result)) {
            throw new UserBusinessException("AUTH_CODE_INVALID", "验证码错误、已过期或已使用，请重新获取");
        }
        requireScriptResult(result);
    }

    public String createSession(long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // SET NX 与 TTL 一次完成；Redis Key 只保存令牌摘要，不保存可直接使用的原始 Token。
        Boolean saved = redis.opsForValue().setIfAbsent(sessionKey(token), Long.toString(userId),
                Duration.ofSeconds(properties.sessionTtlSeconds()));
        if (!Boolean.TRUE.equals(saved)) { throw new IllegalStateException("Session creation failed"); }
        return token;
    }

    public long requireUserId(String token) {
        validateToken(token);
        String value = redis.opsForValue().get(sessionKey(token));
        if (value == null) { throw unauthenticated(); }
        long id = Long.parseLong(value);
        if (id <= 0) { throw new IllegalStateException("Invalid session data"); }
        return id;
    }

    public void deleteSession(String token) {
        validateToken(token);
        // 重复退出或已过期均成功；只删除当前 Token，保留其他设备的会话。
        redis.delete(sessionKey(token));
    }

    private String codeKey(String phone) { return properties.keyPrefix() + "code:" + phone; }
    private String sessionKey(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return properties.keyPrefix() + "session:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private void validateToken(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) { throw unauthenticated(); }
    }
    private void requireScriptResult(Long result) {
        if (!Long.valueOf(1).equals(result)) { throw new IllegalStateException("Invalid authentication script result"); }
    }
    private static DefaultRedisScript<Long> script(String file) {
        var script = new DefaultRedisScript<Long>();
        script.setLocation(new ClassPathResource("redis/" + file));
        script.setResultType(Long.class);
        return script;
    }
    public static UserBusinessException unauthenticated() {
        return new UserBusinessException("AUTH_UNAUTHENTICATED", "请登录或重新登录");
    }
}
