package com.vms.service;

import com.vms.dto.auth.LoginRequest;
import com.vms.dto.auth.LoginResponse;
import com.vms.dto.auth.RegisterRequest;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.repository.UserRepository;
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

    @Inject
    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        if (userRepository.existsByUsername(request.username())) {
            throw new WebApplicationException(
                Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message", "Bu kullanıcı adı zaten kullanılıyor."))
                    .build()
            );
        }

        String hashedPassword = BcryptUtil.bcryptHash(request.password());
        // Public registration users are ALWAYS created as RECEPTIONIST role
        User newUser = new User(request.username(), hashedPassword, Role.RECEPTIONIST);
        userRepository.persist(newUser);
    }
}
