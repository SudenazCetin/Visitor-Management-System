package com.vms.dto.dashboard;

public record DepartmentVisitDto(
    String department,
    long visitCount,
    double percentage
) {}
