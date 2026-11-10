-- 在 002-user.sql 之后执行；旧调试用户保留，不自动绑定手机号。
CREATE TABLE IF NOT EXISTS t_user_phone (
    phone VARCHAR(11) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '本阶段仅支持大陆11位手机号',
    user_id BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL COMMENT 'UTC时间',
    PRIMARY KEY (phone),
    UNIQUE KEY uk_user_phone_user (user_id),
    CONSTRAINT fk_user_phone_user FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
