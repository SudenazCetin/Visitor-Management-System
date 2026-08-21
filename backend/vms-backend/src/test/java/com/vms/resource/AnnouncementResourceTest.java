package com.vms.resource;

import com.vms.dto.announcement.AnnouncementRequest;
import com.vms.dto.announcement.AnnouncementResponse;
import com.vms.entity.Notification;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.enums.AnnouncementTarget;
import com.vms.enums.SocketEvent;
import com.vms.repository.NotificationRepository;
import com.vms.repository.UserRepository;
import com.vms.service.NotificationService;
import com.vms.service.SocketService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementResourceTest {

    private List<User> dbUsers;
    private List<Notification> dbNotifications;
    private NotificationService notificationService;
    private AnnouncementResource announcementResource;

    @BeforeEach
    void setUp() {
        dbUsers = new ArrayList<>();
        dbNotifications = new ArrayList<>();

        dbUsers.add(new User("admin1", "pass", Role.ADMIN));
        dbUsers.add(new User("rec1", "pass", Role.RECEPTIONIST));
        dbUsers.add(new User("p1", "pass", Role.PERSONNEL));
        dbUsers.add(new User("p2", "pass", Role.PERSONNEL));

        UserRepository stubUserRepo = new UserRepository() {
            @Override
            public List<User> listAll() {
                return new ArrayList<>(dbUsers);
            }

            @Override
            public List<User> findByRole(Role role) {
                return dbUsers.stream().filter(u -> u.getRole() == role).toList();
            }
        };

        NotificationRepository stubNotifRepo = new NotificationRepository() {
            @Override
            public void persist(Notification notification) {
                notification.setId((long) (dbNotifications.size() + 1));
                dbNotifications.add(notification);
            }
        };

        SocketService stubSocketService = new SocketService() {
            @Override
            public void sendToUser(String username, com.vms.websocket.SocketMessage message) {}
        };

        notificationService = new NotificationService(stubNotifRepo, stubSocketService, stubUserRepo);
        announcementResource = new AnnouncementResource(notificationService);
    }

    @Test
    void testAdminCanSendAnnouncementToAllUsers() {
        SecurityContext sec = createSecurityContext("admin1", "ADMIN");
        AnnouncementRequest req = new AnnouncementRequest("Planlı Bakım", "Sistem bakımı yapılacaktır.", AnnouncementTarget.ALL);

        Response res = announcementResource.createAnnouncement(sec, req);
        Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), res.getStatus());

        AnnouncementResponse body = (AnnouncementResponse) res.getEntity();
        Assertions.assertEquals(4, body.recipientCount());
        Assertions.assertEquals(4, dbNotifications.size());

        for (Notification n : dbNotifications) {
            Assertions.assertEquals(SocketEvent.SYSTEM_MESSAGE, n.getEvent());
            Assertions.assertEquals("Planlı Bakım", n.getTitle());
        }
    }

    @Test
    void testAdminCanSendAnnouncementToPersonnelOnly() {
        SecurityContext sec = createSecurityContext("admin1", "ADMIN");
        AnnouncementRequest req = new AnnouncementRequest("Personel Toplantısı", "Saat 15:00 toplantı.", AnnouncementTarget.PERSONNEL);

        Response res = announcementResource.createAnnouncement(sec, req);
        Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), res.getStatus());

        AnnouncementResponse body = (AnnouncementResponse) res.getEntity();
        Assertions.assertEquals(2, body.recipientCount());
        Assertions.assertEquals(2, dbNotifications.size());

        for (Notification n : dbNotifications) {
            Assertions.assertEquals(Role.PERSONNEL, n.getRecipient().getRole());
        }
    }

    @Test
    void testNonAdminUserReturns403Forbidden() {
        SecurityContext sec = createSecurityContext("p1", "PERSONNEL");
        AnnouncementRequest req = new AnnouncementRequest("Yetkisiz", "Mesaj", AnnouncementTarget.ALL);

        WebApplicationException ex = Assertions.assertThrows(WebApplicationException.class, () -> {
            announcementResource.createAnnouncement(sec, req);
        });

        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    }

    private SecurityContext createSecurityContext(String username, String role) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return () -> username;
            }

            @Override
            public boolean isUserInRole(String r) {
                return role.equalsIgnoreCase(r);
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return "JWT";
            }
        };
    }
}
