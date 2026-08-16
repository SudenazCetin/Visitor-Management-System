package com.vms.dto.report;

public record TopPersonnelResponse(
    Long personnelId,
    String fullName,
    String department,
    long visitCount
) {}
