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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

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
    public Response getAllVisitors() {
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
}
