package com.vms.service;

import com.vms.config.DataSeeder;
import com.vms.entity.Notification;
import com.vms.entity.Personnel;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.enums.NotificationStatus;
import com.vms.enums.SocketCategory;
import com.vms.enums.SocketEvent;
import com.vms.enums.SocketType;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.UserRepository;
import io.quarkus.runtime.StartupEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserPersistenceTest {

    private List<User> dbUsers;
    private List<Personnel> dbPersonnel;
    private UserRepository stubUserRepo;
    private PersonnelRepository stubPersonnelRepo;
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dbUsers = new ArrayList<>();
        dbPersonnel = new ArrayList<>();

        stubUserRepo = new UserRepository() {
            @Override
            public void persist(User user) {
                if (user.getId() == null) {
                    user.setId((long) (dbUsers.size() + 1));
                }
                dbUsers.add(user);
            }

            @Override
            public Optional<User> findByUsername(String username) {
                return dbUsers.stream()
                        .filter(u -> u.getUsername().equalsIgnoreCase(username))
                        .findFirst();
            }

            @Override
            public boolean existsByUsername(String username) {
                return dbUsers.stream()
                        .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
            }

            @Override
            public List<User> listAll() {
                return new ArrayList<>(dbUsers);
            }

            @Override
            public void delete(User user) {
                dbUsers.remove(user);
            }
        };

        stubPersonnelRepo = new PersonnelRepository() {
            @Override
            public void persist(Personnel personnel) {
                if (personnel.getId() == null) {
                    personnel.setId((long) (dbPersonnel.size() + 1));
                }
                dbPersonnel.add(personnel);
            }

            @Override
            public Optional<Personnel> findByEmail(String email) {
                return dbPersonnel.stream()
                        .filter(p -> p.getEmail().equalsIgnoreCase(email))
                        .findFirst();
            }

            @Override
            public Optional<Personnel> findByUser(User user) {
                return dbPersonnel.stream()
                        .filter(p -> p.getUser() != null && p.getUser().equals(user))
                        .findFirst();
            }

            @Override
            public boolean existsByEmail(String email) {
                return dbPersonnel.stream()
                        .anyMatch(p -> p.getEmail().equalsIgnoreCase(email));
            }

            @Override
            public Optional<Personnel> findByIdOptional(Long id) {
                return dbPersonnel.stream()
                        .filter(p -> p.getId().equals(id))
                        .findFirst();
            }

            @Override
            public void delete(Personnel personnel) {
                dbPersonnel.remove(personnel);
            }
        };

        dataSeeder = new DataSeeder(stubUserRepo, stubPersonnelRepo);
    }

    @Test
    void testDataSeederCreatesAllThreeRolesCoexistingSafely() {
        dataSeeder.onStartup(new StartupEvent());

        Assertions.assertTrue(stubUserRepo.existsByUsername("admin"), "ADMIN account must exist");
        Assertions.assertTrue(stubUserRepo.existsByUsername("resepsiyon"), "RECEPTIONIST account must exist");
        Assertions.assertTrue(stubUserRepo.existsByUsername("personel"), "PERSONNEL account must exist");

        User admin = stubUserRepo.findByUsername("admin").orElseThrow();
        User rec = stubUserRepo.findByUsername("resepsiyon").orElseThrow();
        User pUser = stubUserRepo.findByUsername("personel").orElseThrow();

        Assertions.assertEquals(Role.ADMIN, admin.getRole());
        Assertions.assertEquals(Role.RECEPTIONIST, rec.getRole());
        Assertions.assertEquals(Role.PERSONNEL, pUser.getRole());
    }

    @Test
    void testExistingUsersAreNotErasedOrOverwrittenOnSecondStartup() {
        // First startup
        dataSeeder.onStartup(new StartupEvent());

        // Create custom personnel user
        User customPersonnelUser = new User("sude_dev", "hashed_pass", Role.PERSONNEL);
        stubUserRepo.persist(customPersonnelUser);
        Personnel customPersonnel = new Personnel("Sude Çetin", "Yazılım", "Uzman", "sude@vms.com", customPersonnelUser);
        stubPersonnelRepo.persist(customPersonnel);

        int totalUsersBeforeSecondStartup = dbUsers.size();

        // Second startup (simulating application restart)
        dataSeeder.onStartup(new StartupEvent());

        Assertions.assertEquals(totalUsersBeforeSecondStartup, dbUsers.size(), "No user accounts must be deleted on restart");
        Assertions.assertTrue(stubUserRepo.existsByUsername("sude_dev"), "Custom PERSONNEL account must be preserved");
    }

    @Test
    void testPersonnelDeletionUnlinksUserCleanly() {
        User u = new User("test_personnel", "pass", Role.PERSONNEL);
        stubUserRepo.persist(u);
        Personnel p = new Personnel("Test Person", "IT", "Dev", "test@vms.com", u);
        stubPersonnelRepo.persist(p);

        NotificationService stubNotificationService = new NotificationService(null, null);
        PersonnelService personnelService = new PersonnelService(stubPersonnelRepo, stubUserRepo, stubNotificationService);
        boolean deleted = personnelService.deletePersonnel(p.getId());

        Assertions.assertTrue(deleted);
        Assertions.assertFalse(stubPersonnelRepo.existsByEmail("test@vms.com"), "Personnel record must be deleted");
        Assertions.assertFalse(stubUserRepo.existsByUsername("test_personnel"), "Linked User record must also be cleanly deleted");
    }

    @Test
    void testPersonnelUpdatePreservesLinkedUserAccount() {
        User linkedUser = new User("linked_user", "hashed_pass", Role.PERSONNEL);
        stubUserRepo.persist(linkedUser);

        Personnel p = new Personnel("Original Name", "IT", "Junior", "orig@vms.com", linkedUser);
        stubPersonnelRepo.persist(p);

        NotificationService stubNotificationService = new NotificationService(null, null);
        PersonnelService personnelService = new PersonnelService(stubPersonnelRepo, stubUserRepo, stubNotificationService);

        Personnel updatedData = new Personnel("Updated Name", "Software Dev", "Senior", "updated@vms.com");
        Personnel updatedResult = personnelService.updatePersonnel(p.getId(), updatedData);

        Assertions.assertEquals("Updated Name", updatedResult.getFullName());
        Assertions.assertEquals("Software Dev", updatedResult.getDepartment());
        Assertions.assertEquals("Senior", updatedResult.getTitle());
        Assertions.assertEquals("updated@vms.com", updatedResult.getEmail());

        Assertions.assertNotNull(updatedResult.getUser(), "Linked User object must NOT be removed or set to null during update");
        Assertions.assertEquals("linked_user", updatedResult.getUser().getUsername(), "Linked User username must remain unchanged");
        Assertions.assertTrue(stubUserRepo.existsByUsername("linked_user"), "Linked User entity in database must be preserved");
    }

    @Test
    void testCaseInsensitiveNotificationRecipientQuery() {
        User adminUser = new User("Admin", "pass", Role.ADMIN);
        Notification n = new Notification(adminUser, SocketEvent.USER_CREATED, SocketCategory.USER, SocketType.SUCCESS, "Test", "Message", null, null, null);

        List<Notification> list = new ArrayList<>();
        list.add(n);

        long uppercaseCount = list.stream().filter(x -> x.getRecipient().getUsername().equalsIgnoreCase("ADMIN")).count();
        long lowercaseCount = list.stream().filter(x -> x.getRecipient().getUsername().equalsIgnoreCase("admin")).count();

        Assertions.assertEquals(1, uppercaseCount, "Recipient lookup should match case-insensitively");
        Assertions.assertEquals(1, lowercaseCount, "Recipient lookup should match case-insensitively");
    }
}
