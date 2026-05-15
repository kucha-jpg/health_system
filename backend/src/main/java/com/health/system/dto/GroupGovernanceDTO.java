package com.health.system.dto;

import lombok.Data;

@Data
public class GroupGovernanceDTO {
    private Long groupId;
    private String groupName;
    private String doctorName;
    private Integer patientCount;
    private String governanceStatus;
    private String reviewRemark;
    private String targetDept;
    private String lastActionTime;
}
