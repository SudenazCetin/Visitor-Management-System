package com.vms.service;

import com.vms.dto.dashboard.AdminDashboardSummaryResponse;
import com.vms.dto.dashboard.DepartmentVisitDto;
import com.vms.dto.dashboard.RecentActivityDto;
import com.vms.dto.dashboard.TopPersonnelDto;
import com.vms.entity.Visitor;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.VisitorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class DashboardService {

    private final VisitorRepository visitorRepository;
    private final PersonnelRepository personnelRepository;
    private final EntityManager entityManager;

    @Inject
    public DashboardService(VisitorRepository visitorRepository, PersonnelRepository personnelRepository, EntityManager entityManager) {
        this.visitorRepository = visitorRepository;
        this.personnelRepository = personnelRepository;
        this.entityManager = entityManager;
    }

    public AdminDashboardSummaryResponse getAdminDashboardSummary(String rangeParam) {
        String range = sanitizeRange(rangeParam);
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        LocalDateTime rangeStart = calculateRangeStart(range, today);
        LocalDateTime rangeEnd = todayEnd;

        // 1. Today's Total Visitors
        long todaysTotalVisitors = visitorRepository.count("entryTime >= ?1 and entryTime <= ?2", todayStart, todayEnd);

        // 2. Currently Inside
        long currentlyInside = visitorRepository.countActiveVisitors();

        // 3. Completed Today (check-out completed today)
        long completedToday = visitorRepository.count("exitTime is not null and exitTime >= ?1 and exitTime <= ?2", todayStart, todayEnd);

        // 4. Total Personnel
        long totalPersonnel = personnelRepository.count();

        // 5. Total Visits All-Time
        long totalVisitsAllTime = visitorRepository.count();

        // 6. Top 5 Most Visited Personnel (filtered by date range)
        List<TopPersonnelDto> topPersonnel = getTopVisitedPersonnel(rangeStart, rangeEnd, 5);

        // 7. Department Visit Distribution (filtered by date range)
        List<DepartmentVisitDto> departmentDistribution = getDepartmentVisitDistribution(rangeStart, rangeEnd);

        // 8. Recent Operational Activities
        List<RecentActivityDto> recentActivities = getRecentActivities(10);

        return new AdminDashboardSummaryResponse(
            todaysTotalVisitors,
            currentlyInside,
            completedToday,
            totalPersonnel,
            totalVisitsAllTime,
            range,
            topPersonnel,
            departmentDistribution,
            recentActivities
        );
    }

    private String sanitizeRange(String rangeParam) {
        if (rangeParam == null) return "30d";
        String r = rangeParam.trim().toLowerCase();
        if (r.equals("today") || r.equals("7d") || r.equals("30d") || r.equals("all")) {
            return r;
        }
        return "30d";
    }

    private LocalDateTime calculateRangeStart(String range, LocalDate today) {
        return switch (range) {
            case "today" -> today.atStartOfDay();
            case "7d" -> today.minusDays(7).atStartOfDay();
            case "30d" -> today.minusDays(30).atStartOfDay();
            default -> LocalDateTime.of(1970, 1, 1, 0, 0);
        };
    }

    private List<TopPersonnelDto> getTopVisitedPersonnel(LocalDateTime start, LocalDateTime end, int maxResults) {
        String jpql = "select v.host.id, v.host.fullName, v.host.department, count(v.id) " +
                      "from Visitor v where v.host is not null and v.entryTime >= :start and v.entryTime <= :end " +
                      "group by v.host.id, v.host.fullName, v.host.department " +
                      "order by count(v.id) desc";

        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        query.setMaxResults(maxResults);
        List<Object[]> rows = query.getResultList();

        List<TopPersonnelDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long hostId = (Long) row[0];
            String fullName = (String) row[1];
            String department = (String) row[2];
            long count = (Long) row[3];
            if (count > 0) {
                result.add(new TopPersonnelDto(hostId, fullName, department, count));
            }
        }
        return result;
    }

    private List<DepartmentVisitDto> getDepartmentVisitDistribution(LocalDateTime start, LocalDateTime end) {
        String jpql = "select v.host.department, count(v.id) " +
                      "from Visitor v where v.host is not null and v.host.department is not null and v.entryTime >= :start and v.entryTime <= :end " +
                      "group by v.host.department " +
                      "order by count(v.id) desc";

        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
        query.setParameter("start", start);
        query.setParameter("end", end);
        List<Object[]> rows = query.getResultList();

        long totalDeptVisits = rows.stream().mapToLong(row -> (Long) row[1]).sum();

        List<DepartmentVisitDto> result = new ArrayList<>();
        for (Object[] row : rows) {
            String department = (String) row[0];
            long count = (Long) row[1];
            double percentage = totalDeptVisits == 0 ? 0.0 : Math.round((double) count / totalDeptVisits * 1000.0) / 10.0;
            result.add(new DepartmentVisitDto(department, count, percentage));
        }
        return result;
    }

    private List<RecentActivityDto> getRecentActivities(int limit) {
        List<Visitor> recentVisitors = visitorRepository.list("select v from Visitor v left join fetch v.host order by v.entryTime desc");

        List<RecentActivityDto> activities = new ArrayList<>();
        for (Visitor v : recentVisitors) {
            String hostName = v.getHost() != null ? v.getHost().getFullName() : "Genel";
            String department = v.getHost() != null ? v.getHost().getDepartment() : "Danışma";

            // CHECK_IN Activity
            if (v.getEntryTime() != null) {
                activities.add(new RecentActivityDto(
                    v.getId(),
                    "CHECK_IN",
                    v.getFullName(),
                    hostName,
                    department,
                    v.getEntryTime(),
                    v.getFullName() + " binaya giriş kaydı oluşturuldu."
                ));
            }

            // CHECK_OUT Activity
            if (v.getExitTime() != null) {
                activities.add(new RecentActivityDto(
                    v.getId(),
                    "CHECK_OUT",
                    v.getFullName(),
                    hostName,
                    department,
                    v.getExitTime(),
                    v.getFullName() + " binadan çıkış yaptı."
                ));
            }
        }

        // Sort chronologically descending
        activities.sort(Comparator.comparing(RecentActivityDto::timestamp).reversed());

        if (activities.size() > limit) {
            return activities.subList(0, limit);
        }
        return activities;
    }
}
