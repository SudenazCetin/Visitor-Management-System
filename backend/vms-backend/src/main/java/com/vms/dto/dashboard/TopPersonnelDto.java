package com.vms.dto.dashboard;

public record TopPersonnelDto(
    Long hostId,
    String fullName,
    String department,
    long visitCount
) {}
