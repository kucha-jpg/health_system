USE health_system;

-- Append more realistic records without deleting existing data.

DROP TEMPORARY TABLE IF EXISTS tmp_append_days;
CREATE TEMPORARY TABLE tmp_append_days (n INT PRIMARY KEY);
INSERT INTO tmp_append_days(n)
VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
       (11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
       (21),(22),(23),(24),(25),(26),(27),(28),(29),(30);

-- 1) Additional blood pressure records: every day for 30 days.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
       '血压',
       CONCAT(118 + MOD(p.id + d.n, 38), '/', 76 + MOD(p.id * 2 + d.n, 24)),
       DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 7 HOUR), INTERVAL d.n DAY),
       '晨起加测'
FROM sys_user p
JOIN tmp_append_days d ON 1 = 1
WHERE p.role_type = 'PATIENT' AND p.deleted = 0;

-- 2) Additional blood sugar records: every 2 days.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
       '血糖',
       CAST(ROUND(5.4 + MOD(p.id + d.n, 12) * 0.7, 1) AS CHAR),
       DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 21 HOUR), INTERVAL d.n DAY),
       '晚餐后复测'
FROM sys_user p
JOIN tmp_append_days d ON MOD(d.n, 2) = 0
WHERE p.role_type = 'PATIENT' AND p.deleted = 0;

-- 3) Additional weight records: every 3 days.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
       '体重',
       CAST(ROUND(53 + MOD(p.id + d.n, 34) + MOD(d.n, 4) * 0.2, 1) AS CHAR),
       DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 20 HOUR), INTERVAL d.n DAY),
       '每周趋势观察'
FROM sys_user p
JOIN tmp_append_days d ON MOD(d.n, 3) = 0
WHERE p.role_type = 'PATIENT' AND p.deleted = 0;

-- 4) Additional medication records: every day.
INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
SELECT p.id,
       '服药',
       CASE WHEN MOD(p.id + d.n, 8) IN (0, 1) THEN '未服药' ELSE '已服药' END,
       DATE_SUB(DATE_ADD(CURDATE(), INTERVAL 8 HOUR), INTERVAL d.n DAY),
       '晚间依从性补录'
FROM sys_user p
JOIN tmp_append_days d ON 1 = 1
WHERE p.role_type = 'PATIENT' AND p.deleted = 0;

-- 5) Generate new alerts from recently appended abnormal records only.
INSERT INTO health_alert (
  user_id, health_data_id, indicator_type, value,
  level, risk_score, risk_level, reason_code, reason_text,
  status, source, handled_by, handle_remark, handled_time, create_time
)
SELECT h.user_id,
       h.id,
       h.indicator_type,
       h.value,
       CASE
         WHEN h.indicator_type = '血压' AND (
           CAST(SUBSTRING_INDEX(h.value, '/', 1) AS UNSIGNED) >= 170 OR
           CAST(SUBSTRING_INDEX(h.value, '/', -1) AS UNSIGNED) >= 105
         ) THEN 'HIGH'
         WHEN h.indicator_type = '血糖' AND CAST(h.value AS DECIMAL(5,1)) >= 14.0 THEN 'HIGH'
         ELSE 'MEDIUM'
       END,
       CASE
         WHEN h.indicator_type = '血压' AND CAST(SUBSTRING_INDEX(h.value, '/', 1) AS UNSIGNED) >= 170 THEN 92
         WHEN h.indicator_type = '血糖' AND CAST(h.value AS DECIMAL(5,1)) >= 14.0 THEN 90
         WHEN h.indicator_type = '服药' AND h.value = '未服药' THEN 72
         ELSE 66
       END,
       CASE
         WHEN h.indicator_type IN ('血压', '血糖') THEN 'HIGH'
         ELSE 'MEDIUM'
       END,
       CASE
         WHEN h.indicator_type = '血压' THEN 'THRESHOLD_HIGH'
         WHEN h.indicator_type = '血糖' THEN 'THRESHOLD_HIGH'
         WHEN h.indicator_type = '体重' THEN 'THRESHOLD_MEDIUM'
         ELSE 'MEDICATION_MISSED'
       END,
       CASE
         WHEN h.indicator_type = '服药' THEN '服药依从性下降，请关注执行情况'
         ELSE CONCAT(h.indicator_type, '超出近期安全阈值')
       END,
       CASE WHEN h.report_time >= DATE_SUB(NOW(), INTERVAL 5 DAY) THEN 'OPEN' ELSE 'CLOSED' END,
       'SYSTEM',
       NULL,
       NULL,
       NULL,
       h.report_time
FROM health_data h
WHERE h.deleted = 0
  AND h.create_time >= DATE_SUB(NOW(), INTERVAL 40 MINUTE)
  AND (
    (h.indicator_type = '血压' AND (
      CAST(SUBSTRING_INDEX(h.value, '/', 1) AS UNSIGNED) >= 150 OR
      CAST(SUBSTRING_INDEX(h.value, '/', -1) AS UNSIGNED) >= 95
    ))
    OR (h.indicator_type = '血糖' AND CAST(h.value AS DECIMAL(5,1)) >= 11.1)
    OR (h.indicator_type = '体重' AND CAST(h.value AS DECIMAL(5,1)) >= 85.0)
    OR (h.indicator_type = '服药' AND h.value = '未服药')
  );

