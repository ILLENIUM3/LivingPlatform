package com.liveplatform.user.provider.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public long insert(String nickname, String avatarUrl, LocalDateTime now) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO t_user (nickname, avatar_url, status, version, created_at, updated_at)
                    VALUES (?, ?, 1, 0, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, nickname);
            statement.setString(2, avatarUrl);
            statement.setObject(3, now);
            statement.setObject(4, now);
            return statement;
        }, key);
        if (key.getKey() == null) { throw new IllegalStateException("Missing generated user ID"); }
        return key.getKey().longValue();
    }

    public Optional<UserRow> findById(long id) {
        return jdbc.query("""
                SELECT id, nickname, avatar_url, status, version, created_at, updated_at
                FROM t_user WHERE id = ?
                """, (rs, row) -> new UserRow(rs.getLong("id"), rs.getString("nickname"),
                rs.getString("avatar_url"), rs.getInt("status"), rs.getLong("version"),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class)), id).stream().findFirst();
    }

    public int update(long id, String nickname, String avatarUrl, long version, LocalDateTime now) {
        // 版本比较与更新在同一条 SQL 中，避免先读后写覆盖并发修改。
        return jdbc.update("""
                UPDATE t_user SET nickname = ?, avatar_url = ?, version = version + 1, updated_at = ?
                WHERE id = ? AND version = ? AND status = 1
                """, nickname, avatarUrl, now, id, version);
    }
    //返回受影响的行数
}
