package com.vms.resource;

import com.vms.dto.user.ChangePasswordRequest;
import com.vms.dto.visitor.VisitorResponse;
import com.vms.entity.Personnel;
import com.vms.entity.User;
import com.vms.entity.Visitor;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.UserRepository;
import com.vms.repository.VisitorRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeResource {

    private final PersonnelRepository personnelRepository;
    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final com.vms.service.NotificationService notificationService;

    @Inject
    public MeResource(PersonnelRepository personnelRepository,
                      VisitorRepository visitorRepository,
                      UserRepository userRepository,
                      com.vms.service.NotificationService notificationService) {
        this.personnelRepository = personnelRepository;
        this.visitorRepository = visitorRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @GET
    @Path("/profile")
    public Response getProfile(@Context SecurityContext sec) {
        Personnel personnel = getAuthenticatedPersonnel(sec);
        User user = personnel.getUser();

        Map<String, Object> profile = Map.of(
            "id", personnel.getId(),
            "fullName", personnel.getFullName(),
            "department", personnel.getDepartment(),
            "title", personnel.getTitle() != null ? personnel.getTitle() : "",
            "email", personnel.getEmail(),
            "username", user != null ? user.getUsername() : "",
            "role", user != null ? user.getRole().name() : ""
        );

        return Response.ok(profile).build();
    }

    @PUT
    @Path("/profile")
    @Transactional
    public Response updateProfile(@Context SecurityContext sec, Map<String, String> request) {
        Personnel personnel = getAuthenticatedPersonnel(sec);
        if (request.containsKey("fullName") && request.get("fullName") != null && !request.get("fullName").isBlank()) {
            personnel.setFullName(request.get("fullName").trim());
        }
        if (request.containsKey("department") && request.get("department") != null && !request.get("department").isBlank()) {
            personnel.setDepartment(request.get("department").trim());
        }
        if (request.containsKey("title") && request.get("title") != null) {
            personnel.setTitle(request.get("title").trim());
        }
        if (request.containsKey("email") && request.get("email") != null && !request.get("email").isBlank()) {
            personnel.setEmail(request.get("email").trim());
        }

        User user = personnel.getUser();
        if (user != null) {
            try {
                notificationService.sendProfileUpdatedNotification(user);
            } catch (Exception e) {
                org.jboss.logging.Logger.getLogger(MeResource.class).warnf(e, "Profile update notification failed for user %s", user.getUsername());
            }
        }

        return Response.ok(Map.of("message", "Profil bilgileriniz başarıyla güncellendi.")).build();
    }

    @GET
    @Path("/visitors")
    public Response getMyVisitors(@Context SecurityContext sec) {
        Personnel personnel = getAuthenticatedPersonnel(sec);
        List<VisitorResponse> visitors = visitorRepository.findByHostId(personnel.getId()).stream()
                .map(this::toVisitorResponse)
                .toList();

        return Response.ok(visitors).build();
    }

    @GET
    @Path("/visitors/active")
    public Response getMyActiveVisitors(@Context SecurityContext sec) {
        Personnel personnel = getAuthenticatedPersonnel(sec);
        List<VisitorResponse> activeVisitors = visitorRepository.findByHostId(personnel.getId()).stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsInside()))
                .map(this::toVisitorResponse)
                .toList();

        return Response.ok(activeVisitors).build();
    }

    @GET
    @Path("/visitors/recent")
    public Response getMyRecentVisitors(@Context SecurityContext sec, @QueryParam("limit") @DefaultValue("5") int limit) {
        int validatedLimit = Math.max(1, Math.min(limit, 50));
        Personnel personnel = getAuthenticatedPersonnel(sec);
        List<VisitorResponse> recentVisitors = visitorRepository.findByHostId(personnel.getId()).stream()
                .sorted(Comparator.comparing(Visitor::getEntryTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(validatedLimit)
                .map(this::toVisitorResponse)
                .toList();

        return Response.ok(recentVisitors).build();
    }

    @GET
    @Path("/visitors/{id}")
    public Response getMyVisitorById(@Context SecurityContext sec, @PathParam("id") Long id) {
        Personnel personnel = getAuthenticatedPersonnel(sec);
        Visitor visitor = visitorRepository.findByIdOptional(id)
                .orElseThrow(() -> new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Ziyaretçi bulunamadı."))
                        .build()
                ));

        if (visitor.getHost() == null || !visitor.getHost().getId().equals(personnel.getId())) {
            throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("message", "Bu ziyaretçi kaydına erişim yetkiniz bulunmamaktadır."))
                    .build()
            );
        }

        return Response.ok(toVisitorResponse(visitor)).build();
    }

    @GET
    @Path("/summary")
    public Response getMySummary(@Context SecurityContext sec) {
        Personnel personnel = getAuthenticatedPersonnel(sec);
        List<Visitor> allMyVisitors = visitorRepository.findByHostId(personnel.getId());

        long totalVisitors = allMyVisitors.size();
        long activeVisitors = allMyVisitors.stream().filter(v -> Boolean.TRUE.equals(v.getIsInside())).count();

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        long todayVisitors = allMyVisitors.stream()
                .filter(v -> v.getEntryTime() != null &&
                             !v.getEntryTime().isBefore(todayStart) &&
                             !v.getEntryTime().isAfter(todayEnd))
                .count();

        long completedToday = allMyVisitors.stream()
                .filter(v -> v.getExitTime() != null &&
                             !v.getExitTime().isBefore(todayStart) &&
                             !v.getExitTime().isAfter(todayEnd))
                .count();

        LocalDateTime lastVisitEntry = allMyVisitors.stream()
                .map(Visitor::getEntryTime)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        Map<String, Object> summary = Map.of(
            "totalVisitors", totalVisitors,
            "activeVisitors", activeVisitors,
            "todayVisitors", todayVisitors,
            "completedToday", completedToday,
            "lastVisitTime", lastVisitEntry != null ? lastVisitEntry.toString() : ""
        );

        return Response.ok(summary).build();
    }

    @PUT
    @Path("/change-password")
    @Transactional
    public Response changePassword(@Context SecurityContext sec, @Valid ChangePasswordRequest req) {
        String username = sec.getUserPrincipal() != null ? sec.getUserPrincipal().getName() : null;
        if (username == null || username.isBlank()) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Oturum açmanız gerekiyor."))
                    .build()
            );
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Kullanıcı bulunamadı."))
                        .build()
                ));

        boolean passwordMatches = false;
        if (user.getPassword() != null && (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"))) {
            passwordMatches = BcryptUtil.matches(req.currentPassword(), user.getPassword());
        } else {
            passwordMatches = user.getPassword() != null && user.getPassword().equals(req.currentPassword());
        }

        if (!passwordMatches) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Mevcut şifreniz yanlış."))
                    .build()
            );
        }

        String newHashedPassword = BcryptUtil.bcryptHash(req.newPassword().trim());
        user.setPassword(newHashedPassword);

        try {
            notificationService.sendPasswordChangedNotification(user);
        } catch (Exception e) {
            org.jboss.logging.Logger.getLogger(MeResource.class).warnf(e, "Password change notification failed for user %s", username);
        }

        return Response.ok(Map.of("message", "Şifreniz başarıyla değiştirildi.")).build();
    }

    private Personnel getAuthenticatedPersonnel(SecurityContext sec) {
        if (sec.getUserPrincipal() == null) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Oturum açmanız gerekiyor."))
                    .build()
            );
        }

        String username = sec.getUserPrincipal().getName();
        return personnelRepository.findByUserUsername(username)
                .orElseThrow(() -> new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Giriş yapan kullanıcıya bağlı personel profil kaydı bulunamadı."))
                        .build()
                ));
    }

    private VisitorResponse toVisitorResponse(Visitor v) {
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
