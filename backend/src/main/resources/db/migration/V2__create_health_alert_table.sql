CREATE TABLE IF NOT EXISTS health_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    health_data_id BIGINT NOT NULL,
    indicator_type VARCHAR(20) NOT NULL,
    value VARCHAR(64) NOT NULL,
    level VARCHAR(10) NOT NULL COMMENT 'HIGH/MEDIUM',
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
    handled_by BIGINT,
    handle_remark VARCHAR(255),
    handled_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_alert_status_time (status, create_time),
    INDEX idx_alert_health_data (health_data_id)
);
