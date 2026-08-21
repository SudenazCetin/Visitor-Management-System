package com.vms.service;

import com.vms.dto.auth.LoginRequest;
import com.vms.dto.auth.LoginResponse;
import com.vms.dto.auth.RegisterRequest;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.repository.UserRepository;
import com.vms.entity.Personnel;
import com.vms.repository.PersonnelRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    private final UserRepository userRepository;
    private final PersonnelRepository personnelRepository;
    private final String configuredAdminCode;

    @Inject
    public AuthService(UserRepository userRepository,
                       PersonnelRepository personnelRepository,
                       @ConfigProperty(name = "vms.registration.admin-code") String configuredAdminCode) {
        this.userRepository = userRepository;
        this.personnelRepository = personnelRepository;
        this.configuredAdminCode = configuredAdminCode;
    }

    public LoginResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.username());

        if (userOpt.isEmpty()) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Geçersiz kullanıcı adı veya şifre"))
                    .build()
            );
        }

        User user = userOpt.get();

        boolean passwordMatches = false;
        if (user.getPassword() != null && (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"))) {
            passwordMatches = BcryptUtil.matches(request.password(), user.getPassword());
        } else {
            passwordMatches = user.getPassword() != null && user.getPassword().equals(request.password());
        }

        if (!passwordMatches) {
            throw new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("message", "Geçersiz kullanıcı adı veya şifre"))
                    .build()
            );
        }

        String token = Jwt.issuer("https://vms.com")
                .upn(user.getUsername())
                .groups(Set.of(user.getRole().name()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .expiresIn(28800) // 8 hours
                .sign();

        return new LoginResponse(token, user.getUsername(), user.getRole());
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (request.username() == null || request.username().isBlank()) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Kullanıcı adı boş olamaz."))
                    .build()
            );
        }

        if (request.password() == null || request.password().isBlank()) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Şifre boş olamaz."))
                    .build()
            );
        }

        if (userRepository.existsByUsername(request.username().trim())) {
            throw new WebApplicationException(
                Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message", "Bu kullanıcı adı zaten kullanılıyor."))
                    .build()
            );
        }

        String regType = request.registrationType() != null ? request.registrationType().trim().toUpperCase() : "PERSONNEL";

        // Security check: RECEPTIONIST role can NEVER be registered via public endpoint
        if ("RECEPTIONIST".equals(regType)) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Resepsiyonist hesapları kamuya açık kayıt ekranı üzerinden oluşturulamaz."))
                    .build()
            );
        }

        String hashedPassword = BcryptUtil.bcryptHash(request.password().trim());

        if ("ADMIN".equals(regType)) {
            String code = request.adminRegistrationCode();
            if (code == null || !code.trim().equals(configuredAdminCode.trim())) {
                throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("message", "Geçersiz yönetim kayıt kodu."))
                        .build()
                );
            }

            User newAdminUser = new User(request.username().trim(), hashedPassword, Role.ADMIN);
            userRepository.persist(newAdminUser);
        } else {
            // Default to PERSONNEL role
            String email = request.email() != null && !request.email().isBlank()
                    ? request.email().trim()
                    : request.username().trim() + "@vms.com";

            if (personnelRepository.existsByEmail(email)) {
                throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                        .entity(Map.of("message", "Bu e-posta adresi zaten kullanılıyor."))
                        .build()
                );
            }

            User newPersonnelUser = new User(request.username().trim(), hashedPassword, Role.PERSONNEL);
            userRepository.persist(newPersonnelUser);

            String fullName = request.fullName() != null && !request.fullName().isBlank()
                    ? request.fullName().trim()
                    : request.username().trim();

            Personnel personnel = new Personnel(fullName, "Genel", "Personel", email, newPersonnelUser);
            personnelRepository.persist(personnel);
        }
    }
}
