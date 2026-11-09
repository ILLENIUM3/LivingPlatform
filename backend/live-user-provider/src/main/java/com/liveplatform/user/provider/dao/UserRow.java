package com.liveplatform.user.provider.dao;

import java.time.LocalDateTime;

public record UserRow(long id, String nickname, String avatarUrl, int status, long version,
                      LocalDateTime createdAt, LocalDateTime updatedAt) { }
