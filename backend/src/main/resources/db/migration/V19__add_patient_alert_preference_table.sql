CREATE TABLE IF NOT EXISTS patient_alert_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    indicator_type VARCHAR(20) NOT NULL,
    high_rule VARCHAR(64),
    medium_rule VARCHAR(64),
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1启用个性化阈值 0关闭个性化阈值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_indicator (user_id, indicator_type),
    INDEX idx_user_enabled (user_id, enabled)
);
