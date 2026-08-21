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
    void testSendCheckInAndCheckOutNotificationsWorkProperly() {
        User hostUser = new User("ayse_p", "pass", Role.PERSONNEL);
        Personnel host = new Personnel("Ayşe Demir", "Yazılım", "Uzman", "ayse@firma.com", hostUser);
        Visitor visitor = new Visitor("Kaya Soy", host, LocalDateTime.now(), null, true);
        visitor.setId(50L);

        // 1. CheckIn
        notificationService.sendCheckInNotification(hostUser, visitor);
        Assertions.assertEquals(1, dbNotifications.size());
        Notification checkInNotif = dbNotifications.get(0);
        Assertions.assertEquals("ayse_p", checkInNotif.getRecipient().getUsername());
        Assertions.assertEquals(SocketEvent.VISITOR_CHECKED_IN, checkInNotif.getEvent());
        Assertions.assertEquals(SocketCategory.VISITOR, checkInNotif.getCategory());
        Assertions.assertEquals(SocketType.SUCCESS, checkInNotif.getType());

        // 2. CheckOut
        visitor.setIsInside(false);
        visitor.setExitTime(LocalDateTime.now());
        notificationService.sendCheckOutNotification(hostUser, visitor);
        Assertions.assertEquals(2, dbNotifications.size());
        Notification checkOutNotif = dbNotifications.get(1);
        Assertions.assertEquals(SocketEvent.VISITOR_CHECKED_OUT, checkOutNotif.getEvent());
        Assertions.assertEquals(SocketType.INFO, checkOutNotif.getType());
    }

    @Test
    void testPasswordChangedNotificationTargetedToUser() {
        User personnelUser = new User("personnel_user", "pass", Role.PERSONNEL);
        User adminUser = new User("admin_user", "pass", Role.ADMIN);

        notificationService.sendPasswordChangedNotification(personnelUser);
        notificationService.sendPasswordChangedNotification(adminUser);

        Assertions.assertEquals(2, dbNotifications.size());

        Notification n1 = dbNotifications.get(0);
        Assertions.assertEquals("personnel_user", n1.getRecipient().getUsername());
        Assertions.assertEquals(SocketEvent.PASSWORD_CHANGED, n1.getEvent());
        Assertions.assertEquals(SocketCategory.USER, n1.getCategory());
        Assertions.assertEquals(SocketType.SUCCESS, n1.getType());

        Notification n2 = dbNotifications.get(1);
        Assertions.assertEquals("admin_user", n2.getRecipient().getUsername());
        Assertions.assertEquals(SocketEvent.PASSWORD_CHANGED, n2.getEvent());
    }

    @Test
    void testProfileUpdatedNotificationTargetedToOwner() {
        User user = new User("profile_owner", "pass", Role.PERSONNEL);

        notificationService.sendProfileUpdatedNotification(user);

        Assertions.assertEquals(1, dbNotifications.size());
        Notification n = dbNotifications.get(0);
        Assertions.assertEquals("profile_owner", n.getRecipient().getUsername());
        Assertions.assertEquals(SocketEvent.PROFILE_UPDATED, n.getEvent());
        Assertions.assertEquals(SocketCategory.USER, n.getCategory());
        Assertions.assertEquals(SocketType.INFO, n.getType());
        Assertions.assertEquals("Profil Güncellendi", n.getTitle());
    }

    @Test
    void testUserCreatedNotificationTargetedToNewUserOnly() {
        User newPersonnelUser = new User("new_personnel", "pass", Role.PERSONNEL);

        notificationService.sendUserCreatedNotification(newPersonnelUser);

        Assertions.assertEquals(1, dbNotifications.size());
        Notification n = dbNotifications.get(0);
        Assertions.assertEquals("new_personnel", n.getRecipient().getUsername());
        Assertions.assertEquals(SocketEvent.USER_CREATED, n.getEvent());
        Assertions.assertEquals(SocketCategory.USER, n.getCategory());
        Assertions.assertEquals(SocketType.SUCCESS, n.getType());
        Assertions.assertEquals(NotificationStatus.UNREAD, n.getStatus());
    }

    @Test
    void testAdminCreationNotificationTargetedOnlyToPerformingAdmin() {
        User performingAdmin = new User("adminA", "pass", Role.ADMIN);
        User newPersonnel = new User("new_user", "pass", Role.PERSONNEL);

        notificationService.sendUserCreatedNotification(newPersonnel);
        notificationService.sendUserCreatedNotificationToAdmin(performingAdmin, newPersonnel, "Sudenaz Çetin");

        Assertions.assertEquals(2, dbNotifications.size());

        Notification nNew = dbNotifications.get(0);
        Assertions.assertEquals("new_user", nNew.getRecipient().getUsername());
        Assertions.assertEquals("Hesabınız Oluşturuldu", nNew.getTitle());

        Notification nAdmin = dbNotifications.get(1);
        Assertions.assertEquals("adminA", nAdmin.getRecipient().getUsername());
        Assertions.assertEquals("Yeni Kullanıcı Oluşturuldu", nAdmin.getTitle());
        Assertions.assertTrue(nAdmin.getMessage().contains("Sudenaz Çetin"));

        long adminBNotifs = dbNotifications.stream()
                .filter(n -> n.getRecipient().getUsername().equals("adminB"))
                .count();
        Assertions.assertEquals(0, adminBNotifs, "Other admins must NOT receive notification");
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
