package com.vms.resource;

import com.vms.dto.notification.NotificationResponse;
import com.vms.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private final NotificationService notificationService;

    @Inject
    public NotificationResource(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GET
    public Response getMyNotifications(@Context SecurityContext sec) {
        String username = getAuthenticatedUsername(sec);
        List<NotificationResponse> notifications = notificationService.getUserNotifications(username);
        return Response.ok(notifications).build();
    }

    @GET
    @Path("/unread-count")
    public Response getUnreadCount(@Context SecurityContext sec) {
        String username = getAuthenticatedUsername(sec);
        long count = notificationService.getUnreadCount(username);
        return Response.ok(Map.of("unreadCount", count)).build();
    }

    @PUT
    @Path("/{id}/read")
    public Response markAsRead(@Context SecurityContext sec, @PathParam("id") Long id) {
        String username = getAuthenticatedUsername(sec);
        try {
            NotificationResponse updated = notificationService.markAsRead(id, username);
            if (updated == null) {
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", "Bildirim bulunamadı.")).build();
            }
            return Response.ok(updated).build();
        } catch (SecurityException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", e.getMessage()))
                    .build()
            );
        }
    }

    @PUT
    @Path("/read-all")
    public Response markAllAsRead(@Context SecurityContext sec) {
        String username = getAuthenticatedUsername(sec);
        long updatedCount = notificationService.markAllAsRead(username);
        return Response.ok(Map.of("message", "Tüm bildirimler okundu olarak işaretlendi.", "updatedCount", updatedCount)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteNotification(@Context SecurityContext sec, @PathParam("id") Long id) {
        String username = getAuthenticatedUsername(sec);
        try {
            boolean deleted = notificationService.deleteNotification(id, username);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("message", "Bildirim bulunamadı.")).build();
            }
            return Response.noContent().build();
        } catch (SecurityException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", e.getMessage()))
                    .build()
            );
        }
    }

    @DELETE
    @Path("/read")
    public Response deleteAllRead(@Context SecurityContext sec) {
        String username = getAuthenticatedUsername(sec);
        long deletedCount = notificationService.deleteAllRead(username);
        return Response.ok(Map.of("message", "Okunmuş bildirimler temizlendi.", "deletedCount", deletedCount)).build();
    }

    private String getAuthenticatedUsername(SecurityContext sec) {
        if (sec == null || sec.getUserPrincipal() == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Oturum açmanız gerekiyor."))
                    .build()
            );
        }
        return sec.getUserPrincipal().getName();
    }
}
