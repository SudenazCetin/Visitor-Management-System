package com.vms.service;

import com.vms.dto.auth.RegisterRequest;
import com.vms.entity.Personnel;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.UserRepository;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class AuthServiceRegistrationTest {

    private AuthService authService;
    private List<User> userStore;
    private List<Personnel> personnelStore;

    @BeforeEach
    void setUp() {
        userStore = new ArrayList<>();
        personnelStore = new ArrayList<>();

        UserRepository stubUserRepo = new UserRepository() {
            @Override
            public boolean existsByUsername(String username) {
                return userStore.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
            }

            @Override
            public void persist(User user) {
                userStore.add(user);
            }
        };

        PersonnelRepository stubPersonnelRepo = new PersonnelRepository() {
            @Override
            public boolean existsByEmail(String email) {
                return personnelStore.stream().anyMatch(p -> p.getEmail().equalsIgnoreCase(email));
            }

            @Override
            public void persist(Personnel personnel) {
                personnelStore.add(personnel);
            }
        };

        authService = new AuthService(stubUserRepo, stubPersonnelRepo, "VMS-YONETIM-2026!");
    }

    @Test
    void testPersonnelRegistrationCreatesPersonnelUserAndLinkedProfile() {
        RegisterRequest request = new RegisterRequest(
            "ahmet_personnel",
            "password123",
            "Ahmet Yılmaz",
            "ahmet@firma.com",
            "PERSONNEL",
            null
        );

        authService.register(request);

        Assertions.assertEquals(1, userStore.size());
        User createdUser = userStore.get(0);
        Assertions.assertEquals("ahmet_personnel", createdUser.getUsername());
        Assertions.assertEquals(Role.PERSONNEL, createdUser.getRole());

        Assertions.assertEquals(1, personnelStore.size());
        Personnel createdPersonnel = personnelStore.get(0);
        Assertions.assertEquals("Ahmet Yılmaz", createdPersonnel.getFullName());
        Assertions.assertEquals("ahmet@firma.com", createdPersonnel.getEmail());
        Assertions.assertEquals(createdUser, createdPersonnel.getUser());
    }

    @Test
    void testAdminRegistrationWithValidCodeCreatesAdminUser() {
        RegisterRequest request = new RegisterRequest(
            "admin_user",
            "password123",
            "Admin User",
            "admin@firma.com",
            "ADMIN",
            "VMS-YONETIM-2026!"
        );

        authService.register(request);

        Assertions.assertEquals(1, userStore.size());
        User createdUser = userStore.get(0);
        Assertions.assertEquals("admin_user", createdUser.getUsername());
        Assertions.assertEquals(Role.ADMIN, createdUser.getRole());
        Assertions.assertEquals(0, personnelStore.size());
    }

    @Test
    void testAdminRegistrationWithInvalidCodeThrowsException() {
        RegisterRequest request = new RegisterRequest(
            "admin_fake",
            "password123",
            "Fake Admin",
            "fake@firma.com",
            "ADMIN",
            "WRONG-CODE"
        );

        WebApplicationException exception = Assertions.assertThrows(
            WebApplicationException.class,
            () -> authService.register(request)
        );

        Assertions.assertEquals(400, exception.getResponse().getStatus());
        Assertions.assertEquals(0, userStore.size());
    }

    @Test
    void testReceptionistRegistrationViaPublicEndpointIsRejected() {
        RegisterRequest request = new RegisterRequest(
            "receptionist_fake",
            "password123",
            "Fake Receptionist",
            "rec@firma.com",
            "RECEPTIONIST",
            null
        );

        WebApplicationException exception = Assertions.assertThrows(
            WebApplicationException.class,
            () -> authService.register(request)
        );

        Assertions.assertEquals(400, exception.getResponse().getStatus());
        Assertions.assertEquals(0, userStore.size());
    }
}
