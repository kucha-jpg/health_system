ALTER TABLE doctor_group
    ADD COLUMN governance_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER description,
    ADD COLUMN target_dept VARCHAR(64) DEFAULT NULL AFTER governance_status;

UPDATE doctor_group SET governance_status = 'ACTIVE' WHERE governance_status IS NULL OR governance_status = '';
