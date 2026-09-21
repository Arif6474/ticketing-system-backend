package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.AddProjectMemberRequest;
import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Project;
import com.ticket.system.entity.ProjectMembership;
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
public class ProjectMembershipIntegrationTest {

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
    private User clientUserBeta;
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

        clientUserBeta = new User();
        clientUserBeta.setEmail("user@beta.com");
        clientUserBeta.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUserBeta.setFirstName("BetaUser");
        clientUserBeta.setLastName("User");
        clientUserBeta.setRole(Role.CLIENT_USER);
        clientUserBeta.setOrganization(orgBeta);
        clientUserBeta.setActive(true);
        clientUserBeta = userRepository.saveAndFlush(clientUserBeta);

        projectAcme = projectRepository.saveAndFlush(new Project(orgAcme, "Acme Core App", "ACMEC", "Acme Core App"));
        projectBeta = projectRepository.saveAndFlush(new Project(orgBeta, "Beta Mobile App", "BETAM", "Beta Mobile App"));
    }

    @Test
    @DisplayName("CLIENT_ADMIN can add valid user from own organization as project member")
    void testAddMemberSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        AddProjectMemberRequest request = new AddProjectMemberRequest(clientUserAcme.getId());

        mockMvc.perform(post("/api/projects/" + projectAcme.getId() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email", is("user@acme.com")));

        assertTrue(membershipRepository.existsByProjectIdAndUserId(projectAcme.getId(), clientUserAcme.getId()));
    }

    @Test
    @DisplayName("Cross-organization membership guard rejects adding user from different organization")
    void testCrossOrganizationMembershipRejected() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        // Attempt to add clientUserBeta (orgBeta) to projectAcme (orgAcme)
        AddProjectMemberRequest request = new AddProjectMemberRequest(clientUserBeta.getId());

        mockMvc.perform(post("/api/projects/" + projectAcme.getId() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User must belong to the project's client organization")));
    }

    @Test
    @DisplayName("Duplicate membership addition returns 400 Bad Request")
    void testDuplicateMembershipRejected() throws Exception {
        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme));

        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        AddProjectMemberRequest request = new AddProjectMemberRequest(clientUserAcme.getId());

        mockMvc.perform(post("/api/projects/" + projectAcme.getId() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already a member")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN can remove member from own organization project")
    void testRemoveMemberSuccess() throws Exception {
        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme));
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(delete("/api/projects/" + projectAcme.getId() + "/members/" + clientUserAcme.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("removed successfully")));

        assertFalse(membershipRepository.existsByProjectIdAndUserId(projectAcme.getId(), clientUserAcme.getId()));
    }

    @Test
    @DisplayName("CLIENT_USER gains project visibility only once assigned membership")
    void testClientUserMemberVisibility() throws Exception {
        String userToken = jwtTokenProvider.generateToken(clientUserAcme);

        // Before membership: cannot list or view projectAcme
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));

        mockMvc.perform(get("/api/projects/" + projectAcme.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // Assign membership
        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme));

        // After membership: can list and view projectAcme
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].shortCode", is("ACMEC")));

        mockMvc.perform(get("/api/projects/" + projectAcme.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode", is("ACMEC")));
    }

    @Test
    @DisplayName("CLIENT_USER receives 403 when trying to add member")
    void testClientUserAddMemberForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);
        AddProjectMemberRequest request = new AddProjectMemberRequest(clientUserAcme.getId());

        mockMvc.perform(post("/api/projects/" + projectAcme.getId() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
