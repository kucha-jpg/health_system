ALTER TABLE health_alert
    ADD COLUMN reason_code VARCHAR(32) NULL COMMENT '预警原因代码' AFTER level,
    ADD COLUMN reason_text VARCHAR(255) NULL COMMENT '预警原因文本' AFTER reason_code;

UPDATE health_alert
SET reason_code = CASE level
    WHEN 'HIGH' THEN 'THRESHOLD_HIGH'
    WHEN 'MEDIUM' THEN 'THRESHOLD_MEDIUM'
    ELSE 'THRESHOLD_UNKNOWN'
END,
reason_text = reason
WHERE reason_code IS NULL OR reason_text IS NULL;

ALTER TABLE health_alert
    MODIFY COLUMN reason_code VARCHAR(32) NOT NULL DEFAULT 'THRESHOLD_UNKNOWN' COMMENT '预警原因代码',
    MODIFY COLUMN reason_text VARCHAR(255) NOT NULL COMMENT '预警原因文本';
