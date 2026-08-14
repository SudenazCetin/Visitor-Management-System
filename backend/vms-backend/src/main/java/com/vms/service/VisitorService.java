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

@ApplicationScoped
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final PersonnelRepository personnelRepository;

    @Inject
    public VisitorService(VisitorRepository visitorRepository, PersonnelRepository personnelRepository) {
        this.visitorRepository = visitorRepository;
        this.personnelRepository = personnelRepository;
    }

    public List<Visitor> getActiveVisitors() {
        return visitorRepository.findActiveVisitors();
    }

    public List<Visitor> getAllVisitors() {
        return visitorRepository.listAll();
    }

    public Optional<Visitor> getVisitorById(Long id) {
        return visitorRepository.findByIdOptional(id);
    }

    public List<Visitor> getVisitorsByHostId(Long hostId) {
        return visitorRepository.findByHostId(hostId);
    }

    public List<Visitor> getVisitorsByDateRange(LocalDateTime start, LocalDateTime end) {
        return visitorRepository.findByDateRange(start, end);
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
        return visitor;
    }

    @Transactional
    public Visitor checkOut(Long visitorId) {
        Visitor visitor = visitorRepository.findByIdOptional(visitorId)
                .orElseThrow(() -> new IllegalArgumentException("Visitor not found with id: " + visitorId));

        if (Boolean.FALSE.equals(visitor.getIsInside())) {
            throw new IllegalStateException("Visitor has already checked out.");
        }

        visitor.setExitTime(LocalDateTime.now());
        visitor.setIsInside(false);

        return visitor;
    }
}
