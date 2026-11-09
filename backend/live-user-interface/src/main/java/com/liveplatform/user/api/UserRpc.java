package com.liveplatform.user.api;

public interface UserRpc {
    UserDTO create(String nickname, String avatarUrl) throws UserBusinessException;
    UserDTO getById(Long userId) throws UserBusinessException;
    UserDTO updateProfile(Long userId, String nickname, String avatarUrl, Long expectedVersion)
            throws UserBusinessException;
}
