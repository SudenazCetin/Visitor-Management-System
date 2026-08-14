package com.vms.dto.visitor;

import java.time.LocalDateTime;

public record VisitorResponse(
    Long id,
    String fullName,
    Long hostId,
    String hostName,
    String hostDepartment,
    LocalDateTime entryTime,
    LocalDateTime exitTime,
    Boolean isInside
) {}
