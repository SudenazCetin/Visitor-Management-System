package com.vms.resource;

import com.vms.dto.visitor.VisitorResponse;
import com.vms.entity.Personnel;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.entity.Visitor;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.UserRepository;
import com.vms.repository.VisitorRepository;
import com.vms.service.NotificationService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MeResourceTest {

    private List<User> dbUsers;
    private List<Personnel> dbPersonnel;
    private List<Visitor> dbVisitors;
    private MeResource meResource;

    @BeforeEach
    void setUp() {
        dbUsers = new ArrayList<>();
        dbPersonnel = new ArrayList<>();
        dbVisitors = new ArrayList<>();

        User u1 = new User("personel1", "pass123", Role.PERSONNEL);
        u1.setId(10L);
        User u2 = new User("personel2", "pass456", Role.PERSONNEL);
        u2.setId(20L);
        dbUsers.add(u1);
        dbUsers.add(u2);

        Personnel p1 = new Personnel("Ahmet Yılmaz", "IT", "Yazılım Uzmanı", "ahmet@test.com");
        p1.setId(1L);
        p1.setUser(u1);

        Personnel p2 = new Personnel("Ayşe Kaya", "İnsan Kaynakları", "IK Yöneticisi", "ayse@test.com");
        p2.setId(2L);
        p2.setUser(u2);

        dbPersonnel.add(p1);
        dbPersonnel.add(p2);

        // Visitors for Personnel 1
        Visitor v1 = new Visitor("Ziyaretçi 1", p1, LocalDateTime.now().minusMinutes(20), null, true);
        v1.setId(101L);

        Visitor v2 = new Visitor("Ziyaretçi 2", p1, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusMinutes(10), false);
        v2.setId(102L);

        // Visitor for Personnel 2
        Visitor v3 = new Visitor("Ziyaretçi 3", p2, LocalDateTime.now().minusMinutes(15), null, true);
        v3.setId(103L);

        dbVisitors.add(v1);
        dbVisitors.add(v2);
        dbVisitors.add(v3);

        PersonnelRepository stubPersonnelRepo = new PersonnelRepository() {
            @Override
            public Optional<Personnel> findByUserUsername(String username) {
                return dbPersonnel.stream()
                        .filter(p -> p.getUser() != null && username.equals(p.getUser().getUsername()))
                        .findFirst();
            }
        };

        VisitorRepository stubVisitorRepo = new VisitorRepository() {
            @Override
            public List<Visitor> findByHostId(Long hostId) {
                return dbVisitors.stream()
                        .filter(v -> v.getHost() != null && hostId.equals(v.getHost().getId()))
                        .toList();
            }

            @Override
            public Optional<Visitor> findByIdOptional(Long id) {
                return dbVisitors.stream()
                        .filter(v -> id.equals(v.getId()))
                        .findFirst();
            }
        };

        UserRepository stubUserRepo = new UserRepository() {
            @Override
            public Optional<User> findByUsername(String username) {
                return dbUsers.stream()
                        .filter(u -> username.equals(u.getUsername()))
                        .findFirst();
            }
        };

        NotificationService stubNotifService = new NotificationService(null, null, null);

        meResource = new MeResource(stubPersonnelRepo, stubVisitorRepo, stubUserRepo, stubNotifService);
    }

    @Test
    void testPersonnelCanGetOwnSummary() {
        SecurityContext sec = createSecurityContext("personel1", "PERSONNEL");
        Response res = meResource.getMySummary(sec);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) res.getEntity();
        Assertions.assertEquals(2L, summary.get("totalVisitors"));
        Assertions.assertEquals(1L, summary.get("activeVisitors"));
        Assertions.assertEquals(2L, summary.get("todayVisitors"));
        Assertions.assertEquals(1L, summary.get("completedToday"));
        Assertions.assertNotNull(summary.get("lastVisitTime"));
    }

    @Test
    void testPersonnelDataIsolationForVisitors() {
        SecurityContext sec = createSecurityContext("personel1", "PERSONNEL");
        Response res = meResource.getMyVisitors(sec);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        @SuppressWarnings("unchecked")
        List<VisitorResponse> visitors = (List<VisitorResponse>) res.getEntity();
        Assertions.assertEquals(2, visitors.size());
        for (VisitorResponse v : visitors) {
            Assertions.assertEquals("Ahmet Yılmaz", v.hostName());
        }
    }

    @Test
    void testPersonnelCannotAccessAnotherPersonnelVisitorDetail() {
        SecurityContext secPersonel1 = createSecurityContext("personel1", "PERSONNEL");

        // Visitor 103 belongs to Personnel 2
        WebApplicationException ex = Assertions.assertThrows(WebApplicationException.class, () -> {
            meResource.getMyVisitorById(secPersonel1, 103L);
        });

        Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), ex.getResponse().getStatus());
    }

    @Test
    void testPersonnelCanAccessOwnVisitorDetail() {
        SecurityContext secPersonel1 = createSecurityContext("personel1", "PERSONNEL");

        // Visitor 101 belongs to Personnel 1
        Response res = meResource.getMyVisitorById(secPersonel1, 101L);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        VisitorResponse v = (VisitorResponse) res.getEntity();
        Assertions.assertEquals(101L, v.id());
        Assertions.assertEquals("Ziyaretçi 1", v.fullName());
    }

    @Test
    void testRecentVisitorsLimitValidation() {
        SecurityContext secPersonel1 = createSecurityContext("personel1", "PERSONNEL");

        Response res = meResource.getMyRecentVisitors(secPersonel1, 1);
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        @SuppressWarnings("unchecked")
        List<VisitorResponse> list = (List<VisitorResponse>) res.getEntity();
        Assertions.assertEquals(1, list.size());
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
