package com.vms.dto.report;

public record WeeklyTrafficResponse(
    String day,
    long entryCount,
    long exitCount
) {}
