SET @db_name = DATABASE();

SET @sql_add_reply_content = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE feedback_message ADD COLUMN reply_content VARCHAR(500) NULL',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'feedback_message'
      AND COLUMN_NAME = 'reply_content'
);
PREPARE stmt_add_reply_content FROM @sql_add_reply_content;
EXECUTE stmt_add_reply_content;
DEALLOCATE PREPARE stmt_add_reply_content;

SET @sql_add_replied_time = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE feedback_message ADD COLUMN replied_time DATETIME NULL',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'feedback_message'
      AND COLUMN_NAME = 'replied_time'
);
PREPARE stmt_add_replied_time FROM @sql_add_replied_time;
EXECUTE stmt_add_replied_time;
DEALLOCATE PREPARE stmt_add_replied_time;
