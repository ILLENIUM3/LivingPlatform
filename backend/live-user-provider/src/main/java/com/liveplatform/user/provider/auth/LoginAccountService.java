package com.liveplatform.user.provider.auth;

import com.liveplatform.user.api.UserBusinessException;
import com.liveplatform.user.api.UserDTO;
import com.liveplatform.user.provider.dao.UserPhoneRepository;
import com.liveplatform.user.provider.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** 只管理数据库事务；Redis 验证码与 Token 不放进数据库事务中。 */
@Service
public class LoginAccountService {
    private final UserPhoneRepository phones;
    private final UserService users;
    private final TransactionTemplate transaction;

    public LoginAccountService(UserPhoneRepository phones, UserService users, PlatformTransactionManager manager) {
        this.phones = phones;
        this.users = users;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public UserDTO findOrRegister(String phone) {
        try {
            return transaction.execute(status -> phones.findUserId(phone).map(this::activeUser).orElseGet(() -> {
                UserDTO user = users.create("用户" + phone.substring(7), "");
                phones.insert(phone, user.getId(), LocalDateTime.now(ZoneOffset.UTC));
                return user;
            }));
        } catch (DuplicateKeyException conflict) {
            // 冲突事务已整体回滚，包含刚插入的用户；新事务读取胜出者，避免复用旧快照。
            return transaction.execute(status -> activeUser(phones.findUserId(phone).orElseThrow(() -> conflict)));
        }
    }

    public UserDTO getActiveUser(long userId) {
        // 身份状态必须直接读数据库；UserService 在事务中会绕过可陈旧的资料缓存。
        return transaction.execute(status -> activeUser(userId));
    }

    private UserDTO activeUser(long userId) {
        UserDTO user = users.getById(userId);
        if (user.getStatus() != 1) { throw new UserBusinessException("USER_DISABLED", "用户已禁用"); }
        return user;
    }
}
