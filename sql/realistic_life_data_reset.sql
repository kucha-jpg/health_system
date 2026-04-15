USE health_system;
SET NAMES utf8mb4;

START TRANSACTION;

-- 1) Clean transactional data first.
DELETE FROM feedback_conversation_message;
DELETE FROM health_alert;
DELETE FROM health_data;
DELETE FROM feedback_message;
DELETE FROM operation_log;
DELETE FROM system_notice;
DELETE FROM doctor_group_doctor_member;
DELETE FROM doctor_group_member;
DELETE FROM doctor_group;
DELETE FROM patient_alert_preference;
DELETE FROM patient_archive;

-- 2) Remove old non-admin users and user-role mappings.
DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON ur.user_id = u.id
WHERE u.role_type IN ('PATIENT', 'DOCTOR');

DELETE FROM sys_user WHERE role_type IN ('PATIENT', 'DOCTOR');

-- 3) Ensure base roles exist.
INSERT IGNORE INTO sys_role (role_name, permission)
VALUES
('ADMIN', 'admin:user:manage'),
('DOCTOR', 'doctor:home:view'),
('PATIENT', 'patient:home:view');

-- 4) Ensure baseline indicator rules exist.
INSERT INTO alert_rule (indicator_type, high_rule, medium_rule, enabled)
VALUES
('血压', '180/120', '140/90', 1),
('血糖', '16.7', '11.1', 1),
('体重', '95', '85', 1),
('服药', '未服药连续3天', '未服药', 1)
ON DUPLICATE KEY UPDATE
high_rule = VALUES(high_rule),
medium_rule = VALUES(medium_rule),
enabled = VALUES(enabled),
update_time = CURRENT_TIMESTAMP;

