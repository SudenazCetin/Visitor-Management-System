package com.vms.service;

import com.vms.dto.notification.NotificationResponse;
import com.vms.entity.Notification;
import com.vms.entity.User;
import com.vms.entity.Visitor;
import com.vms.enums.NotificationStatus;
import com.vms.enums.SocketCategory;
import com.vms.enums.SocketEvent;
import com.vms.enums.SocketType;
import com.vms.repository.NotificationRepository;
import com.vms.websocket.SocketMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SocketService socketService;
    private final com.vms.repository.UserRepository userRepository;

    @Inject
    public NotificationService(NotificationRepository notificationRepository,
                               SocketService socketService,
                               com.vms.repository.UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.socketService = socketService;
        this.userRepository = userRepository;
    }

    public NotificationService(NotificationRepository notificationRepository,
                               SocketService socketService) {
        this.notificationRepository = notificationRepository;
        this.socketService = socketService;
        this.userRepository = null;
    }

    @Transactional
    public NotificationResponse createAndSendNotification(User recipient,
                                                          SocketEvent event,
                                                          SocketCategory category,
                                                          SocketType type,
                                                          String title,
                                                          String message,
                                                          String actionUrl,
                                                          String targetEntity,
                                                          Long targetEntityId) {
        return createAndSendNotification(recipient, event, category, type, title, message, actionUrl, targetEntity, targetEntityId, null);
    }

    @Transactional
    public NotificationResponse createAndSendNotification(User recipient,
                                                          SocketEvent event,
                                                          SocketCategory category,
                                                          SocketType type,
                                                          String title,
                                                          String message,
                                                          String actionUrl,
                                                          String targetEntity,
                                                          Long targetEntityId,
                                                          Object extraPayload) {
        if (recipient == null) {
            return null;
        }

        Notification notification = new Notification(
            recipient,
            event,
            category,
            type,
            title,
            message,
            actionUrl,
            targetEntity,
            targetEntityId
        );

        notificationRepository.persist(notification);
        NotificationResponse response = toResponse(notification);

        // Safely attempt real-time WebSocket delivery
        try {
            Object socketPayload = extraPayload != null
                ? java.util.Map.of("notification", response, "visitor", extraPayload)
                : response;

            SocketMessage socketMessage = SocketMessage.of(
                category,
                event,
                type,
                title,
                message,
                actionUrl,
                targetEntity,
                targetEntityId,
                socketPayload,
                recipient.getUsername()
            );
            socketService.sendToUser(recipient.getUsername(), socketMessage);
        } catch (Exception e) {
            LOG.warnf(e, "Could not send real-time WebSocket notification to user %s (stored in DB)", recipient.getUsername());
        }

        return response;
    }

    @Transactional
    public void sendCheckInNotification(User hostUser, Visitor visitor) {
        if (hostUser == null || visitor == null) return;

        String visitorName = visitor.getFullName() != null ? visitor.getFullName() : "Ziyaretçi";
        String title = "Ziyaretçiniz Giriş Yaptı";
        String message = visitorName + " sizinle görüşmek üzere giriş yaptı.";

        createAndSendNotification(
            hostUser,
            SocketEvent.VISITOR_CHECKED_IN,
            SocketCategory.VISITOR,
            SocketType.SUCCESS,
            title,
            message,
            "/my-visitors",
            "VISITOR",
            visitor.getId(),
            toVisitorResponse(visitor)
        );
    }

    @Transactional
    public void sendCheckOutNotification(User hostUser, Visitor visitor) {
        if (hostUser == null || visitor == null) return;

        String visitorName = visitor.getFullName() != null ? visitor.getFullName() : "Ziyaretçi";
        String checkoutTimeStr = "";
        if (visitor.getExitTime() != null) {
            checkoutTimeStr = " saat " + visitor.getExitTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        String title = "Ziyaretçiniz Çıkış Yaptı";
        String message = visitorName + checkoutTimeStr + " itibarıyla çıkış yaptı.";

        createAndSendNotification(
            hostUser,
            SocketEvent.VISITOR_CHECKED_OUT,
            SocketCategory.VISITOR,
            SocketType.INFO,
            title,
            message,
            "/my-visitors",
            "VISITOR",
            visitor.getId(),
            toVisitorResponse(visitor)
        );
    }

    @Transactional
    public void sendPasswordChangedNotification(User user) {
        if (user == null) return;

        createAndSendNotification(
            user,
            SocketEvent.PASSWORD_CHANGED,
            SocketCategory.USER,
            SocketType.SUCCESS,
            "Şifre Değiştirildi",
            "Hesap şifreniz başarıyla değiştirildi.",
            "/profile",
            "USER",
            user.getId()
        );
    }

    @Transactional
    public void sendUserCreatedNotification(User user) {
        if (user == null) return;

        createAndSendNotification(
            user,
            SocketEvent.USER_CREATED,
            SocketCategory.USER,
            SocketType.SUCCESS,
            "Hesabınız Oluşturuldu",
            "Sistem hesabınız başarıyla oluşturuldu.",
            "/profile",
            "USER",
            user.getId()
        );
    }

    @Transactional
    public void sendUserCreatedNotificationToAdmin(User adminUser, User newUser, String personnelFullName) {
        if (adminUser == null || newUser == null) return;

        String nameInfo = (personnelFullName != null && !personnelFullName.isBlank())
            ? personnelFullName
            : newUser.getUsername();

        String title = "Yeni Kullanıcı Oluşturuldu";
        String message = nameInfo + " için yeni " + newUser.getRole() + " hesabı başarıyla oluşturuldu.";

        createAndSendNotification(
            adminUser,
            SocketEvent.USER_CREATED,
            SocketCategory.USER,
            SocketType.SUCCESS,
            title,
            message,
            "/personnel",
            "USER",
            newUser.getId()
        );
    }

    @Transactional
    public void sendProfileUpdatedNotification(User user) {
        if (user == null) return;

        createAndSendNotification(
            user,
            SocketEvent.PROFILE_UPDATED,
            SocketCategory.USER,
            SocketType.INFO,
            "Profil Güncellendi",
            "Profil bilgileriniz başarıyla güncellendi.",
            "/profile",
            "USER",
            user.getId()
        );
    }

    @Transactional
    public long sendAnnouncement(String title, String message, com.vms.enums.AnnouncementTarget target) {
        if (userRepository == null) return 0;

        List<User> targetUsers;
        if (target == null || target == com.vms.enums.AnnouncementTarget.ALL) {
            targetUsers = userRepository.listAll();
        } else {
            com.vms.entity.Role targetRole = switch (target) {
                case ADMIN -> com.vms.entity.Role.ADMIN;
                case RECEPTIONIST -> com.vms.entity.Role.RECEPTIONIST;
                case PERSONNEL -> com.vms.entity.Role.PERSONNEL;
                default -> null;
            };
            if (targetRole != null) {
                targetUsers = userRepository.findByRole(targetRole);
            } else {
                targetUsers = userRepository.listAll();
            }
        }

        if (targetUsers == null || targetUsers.isEmpty()) {
            return 0;
        }

        for (User recipient : targetUsers) {
            createAndSendNotification(
                recipient,
                SocketEvent.SYSTEM_MESSAGE,
                SocketCategory.SYSTEM,
                SocketType.INFO,
                title,
                message,
                null,
                "SYSTEM",
                null
            );
        }

        return targetUsers.size();
    }

    public com.vms.dto.visitor.VisitorResponse toVisitorResponse(Visitor v) {
        if (v == null) return null;
        return new com.vms.dto.visitor.VisitorResponse(
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

    public List<NotificationResponse> getUserNotifications(String username) {
        return notificationRepository.findByRecipientUsername(username).stream()
                .map(this::toResponse)
                .toList();
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countUnreadByRecipientUsername(username);
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String username) {
        Notification notification = notificationRepository.findById(id);
        if (notification == null) {
            return null;
        }

        if (!notification.getRecipient().getUsername().equalsIgnoreCase(username)) {
            throw new SecurityException("Bu bildirimi güncelleme yetkiniz bulunmamaktadır.");
        }

        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(java.time.LocalDateTime.now());
        return toResponse(notification);
    }

    @Transactional
    public long markAllAsRead(String username) {
        return notificationRepository.markAllAsReadByRecipientUsername(username);
    }

    @Transactional
    public boolean deleteNotification(Long id, String username) {
        Notification notification = notificationRepository.findById(id);
        if (notification == null) {
            return false;
        }

        if (!notification.getRecipient().getUsername().equalsIgnoreCase(username)) {
            throw new SecurityException("Bu bildirimi silme yetkiniz bulunmamaktadır.");
        }

        notificationRepository.delete(notification);
        return true;
    }

    @Transactional
    public long deleteAllRead(String username) {
        return notificationRepository.deleteAllReadByRecipientUsername(username);
    }

    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
            n.getId(),
            n.getRecipient() != null ? n.getRecipient().getUsername() : null,
            n.getEvent(),
            n.getCategory(),
            n.getType(),
            n.getTitle(),
            n.getMessage(),
            n.getActionUrl(),
            n.getTargetEntity(),
            n.getTargetEntityId(),
            n.getStatus(),
            n.getCreatedAt(),
            n.getReadAt()
        );
    }
}
