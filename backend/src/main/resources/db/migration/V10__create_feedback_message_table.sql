CREATE TABLE IF NOT EXISTS feedback_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_user_id BIGINT NOT NULL,
    sender_username VARCHAR(64) NOT NULL,
    sender_role_type VARCHAR(20) NOT NULL,
    content VARCHAR(500) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_feedback_sender_time (sender_user_id, create_time),
    INDEX idx_feedback_status_time (status, create_time)
);