-- 5) Insert realistic doctors.
INSERT INTO sys_user (username, password, phone, name, role_type, status, login_version)
VALUES
('doctor_chen_yu', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13770001001', '陈宇', 'DOCTOR', 1, 0),
('doctor_li_xin', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13770001002', '李欣', 'DOCTOR', 1, 0),
('doctor_wang_lei', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13770001003', '王磊', 'DOCTOR', 1, 0),
('doctor_zhou_min', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13770001004', '周敏', 'DOCTOR', 1, 0),
('doctor_sun_qi', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13770001005', '孙琪', 'DOCTOR', 1, 0),
('doctor_he_lan', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13770001006', '何岚', 'DOCTOR', 1, 0);

-- 6) Insert realistic patients.
INSERT INTO sys_user (username, password, phone, name, role_type, status, login_version)
VALUES
('patient_liu_fang', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002001', '刘芳', 'PATIENT', 1, 0),
('patient_zhang_jie', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002002', '张杰', 'PATIENT', 1, 0),
('patient_li_na', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002003', '李娜', 'PATIENT', 1, 0),
('patient_wu_tao', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002004', '吴涛', 'PATIENT', 1, 0),
('patient_zhao_yan', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002005', '赵妍', 'PATIENT', 1, 0),
('patient_qian_ming', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002006', '钱明', 'PATIENT', 1, 0),
('patient_zhou_hui', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002007', '周慧', 'PATIENT', 1, 0),
('patient_xu_qiang', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002008', '徐强', 'PATIENT', 1, 0),
('patient_he_jing', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002009', '何静', 'PATIENT', 1, 0),
('patient_peng_lei', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002010', '彭磊', 'PATIENT', 1, 0),
('patient_deng_yu', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002011', '邓瑜', 'PATIENT', 1, 0),
('patient_tan_fei', '$2a$10$Dow1mbN8M1wlLNLh8cL4..8hoM9xw6/tj0jQ2zXQ1f6n66MEOj2fK', '13970002012', '谭飞', 'PATIENT', 1, 0);

-- 7) Build user-role relations.
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_name = u.role_type
WHERE u.role_type IN ('PATIENT', 'DOCTOR');

-- 8) Build patient archives (realistic but de-identified).
INSERT INTO patient_archive (user_id, name, age, medical_history, medication_history, allergy_history)
SELECT
u.id,
u.name,
CASE u.username
  WHEN 'patient_liu_fang' THEN 68
  WHEN 'patient_zhang_jie' THEN 57
  WHEN 'patient_li_na' THEN 45
  WHEN 'patient_wu_tao' THEN 62
  WHEN 'patient_zhao_yan' THEN 39
  WHEN 'patient_qian_ming' THEN 71
  WHEN 'patient_zhou_hui' THEN 53
  WHEN 'patient_xu_qiang' THEN 48
  WHEN 'patient_he_jing' THEN 66
  WHEN 'patient_peng_lei' THEN 60
  WHEN 'patient_deng_yu' THEN 52
  ELSE 44
END,
CASE u.username
  WHEN 'patient_liu_fang' THEN '2型糖尿病10年，轻度高血压'
  WHEN 'patient_zhang_jie' THEN '高血压5年，血脂偏高'
  WHEN 'patient_li_na' THEN '甲状腺功能减退，体重管理中'
  WHEN 'patient_wu_tao' THEN '冠心病术后随访'
  WHEN 'patient_zhao_yan' THEN '妊娠糖耐量异常病史'
  WHEN 'patient_qian_ming' THEN '慢性肾病2期，高血压'
  WHEN 'patient_zhou_hui' THEN '糖尿病前期'
  WHEN 'patient_xu_qiang' THEN '肥胖相关代谢综合征'
  WHEN 'patient_he_jing' THEN '高血压合并睡眠障碍'
  WHEN 'patient_peng_lei' THEN '心律不齐病史'
  WHEN 'patient_deng_yu' THEN '脂肪肝，体重偏高'
  ELSE '产后体重管理'
END,
CASE u.username
  WHEN 'patient_liu_fang' THEN '二甲双胍,缬沙坦'
  WHEN 'patient_zhang_jie' THEN '氨氯地平,阿托伐他汀'
  WHEN 'patient_li_na' THEN '左甲状腺素钠'
  WHEN 'patient_wu_tao' THEN '阿司匹林,比索洛尔'
  WHEN 'patient_zhao_yan' THEN '营养干预为主'
  WHEN 'patient_qian_ming' THEN '厄贝沙坦,钙片'
  WHEN 'patient_zhou_hui' THEN '无固定用药'
  WHEN 'patient_xu_qiang' THEN '无固定用药'
  WHEN 'patient_he_jing' THEN '缬沙坦'
  WHEN 'patient_peng_lei' THEN '美托洛尔'
  WHEN 'patient_deng_yu' THEN '保肝药按需'
  ELSE '维生素D'
END,
CASE u.username
  WHEN 'patient_liu_fang' THEN '青霉素过敏'
  WHEN 'patient_zhang_jie' THEN '无'
  WHEN 'patient_li_na' THEN '海鲜轻度过敏'
  WHEN 'patient_wu_tao' THEN '无'
  WHEN 'patient_zhao_yan' THEN '花粉过敏'
  WHEN 'patient_qian_ming' THEN '无'
  WHEN 'patient_zhou_hui' THEN '无'
  WHEN 'patient_xu_qiang' THEN '无'
  WHEN 'patient_he_jing' THEN '无'
  WHEN 'patient_peng_lei' THEN '无'
  WHEN 'patient_deng_yu' THEN '青霉素过敏'
  ELSE '无'
END
FROM sys_user u
WHERE u.role_type = 'PATIENT';

-- 9) Build doctor groups.
INSERT INTO doctor_group (doctor_id, group_name, description)
SELECT u.id,
CASE u.username
  WHEN 'doctor_chen_yu' THEN '高血压连续管理组'
  WHEN 'doctor_li_xin' THEN '糖代谢干预组'
  WHEN 'doctor_wang_lei' THEN '心血管随访组'
  WHEN 'doctor_zhou_min' THEN '体重与营养管理组'
  WHEN 'doctor_sun_qi' THEN '慢病综合管理组'
  ELSE '老年健康管理组'
END,
'按社区随访与风险分层建立的小组'
FROM sys_user u
WHERE u.role_type = 'DOCTOR';

-- 10) Allocate patients into groups by stable hash.
INSERT INTO doctor_group_member (group_id, patient_user_id)
SELECT g.id, p.id
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
  FROM sys_user
  WHERE role_type = 'PATIENT'
) p
JOIN (
  SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn, (SELECT COUNT(*) FROM doctor_group) AS gc
  FROM doctor_group
) g
ON ((p.rn - 1) MOD g.gc) + 1 = g.rn;

-- 11) Create 30-day health reports with realistic ranges.
DROP TEMPORARY TABLE IF EXISTS tmp_days;
CREATE TEMPORARY TABLE tmp_days (n INT PRIMARY KEY);
INSERT INTO tmp_days(n)
VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),
       (10),(11),(12),(13),(14),(15),(16),(17),(18),(19),
       (20),(21),(22),(23),(24),(25),(26),(27),(28),(29);

-- Blood pressure every 2 days.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
'血压',
CONCAT(112 + MOD(p.id + d.n * 3, 36), '/', 70 + MOD(p.id + d.n * 5, 22)),
DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 7 HOUR), INTERVAL d.n DAY),
'晨起血压记录'
FROM sys_user p
JOIN tmp_days d ON MOD(d.n, 2) = 0
WHERE p.role_type = 'PATIENT';

-- Blood glucose every 3 days.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
'血糖',
CAST(ROUND(5.0 + (MOD(p.id + d.n * 2, 55) / 10), 1) AS CHAR),
DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 20 HOUR), INTERVAL d.n DAY),
'晚餐后两小时血糖'
FROM sys_user p
JOIN tmp_days d ON MOD(d.n, 3) = 0
WHERE p.role_type = 'PATIENT';

-- Weight every 4 days.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
'体重',
CAST(ROUND(54 + MOD(p.id + d.n, 38) + (MOD(d.n, 3) * 0.3), 1) AS CHAR),
DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 21 HOUR), INTERVAL d.n DAY),
'晚间称重'
FROM sys_user p
JOIN tmp_days d ON MOD(d.n, 4) = 0
WHERE p.role_type = 'PATIENT';

-- Medication check every day.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
'服药',
CASE WHEN MOD(p.id + d.n, 9) IN (0, 1) THEN '未服药' ELSE '已服药' END,
DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 8 HOUR), INTERVAL d.n DAY),
'每日依从性记录'
FROM sys_user p
JOIN tmp_days d ON d.n <= 20
WHERE p.role_type = 'PATIENT';

-- 12) Generate alerts from abnormal records.
INSERT INTO health_alert (
user_id, health_data_id, indicator_type, value,
level, risk_score, risk_level, reason_code, reason_text,
status, source, handled_by, handle_remark, handled_time, create_time
)
SELECT
h.user_id,
h.id,
h.indicator_type,
h.value,
CASE
  WHEN h.indicator_type = '血压' AND (
      CAST(SUBSTRING_INDEX(h.value, '/', 1) AS UNSIGNED) >= 170 OR
      CAST(SUBSTRING_INDEX(h.value, '/', -1) AS UNSIGNED) >= 105
    ) THEN 'HIGH'
  WHEN h.indicator_type = '血糖' AND CAST(h.value AS DECIMAL(5,1)) >= 14.0 THEN 'HIGH'
  WHEN h.indicator_type = '体重' AND CAST(h.value AS DECIMAL(5,1)) >= 92.0 THEN 'HIGH'
  ELSE 'MEDIUM'
