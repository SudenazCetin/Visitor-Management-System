package com.vms.repository;

import com.vms.entity.Notification;
import com.vms.enums.NotificationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class NotificationRepository implements PanacheRepository<Notification> {

    public List<Notification> findByRecipientUsername(String username) {
        if (username == null || username.isBlank()) return List.of();
        return list("lower(recipient.username) = lower(?1)", Sort.by("createdAt").descending(), username.trim());
    }

    public long countUnreadByRecipientUsername(String username) {
        if (username == null || username.isBlank()) return 0;
        return count("lower(recipient.username) = lower(?1) and status = ?2", username.trim(), NotificationStatus.UNREAD);
    }

    @Transactional
    public long markAllAsReadByRecipientUsername(String username) {
        if (username == null || username.isBlank()) return 0;
        return update("status = ?1, readAt = ?2 where lower(recipient.username) = lower(?3) and status = ?4",
                NotificationStatus.READ, LocalDateTime.now(), username.trim(), NotificationStatus.UNREAD);
    }

    @Transactional
    public long deleteAllReadByRecipientUsername(String username) {
        if (username == null || username.isBlank()) return 0;
        return delete("lower(recipient.username) = lower(?1) and status = ?2", username.trim(), NotificationStatus.READ);
    }
}
