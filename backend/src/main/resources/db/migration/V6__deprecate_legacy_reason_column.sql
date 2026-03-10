UPDATE health_alert
SET reason = reason_text
WHERE reason <> reason_text;

ALTER TABLE health_alert
    MODIFY COLUMN reason VARCHAR(255) NOT NULL COMMENT 'Deprecated: use reason_text';
