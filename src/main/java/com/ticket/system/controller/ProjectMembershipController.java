package com.ticket.system.controller;

import com.ticket.system.dto.request.AddProjectMemberRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.dto.response.ProjectMemberResponse;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.ProjectMembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMembershipController {

    private final ProjectMembershipService membershipService;

    public ProjectMembershipController(ProjectMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<ProjectMemberResponse> addProjectMember(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberRequest request) {
        ProjectMemberResponse response = membershipService.addProjectMember(userDetails.getId(), projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<GenericResponse> removeProjectMember(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        GenericResponse response = membershipService.removeProjectMember(userDetails.getId(), projectId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectMemberResponse>> getProjectMembers(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        PageResponse<ProjectMemberResponse> response = membershipService.getProjectMembers(userDetails.getId(), projectId, page, size, search);
        return ResponseEntity.ok(response);
    }
}
