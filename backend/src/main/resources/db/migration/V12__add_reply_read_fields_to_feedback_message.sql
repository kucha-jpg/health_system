ALTER TABLE feedback_message
    ADD COLUMN reply_read TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    ADD COLUMN reply_read_time DATETIME NULL;
