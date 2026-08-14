package com.vms.dto.auth;

import com.vms.entity.Role;

public record LoginResponse(
    String token,
    String username,
    Role role
) {}
