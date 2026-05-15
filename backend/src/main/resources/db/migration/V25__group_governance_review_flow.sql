-- Default new groups to PENDING_REVIEW so admin must approve before use.
ALTER TABLE doctor_group
    MODIFY COLUMN governance_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW/ACTIVE/REJECTED/CROSS_DEPT/ARCHIVED';

-- Existing groups already in use: keep them ACTIVE so doctors can continue operating.
UPDATE doctor_group SET governance_status = 'ACTIVE'
WHERE governance_status IS NULL OR governance_status = '' OR governance_status = 'ACTIVE';
