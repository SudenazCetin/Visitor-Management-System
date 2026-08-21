package com.vms.entity;

import com.vms.enums.NotificationStatus;
import com.vms.enums.SocketCategory;
import com.vms.enums.SocketEvent;
import com.vms.enums.SocketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 50)
    private SocketEvent event;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private SocketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SocketType type;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "action_url", length = 250)
    private String actionUrl;

    @Column(name = "target_entity", length = 50)
    private String targetEntity;

    @Column(name = "target_entity_id")
    private Long targetEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification() {
    }

    public Notification(User recipient,
                        SocketEvent event,
                        SocketCategory category,
                        SocketType type,
                        String title,
                        String message,
                        String actionUrl,
                        String targetEntity,
                        Long targetEntityId) {
        this.recipient = recipient;
        this.event = event;
        this.category = category;
        this.type = type;
        this.title = title;
        this.message = message;
        this.actionUrl = actionUrl;
        this.targetEntity = targetEntity;
        this.targetEntityId = targetEntityId;
        this.status = NotificationStatus.UNREAD;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public SocketEvent getEvent() {
        return event;
    }

    public void setEvent(SocketEvent event) {
        this.event = event;
    }

    public SocketCategory getCategory() {
        return category;
    }

    public void setCategory(SocketCategory category) {
        this.category = category;
    }

    public SocketType getType() {
        return type;
    }

    public void setType(SocketType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public Long getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(Long targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
