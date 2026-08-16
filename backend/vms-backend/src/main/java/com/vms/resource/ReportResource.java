package com.vms.resource;

import com.vms.dto.report.DepartmentReportResponse;
import com.vms.dto.report.SummaryResponse;
import com.vms.dto.report.TopPersonnelResponse;
import com.vms.dto.report.WeeklyReportResponse;
import com.vms.service.ReportService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
public class ReportResource {

    private final ReportService reportService;

    @Inject
    public ReportResource(ReportService reportService) {
        this.reportService = reportService;
    }

    @GET
    @Path("/summary")
    public Response getSummary(
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr) {
        LocalDate start = parseDate(startDateStr, "startDate");
        LocalDate end = parseDate(endDateStr, "endDate");
        validateDates(start, end);

        SummaryResponse summary = reportService.getSummary(start, end);
        return Response.ok(summary).build();
    }

    @GET
    @Path("/weekly")
    public Response getWeeklyReport(
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr) {
        LocalDate start = parseDate(startDateStr, "startDate");
        LocalDate end = parseDate(endDateStr, "endDate");
        validateDates(start, end);

        List<WeeklyReportResponse> weekly = reportService.getWeeklyReport(start, end);
        return Response.ok(weekly).build();
    }

    @GET
    @Path("/top-personnel")
    public Response getTopPersonnel(
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr) {
        LocalDate start = parseDate(startDateStr, "startDate");
        LocalDate end = parseDate(endDateStr, "endDate");
        validateDates(start, end);

        List<TopPersonnelResponse> topPersonnel = reportService.getTopPersonnel(start, end);
        return Response.ok(topPersonnel).build();
    }

    @GET
    @Path("/by-department")
    public Response getDepartmentReport(
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr) {
        LocalDate start = parseDate(startDateStr, "startDate");
        LocalDate end = parseDate(endDateStr, "endDate");
        validateDates(start, end);

        List<DepartmentReportResponse> byDepartment = reportService.getDepartmentReport(start, end);
        return Response.ok(byDepartment).build();
    }

    private LocalDate parseDate(String dateStr, String paramName) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", paramName + " için geçersiz tarih formatı (YYYY-MM-DD olmalıdır)"))
                    .build()
            );
        }
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Başlangıç tarihi bitiş tarihinden sonra olamaz"))
                    .build()
            );
        }
    }
}
