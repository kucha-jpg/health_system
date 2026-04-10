ALTER TABLE system_notice
    ADD COLUMN target_role VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '公告投放角色: ALL/DOCTOR/PATIENT' AFTER content;

UPDATE system_notice
SET target_role = 'ALL'
WHERE target_role IS NULL OR target_role = '';