END,
CASE
  WHEN h.indicator_type = '血压' AND CAST(SUBSTRING_INDEX(h.value, '/', 1) AS UNSIGNED) >= 170 THEN 92
  WHEN h.indicator_type = '血糖' AND CAST(h.value AS DECIMAL(5,1)) >= 14.0 THEN 90
  WHEN h.indicator_type = '服药' AND h.value = '未服药' THEN 70
  ELSE 65
END,
CASE
  WHEN h.indicator_type = '血压' AND CAST(SUBSTRING_INDEX(h.value, '/', 1) AS UNSIGNED) >= 170 THEN 'HIGH'
  WHEN h.indicator_type = '血糖' AND CAST(h.value AS DECIMAL(5,1)) >= 14.0 THEN 'HIGH'
  WHEN h.indicator_type = '服药' AND h.value = '未服药' THEN 'MEDIUM'
  ELSE 'MEDIUM'
END,
CASE
  WHEN h.indicator_type = '血压' THEN 'THRESHOLD_HIGH'
  WHEN h.indicator_type = '血糖' THEN 'THRESHOLD_HIGH'
  WHEN h.indicator_type = '体重' THEN 'THRESHOLD_MEDIUM'
  ELSE 'MEDICATION_MISSED'
END,
CASE
  WHEN h.indicator_type = '服药' THEN '服药依从性不足'
  ELSE CONCAT(h.indicator_type, '超出个体阈值')
