package com.vms.resource;

import com.vms.dto.dashboard.AdminDashboardSummaryResponse;
import com.vms.service.DashboardService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DashboardResource {

    private final DashboardService dashboardService;

    @Inject
    public DashboardResource(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GET
    @Path("/admin-summary")
    @RolesAllowed("ADMIN")
    public Response getAdminDashboardSummary(@QueryParam("range") @DefaultValue("30d") String range) {
        AdminDashboardSummaryResponse summary = dashboardService.getAdminDashboardSummary(range);
        return Response.ok(summary).build();
    }
}
