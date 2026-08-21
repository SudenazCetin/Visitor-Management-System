package com.vms.service;

import com.vms.dto.notification.NotificationResponse;
import com.vms.entity.Notification;
import com.vms.entity.Personnel;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.entity.Visitor;
import com.vms.enums.NotificationStatus;
import com.vms.enums.SocketCategory;
import com.vms.enums.SocketEvent;
import com.vms.enums.SocketType;
import com.vms.repository.NotificationRepository;
import com.vms.websocket.SocketMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NotificationServiceTest {

    private NotificationService notificationService;
    private List<Notification> dbNotifications;
    private AtomicBoolean socketSent;

    @BeforeEach
    void setUp() {
        dbNotifications = new ArrayList<>();
        socketSent = new AtomicBoolean(false);

        NotificationRepository stubRepo = new NotificationRepository() {
            @Override
            public void persist(Notification notification) {
                notification.setId((long) (dbNotifications.size() + 1));
                dbNotifications.add(notification);
            }

            @Override
            public Notification findById(Long id) {
                return dbNotifications.stream()
                        .filter(n -> n.getId().equals(id))
                        .findFirst()
                        .orElse(null);
            }

            @Override
            public List<Notification> findByRecipientUsername(String username) {
                return dbNotifications.stream()
                        .filter(n -> n.getRecipient().getUsername().equalsIgnoreCase(username))
                        .toList();
            }

            @Override
            public long countUnreadByRecipientUsername(String username) {
                return dbNotifications.stream()
                        .filter(n -> n.getRecipient().getUsername().equalsIgnoreCase(username) && n.getStatus() == NotificationStatus.UNREAD)
                        .count();
            }

            @Override
            public void delete(Notification notification) {
                dbNotifications.remove(notification);
            }
        };

        SocketService stubSocketService = new SocketService() {
            @Override
            public void sendToUser(String username, SocketMessage message) {
                socketSent.set(true);
            }
        };

        notificationService = new NotificationService(stubRepo, stubSocketService);
    }

    @Test
    void testSendCheckInNotificationCreatesAndSendsNotificationOnlyToHostUser() {
        User hostUser = new User("ayse_p", "pass", Role.PERSONNEL);
        Personnel host = new Personnel("Ayşe Demir", "Yazılım", "Uzman", "ayse@firma.com", hostUser);
        Visitor visitor = new Visitor("Kaya Soy", host, LocalDateTime.now(), null, true);
        visitor.setId(50L);

        notificationService.sendCheckInNotification(hostUser, visitor);

        Assertions.assertEquals(1, dbNotifications.size());
        Notification n = dbNotifications.get(0);
        Assertions.assertEquals("ayse_p", n.getRecipient().getUsername());
        Assertions.assertEquals(SocketEvent.VISITOR_CHECKED_IN, n.getEvent());
        Assertions.assertEquals(SocketCategory.VISITOR, n.getCategory());
        Assertions.assertEquals(SocketType.SUCCESS, n.getType());
        Assertions.assertTrue(n.getMessage().contains("Kaya Soy"));
        Assertions.assertTrue(socketSent.get(), "Real-time socket message must be dispatched");
    }

    @Test
    void testMarkAsReadSecurityGuard() {
        User user1 = new User("user1", "pass", Role.PERSONNEL);
        User user2 = new User("user2", "pass", Role.PERSONNEL);

        Notification n = new Notification(user1, SocketEvent.SYSTEM_MESSAGE, SocketCategory.SYSTEM, SocketType.INFO, "Title", "Msg", null, null, null);
        dbNotifications.add(n);
        n.setId(10L);

        Assertions.assertThrows(SecurityException.class, () -> {
            notificationService.markAsRead(10L, "user2");
        }, "User2 should not be allowed to mark User1's notification as read");

        NotificationResponse updated = notificationService.markAsRead(10L, "user1");
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(NotificationStatus.READ, updated.status());
    }
}
