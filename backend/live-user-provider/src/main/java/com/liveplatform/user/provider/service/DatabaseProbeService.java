package com.liveplatform.user.provider.service;

import com.liveplatform.user.api.UserProbeResult;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class DatabaseProbeService {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseProbeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserProbeResult probe() {
        // 每次请求都实际读取数据库；启动成功和数据库可用是两个不同的检查。
        return jdbcTemplate.execute((ConnectionCallback<UserProbeResult>) connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT probe_value FROM platform_bootstrap WHERE id = ?")) {
                statement.setInt(1, 1);
                statement.setQueryTimeout(2);
                try (var result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("Missing bootstrap row; apply sql/001-bootstrap.sql");
                    }
                    var metadata = connection.getMetaData();
                    return new UserProbeResult("live-user-provider", connection.getCatalog(),
                            metadata.getDatabaseProductName(), metadata.getDatabaseProductVersion(),
                            result.getString("probe_value"));
                }
            }
        });
    }
}
