package com.vms.service;

import com.vms.dto.personnel.PersonnelRequest;
import com.vms.entity.Personnel;
import com.vms.entity.Role;
import com.vms.entity.User;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.UserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class PersonnelService {

    private final PersonnelRepository personnelRepository;
    private final UserRepository userRepository;

    @Inject
    public PersonnelService(PersonnelRepository personnelRepository, UserRepository userRepository) {
        this.personnelRepository = personnelRepository;
        this.userRepository = userRepository;
    }

    public List<Personnel> getAllPersonnel() {
        return personnelRepository.listAll();
    }

    public Optional<Personnel> getPersonnelById(Long id) {
        return personnelRepository.findByIdOptional(id);
    }

    public List<Personnel> getPersonnelByDepartment(String department) {
        return personnelRepository.findByDepartment(department);
    }

    @Transactional
    public Personnel createPersonnel(PersonnelRequest request) {
        if (personnelRepository.existsByEmail(request.email())) {
            throw new WebApplicationException(
                Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("message", "Personnel with email '" + request.email() + "' already exists."))
                    .build()
            );
        }

        User user = null;
        if (Boolean.TRUE.equals(request.createAccount())) {
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
                        .entity(Map.of("message", "Geçici şifre boş olamaz."))
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

            String hashedPassword = BcryptUtil.bcryptHash(request.password().trim());
            user = new User(request.username().trim(), hashedPassword, Role.PERSONNEL);
            userRepository.persist(user);
        }

        Personnel personnel = new Personnel(
            request.fullName().trim(),
            request.department().trim(),
            request.title() != null ? request.title().trim() : null,
            request.email().trim(),
            user
        );

        personnelRepository.persist(personnel);
        return personnel;
    }

    @Transactional
    public Personnel updatePersonnel(Long id, Personnel updatedPersonnel) {
        Personnel existing = personnelRepository.findByIdOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Personnel not found with id: " + id));

        if (!existing.getEmail().equalsIgnoreCase(updatedPersonnel.getEmail()) &&
            personnelRepository.existsByEmail(updatedPersonnel.getEmail())) {
            throw new IllegalArgumentException("Personnel with email '" + updatedPersonnel.getEmail() + "' already exists.");
        }

        existing.setFullName(updatedPersonnel.getFullName());
        existing.setDepartment(updatedPersonnel.getDepartment());
        existing.setTitle(updatedPersonnel.getTitle());
        existing.setEmail(updatedPersonnel.getEmail());

        return existing;
    }

    @Transactional
    public boolean deletePersonnel(Long id) {
        Optional<Personnel> pOpt = personnelRepository.findByIdOptional(id);
        if (pOpt.isEmpty()) {
            return false;
        }

        Personnel p = pOpt.get();
        User linkedUser = p.getUser();
        p.setUser(null);
        personnelRepository.delete(p);

        if (linkedUser != null) {
            userRepository.delete(linkedUser);
        }
        return true;
    }

    public boolean existsByEmail(String email) {
        return personnelRepository.existsByEmail(email);
    }
}
