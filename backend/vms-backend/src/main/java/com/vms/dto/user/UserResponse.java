package com.vms.dto.user;

import com.vms.entity.Role;

public record UserResponse(
    Long id,
    String username,
    Role role
) {}
