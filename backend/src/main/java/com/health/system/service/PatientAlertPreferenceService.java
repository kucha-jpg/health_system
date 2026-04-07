package com.health.system.service;

import com.health.system.dto.PatientAlertPreferenceDTO;

import java.util.List;
import java.util.Map;

public interface PatientAlertPreferenceService {
    List<Map<String, Object>> listMyPreferences(String username);

    void upsertMyPreference(String username, PatientAlertPreferenceDTO dto);
}
