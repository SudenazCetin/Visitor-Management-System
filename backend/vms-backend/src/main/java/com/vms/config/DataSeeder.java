package com.vms.config;

import com.vms.entity.Personnel;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DataSeeder {

    private static final Logger LOG = Logger.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PersonnelRepository personnelRepository;

    @Inject
    public DataSeeder(UserRepository userRepository, PersonnelRepository personnelRepository) {
        this.userRepository = userRepository;
        this.personnelRepository = personnelRepository;
    }

    @Transactional
    public void onStartup(@Observes StartupEvent ev) {
        // 1. Seed initial ADMIN account if not present
        if (!userRepository.existsByUsername("admin")) {
            String hashedPassword = BcryptUtil.bcryptHash("admin123");
            User adminUser = new User("admin", hashedPassword, Role.ADMIN);
            userRepository.persist(adminUser);
            LOG.info("Initial ADMIN user 'admin' created successfully.");
        } else {
            LOG.info("ADMIN user 'admin' already exists. Skipping admin seed.");
        }

        // 2. Seed initial RECEPTIONIST account if not present
        if (!userRepository.existsByUsername("resepsiyon")) {
            String hashedPassword = BcryptUtil.bcryptHash("123456");
            User recUser = new User("resepsiyon", hashedPassword, Role.RECEPTIONIST);
            userRepository.persist(recUser);
            LOG.info("Initial RECEPTIONIST user 'resepsiyon' created successfully.");
        } else {
            LOG.info("RECEPTIONIST user 'resepsiyon' already exists. Skipping receptionist seed.");
        }

        // 3. Seed initial PERSONNEL account if not present
        User personelUser;
        if (!userRepository.existsByUsername("personel")) {
            String hashedPassword = BcryptUtil.bcryptHash("123456");
            personelUser = new User("personel", hashedPassword, Role.PERSONNEL);
            userRepository.persist(personelUser);
            LOG.info("Initial PERSONNEL user 'personel' created successfully.");
        } else {
            personelUser = userRepository.findByUsername("personel").orElse(null);
            LOG.info("PERSONNEL user 'personel' already exists. Skipping personnel seed.");
        }

        // 4. Ensure Personnel profile exists and is linked to 'personel' user
        if (personelUser != null) {
            Optional<Personnel> existingByEmail = personnelRepository.findByEmail("ayse.demir@firma.com");
            if (existingByEmail.isPresent()) {
                Personnel p = existingByEmail.get();
                if (p.getUser() == null) {
                    p.setUser(personelUser);
                    LOG.info("Linked existing Personnel 'Ayşe Demir' to user 'personel'.");
                }
            } else if (personnelRepository.findByUser(personelUser).isEmpty()) {
                Personnel newPersonnel = new Personnel("Ayşe Demir", "Yazılım", "Kıdemli Uzman", "ayse.demir@firma.com", personelUser);
                personnelRepository.persist(newPersonnel);
                LOG.info("Created linked Personnel profile for 'personel' user.");
            }
        }
    }
}
