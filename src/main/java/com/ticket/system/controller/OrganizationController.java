package com.ticket.system.controller;

import com.ticket.system.dto.request.CreateOrganizationRequest;
import com.ticket.system.dto.request.UpdateOrganizationRequest;
import com.ticket.system.dto.response.ClientOrganizationResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<ClientOrganizationResponse> createOrganization(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @Valid @RequestBody CreateOrganizationRequest request) {
        ClientOrganizationResponse response = organizationService.createOrganization(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<PageResponse<ClientOrganizationResponse>> getOrganizations(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ClientOrganizationResponse> response = organizationService.getOrganizations(userDetails.getId(), page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientOrganizationResponse> getOrganizationById(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        ClientOrganizationResponse response = organizationService.getOrganizationById(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<ClientOrganizationResponse> updateOrganization(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        ClientOrganizationResponse response = organizationService.updateOrganization(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }
}
