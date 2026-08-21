package com.vms.dto.notification;

import com.vms.enums.NotificationStatus;
import com.vms.enums.SocketCategory;
import com.vms.enums.SocketEvent;
import com.vms.enums.SocketType;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String recipientUsername,
    SocketEvent event,
    SocketCategory category,
    SocketType type,
    String title,
    String message,
    String actionUrl,
    String targetEntity,
    Long targetEntityId,
    NotificationStatus status,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {}
