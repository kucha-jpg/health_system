-- ============================================================
-- 真实感假数据脚本
-- 10 名医生 + 30 名患者 + 每人最近 14 天差异化健康数据
-- 密码统一为 123456（BCrypt 加密）
-- ============================================================
USE health_system;
SET NAMES utf8mb4;

-- ============================================================
-- 一、清理旧测试数据（保留管理员 admin 和系统表）
-- ============================================================
START TRANSACTION;

DELETE FROM health_alert;
DELETE FROM health_data;
DELETE FROM patient_alert_preference;
DELETE FROM patient_archive;
DELETE FROM doctor_group_doctor_member;
DELETE FROM doctor_group_member;
DELETE FROM doctor_group;
DELETE FROM feedback_message;
DELETE FROM operation_log;
DELETE FROM system_notice;
DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username != 'admin');
DELETE FROM sys_user WHERE username != 'admin';

-- 重置自增 ID
ALTER TABLE sys_user AUTO_INCREMENT = 1;
ALTER TABLE patient_archive AUTO_INCREMENT = 1;
ALTER TABLE health_data AUTO_INCREMENT = 1;
ALTER TABLE health_alert AUTO_INCREMENT = 1;
ALTER TABLE doctor_group AUTO_INCREMENT = 1;
ALTER TABLE doctor_group_member AUTO_INCREMENT = 1;
ALTER TABLE doctor_group_doctor_member AUTO_INCREMENT = 1;

COMMIT;

-- ============================================================
-- 二、创建 10 名医生账号
-- ============================================================
-- BCrypt 密码：123456
SET @pw = '$2a$10$9pQSWn/t60LoEORBgQYZ6uaIXc61bZeFEjbIMVWOVldUp27hzgfu6';

INSERT INTO sys_user (username, password, phone, name, role_type, status) VALUES
('doctor01', @pw, '13800000001', '陈志远', 'DOCTOR', 1),
('doctor02', @pw, '13800000002', '林雪梅', 'DOCTOR', 1),
('doctor03', @pw, '13800000003', '王建国', 'DOCTOR', 1),
('doctor04', @pw, '13800000004', '赵雅琴', 'DOCTOR', 1),
('doctor05', @pw, '13800000005', '刘明辉', 'DOCTOR', 1),
('doctor06', @pw, '13800000006', '孙丽华', 'DOCTOR', 1),
('doctor07', @pw, '13800000007', '张伟强', 'DOCTOR', 1),
('doctor08', @pw, '13800000008', '吴晓芳', 'DOCTOR', 1),
('doctor09', @pw, '13800000009', '郑文博', 'DOCTOR', 1),
('doctor10', @pw, '13800000010', '黄慧敏', 'DOCTOR', 1);

-- 医生绑定 DOCTOR 角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.role_type = 'DOCTOR' AND r.role_name = 'DOCTOR';

-- ============================================================
-- 三、创建 30 名患者账号
-- ============================================================
INSERT INTO sys_user (username, password, phone, name, role_type, status) VALUES
('patient01', @pw, '13900000001', '张明', 'PATIENT', 1),
('patient02', @pw, '13900000002', '李娟', 'PATIENT', 1),
('patient03', @pw, '13900000003', '王芳', 'PATIENT', 1),
('patient04', @pw, '13900000004', '刘强', 'PATIENT', 1),
('patient05', @pw, '13900000005', '陈秀英', 'PATIENT', 1),
('patient06', @pw, '13900000006', '杨文辉', 'PATIENT', 1),
('patient07', @pw, '13900000007', '赵敏', 'PATIENT', 1),
('patient08', @pw, '13900000008', '黄志强', 'PATIENT', 1),
('patient09', @pw, '13900000009', '周丽', 'PATIENT', 1),
('patient10', @pw, '13900000010', '吴伟', 'PATIENT', 1),
('patient11', @pw, '13900000011', '徐静', 'PATIENT', 1),
('patient12', @pw, '13900000012', '孙涛', 'PATIENT', 1),
('patient13', @pw, '13900000013', '马小红', 'PATIENT', 1),
('patient14', @pw, '13900000014', '胡建国', 'PATIENT', 1),
('patient15', @pw, '13900000015', '林燕', 'PATIENT', 1),
('patient16', @pw, '13900000016', '何勇', 'PATIENT', 1),
('patient17', @pw, '13900000017', '郭婷', 'PATIENT', 1),
('patient18', @pw, '13900000018', '高建军', 'PATIENT', 1),
('patient19', @pw, '13900000019', '罗琳', 'PATIENT', 1),
('patient20', @pw, '13900000020', '梁志明', 'PATIENT', 1),
('patient21', @pw, '13900000021', '宋雅文', 'PATIENT', 1),
('patient22', @pw, '13900000022', '唐磊', 'PATIENT', 1),
('patient23', @pw, '13900000023', '韩雪', 'PATIENT', 1),
('patient24', @pw, '13900000024', '冯刚', 'PATIENT', 1),
('patient25', @pw, '13900000025', '董丽娜', 'PATIENT', 1),
('patient26', @pw, '13900000026', '程浩', 'PATIENT', 1),
('patient27', @pw, '13900000027', '曹艳', 'PATIENT', 1),
('patient28', @pw, '13900000028', '邓鹏', 'PATIENT', 1),
('patient29', @pw, '13900000029', '彭晓燕', 'PATIENT', 1),
('patient30', @pw, '13900000030', '沈立', 'PATIENT', 1);

