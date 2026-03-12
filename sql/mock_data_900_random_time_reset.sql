USE health_system;

SET NAMES utf8mb4;

-- Reset existing mock data first so this script is repeatable.
DELETE a
FROM health_alert a
JOIN sys_user u ON a.user_id = u.id
WHERE u.username LIKE 'mock_%';

DELETE h
FROM health_data h
JOIN sys_user u ON h.user_id = u.id
WHERE u.username LIKE 'mock_%';

DELETE f
FROM feedback_message f
JOIN sys_user u ON f.sender_user_id = u.id
WHERE u.username LIKE 'mock_%';

DELETE gm
FROM doctor_group_member gm
JOIN sys_user u ON gm.patient_user_id = u.id
WHERE u.username LIKE 'mock_%';

DELETE g
FROM doctor_group g
JOIN sys_user u ON g.doctor_id = u.id
WHERE u.username LIKE 'mock_%';

DELETE pa
FROM patient_archive pa
JOIN sys_user u ON pa.user_id = u.id
WHERE u.username LIKE 'mock_%';

DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON ur.user_id = u.id
WHERE u.username LIKE 'mock_%';

DELETE FROM operation_log WHERE username LIKE 'mock_%';
DELETE FROM system_notice WHERE title LIKE '模拟公告-%';
DELETE FROM sys_user WHERE username LIKE 'mock_%';

-- Ensure core roles exist for user-role mapping.
INSERT IGNORE INTO sys_role (role_name, permission)
VALUES
('ADMIN', 'admin:user:manage'),
('DOCTOR', 'doctor:home:view'),
('PATIENT', 'patient:home:view');

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY);
INSERT INTO tmp_seq (n)
SELECT a.d * 100 + b.d * 10 + c.d + 1
FROM (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a
JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b
JOIN (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c;

-- 1) Create 30 mock patients.
INSERT IGNORE INTO sys_user (username, password, phone, name, role_type, status, login_version)
SELECT
    CONCAT('mock_patient_', LPAD(s.n, 3, '0')),
    '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK',
    CONCAT('139', LPAD(FLOOR(RAND(s.n * 17) * 100000000), 8, '0')),
    CONCAT('模拟患者', s.n),
    'PATIENT',
    1,
    0
FROM tmp_seq s
WHERE s.n <= 30;

-- 2) Create 6 mock doctors.
INSERT IGNORE INTO sys_user (username, password, phone, name, role_type, status, login_version)
SELECT
    CONCAT('mock_doctor_', LPAD(s.n, 3, '0')),
    '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK',
    CONCAT('137', LPAD(FLOOR(RAND(s.n * 31) * 100000000), 8, '0')),
    CONCAT('模拟医生', s.n),
    'DOCTOR',
    1,
    0
FROM tmp_seq s
WHERE s.n <= 6;

-- 3) Build user-role relations for mock accounts.
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_name = 'PATIENT'
WHERE u.username LIKE 'mock_patient_%';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_name = 'DOCTOR'
WHERE u.username LIKE 'mock_doctor_%';

-- 4) Upsert patient archives.
INSERT INTO patient_archive (user_id, name, age, medical_history, medication_history, allergy_history)
SELECT
    u.id,
    u.name,
    20 + FLOOR(RAND(u.id * 3) * 60),
    ELT(1 + FLOOR(RAND(u.id * 5) * 4), '高血压', '糖尿病', '冠心病', '无明显慢性病史'),
    ELT(1 + FLOOR(RAND(u.id * 7) * 4), '二甲双胍', '缬沙坦', '阿托伐他汀', '无固定用药'),
    ELT(1 + FLOOR(RAND(u.id * 11) * 4), '青霉素过敏', '海鲜过敏', '花粉过敏', '无过敏史')
FROM sys_user u
WHERE u.username LIKE 'mock_patient_%'
  AND u.deleted = 0
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    age = VALUES(age),
    medical_history = VALUES(medical_history),
    medication_history = VALUES(medication_history),
    allergy_history = VALUES(allergy_history),
    update_time = CURRENT_TIMESTAMP;

-- 5) Create doctor groups.
INSERT IGNORE INTO doctor_group (doctor_id, group_name, description)
SELECT
    u.id,
    CONCAT('模拟分组-', RIGHT(u.username, 3)),
    '批量模拟数据分组'
