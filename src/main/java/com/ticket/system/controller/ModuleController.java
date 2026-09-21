package com.ticket.system.controller;

import com.ticket.system.dto.request.CreateModuleRequest;
import com.ticket.system.dto.request.UpdateModuleRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.ModuleResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.ModuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

    private final ModuleService moduleService;

    public ModuleController(ModuleService moduleService) {
        this.moduleService = moduleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<ModuleResponse> createModule(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @Valid @RequestBody CreateModuleRequest request) {
        ModuleResponse response = moduleService.createModule(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ModuleResponse>> getModules(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID projectId) {
        PageResponse<ModuleResponse> response = moduleService.getModules(userDetails.getId(), page, size, search, active, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleResponse> getModuleById(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        ModuleResponse response = moduleService.getModuleById(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<ModuleResponse> updateModule(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateModuleRequest request) {
        ModuleResponse response = moduleService.updateModule(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<GenericResponse> deleteModule(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        GenericResponse response = moduleService.deleteModule(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<ModuleResponse> activateModule(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        ModuleResponse response = moduleService.activateModule(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<ModuleResponse> deactivateModule(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        ModuleResponse response = moduleService.deactivateModule(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }
}
