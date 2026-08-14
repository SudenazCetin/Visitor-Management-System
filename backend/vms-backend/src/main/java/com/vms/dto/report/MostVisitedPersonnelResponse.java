package com.vms.dto.report;

public record MostVisitedPersonnelResponse(
    Long personnelId,
    String personnelName,
    String department,
    long visitorCount
) {}
