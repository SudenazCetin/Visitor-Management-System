package com.vms.dto.dashboard;

import java.time.LocalDateTime;

public record RecentActivityDto(
    Long visitorId,
    String type,
    String visitorName,
    String hostName,
    String department,
    LocalDateTime timestamp,
    String description
) {}
