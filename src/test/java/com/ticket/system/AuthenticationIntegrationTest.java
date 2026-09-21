package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.*;
import com.ticket.system.entity.PasswordResetToken;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.repository.PasswordResetTokenRepository;
import com.ticket.system.repository.UserRepository;
import com.ticket.system.security.JwtTokenProvider;
import com.ticket.system.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthService authService;

    private User testUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("active@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("Password123"));
        testUser.setFirstName("Active");
        testUser.setLastName("User");
        testUser.setMobile("1234567890");
        testUser.setDesignation("Developer");
        testUser.setOffice("HQ");
        testUser.setRole(Role.CLIENT_USER);
        testUser.setActive(true);
        testUser = userRepository.save(testUser);

        inactiveUser = new User();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setPasswordHash(passwordEncoder.encode("Password123"));
        inactiveUser.setFirstName("Inactive");
        inactiveUser.setLastName("User");
        inactiveUser.setRole(Role.CLIENT_USER);
        inactiveUser.setActive(false);
        inactiveUser = userRepository.save(inactiveUser);
    }

    // =========================================================================
    // 1. User / Password Tests
    // =========================================================================
    @Test
    @DisplayName("BCrypt password is not stored as plaintext")
    void testPasswordNotPlaintext() {
        assertNotEquals("Password123", testUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("Password123", testUser.getPasswordHash()));
    }

    @Test
    @DisplayName("User email uniqueness enforcement")
    void testEmailUniqueness() {
        User duplicate = new User();
        duplicate.setEmail("active@example.com");
        duplicate.setPasswordHash("hash");
        duplicate.setFirstName("Dup");
        duplicate.setLastName("User");
        duplicate.setRole(Role.CLIENT_USER);

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(duplicate);
        });
    }

    // =========================================================================
    // 2. Login Tests
    // =========================================================================
    @Test
    @DisplayName("Successful login returns JWT and user profile without password hash")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("active@example.com", "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is("active@example.com")))
                .andExpect(jsonPath("$.user.role", is("CLIENT_USER")))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Login fails with invalid password")
    void testLoginInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("active@example.com", "WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Login fails with unknown email")
    void testLoginUnknownEmail() throws Exception {
        LoginRequest request = new LoginRequest("unknown@example.com", "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Login fails for inactive user")
    void testLoginInactiveUser() throws Exception {
        LoginRequest request = new LoginRequest("inactive@example.com", "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("inactive")));
    }

    // =========================================================================
    // 3. JWT & Security Tests
    // =========================================================================
    @Test
    @DisplayName("Protected endpoint without token returns 401")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoint with invalid token returns 401")
    void testProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-jwt-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Valid token allows access to /api/auth/me")
    void testProtectedEndpointWithValidToken() throws Exception {
        String token = jwtTokenProvider.generateToken(testUser);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("active@example.com")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // =========================================================================
    // 4. Profile Tests
    // =========================================================================
    @Test
    @DisplayName("Authenticated user can update allowed profile fields")
    void testUpdateAllowedProfileFields() throws Exception {
        String token = jwtTokenProvider.generateToken(testUser);
        UpdateProfileRequest request = new UpdateProfileRequest("NewFirst", "NewLast", "9876543210", "Lead", "Branch");

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("NewFirst")))
                .andExpect(jsonPath("$.lastName", is("NewLast")))
                .andExpect(jsonPath("$.mobile", is("9876543210")))
                .andExpect(jsonPath("$.designation", is("Lead")))
                .andExpect(jsonPath("$.office", is("Branch")))
                .andExpect(jsonPath("$.email", is("active@example.com"))) // Email unchanged
                .andExpect(jsonPath("$.role", is("CLIENT_USER"))); // Role unchanged
    }

    // =========================================================================
    // 5. Change Password Tests
    // =========================================================================
    @Test
    @DisplayName("Change password with correct current password succeeds")
    void testChangePasswordSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(testUser);
        ChangePasswordRequest request = new ChangePasswordRequest("Password123", "NewSecurePassword123");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        // Verify login works with new password
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecurePassword123", updated.getPasswordHash()));
    }

    @Test
    @DisplayName("Change password with incorrect current password fails")
    void testChangePasswordIncorrectCurrent() throws Exception {
        String token = jwtTokenProvider.generateToken(testUser);
        ChangePasswordRequest request = new ChangePasswordRequest("WrongCurrentPassword", "NewPassword123");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Current password is incorrect")));
    }

    // =========================================================================
    // 6. Forgot & Reset Password Tests
    // =========================================================================
    @Test
    @DisplayName("Forgot password returns generic message for both existing and unknown emails")
    void testForgotPasswordGenericResponse() throws Exception {
        ForgotPasswordRequest existingRequest = new ForgotPasswordRequest("active@example.com");
        ForgotPasswordRequest unknownRequest = new ForgotPasswordRequest("nonexistent@example.com");

        String expectedMessage = "If an account exists for this email, password reset instructions have been sent.";

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(expectedMessage)));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(expectedMessage)));
    }

    @Test
    @DisplayName("Reset token is stored hashed and raw token resets password successfully")
    void testResetPasswordFlow() throws Exception {
        String rawToken = "raw-reset-token-12345";
        String tokenHash = authService.hashToken(rawToken);

        PasswordResetToken resetTokenEntity = new PasswordResetToken(
                testUser, tokenHash, OffsetDateTime.now().plusHours(1));
        resetTokenRepository.save(resetTokenEntity);

        // Verify raw token is NOT in database
        assertTrue(resetTokenRepository.findByTokenHash(rawToken).isEmpty());
        assertTrue(resetTokenRepository.findByTokenHash(tokenHash).isPresent());

        // Perform password reset using raw token
        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, "ResetPass@2026");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password reset successfully")));

        // Verify password updated
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("ResetPass@2026", updated.getPasswordHash()));

        // Verify token is marked as used and cannot be reused
        PasswordResetToken usedToken = resetTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertNotNull(usedToken.getUsedAt());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));
    }

    @Test
    @DisplayName("Expired reset token fails to reset password")
    void testExpiredResetToken() throws Exception {
        String rawToken = "expired-token-9999";
        String tokenHash = authService.hashToken(rawToken);

        // Expired 10 minutes ago
        PasswordResetToken expiredTokenEntity = new PasswordResetToken(
                testUser, tokenHash, OffsetDateTime.now().minusMinutes(10));
        resetTokenRepository.save(expiredTokenEntity);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest(rawToken, "NewPass12345");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid or expired")));
    }
}
