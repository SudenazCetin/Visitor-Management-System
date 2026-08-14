package com.vms.repository;

import com.vms.entity.Visitor;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class VisitorRepository implements PanacheRepository<Visitor> {

    public Optional<Visitor> findByIdWithHost(Long id) {
        return find("select v from Visitor v join fetch v.host where v.id = ?1", id).firstResultOptional();
    }

    public List<Visitor> findActiveVisitors() {
        return list("select v from Visitor v join fetch v.host where v.isInside = true order by v.entryTime desc");
    }

    public List<Visitor> findAllWithHost() {
        return list("select v from Visitor v join fetch v.host order by v.entryTime desc");
    }

    public List<Visitor> findByHostId(Long hostId) {
        return list("select v from Visitor v join fetch v.host where v.host.id = ?1 order by v.entryTime desc", hostId);
    }

    public List<Visitor> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return list("select v from Visitor v join fetch v.host where v.entryTime >= ?1 and v.entryTime <= ?2 order by v.entryTime desc", start, end);
    }

    public long countActiveVisitors() {
        return count("isInside", true);
    }
}

