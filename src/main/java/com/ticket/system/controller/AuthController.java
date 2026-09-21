package com.ticket.system.controller;

import com.ticket.system.dto.request.*;
import com.ticket.system.dto.response.AuthResponse;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.UserResponse;
import com.ticket.system.security.UserSecurityDetails;
import com.ticket.system.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserSecurityDetails userDetails) {
        UserResponse userResponse = authService.getCurrentUserProfile(userDetails.getId());
        return ResponseEntity.ok(userResponse);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse userResponse = authService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(userResponse);
    }

    @PostMapping("/change-password")
    public ResponseEntity<GenericResponse> changePassword(
            @AuthenticationPrincipal UserSecurityDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        GenericResponse response = authService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<GenericResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        GenericResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GenericResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        GenericResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
