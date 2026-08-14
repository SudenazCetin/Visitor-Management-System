package com.vms.dto.personnel;

public record PersonnelResponse(
    Long id,
    String fullName,
    String department,
    String title,
    String email
) {}
