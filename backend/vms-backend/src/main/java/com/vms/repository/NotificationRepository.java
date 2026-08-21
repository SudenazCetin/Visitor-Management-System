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
        return list("recipient.username", Sort.by("createdAt").descending(), username);
    }

    public long countUnreadByRecipientUsername(String username) {
        return count("recipient.username = ?1 and status = ?2", username, NotificationStatus.UNREAD);
    }

    @Transactional
    public long markAllAsReadByRecipientUsername(String username) {
        return update("status = ?1, readAt = ?2 where recipient.username = ?3 and status = ?4",
                NotificationStatus.READ, LocalDateTime.now(), username, NotificationStatus.UNREAD);
    }

    @Transactional
    public long deleteAllReadByRecipientUsername(String username) {
        return delete("recipient.username = ?1 and status = ?2", username, NotificationStatus.READ);
    }
}
