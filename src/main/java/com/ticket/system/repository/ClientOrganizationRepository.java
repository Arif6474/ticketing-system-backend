package com.ticket.system.repository;

import com.ticket.system.entity.ClientOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientOrganizationRepository extends JpaRepository<ClientOrganization, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<ClientOrganization> findByCodeIgnoreCase(String code);
}
