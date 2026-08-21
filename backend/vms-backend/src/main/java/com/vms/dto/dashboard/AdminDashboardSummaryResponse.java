package com.vms.dto.dashboard;

import java.util.List;

public record AdminDashboardSummaryResponse(
    long todaysTotalVisitors,
    long currentlyInside,
    long completedToday,
    long totalPersonnel,
    long totalVisitsAllTime,
    String activeRange,
    List<TopPersonnelDto> topPersonnel,
    List<DepartmentVisitDto> departmentDistribution,
    List<RecentActivityDto> recentActivities
) {}
