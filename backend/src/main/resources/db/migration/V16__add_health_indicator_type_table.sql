CREATE TABLE IF NOT EXISTS health_indicator_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    indicator_type VARCHAR(64) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_indicator_type (indicator_type)
);

INSERT INTO health_indicator_type (indicator_type, display_name, enabled)
VALUES
    ('血压', '血压', 1),
    ('血糖', '血糖', 1),
    ('体重', '体重', 1),
    ('服药', '服药', 1)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    enabled = VALUES(enabled),
    update_time = CURRENT_TIMESTAMP;
