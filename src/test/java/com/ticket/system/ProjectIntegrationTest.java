package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.CreateProjectRequest;
import com.ticket.system.dto.request.UpdateProjectRequest;
import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Project;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.repository.*;
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
public class ProjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientOrganizationRepository orgRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMembershipRepository membershipRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User appAdmin;
    private User clientAdminAcme;
    private User clientUserAcme;
    private ClientOrganization orgAcme;
    private ClientOrganization orgBeta;
    private Project projectAcme;
    private Project projectBeta;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        projectRepository.deleteAll();
        resetTokenRepository.deleteAll();
        userRepository.deleteAll();
        orgRepository.deleteAll();

        orgAcme = orgRepository.saveAndFlush(new ClientOrganization("Acme Corp", "ACME", "Acme Description"));
        orgBeta = orgRepository.saveAndFlush(new ClientOrganization("Beta Corp", "BETA", "Beta Description"));

        appAdmin = new User();
        appAdmin.setEmail("admin@example.com");
        appAdmin.setPasswordHash(passwordEncoder.encode("Password123"));
        appAdmin.setFirstName("Sys");
        appAdmin.setLastName("Admin");
        appAdmin.setRole(Role.APP_ADMIN);
        appAdmin.setActive(true);
        appAdmin = userRepository.saveAndFlush(appAdmin);

        clientAdminAcme = new User();
        clientAdminAcme.setEmail("admin@acme.com");
        clientAdminAcme.setPasswordHash(passwordEncoder.encode("Password123"));
        clientAdminAcme.setFirstName("AcmeAdmin");
        clientAdminAcme.setLastName("User");
        clientAdminAcme.setRole(Role.CLIENT_ADMIN);
        clientAdminAcme.setOrganization(orgAcme);
        clientAdminAcme.setActive(true);
        clientAdminAcme = userRepository.saveAndFlush(clientAdminAcme);

        clientUserAcme = new User();
        clientUserAcme.setEmail("user@acme.com");
        clientUserAcme.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUserAcme.setFirstName("AcmeUser");
        clientUserAcme.setLastName("User");
        clientUserAcme.setRole(Role.CLIENT_USER);
        clientUserAcme.setOrganization(orgAcme);
        clientUserAcme.setActive(true);
        clientUserAcme = userRepository.saveAndFlush(clientUserAcme);

        projectAcme = projectRepository.saveAndFlush(new Project(orgAcme, "Acme Core App", "ACMEC", "Acme Core App"));
        projectBeta = projectRepository.saveAndFlush(new Project(orgBeta, "Beta Mobile App", "BETAM", "Beta Mobile App"));
    }

    @Test
    @DisplayName("APP_ADMIN can create project with normalized shortCode")
    void testAppAdminCreateProject() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateProjectRequest request = new CreateProjectRequest("Acme Web Portal", " acme_web ", "Web Portal Project", orgAcme.getId());

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Acme Web Portal")))
                .andExpect(jsonPath("$.shortCode", is("ACME_WEB")))
                .andExpect(jsonPath("$.organization.code", is("ACME")))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot create project")
    void testClientAdminCreateProjectForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        CreateProjectRequest request = new CreateProjectRequest("Forbidden Project", "FORBID", "Desc", orgAcme.getId());

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("APP_ADMIN lists projects across all organizations")
    void testAppAdminListProjectsAll() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("CLIENT_ADMIN lists only projects in their own organization")
    void testClientAdminListOwnOrgProjectsOnly() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].shortCode", is("ACMEC")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN attempting to specify another organization parameter is overridden")
    void testClientAdminOrgParamOverridden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        // Attempting to pass Beta's organization ID
        mockMvc.perform(get("/api/projects")
                        .param("organizationId", orgBeta.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].shortCode", is("ACMEC")));
    }

    @Test
    @DisplayName("CLIENT_USER with no project memberships receives zero projects")
    void testClientUserWithoutMembershipReceivesEmptyList() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("Duplicate project shortCode returns 400 Bad Request")
    void testDuplicateProjectShortCode() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateProjectRequest request = new CreateProjectRequest("Duplicate Project", "acmec", "Desc", orgAcme.getId());

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("short code already exists")));
    }

    @Test
    @DisplayName("APP_ADMIN can update project details")
    void testAppAdminUpdateProject() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        UpdateProjectRequest request = new UpdateProjectRequest("Updated Acme Core", "ACMEC_UPDATED", "New Desc", false);

        mockMvc.perform(put("/api/projects/" + projectAcme.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Acme Core")))
                .andExpect(jsonPath("$.shortCode", is("ACMEC_UPDATED")))
                .andExpect(jsonPath("$.isActive", is(false)));
    }

    @Test
    @DisplayName("APP_ADMIN can delete project")
    void testAppAdminDeleteProject() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(delete("/api/projects/" + projectBeta.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        assertTrue(projectRepository.findById(projectBeta.getId()).isEmpty());
    }
}