FROM sys_user u
WHERE u.username LIKE 'mock_doctor_%'
  AND u.deleted = 0;

-- 6) Add each mock patient into one doctor group.
INSERT IGNORE INTO doctor_group_member (group_id, patient_user_id)
SELECT
    g.id,
    p.id
FROM sys_user p
JOIN doctor_group g ON g.group_name = CONCAT('模拟分组-', RIGHT(CONCAT('000', MOD(CAST(RIGHT(p.username, 3) AS UNSIGNED) - 1, 6) + 1), 3))
WHERE p.username LIKE 'mock_patient_%'
  AND p.deleted = 0
  AND g.deleted = 0;

-- 7) Insert 600 health_data rows with random indicator values and random times in last 180 days.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT
    p.id AS user_id,
    ELT(MOD(s.n, 4) + 1, '血压', '血糖', '体重', '服药') AS indicator_type,
    CASE
        WHEN MOD(s.n, 4) = 1 THEN CONCAT(105 + FLOOR(RAND(s.n * 13) * 95), '/', 65 + FLOOR(RAND(s.n * 19) * 60))
        WHEN MOD(s.n, 4) = 2 THEN CAST(ROUND(4 + RAND(s.n * 23) * 14, 1) AS CHAR)
        WHEN MOD(s.n, 4) = 3 THEN CAST(ROUND(45 + RAND(s.n * 29) * 60, 1) AS CHAR)
        ELSE ELT(1 + FLOOR(RAND(s.n * 37) * 2), '已服药', '未服药')
    END AS value,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(s.n * 41) * 180 * 24 * 3600) SECOND) AS report_time,
    '模拟批量生成' AS remark
FROM tmp_seq s
JOIN sys_user p ON p.username = CONCAT('mock_patient_', LPAD(MOD(s.n - 1, 30) + 1, 3, '0'))
WHERE s.n <= 600;

-- 8) Insert 120 health_alert rows based on random mock health_data rows.
INSERT INTO health_alert (
    user_id, health_data_id, indicator_type, value,
    level, reason_code, reason_text, status, source,
    handled_by, handle_remark, handled_time, create_time
)
SELECT
    t.user_id,
    t.id AS health_data_id,
    t.indicator_type,
    t.value,
    ELT(1 + FLOOR(RAND(t.id * 3) * 2), 'HIGH', 'MEDIUM') AS level,
    ELT(1 + FLOOR(RAND(t.id * 5) * 2), 'THRESHOLD_HIGH', 'THRESHOLD_MEDIUM') AS reason_code,
    CONCAT(t.indicator_type, '指标超阈值') AS reason_text,
    ELT(1 + FLOOR(RAND(t.id * 7) * 2), 'OPEN', 'CLOSED') AS status,
    'SYSTEM' AS source,
    NULL AS handled_by,
    NULL AS handle_remark,
    NULL AS handled_time,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(t.id * 11) * 180 * 24 * 3600) SECOND) AS create_time
FROM (
    SELECT h.id, h.user_id, h.indicator_type, h.value
    FROM health_data h
    JOIN sys_user u ON u.id = h.user_id
    WHERE u.username LIKE 'mock_patient_%' AND h.deleted = 0
    ORDER BY RAND()
    LIMIT 120
) t;

-- 9) Insert 40 feedback messages.
INSERT INTO feedback_message (
    sender_user_id,
    sender_username,
    sender_role_type,
    content,
    status,
    reply_content,
    replied_time,
    reply_read,
    reply_read_time,
    create_time
)
SELECT
    p.id,
    p.username,
    'PATIENT',
    CONCAT('模拟反馈#', LPAD(s.n, 3, '0'), '：系统使用建议与问题描述'),
    IF(RAND(s.n * 13) > 0.4, 1, 0),
    IF(RAND(s.n * 17) > 0.4, '感谢反馈，已处理。', NULL),
    IF(RAND(s.n * 19) > 0.4, DATE_SUB(NOW(), INTERVAL FLOOR(RAND(s.n * 23) * 30 * 24 * 3600) SECOND), NULL),
    IF(RAND(s.n * 29) > 0.5, 1, 0),
    IF(RAND(s.n * 31) > 0.5, DATE_SUB(NOW(), INTERVAL FLOOR(RAND(s.n * 37) * 15 * 24 * 3600) SECOND), NULL),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(s.n * 41) * 120 * 24 * 3600) SECOND)
