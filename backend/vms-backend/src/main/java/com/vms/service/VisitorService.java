package com.vms.service;

import com.vms.entity.Personnel;
import com.vms.entity.Visitor;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.VisitorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class VisitorService {

    private static final Logger LOG = Logger.getLogger(VisitorService.class);

    private final VisitorRepository visitorRepository;
    private final PersonnelRepository personnelRepository;
    private final NotificationService notificationService;

    @Inject
    public VisitorService(VisitorRepository visitorRepository,
                          PersonnelRepository personnelRepository,
                          NotificationService notificationService) {
        this.visitorRepository = visitorRepository;
        this.personnelRepository = personnelRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<Visitor> getActiveVisitors() {
        List<Visitor> visitors = visitorRepository.findActiveVisitors();
        visitors.forEach(v -> initializeHost(v.getHost()));
        return visitors;
    }

    @Transactional
    public List<Visitor> getAllVisitors() {
        List<Visitor> visitors = visitorRepository.findAllWithHost();
        visitors.forEach(v -> initializeHost(v.getHost()));
        return visitors;
    }

    @Transactional
    public Optional<Visitor> getVisitorById(Long id) {
        Optional<Visitor> visitorOpt = visitorRepository.findByIdWithHost(id);
        visitorOpt.ifPresent(v -> initializeHost(v.getHost()));
        return visitorOpt;
    }

    public List<Visitor> getVisitorsByHostId(Long hostId) {
        List<Visitor> visitors = visitorRepository.findByHostId(hostId);
        visitors.forEach(v -> initializeHost(v.getHost()));
        return visitors;
    }

    public List<Visitor> getVisitorsByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Visitor> visitors = visitorRepository.findByDateRange(start, end);
        visitors.forEach(v -> initializeHost(v.getHost()));
        return visitors;
    }

    public long getActiveVisitorCount() {
        return visitorRepository.countActiveVisitors();
    }

    @Transactional
    public Visitor checkIn(String visitorFullName, Long hostPersonnelId) {
        Personnel host = personnelRepository.findByIdOptional(hostPersonnelId)
                .orElseThrow(() -> new IllegalArgumentException("Host personnel not found with id: " + hostPersonnelId));

        Visitor visitor = new Visitor(
                visitorFullName,
                host,
                LocalDateTime.now(),
                null,
                true
        );

        visitorRepository.persist(visitor);
        initializeHost(visitor.getHost());

        if (host.getUser() != null) {
            try {
                notificationService.sendCheckInNotification(host.getUser(), visitor);
            } catch (Exception e) {
                LOG.warnf(e, "Check-in notification failed for visitor %d (main transaction unaffected)", visitor.getId());
            }
        }

        return visitor;
    }

    @Transactional
    public Visitor checkOut(Long visitorId) {
        Visitor visitor = visitorRepository.findByIdWithHost(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found with id: " + visitorId));

        if (Boolean.FALSE.equals(visitor.getIsInside())) {
            throw new IllegalStateException("Visitor has already checked out.");
        }

        visitor.setExitTime(LocalDateTime.now());
        visitor.setIsInside(false);
        initializeHost(visitor.getHost());

        simulateCheckoutAuditLog(visitor);

        if (visitor.getHost() != null && visitor.getHost().getUser() != null) {
            try {
                notificationService.sendCheckOutNotification(visitor.getHost().getUser(), visitor);
            } catch (Exception e) {
                LOG.warnf(e, "Check-out notification failed for visitor %d (main transaction unaffected)", visitor.getId());
            }
        }

        return visitor;
    }

    private void simulateCheckoutAuditLog(Visitor visitor) {
        boolean isVirtual = Thread.currentThread().isVirtual();
        LOG.infof(
            "[VIRTUAL THREAD AUDIT LOG] Ziyaretçi Çıkış İşlemi Tamamlandı -> ID: %d, Adı: %s, Çıkış Zamanı: %s, IsVirtualThread: %b, Thread Bilgisi: %s",
            visitor.getId(),
            visitor.getFullName(),
            visitor.getExitTime(),
            isVirtual,
            Thread.currentThread()
        );
    }

    private void initializeHost(Personnel host) {
        if (host != null) {
            host.getFullName();
            host.getDepartment();
        }
    }
}
