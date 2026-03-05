package com.health.system.service;

import com.health.system.dto.PatientArchiveDTO;
import com.health.system.entity.PatientArchive;

public interface PatientArchiveService {
    PatientArchive getMyArchive(String username);

    void saveMyArchive(String username, PatientArchiveDTO dto);

    void deleteMyArchive(String username, Long id);
}
