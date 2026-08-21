package com.vms.resource;

import com.vms.dto.dashboard.AdminDashboardSummaryResponse;
import com.vms.entity.Personnel;
import com.vms.entity.Visitor;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.VisitorRepository;
import com.vms.service.DashboardService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DashboardResourceTest {

    private List<Visitor> dbVisitors;
    private List<Personnel> dbPersonnel;
    private DashboardService dashboardService;
    private DashboardResource dashboardResource;

    @BeforeEach
    void setUp() {
        dbVisitors = new ArrayList<>();
        dbPersonnel = new ArrayList<>();

        Personnel p1 = new Personnel("Ahmet Yılmaz", "IT", "Yazılım Uzmanı", "ahmet@test.com");
        p1.setId(1L);
        Personnel p2 = new Personnel("Ayşe Kaya", "İnsan Kaynakları", "IK Yöneticisi", "ayse@test.com");
        p2.setId(2L);
        dbPersonnel.add(p1);
        dbPersonnel.add(p2);

        Visitor v1 = new Visitor("Mehmet Demir", p1, LocalDateTime.now().minusMinutes(30), null, true);
        v1.setId(101L);

        Visitor v2 = new Visitor("Canan Şahin", p1, LocalDateTime.now().minusHours(2), LocalDateTime.now().minusMinutes(10), false);
        v2.setId(102L);

        dbVisitors.add(v1);
        dbVisitors.add(v2);

        VisitorRepository stubVisitorRepo = new VisitorRepository() {
            @Override
            public long count() {
                return dbVisitors.size();
            }

            @Override
            public long count(String query, Object... params) {
                if (query.contains("entryTime >=")) {
                    return dbVisitors.stream().filter(v -> v.getEntryTime() != null).count();
                }
                if (query.contains("exitTime is not null")) {
                    return dbVisitors.stream().filter(v -> v.getExitTime() != null).count();
                }
                return dbVisitors.size();
            }

            @Override
            public long countActiveVisitors() {
                return dbVisitors.stream().filter(v -> Boolean.TRUE.equals(v.getIsInside())).count();
            }

            @Override
            public List<Visitor> list(String query, Object... params) {
                return new ArrayList<>(dbVisitors);
            }
        };

        PersonnelRepository stubPersonnelRepo = new PersonnelRepository() {
            @Override
            public long count() {
                return dbPersonnel.size();
            }
        };

        EntityManager stubEntityManager = createEntityManagerProxy(dbVisitors);

        dashboardService = new DashboardService(stubVisitorRepo, stubPersonnelRepo, stubEntityManager);
        dashboardResource = new DashboardResource(dashboardService);
    }

    @Test
    void testGetAdminDashboardSummaryReturnsValidData() {
        Response res = dashboardResource.getAdminDashboardSummary("30d");
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        AdminDashboardSummaryResponse body = (AdminDashboardSummaryResponse) res.getEntity();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(2, body.todaysTotalVisitors());
        Assertions.assertEquals(1, body.currentlyInside());
        Assertions.assertEquals(1, body.completedToday());
        Assertions.assertEquals(2, body.totalPersonnel());
        Assertions.assertEquals(2, body.totalVisitsAllTime());
        Assertions.assertEquals("30d", body.activeRange());
        Assertions.assertNotNull(body.topPersonnel());
        Assertions.assertFalse(body.topPersonnel().isEmpty());
        Assertions.assertEquals("Ahmet Yılmaz", body.topPersonnel().get(0).fullName());
        Assertions.assertEquals(2, body.topPersonnel().get(0).visitCount());
        Assertions.assertNotNull(body.departmentDistribution());
        Assertions.assertNotNull(body.recentActivities());
        Assertions.assertTrue(body.recentActivities().size() > 0);
    }

    @Test
    void testGetAdminDashboardSummaryWithDifferentRanges() {
        Response res7d = dashboardResource.getAdminDashboardSummary("7d");
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), res7d.getStatus());
        AdminDashboardSummaryResponse body7d = (AdminDashboardSummaryResponse) res7d.getEntity();
        Assertions.assertEquals("7d", body7d.activeRange());

        Response resToday = dashboardResource.getAdminDashboardSummary("today");
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), resToday.getStatus());
        AdminDashboardSummaryResponse bodyToday = (AdminDashboardSummaryResponse) resToday.getEntity();
        Assertions.assertEquals("today", bodyToday.activeRange());

        Response resInvalid = dashboardResource.getAdminDashboardSummary("invalid_value");
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), resInvalid.getStatus());
        AdminDashboardSummaryResponse bodyInvalid = (AdminDashboardSummaryResponse) resInvalid.getEntity();
        Assertions.assertEquals("30d", bodyInvalid.activeRange());
    }

    @Test
    void testEmptyDataHandledWithoutExceptions() {
        dbVisitors.clear();
        dbPersonnel.clear();

        Response res = dashboardResource.getAdminDashboardSummary("30d");
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());

        AdminDashboardSummaryResponse body = (AdminDashboardSummaryResponse) res.getEntity();
        Assertions.assertEquals(0, body.todaysTotalVisitors());
        Assertions.assertEquals(0, body.currentlyInside());
        Assertions.assertEquals(0, body.completedToday());
        Assertions.assertEquals(0, body.totalPersonnel());
        Assertions.assertEquals(0, body.totalVisitsAllTime());
        Assertions.assertTrue(body.topPersonnel().isEmpty());
        Assertions.assertTrue(body.departmentDistribution().isEmpty());
        Assertions.assertTrue(body.recentActivities().isEmpty());
    }

    private static EntityManager createEntityManagerProxy(List<Visitor> visitors) {
        return (EntityManager) Proxy.newProxyInstance(
            EntityManager.class.getClassLoader(),
            new Class<?>[]{EntityManager.class},
            (proxy, method, args) -> {
                if ("createQuery".equals(method.getName())) {
                    String qlString = (String) args[0];
                    return createTypedQueryProxy(visitors, qlString);
                }
                return null;
            }
        );
    }

    private static TypedQuery<?> createTypedQueryProxy(List<Visitor> visitors, String qlString) {
        return (TypedQuery<?>) Proxy.newProxyInstance(
            TypedQuery.class.getClassLoader(),
            new Class<?>[]{TypedQuery.class},
            (proxy, method, args) -> {
                if ("getResultList".equals(method.getName())) {
                    if (visitors.isEmpty()) {
                        return new ArrayList<>();
                    }
                    if (qlString.contains("v.host.id")) {
                        List<Object[]> rows = new ArrayList<>();
                        rows.add(new Object[]{1L, "Ahmet Yılmaz", "IT", 2L});
                        return rows;
                    }
                    if (qlString.contains("v.host.department")) {
                        List<Object[]> rows = new ArrayList<>();
                        rows.add(new Object[]{"IT", 2L});
                        return rows;
                    }
                    return new ArrayList<>();
                }
                if ("setMaxResults".equals(method.getName()) || "setParameter".equals(method.getName())) {
                    return proxy;
                }
                return null;
            }
        );
    }
}
