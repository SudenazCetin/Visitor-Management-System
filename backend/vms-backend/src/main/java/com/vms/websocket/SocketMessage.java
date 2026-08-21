package com.vms.websocket;

import com.vms.enums.SocketCategory;
import com.vms.enums.SocketEvent;
import com.vms.enums.SocketType;
import java.time.LocalDateTime;

public record SocketMessage(
    SocketCategory category,
    SocketEvent event,
    SocketType type,
    String title,
    String message,
    String actionUrl,
    String targetEntity,
    Long targetEntityId,
    Object payload,
    LocalDateTime timestamp,
    String recipientUsername
) {
    public static SocketMessage of(SocketCategory category,
                                   SocketEvent event,
                                   SocketType type,
                                   String title,
                                   String message,
                                   String actionUrl,
                                   String targetEntity,
                                   Long targetEntityId,
                                   Object payload,
                                   String recipientUsername) {
        return new SocketMessage(
            category,
            event,
            type,
            title,
            message,
            actionUrl,
            targetEntity,
            targetEntityId,
            payload,
            LocalDateTime.now(),
            recipientUsername
        );
    }
}
