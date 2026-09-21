package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.CreateOrganizationRequest;
import com.ticket.system.dto.request.LoginRequest;
import com.ticket.system.dto.request.UpdateOrganizationRequest;
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
public class OrganizationIntegrationTest {

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
    private User clientAdmin;
    private User clientUser;
    private ClientOrganization orgAcme;

    @BeforeEach
    void setUp() {
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();
        orgRepository.deleteAll();

        orgAcme = new ClientOrganization("Acme Corp", "ACME", "Acme Corporation");
        orgAcme = orgRepository.save(orgAcme);

        appAdmin = new User();
        appAdmin.setEmail("admin@example.com");
        appAdmin.setPasswordHash(passwordEncoder.encode("Password123"));
        appAdmin.setFirstName("Sys");
        appAdmin.setLastName("Admin");
        appAdmin.setRole(Role.APP_ADMIN);
        appAdmin.setOrganization(null);
        appAdmin.setActive(true);
        appAdmin = userRepository.save(appAdmin);

        clientAdmin = new User();
        clientAdmin.setEmail("clientadmin@acme.com");
        clientAdmin.setPasswordHash(passwordEncoder.encode("Password123"));
        clientAdmin.setFirstName("Client");
        clientAdmin.setLastName("Admin");
        clientAdmin.setRole(Role.CLIENT_ADMIN);
        clientAdmin.setOrganization(orgAcme);
        clientAdmin.setActive(true);
        clientAdmin = userRepository.save(clientAdmin);

        clientUser = new User();
        clientUser.setEmail("clientuser@acme.com");
        clientUser.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUser.setFirstName("Client");
        clientUser.setLastName("User");
        clientUser.setRole(Role.CLIENT_USER);
        clientUser.setOrganization(orgAcme);
        clientUser.setActive(true);
        clientUser = userRepository.save(clientUser);
    }

    @Test
    @DisplayName("APP_ADMIN can create organization with normalized code")
    void testCreateOrganizationByAppAdmin() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateOrganizationRequest request = new CreateOrganizationRequest("Beta Ltd", " beta_code ", "Beta Description");

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Beta Ltd")))
                .andExpect(jsonPath("$.code", is("BETA_CODE")))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot create organization")
    void testCreateOrganizationByClientAdminForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdmin);
        CreateOrganizationRequest request = new CreateOrganizationRequest("Forbidden Org", "FORBIDDEN", "Desc");

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Duplicate organization code returns 400 Bad Request")
    void testDuplicateOrganizationCode() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateOrganizationRequest request = new CreateOrganizationRequest("Duplicate Org", "acme", "Desc");

        mockMvc.perform(post("/api/organizations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("code already exists")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN can view own organization details")
    void testGetOwnOrganizationDetails() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdmin);

        mockMvc.perform(get("/api/organizations/" + orgAcme.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Acme Corp")))
                .andExpect(jsonPath("$.code", is("ACME")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot view another organization details")
    void testGetOtherOrganizationDetailsForbidden() throws Exception {
        ClientOrganization otherOrg = orgRepository.save(new ClientOrganization("Other Corp", "OTHER", "Other"));
        String token = jwtTokenProvider.generateToken(clientAdmin);

        mockMvc.perform(get("/api/organizations/" + otherOrg.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deactivating an organization immediately blocks client user authentication and protected access")
    void testDeactivateOrganizationBlocksAuthentication() throws Exception {
        String adminToken = jwtTokenProvider.generateToken(appAdmin);

        // Deactivate orgAcme
        UpdateOrganizationRequest updateRequest = new UpdateOrganizationRequest("Acme Corp", "Deactivated", false);
        mockMvc.perform(put("/api/organizations/" + orgAcme.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive", is(false)));

        // Login as clientAdmin fails
        LoginRequest loginRequest = new LoginRequest("clientadmin@acme.com", "Password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("organization is inactive")));

        // Protected endpoint with previously valid token fails
        String clientUserToken = jwtTokenProvider.generateToken(clientUser);
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + clientUserToken))
                .andExpect(status().isUnauthorized());
    }
}
