CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    indicator_type VARCHAR(20) NOT NULL,
    high_rule VARCHAR(64) NOT NULL,
    medium_rule VARCHAR(64),
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_indicator_type (indicator_type)
);

INSERT IGNORE INTO alert_rule (indicator_type, high_rule, medium_rule, enabled) VALUES
('血压', '180/120', '140/90', 1),
('血糖', '16.7', '11.1', 1),
('体重', '200', '', 1);
