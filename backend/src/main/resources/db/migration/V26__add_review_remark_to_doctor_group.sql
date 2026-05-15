ALTER TABLE doctor_group
    ADD COLUMN review_remark VARCHAR(500) NULL COMMENT '管理员审核备注（通过理由或驳回原因）' AFTER governance_status;
