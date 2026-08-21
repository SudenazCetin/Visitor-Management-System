package com.vms.resource;

import com.vms.dto.personnel.PersonnelRequest;
import com.vms.dto.personnel.PersonnelResponse;
import com.vms.entity.Personnel;
import com.vms.service.PersonnelService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;

@Path("/api/personnel")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonnelResource {

    private final PersonnelService personnelService;

    @Inject
    public PersonnelResource(PersonnelService personnelService) {
        this.personnelService = personnelService;
    }

    @GET
    public Response getAllPersonnel(@QueryParam("department") String department) {
        List<Personnel> personnelList;
        if (department != null && !department.isBlank()) {
            personnelList = personnelService.getPersonnelByDepartment(department);
        } else {
            personnelList = personnelService.getAllPersonnel();
        }

        List<PersonnelResponse> responseList = personnelList.stream()
                .map(this::toResponse)
                .toList();

        return Response.ok(responseList).build();
    }

    @GET
    @Path("/{id}")
    public Response getPersonnelById(@PathParam("id") Long id) {
        return personnelService.getPersonnelById(id)
                .map(this::toResponse)
                .map(response -> Response.ok(response).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    public Response createPersonnel(@Context SecurityContext sec, @Valid PersonnelRequest request) {
        checkNotPersonnel(sec);
        Personnel saved = personnelService.createPersonnel(request);
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @PUT
    @Path("/{id}")
    public Response updatePersonnel(@Context SecurityContext sec, @PathParam("id") Long id, @Valid PersonnelRequest request) {
        checkNotPersonnel(sec);
        Personnel updateData = toEntity(request);
        Personnel updated = personnelService.updatePersonnel(id, updateData);
        return Response.ok(toResponse(updated)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletePersonnel(@Context SecurityContext sec, @PathParam("id") Long id) {
        checkNotPersonnel(sec);
        boolean deleted = personnelService.deletePersonnel(id);
        if (deleted) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private void checkNotPersonnel(SecurityContext sec) {
        if (sec != null && sec.isUserInRole("PERSONNEL")) {
            throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "Personnel rolündeki kullanıcılar personel yönetimi yapamaz."))
                    .build()
            );
        }
    }

    private PersonnelResponse toResponse(Personnel p) {
        boolean hasAccount = p.getUser() != null;
        String username = p.getUser() != null ? p.getUser().getUsername() : null;
        return new PersonnelResponse(
                p.getId(),
                p.getFullName(),
                p.getDepartment(),
                p.getTitle(),
                p.getEmail(),
                hasAccount,
                username
        );
    }

    private Personnel toEntity(PersonnelRequest req) {
        return new Personnel(
                req.fullName(),
                req.department(),
                req.title(),
                req.email()
        );
    }
}
