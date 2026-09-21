package com.ticket.system.controller;

import com.ticket.system.dto.request.CreateProjectRequest;
import com.ticket.system.dto.request.UpdateProjectRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.dto.response.ProjectResponse;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> getProjects(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID organizationId) {
        PageResponse<ProjectResponse> response = projectService.getProjects(userDetails.getId(), page, size, search, active, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        ProjectResponse response = projectService.getProjectById(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        ProjectResponse response = projectService.updateProject(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<GenericResponse> deleteProject(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        GenericResponse response = projectService.deleteProject(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }
}
