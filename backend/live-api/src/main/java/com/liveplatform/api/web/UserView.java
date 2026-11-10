package com.liveplatform.api.web;

import com.liveplatform.user.api.UserDTO;

/** 对外 ID 使用字符串，手机号与会话密钥不属于公开用户资料。 */
public record UserView(String id, String nickname, String avatarUrl, int status, long version,
                       String createdAt, String updatedAt) {
    public static UserView from(UserDTO dto) {
        return new UserView(dto.getId().toString(), dto.getNickname(), dto.getAvatarUrl(),
                dto.getStatus(), dto.getVersion(), dto.getCreatedAt(), dto.getUpdatedAt());
    }
}
