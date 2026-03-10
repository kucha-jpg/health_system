package com.health.system.service;

import com.health.system.dto.UserDTO;
import com.health.system.entity.User;

import java.util.List;

public interface UserService {
    List<User> listUsers(String keyword, String roleType, Integer status);

    void createUser(UserDTO dto);

    void updateUser(UserDTO dto);

    void updateStatus(Long id, Integer status);
}
