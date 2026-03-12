SET @db_name = DATABASE();

SET @sql_add_reply_read = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE feedback_message ADD COLUMN reply_read TINYINT NOT NULL DEFAULT 0 COMMENT ''0未读 1已读''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'feedback_message'
      AND COLUMN_NAME = 'reply_read'
);
PREPARE stmt_add_reply_read FROM @sql_add_reply_read;
EXECUTE stmt_add_reply_read;
DEALLOCATE PREPARE stmt_add_reply_read;

SET @sql_add_reply_read_time = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE feedback_message ADD COLUMN reply_read_time DATETIME NULL',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'feedback_message'
      AND COLUMN_NAME = 'reply_read_time'
);
PREPARE stmt_add_reply_read_time FROM @sql_add_reply_read_time;
EXECUTE stmt_add_reply_read_time;
DEALLOCATE PREPARE stmt_add_reply_read_time;
