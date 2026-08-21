package com.vms.dto.announcement;

public record AnnouncementResponse(
    String message,
    long recipientCount
) {}
