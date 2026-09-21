package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.CreateUserRequest;
import com.ticket.system.dto.request.ForcePasswordResetRequest;
import com.ticket.system.dto.request.UpdateUserRequest;
import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.repository.ClientOrganizationRepository;
import com.ticket.system.repository.PasswordResetTokenRepository;
import com.ticket.system.repository.UserRepository;
import com.ticket.system.security.JwtTokenProvider;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class UserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientOrganizationRepository orgRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User appAdmin;
    private User clientAdminAcme;
    private User clientUserAcme;
    private User clientUserBeta;
    private ClientOrganization orgAcme;
    private ClientOrganization orgBeta;

    @BeforeEach
    void setUp() {
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();
        orgRepository.deleteAll();

        orgAcme = orgRepository.save(new ClientOrganization("Acme Corp", "ACME", "Acme Description"));
        orgBeta = orgRepository.save(new ClientOrganization("Beta Corp", "BETA", "Beta Description"));

        appAdmin = new User();
        appAdmin.setEmail("admin@example.com");
        appAdmin.setPasswordHash(passwordEncoder.encode("Password123"));
        appAdmin.setFirstName("System");
        appAdmin.setLastName("Admin");
        appAdmin.setRole(Role.APP_ADMIN);
        appAdmin.setOrganization(null);
        appAdmin.setActive(true);
        appAdmin = userRepository.save(appAdmin);

        clientAdminAcme = new User();
        clientAdminAcme.setEmail("admin@acme.com");
        clientAdminAcme.setPasswordHash(passwordEncoder.encode("Password123"));
        clientAdminAcme.setFirstName("AcmeAdmin");
        clientAdminAcme.setLastName("User");
        clientAdminAcme.setRole(Role.CLIENT_ADMIN);
        clientAdminAcme.setOrganization(orgAcme);
        clientAdminAcme.setActive(true);
        clientAdminAcme = userRepository.save(clientAdminAcme);

        clientUserAcme = new User();
        clientUserAcme.setEmail("user@acme.com");
        clientUserAcme.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUserAcme.setFirstName("AcmeUser");
        clientUserAcme.setLastName("User");
        clientUserAcme.setRole(Role.CLIENT_USER);
        clientUserAcme.setOrganization(orgAcme);
        clientUserAcme.setActive(true);
        clientUserAcme = userRepository.save(clientUserAcme);

        clientUserBeta = new User();
        clientUserBeta.setEmail("user@beta.com");
        clientUserBeta.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUserBeta.setFirstName("BetaUser");
        clientUserBeta.setLastName("User");
        clientUserBeta.setRole(Role.CLIENT_USER);
        clientUserBeta.setOrganization(orgBeta);
        clientUserBeta.setActive(true);
        clientUserBeta = userRepository.save(clientUserBeta);
    }

    // =========================================================================
    // 1. List & Search Users Tests
    // =========================================================================
    @Test
    @DisplayName("APP_ADMIN can list all users across all organizations")
    void testAppAdminListsAllUsers() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(4)));
    }

    @Test
    @DisplayName("CLIENT_ADMIN lists only users in their own organization")
    void testClientAdminListsOwnOrganizationUsersOnly() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder("admin@acme.com", "user@acme.com")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN requesting another organizationId parameter is strictly overridden to own organization")
    void testClientAdminOrganizationParamOverridden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        // Attempting to pass Beta's organization ID
        mockMvc.perform(get("/api/users")
                        .param("organizationId", orgBeta.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].email", containsInAnyOrder("admin@acme.com", "user@acme.com")));
    }

    @Test
    @DisplayName("CLIENT_USER receives 403 when attempting to access user management list")
    void testClientUserListUsersForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Search filtering by name/email works at database level")
    void testSearchUserFilter() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(get("/api/users")
                        .param("search", "BetaUser")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].email", is("user@beta.com")));
    }

    // =========================================================================
    // 2. Create User Tests
    // =========================================================================
    @Test
    @DisplayName("APP_ADMIN can create APP_ADMIN user without organization")
    void testAppAdminCreateAppAdmin() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateUserRequest request = new CreateUserRequest(
                "newadmin@example.com", "SecurePass123", "New", "Admin",
                "12345", "Lead", "HQ", Role.APP_ADMIN, null
        );

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("newadmin@example.com")))
                .andExpect(jsonPath("$.role", is("APP_ADMIN")))
                .andExpect(jsonPath("$.organization").value(nullValue()));
    }

    @Test
    @DisplayName("CLIENT_ADMIN can create CLIENT_USER automatically assigned to own organization")
    void testClientAdminCreateClientUser() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        // Payload passes orgBeta ID to attempt tampering, but service MUST force orgAcme
        CreateUserRequest request = new CreateUserRequest(
                "newuser@acme.com", "SecurePass123", "New", "AcmeUser",
                "12345", "Dev", "Acme Office", Role.CLIENT_USER, orgBeta.getId()
        );

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("newuser@acme.com")))
                .andExpect(jsonPath("$.role", is("CLIENT_USER")))
                .andExpect(jsonPath("$.organization.code", is("ACME"))); // Tampering prevented!
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot create APP_ADMIN user")
    void testClientAdminCreateAppAdminForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        CreateUserRequest request = new CreateUserRequest(
                "hackedadmin@example.com", "SecurePass123", "Hacked", "Admin",
                "12345", "Dev", "HQ", Role.APP_ADMIN, null
        );

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Edit & Security Boundary Tests
    // =========================================================================
    @Test
    @DisplayName("CLIENT_ADMIN cannot edit user in another organization")
    void testClientAdminEditOtherOrgUserForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        UpdateUserRequest request = new UpdateUserRequest("Modified", "User", "123", "Dev", "Office", Role.CLIENT_USER, orgBeta.getId(), true);

        mockMvc.perform(put("/api/users/" + clientUserBeta.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot edit or promote user to APP_ADMIN")
    void testClientAdminPromoteToAppAdminForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        UpdateUserRequest request = new UpdateUserRequest("AcmeUser", "User", "123", "Dev", "Office", Role.APP_ADMIN, orgAcme.getId(), true);

        mockMvc.perform(put("/api/users/" + clientUserAcme.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. Deactivate & Password Reset Tests
    // =========================================================================
    @Test
    @DisplayName("CLIENT_ADMIN can deactivate user in own organization")
    void testClientAdminDeactivateUser() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(patch("/api/users/" + clientUserAcme.getId() + "/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", is(false)));

        User updated = userRepository.findById(clientUserAcme.getId()).orElseThrow();
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("CLIENT_ADMIN force reset password updates password hash successfully")
    void testClientAdminForcePasswordReset() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        ForcePasswordResetRequest request = new ForcePasswordResetRequest("NewForcedPassword123");

        mockMvc.perform(post("/api/users/" + clientUserAcme.getId() + "/force-password-reset")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password reset successfully")));

        User updated = userRepository.findById(clientUserAcme.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewForcedPassword123", updated.getPasswordHash()));
    }

    // =========================================================================
    // 5. Hard Delete Tests
    // =========================================================================
    @Test
    @DisplayName("APP_ADMIN can hard delete user")
    void testAppAdminHardDeleteUser() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(delete("/api/users/" + clientUserBeta.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        assertTrue(userRepository.findById(clientUserBeta.getId()).isEmpty());
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot hard delete user")
    void testClientAdminHardDeleteUserForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(delete("/api/users/" + clientUserAcme.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
