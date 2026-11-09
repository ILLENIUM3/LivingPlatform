-- 在 live_platform 库执行；已有数据卷需手动执行本文件。
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID，首版单库自增',
    nickname VARCHAR(32) NOT NULL,
    avatar_url VARCHAR(512) NOT NULL DEFAULT '',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常，0禁用',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME(3) NOT NULL COMMENT 'UTC时间',
    updated_at DATETIME(3) NOT NULL COMMENT 'UTC时间',
    PRIMARY KEY (id),
    CONSTRAINT chk_user_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_user_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
