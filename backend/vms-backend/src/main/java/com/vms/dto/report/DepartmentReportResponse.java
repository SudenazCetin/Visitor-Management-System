package com.vms.dto.report;

public record DepartmentReportResponse(
    String department,
    long visitCount,
    double percentage
) {}
