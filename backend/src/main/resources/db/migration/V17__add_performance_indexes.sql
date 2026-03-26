CREATE INDEX idx_health_data_user_indicator_time ON health_data (user_id, indicator_type, report_time);
CREATE INDEX idx_health_alert_status_risk_time ON health_alert (status, risk_level, risk_score, create_time);
CREATE INDEX idx_health_alert_user_time ON health_alert (user_id, create_time);
CREATE INDEX idx_operation_log_role_success_time ON operation_log (role_type, success, create_time);
