package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.SensitiveDataCipher;
import com.health.system.dto.PatientArchiveDTO;
import com.health.system.entity.PatientArchive;
import com.health.system.entity.User;
import com.health.system.mapper.PatientArchiveMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.PatientArchiveService;
import org.springframework.stereotype.Service;

@Service
public class PatientArchiveServiceImpl implements PatientArchiveService {

    private final PatientArchiveMapper patientArchiveMapper;
    private final UserMapper userMapper;
    private final SensitiveDataCipher sensitiveDataCipher;

    public PatientArchiveServiceImpl(PatientArchiveMapper patientArchiveMapper,
                                     UserMapper userMapper,
                                     SensitiveDataCipher sensitiveDataCipher) {
        this.patientArchiveMapper = patientArchiveMapper;
        this.userMapper = userMapper;
        this.sensitiveDataCipher = sensitiveDataCipher;
    }

    @Override
    public PatientArchive getMyArchive(String username) {
        Long userId = getCurrentUserId(username);
        PatientArchive archive = patientArchiveMapper.selectOne(new LambdaQueryWrapper<PatientArchive>().eq(PatientArchive::getUserId, userId));
        decryptArchive(archive);
        return archive;
    }

    @Override
    public void saveMyArchive(String username, PatientArchiveDTO dto) {
        Long userId = getCurrentUserId(username);
        PatientArchive archive = patientArchiveMapper.selectOne(new LambdaQueryWrapper<PatientArchive>().eq(PatientArchive::getUserId, userId));
        if (archive == null) {
            archive = new PatientArchive();
            archive.setUserId(userId);
        }
        archive.setName(dto.getName());
        archive.setAge(dto.getAge());
        archive.setMedicalHistory(sensitiveDataCipher.encrypt(dto.getMedicalHistory()));
        archive.setMedicationHistory(sensitiveDataCipher.encrypt(dto.getMedicationHistory()));
        archive.setAllergyHistory(sensitiveDataCipher.encrypt(dto.getAllergyHistory()));

        if (archive.getId() == null) {
            patientArchiveMapper.insert(archive);
        } else {
            patientArchiveMapper.updateById(archive);
        }
    }

    @Override
    public void deleteMyArchive(String username, Long id) {
        Long userId = getCurrentUserId(username);
        PatientArchive archive = patientArchiveMapper.selectById(id);
        if (archive == null || !userId.equals(archive.getUserId())) {
            throw BusinessException.forbidden("档案不存在或无权限");
        }
        patientArchiveMapper.deleteById(id);
    }

    private Long getCurrentUserId(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user.getId();
    }

    private void decryptArchive(PatientArchive archive) {
        if (archive == null) {
            return;
        }
        archive.setMedicalHistory(sensitiveDataCipher.decrypt(archive.getMedicalHistory()));
        archive.setMedicationHistory(sensitiveDataCipher.decrypt(archive.getMedicationHistory()));
        archive.setAllergyHistory(sensitiveDataCipher.decrypt(archive.getAllergyHistory()));
    }
}
