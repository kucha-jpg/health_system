ALTER TABLE health_alert
    ADD COLUMN risk_score INT NULL COMMENT '风险评分(0-100)' AFTER level,
    ADD COLUMN risk_level VARCHAR(10) NULL COMMENT '风险等级(LOW/MEDIUM/HIGH)' AFTER risk_score;

UPDATE health_alert
SET risk_score = CASE level
    WHEN 'HIGH' THEN 85
    WHEN 'MEDIUM' THEN 60
    ELSE 30
END,
risk_level = CASE level
    WHEN 'HIGH' THEN 'HIGH'
    WHEN 'MEDIUM' THEN 'MEDIUM'
    ELSE 'LOW'
END
WHERE risk_score IS NULL OR risk_level IS NULL;

ALTER TABLE health_alert
    MODIFY COLUMN risk_score INT NOT NULL DEFAULT 60 COMMENT '风险评分(0-100)',
    MODIFY COLUMN risk_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级(LOW/MEDIUM/HIGH)';
