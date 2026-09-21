package com.ticket.system.controller;

import com.ticket.system.dto.request.CreateUserRequest;
import com.ticket.system.dto.request.ForcePasswordResetRequest;
import com.ticket.system.dto.request.UpdateUserRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.dto.response.UserResponse;
import com.ticket.system.entity.Role;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) UUID organizationId) {
        PageResponse<UserResponse> response = userService.getUsers(userDetails.getId(), page, size, search, role, active, organizationId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<UserResponse> getUserById(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        UserResponse response = userService.getUserById(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<UserResponse> deactivateUser(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        UserResponse response = userService.deactivateUser(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('APP_ADMIN')")
    public ResponseEntity<GenericResponse> hardDeleteUser(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id) {
        GenericResponse response = userService.hardDeleteUser(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/force-password-reset")
    @PreAuthorize("hasAnyRole('APP_ADMIN', 'CLIENT_ADMIN')")
    public ResponseEntity<GenericResponse> forcePasswordReset(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody ForcePasswordResetRequest request) {
        GenericResponse response = userService.forcePasswordReset(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }
}