END,
CASE WHEN h.report_time >= DATE_SUB(NOW(), INTERVAL 4 DAY) THEN 'OPEN' ELSE 'CLOSED' END,
'SYSTEM',
CASE WHEN h.report_time < DATE_SUB(NOW(), INTERVAL 4 DAY)
  THEN (
    SELECT g.doctor_id
    FROM doctor_group_member gm
    JOIN doctor_group g ON g.id = gm.group_id
    WHERE gm.patient_user_id = h.user_id
    ORDER BY gm.id LIMIT 1
  )
  ELSE NULL
END,
CASE WHEN h.report_time < DATE_SUB(NOW(), INTERVAL 4 DAY) THEN '已电话随访并给出生活方式建议' ELSE NULL END,
CASE WHEN h.report_time < DATE_SUB(NOW(), INTERVAL 4 DAY) THEN DATE_ADD(h.report_time, INTERVAL 8 HOUR) ELSE NULL END,
h.report_time
FROM health_data h
WHERE h.deleted = 0
  AND (
    (h.indicator_type = '血压' AND (
      CAST(SUBSTRING_INDEX(h.value, '/', 1) AS UNSIGNED) >= 150 OR
      CAST(SUBSTRING_INDEX(h.value, '/', -1) AS UNSIGNED) >= 95
    ))
    OR (h.indicator_type = '血糖' AND CAST(h.value AS DECIMAL(5,1)) >= 11.1)
    OR (h.indicator_type = '体重' AND CAST(h.value AS DECIMAL(5,1)) >= 85.0)
    OR (h.indicator_type = '服药' AND h.value = '未服药')
  );

-- 13) Personalized threshold preferences.
INSERT INTO patient_alert_preference (user_id, indicator_type, high_rule, medium_rule, enabled)
SELECT p.id, '血压', '165/100', '145/92', 1
FROM sys_user p WHERE p.role_type = 'PATIENT';

INSERT INTO patient_alert_preference (user_id, indicator_type, high_rule, medium_rule, enabled)
SELECT p.id, '血糖', '14.0', '10.5', 1
FROM sys_user p WHERE p.role_type = 'PATIENT';

INSERT INTO patient_alert_preference (user_id, indicator_type, high_rule, medium_rule, enabled)
SELECT p.id, '体重', '92', '85', 1
FROM sys_user p WHERE p.role_type = 'PATIENT';

-- 14) Feedback data in realistic wording.
INSERT INTO feedback_message (sender_user_id, sender_username, sender_role_type, content, status, reply_content, replied_time, reply_read, reply_read_time, create_time)
SELECT p.id, p.username, 'PATIENT',
CASE MOD(p.id, 4)
  WHEN 0 THEN '本周晚上血压偏高，想确认是否需要调整测量时间。'
  WHEN 1 THEN '最近体重下降较慢，饮食记录是否需要上传图片。'
  WHEN 2 THEN '服药提醒时间和实际作息有冲突，建议可自定义。'
  ELSE '能否在报告页增加近7天趋势对比。'
END,
CASE WHEN MOD(p.id, 3) = 0 THEN 1 ELSE 0 END,
CASE WHEN MOD(p.id, 3) = 0 THEN '已收到，建议先保持当前方案并连续记录3天。' ELSE NULL END,
CASE WHEN MOD(p.id, 3) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(p.id, 5) DAY) ELSE NULL END,
CASE WHEN MOD(p.id, 2) = 0 THEN 1 ELSE 0 END,
CASE WHEN MOD(p.id, 2) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(p.id, 4) DAY) ELSE NULL END,
DATE_SUB(NOW(), INTERVAL MOD(p.id, 10) DAY)
FROM sys_user p
WHERE p.role_type = 'PATIENT';

