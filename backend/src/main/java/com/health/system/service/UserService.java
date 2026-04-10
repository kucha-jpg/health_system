package com.health.system.service;

import com.health.system.dto.UserDTO;

import java.util.Map;

public interface UserService {
    Map<String, Object> listUsers(String keyword, String roleType, Integer status, Integer pageNo, Integer pageSize);

    void createUser(UserDTO dto);

    void updateUser(UserDTO dto);

    void updateStatus(Long id, Integer status);
}
