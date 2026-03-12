CREATE DATABASE IF NOT EXISTS health_system DEFAULT CHARACTER SET utf8mb4;
USE health_system;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    name VARCHAR(64) NOT NULL,
    role_type VARCHAR(20) NOT NULL COMMENT 'PATIENT/DOCTOR/ADMIN',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(20) NOT NULL UNIQUE,
    permission VARCHAR(255) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    perm_name VARCHAR(64) NOT NULL,
    perm_code VARCHAR(64) NOT NULL UNIQUE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
);

CREATE TABLE IF NOT EXISTS patient_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    age INT,
    medical_history TEXT,
    medication_history TEXT,
    allergy_history TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS health_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    indicator_type VARCHAR(20) NOT NULL COMMENT '血压/血糖/体重/服药',
    value VARCHAR(64) NOT NULL,
    report_time DATETIME NOT NULL,
    remark VARCHAR(255),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_time (user_id, report_time)
);

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

CREATE TABLE IF NOT EXISTS feedback_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_user_id BIGINT NOT NULL,
    sender_username VARCHAR(64) NOT NULL,
    sender_role_type VARCHAR(20) NOT NULL,
    content VARCHAR(500) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理',
    reply_content VARCHAR(500) NULL,
    replied_time DATETIME NULL,
    reply_read TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    reply_read_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_feedback_sender_time (sender_user_id, create_time),
    INDEX idx_feedback_status_time (status, create_time)
);

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

INSERT INTO sys_permission (perm_name, perm_code) VALUES
('管理员用户管理', 'admin:user:manage'),
('医生首页访问', 'doctor:home:view'),
('患者首页访问', 'patient:home:view');

INSERT INTO sys_role (role_name, permission) VALUES
('ADMIN', 'admin:user:manage'),
('DOCTOR', 'doctor:home:view'),
('PATIENT', 'patient:home:view');

INSERT INTO alert_rule (indicator_type, high_rule, medium_rule, enabled) VALUES
('血压', '180/120', '140/90', 1),
('血糖', '16.7', '11.1', 1),
('体重', '200', '', 1);

-- 预置管理员账号：admin / 123456
INSERT INTO sys_user (username, password, phone, name, role_type, status)
VALUES ('admin', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13800000000', '系统管理员', 'ADMIN', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username='admin' AND r.role_name='ADMIN';