-- 15) Notices.
INSERT INTO system_notice (title, content, target_role, status, create_time)
VALUES
('门诊高峰期慢病随访提醒', '近期门诊高峰，请慢病患者尽量在早晨7:00-9:00完成上报，便于医生当日处理。', 'ALL', 1, DATE_SUB(NOW(), INTERVAL 9 DAY)),
('血压家庭测量规范更新', '请保持坐位静息5分钟后测量，连续两次取平均值，减少误差。', 'PATIENT', 1, DATE_SUB(NOW(), INTERVAL 7 DAY)),
('医生端分组随访排班调整', '周三下午新增线上随访时段，请医生在群组管理中补充本周任务分配。', 'DOCTOR', 1, DATE_SUB(NOW(), INTERVAL 6 DAY)),
('系统升级公告', '本周完成反馈通道与预警规则优化，若遇到异常请通过反馈通道提交。', 'ALL', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('历史公告（下线）', '该公告仅用于测试下线状态展示。', 'ALL', 0, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- 16) Operation logs for recent activity.
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (n INT PRIMARY KEY);
INSERT INTO tmp_seq(n)
VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
       (11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
       (21),(22),(23),(24),(25),(26),(27),(28),(29),(30),
       (31),(32),(33),(34),(35),(36),(37),(38),(39),(40);

DROP TEMPORARY TABLE IF EXISTS tmp_doctors;
DROP TEMPORARY TABLE IF EXISTS tmp_patients;

CREATE TEMPORARY TABLE tmp_doctors AS
SELECT username, ROW_NUMBER() OVER (ORDER BY id) AS rn
FROM sys_user
WHERE role_type = 'DOCTOR';

CREATE TEMPORARY TABLE tmp_patients AS
SELECT username, ROW_NUMBER() OVER (ORDER BY id) AS rn
FROM sys_user
WHERE role_type = 'PATIENT';

SET @doc_cnt := (SELECT COUNT(*) FROM tmp_doctors);
SET @pat_cnt := (SELECT COUNT(*) FROM tmp_patients);

INSERT INTO operation_log (username, role_type, request_method, request_uri, success, message, create_time)
SELECT
CASE WHEN MOD(s.n, 5) = 0 THEN 'admin'
  WHEN MOD(s.n, 2) = 0 THEN td.username
  ELSE tp.username
END,
CASE WHEN MOD(s.n, 5) = 0 THEN 'ADMIN'
     WHEN MOD(s.n, 2) = 0 THEN 'DOCTOR'
     ELSE 'PATIENT'
END,
CASE MOD(s.n, 4) WHEN 0 THEN 'GET' WHEN 1 THEN 'POST' WHEN 2 THEN 'PUT' ELSE 'PATCH' END,
CASE
  WHEN MOD(s.n, 5) = 0 THEN '/api/admin/monitor/overview'
  WHEN MOD(s.n, 3) = 0 THEN '/api/doctor/alerts'
  WHEN MOD(s.n, 3) = 1 THEN '/api/patient/data'
  ELSE '/api/feedback'
END,
CASE WHEN MOD(s.n, 9) = 0 THEN 0 ELSE 1 END,
CASE WHEN MOD(s.n, 9) = 0 THEN '参数校验失败' ELSE '处理成功' END,
DATE_SUB(NOW(), INTERVAL s.n HOUR)
FROM tmp_seq s
LEFT JOIN tmp_doctors td
  ON td.rn = ((s.n - 1) MOD @doc_cnt) + 1
LEFT JOIN tmp_patients tp
  ON tp.rn = ((s.n - 1) MOD @pat_cnt) + 1;

DROP TEMPORARY TABLE IF EXISTS tmp_days;
DROP TEMPORARY TABLE IF EXISTS tmp_seq;
DROP TEMPORARY TABLE IF EXISTS tmp_doctors;
DROP TEMPORARY TABLE IF EXISTS tmp_patients;

COMMIT;

-- 17) Quick validation.
SELECT 'users_admin' AS metric, COUNT(*) AS cnt FROM sys_user WHERE role_type = 'ADMIN' AND deleted = 0
UNION ALL SELECT 'users_doctor', COUNT(*) FROM sys_user WHERE role_type = 'DOCTOR' AND deleted = 0
UNION ALL SELECT 'users_patient', COUNT(*) FROM sys_user WHERE role_type = 'PATIENT' AND deleted = 0
UNION ALL SELECT 'archives', COUNT(*) FROM patient_archive WHERE deleted = 0
UNION ALL SELECT 'groups', COUNT(*) FROM doctor_group WHERE deleted = 0
UNION ALL SELECT 'group_members', COUNT(*) FROM doctor_group_member WHERE deleted = 0
UNION ALL SELECT 'health_data', COUNT(*) FROM health_data WHERE deleted = 0
UNION ALL SELECT 'alerts', COUNT(*) FROM health_alert WHERE deleted = 0
UNION ALL SELECT 'feedback', COUNT(*) FROM feedback_message WHERE deleted = 0
UNION ALL SELECT 'notices', COUNT(*) FROM system_notice WHERE deleted = 0
UNION ALL SELECT 'operation_logs', COUNT(*) FROM operation_log WHERE deleted = 0;
