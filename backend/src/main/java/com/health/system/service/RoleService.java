package com.health.system.service;

import com.health.system.entity.Role;

import java.util.List;

public interface RoleService {
    List<Role> listRoles();

    void updatePermission(Long id, String permission);
}
