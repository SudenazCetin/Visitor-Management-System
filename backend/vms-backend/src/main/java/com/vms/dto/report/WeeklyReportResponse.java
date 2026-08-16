package com.vms.dto.report;

public record WeeklyReportResponse(
    String date,
    String dayName,
    long count
) {}
