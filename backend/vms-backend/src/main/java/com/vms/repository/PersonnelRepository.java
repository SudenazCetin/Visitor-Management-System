package com.vms.repository;

import com.vms.entity.Personnel;
import com.vms.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PersonnelRepository implements PanacheRepository<Personnel> {

    public List<Personnel> findByDepartment(String department) {
        return list("department", department);
    }

    public Optional<Personnel> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public Optional<Personnel> findByUser(User user) {
        if (user == null) return Optional.empty();
        return find("user", user).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }

    public Optional<Personnel> findByUserUsername(String username) {
        return find("user.username", username).firstResultOptional();
    }
}
