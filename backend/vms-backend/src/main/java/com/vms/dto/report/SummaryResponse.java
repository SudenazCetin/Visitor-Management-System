package com.vms.dto.report;

public record SummaryResponse(
    long totalVisitors,
    long todayVisitors,
    long activeVisitors,
    long averageStayMinutes
) {}
