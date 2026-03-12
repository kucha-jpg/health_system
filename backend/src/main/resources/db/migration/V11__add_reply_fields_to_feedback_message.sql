ALTER TABLE feedback_message
    ADD COLUMN reply_content VARCHAR(500) NULL,
    ADD COLUMN replied_time DATETIME NULL;
