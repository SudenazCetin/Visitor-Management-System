package com.vms.service;

import com.vms.entity.User;
import com.vms.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;

    @Inject
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.listAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findByIdOptional(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("User with username '" + user.getUsername() + "' already exists.");
        }
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
            user.setPassword(BcryptUtil.bcryptHash(user.getPassword()));
        }
        userRepository.persist(user);
        return user;
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existing = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        if (!existing.getUsername().equalsIgnoreCase(updatedUser.getUsername()) &&
            userRepository.existsByUsername(updatedUser.getUsername())) {
            throw new IllegalArgumentException("User with username '" + updatedUser.getUsername() + "' already exists.");
        }

        existing.setUsername(updatedUser.getUsername());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            if (!updatedUser.getPassword().startsWith("$2a$") && !updatedUser.getPassword().startsWith("$2b$")) {
                existing.setPassword(BcryptUtil.bcryptHash(updatedUser.getPassword()));
            } else {
                existing.setPassword(updatedUser.getPassword());
            }
        }
        existing.setRole(updatedUser.getRole());

        return existing;
    }

    @Transactional
    public boolean deleteUser(Long id) {
        return userRepository.deleteById(id);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
