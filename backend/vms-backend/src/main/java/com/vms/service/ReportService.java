package com.vms.service;

import com.vms.dto.report.DepartmentReportResponse;
import com.vms.dto.report.SummaryResponse;
import com.vms.dto.report.TopPersonnelResponse;
import com.vms.dto.report.WeeklyReportResponse;
import com.vms.entity.Visitor;
import com.vms.repository.VisitorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ReportService {

    private final VisitorRepository visitorRepository;
    private final EntityManager entityManager;

    @Inject
    public ReportService(VisitorRepository visitorRepository, EntityManager entityManager) {
        this.visitorRepository = visitorRepository;
        this.entityManager = entityManager;
    }

    public SummaryResponse getSummary(LocalDate startDate, LocalDate endDate) {
        long totalVisitors;
        if (startDate != null || endDate != null) {
            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
            totalVisitors = visitorRepository.count("entryTime >= ?1 and entryTime <= ?2", startDateTime, endDateTime);
        } else {
            totalVisitors = visitorRepository.count();
        }

        // Real-time metrics (NEVER filtered by date range)
        long activeVisitors = visitorRepository.countActiveVisitors();
        LocalDate today = LocalDate.now();
        long todayVisitors = visitorRepository.count("entryTime >= ?1 and entryTime <= ?2", today.atStartOfDay(), today.atTime(LocalTime.MAX));

        // Average stay duration filtered by date range
        List<Visitor> finishedVisitors;
        if (startDate != null || endDate != null) {
            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
            finishedVisitors = visitorRepository.list("exitTime is not null and entryTime >= ?1 and entryTime <= ?2", startDateTime, endDateTime);
        } else {
            finishedVisitors = visitorRepository.list("exitTime is not null");
        }

        long averageStayMinutes = 0;
        if (!finishedVisitors.isEmpty()) {
            double avg = finishedVisitors.stream()
                    .mapToLong(v -> Duration.between(v.getEntryTime(), v.getExitTime()).toMinutes())
                    .average()
                    .orElse(0.0);
            averageStayMinutes = Math.round(avg);
        }

        return new SummaryResponse(totalVisitors, todayVisitors, activeVisitors, averageStayMinutes);
    }

    public List<WeeklyReportResponse> getWeeklyReport(LocalDate startDate, LocalDate endDate) {
        List<WeeklyReportResponse> list = new ArrayList<>();

        if (startDate != null && endDate != null) {
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            for (long i = 0; i <= days; i++) {
                LocalDate date = startDate.plusDays(i);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.atTime(LocalTime.MAX);

                long count = visitorRepository.count("entryTime >= ?1 and entryTime <= ?2", start, end);
                String dayName = getTurkishDayName(date.getDayOfWeek());

                list.add(new WeeklyReportResponse(date.toString(), dayName, count));
            }
            return list;
        }

        // Default: last 7 days
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            long count = visitorRepository.count("entryTime >= ?1 and entryTime <= ?2", start, end);
            String dayName = getTurkishDayName(date.getDayOfWeek());

            list.add(new WeeklyReportResponse(date.toString(), dayName, count));
        }

        return list;
    }

    public List<TopPersonnelResponse> getTopPersonnel(LocalDate startDate, LocalDate endDate) {
        StringBuilder jpql = new StringBuilder(
            "select v.host.id, v.host.fullName, v.host.department, count(v.id) " +
            "from Visitor v where v.host is not null "
        );

        boolean hasDates = startDate != null || endDate != null;
        if (hasDates) {
            jpql.append("and v.entryTime >= :start and v.entryTime <= :end ");
        }

        jpql.append("group by v.host.id, v.host.fullName, v.host.department ");
        jpql.append("order by count(v.id) desc");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        if (hasDates) {
            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
            query.setParameter("start", startDateTime);
            query.setParameter("end", endDateTime);
        }

        query.setMaxResults(5);
        List<Object[]> rows = query.getResultList();

        List<TopPersonnelResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long hostId = (Long) row[0];
            String fullName = (String) row[1];
            String department = (String) row[2];
            long count = (Long) row[3];
            result.add(new TopPersonnelResponse(hostId, fullName, department, count));
        }
        return result;
    }

    public List<DepartmentReportResponse> getDepartmentReport(LocalDate startDate, LocalDate endDate) {
        StringBuilder jpql = new StringBuilder(
            "select v.host.department, count(v.id) " +
            "from Visitor v where v.host is not null and v.host.department is not null "
        );

        boolean hasDates = startDate != null || endDate != null;
        if (hasDates) {
            jpql.append("and v.entryTime >= :start and v.entryTime <= :end ");
        }

        jpql.append("group by v.host.department ");
        jpql.append("order by count(v.id) desc");

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        if (hasDates) {
            LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
            query.setParameter("start", startDateTime);
            query.setParameter("end", endDateTime);
        }

        List<Object[]> rows = query.getResultList();
        long totalDeptVisits = rows.stream().mapToLong(row -> (Long) row[1]).sum();

        List<DepartmentReportResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            String department = (String) row[0];
            long count = (Long) row[1];
            double percentage = totalDeptVisits == 0 ? 0.0 : Math.round((double) count / totalDeptVisits * 1000.0) / 10.0;
            result.add(new DepartmentReportResponse(department, count, percentage));
        }
        return result;
    }

    private String getTurkishDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Pazartesi";
            case TUESDAY -> "Salı";
            case WEDNESDAY -> "Çarşamba";
            case THURSDAY -> "Perşembe";
            case FRIDAY -> "Cuma";
            case SATURDAY -> "Cumartesi";
            case SUNDAY -> "Pazar";
        };
    }
}
