package com.liveplatform.user.provider.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserPhoneRepository {
    private final JdbcTemplate jdbc;
    public UserPhoneRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Long> findUserId(String phone) {
        return jdbc.query("SELECT user_id FROM t_user_phone WHERE phone = ?",
                (rs, row) -> rs.getLong("user_id"), phone).stream().findFirst();
    }
    public void insert(String phone, long userId, LocalDateTime now) {
        jdbc.update("INSERT INTO t_user_phone (phone, user_id, created_at) VALUES (?, ?, ?)", phone, userId, now);
    }
}
