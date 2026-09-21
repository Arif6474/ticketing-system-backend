package com.ticket.system.service;

import com.ticket.system.dto.request.CreateOrganizationRequest;
import com.ticket.system.dto.request.UpdateOrganizationRequest;
import com.ticket.system.dto.response.ClientOrganizationResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.ClientOrganizationRepository;
import com.ticket.system.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrganizationService {

    private final ClientOrganizationRepository orgRepository;
    private final UserRepository userRepository;

    public OrganizationService(ClientOrganizationRepository orgRepository, UserRepository userRepository) {
        this.orgRepository = orgRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ClientOrganizationResponse createOrganization(UUID currentUserId, CreateOrganizationRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() != Role.APP_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only APP_ADMIN can create organizations");
        }

        String normalizedCode = request.getCode().trim().toUpperCase();
        if (orgRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Organization code already exists: " + normalizedCode);
        }

        ClientOrganization org = new ClientOrganization(request.getName(), normalizedCode, request.getDescription());
        ClientOrganization saved = orgRepository.save(org);
        return ClientOrganizationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientOrganizationResponse> getOrganizations(UUID currentUserId, int page, int size) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() != Role.APP_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only APP_ADMIN can list all organizations");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<ClientOrganizationResponse> dtoPage = orgRepository.findAll(pageable)
                .map(ClientOrganizationResponse::fromEntity);
        return PageResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public ClientOrganizationResponse getOrganizationById(UUID currentUserId, UUID orgId) {
        User currentUser = getCurrentUser(currentUserId);
        ClientOrganization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Organization not found"));

        if (currentUser.getRole() != Role.APP_ADMIN) {
            if (currentUser.getOrganization() == null || !currentUser.getOrganization().getId().equals(orgId)) {
                throw new AppException(HttpStatus.FORBIDDEN, "Access denied to organization details");
            }
        }

        return ClientOrganizationResponse.fromEntity(org);
    }

    @Transactional
    public ClientOrganizationResponse updateOrganization(UUID currentUserId, UUID orgId, UpdateOrganizationRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() != Role.APP_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only APP_ADMIN can update organizations");
        }

        ClientOrganization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Organization not found"));

        org.setName(request.getName());
        org.setDescription(request.getDescription());
        if (request.getActive() != null) {
            org.setActive(request.getActive());
        }

        ClientOrganization updated = orgRepository.save(org);
        return ClientOrganizationResponse.fromEntity(updated);
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
