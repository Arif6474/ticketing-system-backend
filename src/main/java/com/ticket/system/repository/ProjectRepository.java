package com.ticket.system.repository;

import com.ticket.system.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    boolean existsByShortCodeIgnoreCase(String shortCode);

    boolean existsByShortCodeIgnoreCaseAndIdNot(String shortCode, UUID id);

    Optional<Project> findByShortCodeIgnoreCase(String shortCode);
}
