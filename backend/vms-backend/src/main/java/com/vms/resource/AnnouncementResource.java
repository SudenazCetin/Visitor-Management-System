package com.vms.resource;

import com.vms.dto.announcement.AnnouncementRequest;
import com.vms.dto.announcement.AnnouncementResponse;
import com.vms.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Map;

@Path("/api/announcements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnnouncementResource {

    private final NotificationService notificationService;

    @Inject
    public AnnouncementResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @POST
    public Response createAnnouncement(@Context SecurityContext sec, @Valid AnnouncementRequest request) {
        if (sec == null || sec.getUserPrincipal() == null || !sec.isUserInRole("ADMIN")) {
            throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "Duyuru göndermek için ADMIN yetkisi gereklidir."))
                    .build()
            );
        }

        long recipientCount = notificationService.sendAnnouncement(request.title(), request.message(), request.target());

        AnnouncementResponse response = new AnnouncementResponse(
            "Duyuru başarıyla gönderildi.",
            recipientCount
        );

        return Response.status(Response.Status.CREATED).entity(response).build();
    }
}
