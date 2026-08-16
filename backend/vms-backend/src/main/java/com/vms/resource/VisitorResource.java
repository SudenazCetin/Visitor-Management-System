package com.vms.resource;

import com.vms.dto.visitor.VisitorCheckInRequest;
import com.vms.dto.visitor.VisitorResponse;
import com.vms.entity.Visitor;
import com.vms.service.VisitorService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Path("/api/visitors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VisitorResource {

    private final VisitorService visitorService;

    @Inject
    public VisitorResource(VisitorService visitorService) {
        this.visitorService = visitorService;
    }

    @GET
    public Response getAllVisitors(
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr) {

        if ((startDateStr != null && !startDateStr.isBlank()) || (endDateStr != null && !endDateStr.isBlank())) {
            LocalDate start = parseDate(startDateStr, "startDate");
            LocalDate end = parseDate(endDateStr, "endDate");
            validateDates(start, end);

            LocalDateTime startDateTime = start != null ? start.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime endDateTime = end != null ? end.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59, 59);

            List<VisitorResponse> visitors = visitorService.getVisitorsByDateRange(startDateTime, endDateTime).stream()
                    .map(this::toResponse)
                    .toList();
            return Response.ok(visitors).build();
        }

        List<VisitorResponse> visitors = visitorService.getAllVisitors().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(visitors).build();
    }

    @GET
    @Path("/active")
    public Response getActiveVisitors() {
        List<VisitorResponse> activeVisitors = visitorService.getActiveVisitors().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(activeVisitors).build();
    }

    @GET
    @Path("/{id}")
    public Response getVisitorById(@PathParam("id") Long id) {
        return visitorService.getVisitorById(id)
                .map(this::toResponse)
                .map(response -> Response.ok(response).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/host/{hostId}")
    public Response getVisitorsByHostId(@PathParam("hostId") Long hostId) {
        List<VisitorResponse> visitors = visitorService.getVisitorsByHostId(hostId).stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(visitors).build();
    }

    @POST
    @Path("/check-in")
    public Response checkIn(@Valid VisitorCheckInRequest request) {
        Visitor visitor = visitorService.checkIn(request.fullName(), request.hostId());
        return Response.status(Response.Status.CREATED).entity(toResponse(visitor)).build();
    }

    @PUT
    @Path("/{id}/check-out")
    public Response checkOut(@PathParam("id") Long id) {
        Visitor visitor = visitorService.checkOut(id);
        return Response.ok(toResponse(visitor)).build();
    }

    private VisitorResponse toResponse(Visitor v) {
        return new VisitorResponse(
                v.getId(),
                v.getFullName(),
                v.getHost() != null ? v.getHost().getId() : null,
                v.getHost() != null ? v.getHost().getFullName() : null,
                v.getHost() != null ? v.getHost().getDepartment() : null,
                v.getEntryTime(),
                v.getExitTime(),
                v.getIsInside()
        );
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
