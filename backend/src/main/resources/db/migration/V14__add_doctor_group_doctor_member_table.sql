CREATE TABLE IF NOT EXISTS doctor_group_doctor_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    doctor_user_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_group_doctor (group_id, doctor_user_id),
    INDEX idx_group_id (group_id),
    INDEX idx_doctor_user_id (doctor_user_id)
);
