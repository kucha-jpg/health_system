package com.health.system.service;

import com.health.system.dto.LoginRequest;
import com.health.system.dto.RegisterRequest;

import java.util.Map;

public interface AuthService {
    Map<String, Object> login(LoginRequest request);

    Map<String, Object> register(RegisterRequest request);
}