FROM tmp_seq s
JOIN sys_user p ON p.username = CONCAT('mock_patient_', LPAD(MOD(s.n - 1, 30) + 1, 3, '0'))
WHERE s.n <= 40;

-- 10) Insert 10 system notices.
INSERT INTO system_notice (title, content, status, create_time)
SELECT
    CONCAT('模拟公告-', LPAD(s.n, 2, '0')),
    CONCAT('这是系统自动生成的模拟公告内容，序号 ', s.n, '。'),
    IF(RAND(s.n * 7) > 0.2, 1, 0),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(s.n * 11) * 90 * 24 * 3600) SECOND)
FROM tmp_seq s
WHERE s.n <= 10;

-- 11) Insert 40 operation logs.
INSERT INTO operation_log (
    username,
    role_type,
    request_method,
    request_uri,
    success,
    message,
    create_time
)
SELECT
    CASE WHEN MOD(s.n, 2) = 0
        THEN CONCAT('mock_doctor_', LPAD(MOD(s.n - 1, 6) + 1, 3, '0'))
        ELSE CONCAT('mock_patient_', LPAD(MOD(s.n - 1, 30) + 1, 3, '0'))
    END AS username,
    CASE WHEN MOD(s.n, 2) = 0 THEN 'DOCTOR' ELSE 'PATIENT' END AS role_type,
    ELT(1 + FLOOR(RAND(s.n * 3) * 4), 'GET', 'POST', 'PUT', 'DELETE'),
    CASE
        WHEN MOD(s.n, 2) = 0 THEN ELT(1 + FLOOR(RAND(s.n * 5) * 3), '/api/doctor/groups', '/api/doctor/alerts', '/api/doctor/patients')
        ELSE ELT(1 + FLOOR(RAND(s.n * 7) * 3), '/api/patient/data', '/api/patient/archive', '/api/patient/alerts')
    END,
    IF(RAND(s.n * 11) > 0.15, 1, 0),
    ELT(1 + FLOOR(RAND(s.n * 13) * 3), '操作成功', '业务校验失败', '参数错误'),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND(s.n * 17) * 60 * 24 * 3600) SECOND)
FROM tmp_seq s
WHERE s.n <= 40;

DROP TEMPORARY TABLE IF EXISTS tmp_seq;

-- Check row counts of major modules after seeding.
SELECT 'sys_user.mock' AS metric, COUNT(*) AS cnt FROM sys_user WHERE username LIKE 'mock_%' AND deleted = 0
UNION ALL
SELECT 'patient_archive.mock', COUNT(*) FROM patient_archive pa JOIN sys_user u ON pa.user_id = u.id WHERE u.username LIKE 'mock_patient_%' AND pa.deleted = 0
UNION ALL
SELECT 'doctor_group.mock', COUNT(*) FROM doctor_group WHERE group_name LIKE '模拟分组-%' AND deleted = 0
UNION ALL
SELECT 'doctor_group_member.mock', COUNT(*) FROM doctor_group_member gm JOIN sys_user u ON gm.patient_user_id = u.id WHERE u.username LIKE 'mock_patient_%' AND gm.deleted = 0
UNION ALL
SELECT 'health_data.mock', COUNT(*) FROM health_data h JOIN sys_user u ON h.user_id = u.id WHERE u.username LIKE 'mock_patient_%' AND h.deleted = 0
UNION ALL
SELECT 'health_alert.mock', COUNT(*) FROM health_alert a JOIN sys_user u ON a.user_id = u.id WHERE u.username LIKE 'mock_patient_%' AND a.deleted = 0
UNION ALL
SELECT 'feedback_message.mock', COUNT(*) FROM feedback_message f JOIN sys_user u ON f.sender_user_id = u.id WHERE u.username LIKE 'mock_patient_%' AND f.deleted = 0
UNION ALL
SELECT 'operation_log.mock', COUNT(*) FROM operation_log WHERE username LIKE 'mock_%' AND deleted = 0
UNION ALL
SELECT 'system_notice.mock', COUNT(*) FROM system_notice WHERE title LIKE '模拟公告-%' AND deleted = 0;
