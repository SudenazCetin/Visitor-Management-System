package com.vms.repository;

import com.vms.entity.Visitor;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class VisitorRepository implements PanacheRepository<Visitor> {

    public List<Visitor> findActiveVisitors() {
        return list("isInside = true order by entryTime desc");
    }

    public List<Visitor> findByHostId(Long hostId) {
        return list("host.id", hostId);
    }

    public List<Visitor> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return list("entryTime >= ?1 and entryTime <= ?2 order by entryTime desc", start, end);
    }

    public long countActiveVisitors() {
        return count("isInside", true);
    }
}
