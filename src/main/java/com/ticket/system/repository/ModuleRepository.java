package com.ticket.system.repository;

import com.ticket.system.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID>, JpaSpecificationExecutor<Module> {

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    boolean existsByProjectId(UUID projectId);
}
