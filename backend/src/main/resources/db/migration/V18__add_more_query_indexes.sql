CREATE INDEX idx_health_data_user_time_indicator ON health_data (user_id, report_time, indicator_type);

CREATE INDEX idx_health_alert_status_user_risk_time ON health_alert (status, user_id, risk_level, risk_score, create_time);

CREATE INDEX idx_feedback_role_status_time ON feedback_message (sender_role_type, status, create_time);

CREATE INDEX idx_feedback_sender_reply_status ON feedback_message (sender_user_id, reply_read, status);