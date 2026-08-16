package com.vms.config;

import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);

    private final UserRepository userRepository;

    @Inject
    public DataSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void onStartup(@Observes StartupEvent ev) {
        // Seed initial ADMIN account if not present
        if (!userRepository.existsByUsername("admin")) {
            String hashedPassword = BcryptUtil.bcryptHash("admin123");
            User adminUser = new User("admin", hashedPassword, Role.ADMIN);
            userRepository.persist(adminUser);
            LOG.info("Initial ADMIN user 'admin' created successfully with Bcrypt password hashing.");
        } else {
            LOG.info("ADMIN user 'admin' already exists. Skipping admin seed.");
        }

        // Seed initial RECEPTIONIST account if not present
        if (!userRepository.existsByUsername("resepsiyon")) {
            String hashedPassword = BcryptUtil.bcryptHash("123456");
            User recUser = new User("resepsiyon", hashedPassword, Role.RECEPTIONIST);
            userRepository.persist(recUser);
            LOG.info("Initial RECEPTIONIST user 'resepsiyon' created successfully.");
        }
    }
}
