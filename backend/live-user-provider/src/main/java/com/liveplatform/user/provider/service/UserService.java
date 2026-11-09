package com.liveplatform.user.provider.service;

import com.liveplatform.user.api.UserBusinessException;
import com.liveplatform.user.api.UserDTO;
import com.liveplatform.user.provider.dao.UserRepository;
import com.liveplatform.user.provider.dao.UserRow;
import com.liveplatform.user.provider.cache.UserCache;
import com.liveplatform.user.provider.cache.UserChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class UserService {
    private final UserRepository repository;
    private final UserCache cache;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository repository, UserCache cache, ApplicationEventPublisher events) {
        this.repository = repository;
        this.cache = cache;
        this.events = events;
    }

    @Transactional
    public UserDTO create(String nickname, String avatarUrl) {
        long id = repository.insert(nickname(nickname), avatar(avatarUrl), LocalDateTime.now(ZoneOffset.UTC));
        // 事务内直接查库，避免把尚未提交的用户写入缓存.
        //这里创建也要加@Transactional注解,让事务监听,因为曾经可能查询过某个不存在的id,创建成功后删除该 Key，可避免继续返回“用户不存在”
        UserDTO user = dto(requireUser(id));
        events.publishEvent(new UserChangedEvent(id));
        return user;
    }

    public UserDTO getById(Long id) {
        validateId(id);
        // 事务内读取不走缓存，也不回填事务快照中的数据。
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            return dto(requireUser(id));
        }
        UserCache.Lookup lookup = cache.get(id);
        if (lookup.hit()) {
            if (lookup.user() == null) { throw notFound(); }
            return lookup.user();
        }
        UserDTO user = repository.findById(id).map(this::dto).orElse(null);
        cache.put(id, user);
        if (user == null) { throw notFound(); }
        return user;
    }

    @Transactional
    public UserDTO updateProfile(Long id, String nickname, String avatarUrl, Long version) {
        validateId(id);
        if (version == null || version < 0 || version == Long.MAX_VALUE) {
            throw invalid("version 必须为非负且可递增的整数");
        }

        String name = nickname(nickname);
        String avatar = avatar(avatarUrl);
        if (repository.update(id, name, avatar, version, LocalDateTime.now(ZoneOffset.UTC)) == 0) {
            UserRow existing = requireUser(id);
            if (existing.status() != 1) {
                throw new UserBusinessException("USER_DISABLED", "用户已禁用");
            }
            throw new UserBusinessException("USER_VERSION_CONFLICT", "资料已变更，请重新查询后再提交");
        }
        UserDTO updated = dto(requireUser(id));
        events.publishEvent(new UserChangedEvent(id));
        return updated;
    }

    private UserRow requireUser(long id) {
        return repository.findById(id).orElseThrow(this::notFound);
    }

    private UserBusinessException notFound() {
        return new UserBusinessException("USER_NOT_FOUND", "用户不存在");
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) { throw invalid("用户 ID 必须为正整数"); }
    }

    private String nickname(String value) {
        if (value == null || value.isBlank()) { throw invalid("昵称不能为空"); }
        String result = value.strip();
        if (result.codePointCount(0, result.length()) > 32 || result.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid("昵称最多 32 个字符，不能包含控制字符");
        }
        return result;
    }

    private String avatar(String value) {
        if (value == null || value.isBlank()) { return ""; }
        String result = value.strip();
        if (result.length() > 512) { throw invalid("头像地址不能超过 512 个字符"); }
        try {
            URI uri = URI.create(result);
            if ((!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw invalid("头像必须是有效的 HTTP/HTTPS 地址");
            }
        } catch (IllegalArgumentException exception) {
            throw invalid("头像地址格式错误");
        }
        return result;
    }

    private UserBusinessException invalid(String message) {
        return new UserBusinessException("INVALID_ARGUMENT", message);
    }

    private UserDTO dto(UserRow row) {
        var dto = new UserDTO();
        dto.setId(row.id());
        dto.setNickname(row.nickname());
        dto.setAvatarUrl(row.avatarUrl());
        dto.setStatus(row.status());
        dto.setVersion(row.version());
        dto.setCreatedAt(row.createdAt().toInstant(ZoneOffset.UTC).toString());
        dto.setUpdatedAt(row.updatedAt().toInstant(ZoneOffset.UTC).toString());
        return dto;
    }
}
