package com.health.system.service;

import com.health.system.dto.ComprehensiveScoreResult;

public interface ComprehensiveScoreService {
    ComprehensiveScoreResult calculateForPatient(String doctorUsername, Long patientUserId);
    ComprehensiveScoreResult calculate(String patientUsername);
}
