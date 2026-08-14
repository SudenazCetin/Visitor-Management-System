package com.vms.service;

import com.vms.entity.Personnel;
import com.vms.repository.PersonnelRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PersonnelService {

    private final PersonnelRepository personnelRepository;

    @Inject
    public PersonnelService(PersonnelRepository personnelRepository) {
        this.personnelRepository = personnelRepository;
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
    public Personnel createPersonnel(Personnel personnel) {
        if (personnelRepository.existsByEmail(personnel.getEmail())) {
            throw new IllegalArgumentException("Personnel with email '" + personnel.getEmail() + "' already exists.");
        }
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
        return personnelRepository.deleteById(id);
    }

    public boolean existsByEmail(String email) {
        return personnelRepository.existsByEmail(email);
    }
}