-- 6) Add more feedback messages.
INSERT INTO feedback_message (sender_user_id, sender_username, sender_role_type, content, status, reply_content, replied_time, reply_read, reply_read_time, create_time)
SELECT p.id,
       p.username,
       'PATIENT',
       CASE MOD(p.id, 5)
         WHEN 0 THEN '最近夜间血压波动较大，是否建议增加睡前测量？'
         WHEN 1 THEN '血糖在周末偏高，能否给一个饮食模板参考？'
         WHEN 2 THEN '本周两次忘记服药，想设置更早提醒。'
         WHEN 3 THEN '周报里希望看到同龄参考区间。'
         ELSE '最近体重变化不明显，是否需要调整运动强度？'
       END,
       CASE WHEN MOD(p.id, 4) = 0 THEN 1 ELSE 0 END,
       CASE WHEN MOD(p.id, 4) = 0 THEN '已记录，建议保持连续打卡并观察一周趋势。' ELSE NULL END,
       CASE WHEN MOD(p.id, 4) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(p.id, 6) DAY) ELSE NULL END,
       CASE WHEN MOD(p.id, 3) = 0 THEN 1 ELSE 0 END,
       CASE WHEN MOD(p.id, 3) = 0 THEN DATE_SUB(NOW(), INTERVAL MOD(p.id, 5) DAY) ELSE NULL END,
       DATE_SUB(NOW(), INTERVAL MOD(p.id, 8) DAY)
FROM sys_user p
WHERE p.role_type = 'PATIENT' AND p.deleted = 0;

-- 7) Add announcement examples with rich HTML.
INSERT INTO system_notice (title, content, target_role, status, create_time)
VALUES
('春季慢病复测提醒', '<h3>春季慢病复测提醒</h3><p>近期昼夜温差较大，建议慢病患者在 <strong>07:00-09:00</strong> 完成晨间上报。</p><p><img src="https://images.unsplash.com/photo-1576091160550-2173dba999ef?auto=format&fit=crop&w=1200&q=80" alt="健康提醒" style="max-width:100%;border-radius:12px;"/></p><ul><li>血压连续偏高请及时反馈。</li><li>服药异常请在备注中说明原因。</li></ul>', 'ALL', 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('医生随访排班更新', '<h3>医生随访排班更新</h3><p>本周新增周四晚间线上随访时段，请在群组中提前分配重点患者。</p>', 'DOCTOR', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('患者上报填写规范', '<h3>上报填写规范</h3><p>请尽量使用标准格式：血压 <strong>收缩压/舒张压</strong>，血糖填写数值并附测量时段。</p>', 'PATIENT', 1, DATE_SUB(NOW(), INTERVAL 12 HOUR));

-- 8) Add operation logs for richer audit history.
DROP TEMPORARY TABLE IF EXISTS tmp_append_seq;
CREATE TEMPORARY TABLE tmp_append_seq (n INT PRIMARY KEY);
INSERT INTO tmp_append_seq(n)
VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
       (11),(12),(13),(14),(15),(16),(17),(18),(19),(20);

INSERT INTO operation_log (username, role_type, request_method, request_uri, success, message, create_time)
SELECT CASE WHEN MOD(s.n, 6) = 0 THEN 'admin' ELSE p.username END,
       CASE WHEN MOD(s.n, 6) = 0 THEN 'ADMIN' ELSE 'PATIENT' END,
       CASE MOD(s.n, 4) WHEN 0 THEN 'GET' WHEN 1 THEN 'POST' WHEN 2 THEN 'PUT' ELSE 'PATCH' END,
       CASE WHEN MOD(s.n, 5) = 0 THEN '/api/admin/monitor/overview'
            WHEN MOD(s.n, 5) = 1 THEN '/api/patient/data'
            WHEN MOD(s.n, 5) = 2 THEN '/api/patient/alerts'
            WHEN MOD(s.n, 5) = 3 THEN '/api/feedback'
            ELSE '/api/patient/report-summary'
       END,
       CASE WHEN MOD(s.n, 11) = 0 THEN 0 ELSE 1 END,
       CASE WHEN MOD(s.n, 11) = 0 THEN '请求参数不完整' ELSE '处理成功' END,
       DATE_SUB(NOW(), INTERVAL s.n HOUR)
FROM tmp_append_seq s
JOIN (
  SELECT id, username, ROW_NUMBER() OVER (ORDER BY id) rn
  FROM sys_user
  WHERE role_type = 'PATIENT' AND deleted = 0
) p ON p.rn = MOD(s.n, 10) + 1;

DROP TEMPORARY TABLE IF EXISTS tmp_append_days;
DROP TEMPORARY TABLE IF EXISTS tmp_append_seq;

SELECT 'health_data_total' AS item, COUNT(*) AS total FROM health_data WHERE deleted = 0
UNION ALL
SELECT 'health_alert_total', COUNT(*) FROM health_alert WHERE deleted = 0
UNION ALL
SELECT 'feedback_message_total', COUNT(*) FROM feedback_message WHERE deleted = 0
UNION ALL
SELECT 'system_notice_total', COUNT(*) FROM system_notice WHERE deleted = 0
UNION ALL
SELECT 'operation_log_total', COUNT(*) FROM operation_log WHERE deleted = 0;