-- 患者绑定 PATIENT 角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.role_type = 'PATIENT' AND r.role_name = 'PATIENT';

-- ============================================================
-- 四、创建患者档案（30 份，年龄、病史各有差异）
-- ============================================================
INSERT INTO patient_archive (user_id, name, age, medical_history, medication_history, allergy_history) VALUES
((SELECT id FROM sys_user WHERE username='patient01'), '张明', 45, '高血压病史3年', '硝苯地平', '无'),
((SELECT id FROM sys_user WHERE username='patient02'), '李娟', 38, '无', '无', '青霉素'),
((SELECT id FROM sys_user WHERE username='patient03'), '王芳', 52, '2型糖尿病5年', '二甲双胍', '无'),
((SELECT id FROM sys_user WHERE username='patient04'), '刘强', 29, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient05'), '陈秀英', 61, '高血压10年，冠心病', '阿司匹林，硝苯地平', '无'),
((SELECT id FROM sys_user WHERE username='patient06'), '杨文辉', 41, '高血脂', '阿托伐他汀', '无'),
((SELECT id FROM sys_user WHERE username='patient07'), '赵敏', 35, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient08'), '黄志强', 55, '高血压5年，糖尿病3年', '厄贝沙坦，二甲双胍', '磺胺类'),
((SELECT id FROM sys_user WHERE username='patient09'), '周丽', 27, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient10'), '吴伟', 48, '脂肪肝', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient11'), '徐静', 33, '妊娠期糖尿病史', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient12'), '孙涛', 58, '糖尿病8年，高血压', '胰岛素，缬沙坦', '无'),
((SELECT id FROM sys_user WHERE username='patient13'), '马小红', 42, '甲状腺功能减退', '左甲状腺素钠', '无'),
((SELECT id FROM sys_user WHERE username='patient14'), '胡建国', 64, '高血压15年，脑梗后遗症', '氯吡格雷，硝苯地平', '阿司匹林'),
((SELECT id FROM sys_user WHERE username='patient15'), '林燕', 31, '无', '无', '花粉过敏'),
((SELECT id FROM sys_user WHERE username='patient16'), '何勇', 46, '痛风', '别嘌醇', '无'),
((SELECT id FROM sys_user WHERE username='patient17'), '郭婷', 25, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient18'), '高建军', 53, '高血压3年', '氨氯地平', '无'),
((SELECT id FROM sys_user WHERE username='patient19'), '罗琳', 39, '偏头痛', '布洛芬（按需）', '无'),
((SELECT id FROM sys_user WHERE username='patient20'), '梁志明', 50, '2型糖尿病2年', '二甲双胍', '头孢类'),
((SELECT id FROM sys_user WHERE username='patient21'), '宋雅文', 28, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient22'), '唐磊', 44, '高血压', '厄贝沙坦', '无'),
((SELECT id FROM sys_user WHERE username='patient23'), '韩雪', 36, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient24'), '冯刚', 59, '冠心病，支架术后', '阿司匹林，氯吡格雷，瑞舒伐他汀', '无'),
((SELECT id FROM sys_user WHERE username='patient25'), '董丽娜', 47, '高血脂，肥胖', '阿托伐他汀', '无'),
((SELECT id FROM sys_user WHERE username='patient26'), '程浩', 32, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient27'), '曹艳', 54, '高血压5年', '硝苯地平缓释片', '无'),
((SELECT id FROM sys_user WHERE username='patient28'), '邓鹏', 26, '无', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient29'), '彭晓燕', 43, '乳腺结节术后', '无', '无'),
((SELECT id FROM sys_user WHERE username='patient30'), '沈立', 57, '糖尿病6年，高血压', '胰岛素，氨氯地平', '无');

-- ============================================================
-- 五、生成最近 14 天的健康数据
-- 策略：每个患者的数据量和频率不同，指标值在合理范围内差异化波动
-- ============================================================

-- 5.1 创建日期序列辅助表（最近 14 天：2026-05-07 ~ 2026-05-20）
DROP TEMPORARY TABLE IF EXISTS tmp_days;
CREATE TEMPORARY TABLE tmp_days (day_offset INT, report_date DATE);
INSERT INTO tmp_days VALUES
(0, '2026-05-20'),(1, '2026-05-19'),(2, '2026-05-18'),(3, '2026-05-17'),
(4, '2026-05-16'),(5, '2026-05-15'),(6, '2026-05-14'),(7, '2026-05-13'),
(8, '2026-05-12'),(9, '2026-05-11'),(10,'2026-05-10'),(11,'2026-05-09'),
(12,'2026-05-08'),(13,'2026-05-07');

-- 5.2 创建患者指标档案表（定义每个人的基础值和上传频率）
DROP TEMPORARY TABLE IF EXISTS tmp_patient_profile;
CREATE TEMPORARY TABLE tmp_patient_profile (
    username VARCHAR(20),
    bp_sys_base INT,      -- 收缩压基础值
    bp_dia_base INT,      -- 舒张压基础值
    bs_base DECIMAL(4,1), -- 血糖基础值
    wt_base DECIMAL(5,1), -- 体重基础值
    bp_freq INT,          -- 血压上传频率（0-13 天中上传几天）
    bs_freq INT,          -- 血糖上传频率
    wt_freq INT,          -- 体重上传频率
    med_freq INT,         -- 服药记录频率
    bp_sys_var INT,       -- 收缩压波动范围
    bs_var DECIMAL(3,1),  -- 血糖波动范围
    wt_var DECIMAL(3,1),  -- 体重波动范围
    wt_trend DECIMAL(4,1) -- 体重趋势（正值上升，负值下降）
);

-- 30 个患者，分 6 种健康画像，每种 5 人，数值差异化

-- 画像 A：正常健康（patient01/07/09/17/21/23/26/28）- 8人 - 各项正常
INSERT INTO tmp_patient_profile VALUES
('patient01',120,80,5.2,68.0, 10,6,5,8, 8,0.6,0.8,-0.2),
('patient07',118,78,5.0,55.5, 8,5,4,7, 6,0.5,0.5,0.0),
('patient09',115,75,5.3,52.0, 7,4,4,6, 5,0.4,0.3,0.1),
('patient17',122,82,5.1,58.5, 9,6,5,7, 7,0.5,0.6,-0.1),
('patient21',116,76,4.9,50.0, 8,5,3,6, 6,0.4,0.4,0.0),
('patient23',120,80,5.4,56.0, 9,6,5,8, 7,0.6,0.5,0.0),
('patient26',124,82,5.0,72.0, 10,5,5,7, 8,0.5,0.7,0.0),
('patient28',118,78,5.2,63.0, 7,4,4,5, 5,0.4,0.4,0.1);

-- 画像 B：高血压（patient04/10/18/22/27）- 5人 - 血压偏高，服药控制
INSERT INTO tmp_patient_profile VALUES
('patient04',145,95,5.5,75.0, 12,7,6,10, 10,0.8,0.8,-0.3),
('patient10',155,100,5.8,82.0, 13,8,7,12, 12,1.0,1.0,-0.2),
('patient18',138,90,5.3,70.5, 11,6,5,9, 8,0.6,0.6,-0.1),
('patient22',148,96,5.6,78.0, 12,7,6,10, 9,0.8,0.7,-0.2),
('patient27',160,105,6.0,85.0, 13,9,8,12, 14,1.2,1.2,-0.4);

-- 画像 C：糖尿病（patient03/12/20/30）- 4人 - 血糖偏高
INSERT INTO tmp_patient_profile VALUES
('patient03',130,85,8.5,65.0, 11,13,7,12, 8,2.0,0.6,0.0),
('patient12',140,90,10.2,72.0, 12,13,8,13, 10,2.5,0.8,-0.1),
('patient20',125,82,7.8,68.5, 10,12,6,11, 7,1.8,0.5,0.0),
('patient30',135,88,9.5,76.0, 12,13,7,13, 10,2.2,0.7,-0.2);

-- 画像 D：高血压+糖尿病合并（patient05/08/14/24）- 4人 - 双高
INSERT INTO tmp_patient_profile VALUES
('patient05',155,98,9.2,68.0, 13,13,8,13, 12,2.5,1.0,-0.1),
('patient08',148,92,11.0,80.0, 12,13,7,12, 10,3.0,1.2,-0.2),
('patient14',165,105,8.8,73.0, 13,13,8,13, 14,2.8,0.9,0.0),
('patient24',152,96,10.5,70.0, 13,13,7,13, 11,2.6,1.1,-0.1);

-- 画像 E：体重偏重/肥胖，减重中（patient06/11/25/29）- 4人
INSERT INTO tmp_patient_profile VALUES
('patient06',128,84,5.6,88.0, 8,5,13,6, 8,0.6,1.5,-0.8),
('patient11',120,80,5.2,79.5, 7,4,12,5, 6,0.5,1.2,-0.5),
('patient25',130,86,5.7,92.0, 9,6,13,7, 9,0.7,1.8,-1.0),
('patient29',122,82,5.4,85.0, 8,5,12,6, 7,0.6,1.4,-0.6);

-- 画像 F：波动型/边缘型（patient02/13/15/16/19）- 5人 - 指标在阈值附近波动
INSERT INTO tmp_patient_profile VALUES
('patient02',130,88,6.5,60.0, 10,7,5,8, 12,1.5,0.8,0.2),
('patient13',135,92,6.8,62.5, 11,8,6,9, 14,1.8,1.0,-0.1),
('patient15',125,85,5.5,57.0, 9,6,5,7, 10,1.2,0.6,0.1),
('patient16',140,94,7.2,74.0, 12,9,7,10, 13,2.0,1.1,-0.3),
('patient19',128,86,6.0,58.5, 10,7,5,8, 11,1.4,0.7,0.0);

-- ============================================================
-- 六、用存储过程批量生成 health_data
-- ============================================================
DROP PROCEDURE IF EXISTS generate_health_data;
DELIMITER //
CREATE PROCEDURE generate_health_data()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_uid BIGINT;
    DECLARE v_uname VARCHAR(20);
    DECLARE v_bp_sys INT;
    DECLARE v_bp_dia INT;
    DECLARE v_bs DECIMAL(4,1);
    DECLARE v_wt DECIMAL(5,1);
    DECLARE v_bp_f INT;
    DECLARE v_bs_f INT;
    DECLARE v_wt_f INT;
    DECLARE v_med_f INT;
    DECLARE v_bp_var INT;
    DECLARE v_bs_var DECIMAL(3,1);
    DECLARE v_wt_var DECIMAL(3,1);
    DECLARE v_wt_trend DECIMAL(4,1);

    DECLARE v_day VARCHAR(10);
    DECLARE v_sys INT;
    DECLARE v_dia INT;
    DECLARE v_bs_val DECIMAL(4,1);
    DECLARE v_wt_val DECIMAL(5,1);
    DECLARE v_remark VARCHAR(100);
    DECLARE v_hour INT;
    DECLARE v_min INT;
    DECLARE v_report_time VARCHAR(19);

    DECLARE cur CURSOR FOR
        SELECT pp.*, u.id
        FROM tmp_patient_profile pp
        JOIN sys_user u ON u.username = pp.username;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_uname, v_bp_sys, v_bp_dia, v_bs, v_wt,
                      v_bp_f, v_bs_f, v_wt_f, v_med_f,
                      v_bp_var, v_bs_var, v_wt_var, v_wt_trend, v_uid;
        IF done THEN LEAVE read_loop; END IF;

        -- 遍历最近 14 天
        SET v_day = '2026-05-07';

        WHILE v_day <= '2026-05-20' DO
            SET v_hour = 6 + FLOOR(RAND() * 12);  -- 6:00~18:00
            SET v_min = FLOOR(RAND() * 60);
            SET v_report_time = CONCAT(v_day, ' ', LPAD(v_hour,2,'0'), ':', LPAD(v_min,2,'0'), ':00');

            -- 血压（频率决定是否当天录入）
            IF MOD(DAYOFYEAR(v_day) + v_uid, 14) < v_bp_f THEN
                SET v_sys = v_bp_sys + FLOOR(RAND() * v_bp_var * 2) - v_bp_var;
                SET v_dia = v_bp_dia + FLOOR(RAND() * (v_bp_var/2) * 2) - (v_bp_var/2);
                IF v_sys < 90 THEN SET v_sys = 90; END IF;
                IF v_sys > 210 THEN SET v_sys = 210; END IF;
                IF v_dia < 50 THEN SET v_dia = 50; END IF;
                IF v_dia > 140 THEN SET v_dia = 140; END IF;
                SET v_remark = CASE
                    WHEN v_sys > 150 THEN '早上测量偏高'
                    WHEN FLOOR(RAND()*5)=0 THEN '晨起空腹测量'
                    WHEN FLOOR(RAND()*5)=0 THEN '服药后测量'
                    ELSE ''
                END;
                INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
                VALUES (v_uid, '血压', CONCAT(v_sys, '/', v_dia), v_report_time, v_remark);
            END IF;

            -- 血糖（频率决定）
            IF MOD(DAYOFYEAR(v_day) + v_uid + 3, 14) < v_bs_f THEN
                SET v_bs_val = v_bs + (RAND() * v_bs_var * 2) - v_bs_var;
                IF v_bs_val < 2.0 THEN SET v_bs_val = 2.0; END IF;
                IF v_bs_val > 25.0 THEN SET v_bs_val = 25.0; END IF;
                SET v_bs_val = ROUND(v_bs_val, 1);
                SET v_remark = CASE
                    WHEN FLOOR(RAND()*4)=0 THEN '空腹血糖'
                    WHEN FLOOR(RAND()*4)=0 THEN '餐后2小时'
                    ELSE ''
                END;
                INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
                VALUES (v_uid, '血糖', CAST(v_bs_val AS CHAR), v_report_time, v_remark);
            END IF;

            -- 体重（频率通常更低）
            IF MOD(DAYOFYEAR(v_day) + v_uid + 7, 14) < v_wt_f THEN
                -- 体重随天数微调（趋势）
                SET v_wt_val = v_wt + v_wt_trend * (DATEDIFF(v_day, '2026-05-07') / 14.0)
                             + (RAND() * v_wt_var * 2) - v_wt_var;
                SET v_wt_val = ROUND(v_wt_val, 1);
                IF v_wt_val < 35.0 THEN SET v_wt_val = 35.0; END IF;
                IF v_wt_val > 130.0 THEN SET v_wt_val = 130.0; END IF;
                SET v_remark = IF(FLOOR(RAND()*3)=0, '晨起空腹称重', '');
                INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
                VALUES (v_uid, '体重', CAST(v_wt_val AS CHAR), v_report_time, v_remark);
            END IF;

            -- 服药（频率通常更高）
            IF MOD(DAYOFYEAR(v_day) + v_uid + 1, 14) < v_med_f THEN
                INSERT INTO health_data (user_id, indicator_type, value, report_time, remark)
                VALUES (v_uid, '服药', IF(RAND()>0.15, '已服药', '未服药'), v_report_time, '');
            END IF;

            SET v_day = DATE_ADD(v_day, INTERVAL 1 DAY);
        END WHILE;
    END LOOP;
    CLOSE cur;
END //
DELIMITER ;

CALL generate_health_data();
DROP PROCEDURE IF EXISTS generate_health_data;

-- ============================================================
-- 七、为部分异常数据自动触发预警（模拟系统生成的预警记录）
-- ============================================================
INSERT INTO health_alert (user_id, health_data_id, indicator_type, value, level,
                          reason_code, reason_text, risk_score, risk_level, source, status)
SELECT
    hd.user_id,
    hd.id,
    hd.indicator_type,
    hd.value,
    CASE
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 160 THEN 'HIGH'
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 140 THEN 'MEDIUM'
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 16.7 THEN 'HIGH'
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 11.1 THEN 'MEDIUM'
        ELSE 'MEDIUM'
    END,
    CASE
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 160 THEN 'BP_HIGH_THRESHOLD'
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 140 THEN 'BP_MEDIUM_THRESHOLD'
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 16.7 THEN 'BS_HIGH_THRESHOLD'
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 11.1 THEN 'BS_MEDIUM_THRESHOLD'
        ELSE 'THRESHOLD_UNKNOWN'
    END,
    CASE
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 160
             THEN CONCAT('收缩压 ', SUBSTRING_INDEX(hd.value, '/', 1), ' 超过高危阈值 180/120')
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 140
             THEN CONCAT('收缩压 ', SUBSTRING_INDEX(hd.value, '/', 1), ' 超过中危阈值 140/90')
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 16.7
             THEN CONCAT('血糖 ', hd.value, ' 超过高危阈值 16.7')
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 11.1
             THEN CONCAT('血糖 ', hd.value, ' 超过中危阈值 11.1')
        ELSE CONCAT(hd.indicator_type, ' 指标异常')
    END,
    CASE
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 160 THEN 75 + FLOOR(RAND() * 20)
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 140 THEN 50 + FLOOR(RAND() * 25)
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 16.7 THEN 80 + FLOOR(RAND() * 15)
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 11.1 THEN 55 + FLOOR(RAND() * 25)
        ELSE 40 + FLOOR(RAND() * 20)
    END,
    CASE
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 160 THEN 'HIGH'
        WHEN hd.indicator_type = '血压'
             AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 140 THEN 'MEDIUM'
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 16.7 THEN 'HIGH'
        WHEN hd.indicator_type = '血糖'
             AND CAST(hd.value AS DECIMAL(10,1)) >= 11.1 THEN 'MEDIUM'
        ELSE 'LOW'
    END,
    'SYSTEM',
    'OPEN'
FROM health_data hd
WHERE
    (hd.indicator_type = '血压'
     AND CAST(SUBSTRING_INDEX(hd.value, '/', 1) AS SIGNED) >= 140)
    OR
    (hd.indicator_type = '血糖'
     AND CAST(hd.value AS DECIMAL(10,1)) >= 11.1);

-- 标记部分预警为已处理（前 40%）
UPDATE health_alert SET status = 'CLOSED', handled_time = DATE_ADD(create_time, INTERVAL FLOOR(RAND()*24) HOUR)
WHERE id % 5 IN (0, 1);

-- 修复 health_data.create_time 同步到 report_time（解决监控图表数据时间问题）
UPDATE health_data SET create_time = report_time WHERE DATE(create_time) != DATE(report_time);
UPDATE health_data SET update_time = create_time;

-- 修复 health_alert.create_time 同步到对应 health_data 的 report_time
UPDATE health_alert ha
JOIN health_data hd ON ha.health_data_id = hd.id
SET ha.create_time = hd.report_time, ha.update_time = hd.report_time
WHERE DATE(ha.create_time) != DATE(hd.report_time);

UPDATE health_alert
SET handled_time = DATE_ADD(create_time, INTERVAL FLOOR(2 + RAND() * 24) HOUR)
WHERE status = 'CLOSED' AND handled_time IS NULL;

-- ============================================================
-- 八、创建医生群组（每个医生 1 个群组，各管 3-5 个患者）
-- ============================================================
-- 获取医生ID对应关系
DROP TEMPORARY TABLE IF EXISTS tmp_doc_ids;
CREATE TEMPORARY TABLE tmp_doc_ids AS
SELECT id, username, name FROM sys_user WHERE role_type = 'DOCTOR' AND username LIKE 'doctor%' ORDER BY username;

DROP TEMPORARY TABLE IF EXISTS tmp_pat_ids;
CREATE TEMPORARY TABLE tmp_pat_ids AS
SELECT id, username, name FROM sys_user WHERE role_type = 'PATIENT' ORDER BY username;

-- 创建 10 个群组
INSERT INTO doctor_group (doctor_id, group_name, description)
SELECT id, CONCAT(name, '的随访组'), CONCAT('负责高血压/糖尿病慢病管理，共', (id % 4 + 3), '名患者')
FROM tmp_doc_ids;

-- 每个群组加 3-5 名患者
DROP TEMPORARY TABLE IF EXISTS tmp_group_ids;
CREATE TEMPORARY TABLE tmp_group_ids AS
SELECT dg.id AS gid, dg.doctor_id, ROW_NUMBER() OVER (ORDER BY dg.id) AS rn
FROM doctor_group dg;

-- 按顺序给群组分配患者（不同数量）
INSERT INTO doctor_group_member (group_id, patient_user_id)
SELECT g.gid, p.id
FROM tmp_group_ids g
JOIN tmp_pat_ids p ON MOD(p.id - 1, 10) + 1 = g.rn
WHERE (p.id - 1) % 30 < g.rn * 3 + 2   -- 每个群组不同数量 (3~5 人)
   OR (p.id - 1) % 30 = g.rn - 1;

-- 确保每个群组至少有 3 名患者：补充
INSERT IGNORE INTO doctor_group_member (group_id, patient_user_id)
SELECT g.gid, p.id
FROM tmp_group_ids g
CROSS JOIN tmp_pat_ids p
WHERE MOD(p.id + g.rn, 10) = 0
  AND NOT EXISTS (
    SELECT 1 FROM doctor_group_member gm
    WHERE gm.group_id = g.gid AND gm.patient_user_id = p.id
  )
LIMIT 100;

-- ============================================================
-- 九、添加系统公告
-- ============================================================
INSERT INTO system_notice (title, content, status, target_role, target_username, create_time, update_time) VALUES
('系统升级通知', '<p>各位用户：</p><p>健康管理系统已完成升级，新增<strong>14天趋势分析</strong>和<strong>个性化预警阈值</strong>功能。</p>', 1, 'ALL', NULL, '2026-05-07 09:00:00', '2026-05-07 09:00:00'),
('健康数据上报提醒', '<p>各位患者请注意：</p><p>为保证健康趋势分析准确，建议<strong>每周至少上报3次</strong>血压和血糖数据。</p>', 1, 'PATIENT', NULL, '2026-05-10 08:30:00', '2026-05-10 08:30:00'),
('假期健康建议', '<p>假期期间请保持规律作息，按时服药，每天测量并上报。</p>', 1, 'ALL', NULL, '2026-05-14 10:00:00', '2026-05-14 10:00:00'),
('医生协作群组功能', '<p>新版<strong>群组管理</strong>功能已上线，支持按病种创建随访群组并邀请其他医生协作。</p>', 1, 'DOCTOR', NULL, '2026-05-08 14:00:00', '2026-05-08 14:00:00'),
('高风险预警处理规范', '<p>高风险预警须在<strong>24小时内</strong>处理，中风险预警须在<strong>48小时内</strong>处理。</p>', 1, 'DOCTOR', NULL, '2026-05-12 09:30:00', '2026-05-12 09:30:00'),
('个性化阈值使用指南', '<p>您现在可以设置<strong>个性化的预警阈值</strong>，进入"个性化阈值"页面即可配置。</p>', 1, 'PATIENT', NULL, '2026-05-09 11:00:00', '2026-05-09 11:00:00'),
('旧版系统停用通知', '<p>旧版系统已于4月30日停止服务。</p>', 0, 'ALL', NULL, '2026-04-30 18:00:00', '2026-05-01 00:00:00');

-- ============================================================
-- 十、添加反馈记录
-- ============================================================
INSERT INTO feedback_message (sender_user_id, sender_username, sender_role_type, content, status,
                              reply_content, replied_time, reply_read, reply_read_time, create_time, update_time)
SELECT u.id, u.username, 'PATIENT', fb.content, fb.status, fb.reply_content,
       fb.replied_time, fb.reply_read, fb.reply_read_time, fb.create_time, fb.create_time
FROM sys_user u
CROSS JOIN (
    SELECT 'patient03' AS uname, '请问血压测量是早上空腹测比较准还是晚上测？' AS content,
           1 AS status, '建议在早晨起床后1小时内、服药前测量，此时数据最具参考价值。' AS reply_content,
           '2026-05-11 10:30:00' AS replied_time, 1 AS reply_read, '2026-05-11 18:00:00' AS reply_read_time,
           '2026-05-10 20:15:00' AS create_time
    UNION ALL
    SELECT 'patient08', '我最近血糖一直偏高，是否需要增加药量？',
           1, '请勿自行调整药量。建议携带近两周血糖记录来院复诊。', '2026-05-13 15:00:00', 1, '2026-05-13 20:30:00', '2026-05-12 09:45:00'
    UNION ALL
    SELECT 'patient12', '系统上报数据时偶尔会弹出网络异常提示。',
           1, '技术团队已优化超时策略，请再试一次。', '2026-05-15 11:00:00', 0, NULL, '2026-05-14 16:30:00'
    UNION ALL
    SELECT 'patient18', '服药记录里的未服药能不能加个备注功能？',
           0, NULL, NULL, 0, NULL, '2026-05-18 14:20:00'
    UNION ALL
    SELECT 'patient22', '体重数据能不能按月出个变化曲线？',
           0, NULL, NULL, 0, NULL, '2026-05-19 08:10:00'
    UNION ALL
    SELECT 'patient27', '个性化阈值的中风险是什么意思？',
           1, '中风险表示指标已偏离正常但尚未达高危水平，建议重点关注。', '2026-05-16 09:00:00', 1, '2026-05-16 19:30:00', '2026-05-15 07:45:00'
) AS fb ON u.username = fb.uname;

INSERT INTO feedback_message (sender_user_id, sender_username, sender_role_type, content, status,
                              reply_content, replied_time, reply_read, reply_read_time, create_time, update_time)
SELECT u.id, u.username, 'DOCTOR', fb.content, 1, fb.reply_content,
       fb.replied_time, 1, fb.reply_read_time, fb.create_time, fb.create_time
FROM sys_user u
CROSS JOIN (
    SELECT 'doctor03' AS uname, '建议增加批量导出群组患者数据功能。' AS content,
           '已记录该需求，预计下个版本支持群组级别批量导出。' AS reply_content,
           '2026-05-16 15:00:00' AS replied_time, '2026-05-17 09:00:00' AS reply_read_time,
           '2026-05-13 11:20:00' AS create_time
    UNION ALL
    SELECT 'doctor08', '患者洞察页的趋势图能不能支持打印功能？',
           '图表导出图片功能已在开发中，打印功能将在后续版本加入。', '2026-05-18 10:00:00', '2026-05-18 14:00:00', '2026-05-17 16:45:00'
) AS fb ON u.username = fb.uname;

-- ============================================================
-- 十一、清理临时表
-- ============================================================
DROP TEMPORARY TABLE IF EXISTS tmp_days;
DROP TEMPORARY TABLE IF EXISTS tmp_patient_profile;
DROP TEMPORARY TABLE IF EXISTS tmp_doc_ids;
DROP TEMPORARY TABLE IF EXISTS tmp_pat_ids;
DROP TEMPORARY TABLE IF EXISTS tmp_group_ids;

-- ============================================================
-- 十二、验证汇总
-- ============================================================
-- ⚠️ 注入完成后，需手动刷新 Redis 缓存，否则监控页等可能显示旧数据：
--    docker exec health-redis redis-cli -a change_me_redis_password FLUSHALL
-- ============================================================
SELECT '=== 数据注入完成 ===' AS status;
SELECT 'DOCTOR' AS role, COUNT(*) AS count FROM sys_user WHERE role_type = 'DOCTOR'
UNION ALL
SELECT 'PATIENT', COUNT(*) FROM sys_user WHERE role_type = 'PATIENT'
UNION ALL
SELECT 'ADMIN', COUNT(*) FROM sys_user WHERE role_type = 'ADMIN';

SELECT '血压' AS indicator, COUNT(*) AS records FROM health_data WHERE indicator_type = '血压'
UNION ALL
SELECT '血糖', COUNT(*) FROM health_data WHERE indicator_type = '血糖'
UNION ALL
SELECT '体重', COUNT(*) FROM health_data WHERE indicator_type = '体重'
UNION ALL
SELECT '服药', COUNT(*) FROM health_data WHERE indicator_type = '服药';

SELECT 'health_alert' AS tbl, COUNT(*) AS records FROM health_alert
UNION ALL
SELECT 'doctor_group', COUNT(*) FROM doctor_group
UNION ALL
SELECT 'group_member', COUNT(*) FROM doctor_group_member
UNION ALL
SELECT 'system_notice', COUNT(*) FROM system_notice
UNION ALL
SELECT 'feedback', COUNT(*) FROM feedback_message;
