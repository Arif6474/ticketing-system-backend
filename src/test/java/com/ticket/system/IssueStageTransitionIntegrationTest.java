package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.UpdateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueStageRequest;
import com.ticket.system.entity.*;
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
public class IssueStageTransitionIntegrationTest {

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
    private IssueRepository issueRepository;

    @Autowired
    private ProjectMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User appAdmin;
    private User clientAdminAcme;
    private User clientUserAcme;
    private User clientUserAcme2;
    private User clientUserBeta;

    private ClientOrganization orgAcme;
    private ClientOrganization orgBeta;

    private Project projectAcme;
    private Project projectBeta;

    private Issue issueAcme;
    private Issue issueBeta;

    @BeforeEach
    void setUp() {
        issueRepository.deleteAll();
        membershipRepository.deleteAll();
        projectRepository.deleteAll();
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
        clientUserAcme.setEmail("user1@acme.com");
        clientUserAcme.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUserAcme.setFirstName("AcmeUser1");
        clientUserAcme.setLastName("Reporter");
        clientUserAcme.setRole(Role.CLIENT_USER);
        clientUserAcme.setOrganization(orgAcme);
        clientUserAcme.setActive(true);
        clientUserAcme = userRepository.saveAndFlush(clientUserAcme);

        clientUserAcme2 = new User();
        clientUserAcme2.setEmail("user2@acme.com");
        clientUserAcme2.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUserAcme2.setFirstName("AcmeUser2");
        clientUserAcme2.setLastName("OtherUser");
        clientUserAcme2.setRole(Role.CLIENT_USER);
        clientUserAcme2.setOrganization(orgAcme);
        clientUserAcme2.setActive(true);
        clientUserAcme2 = userRepository.saveAndFlush(clientUserAcme2);

        clientUserBeta = new User();
        clientUserBeta.setEmail("user@beta.com");
        clientUserBeta.setPasswordHash(passwordEncoder.encode("Password123"));
        clientUserBeta.setFirstName("BetaUser");
        clientUserBeta.setLastName("User");
        clientUserBeta.setRole(Role.CLIENT_USER);
        clientUserBeta.setOrganization(orgBeta);
        clientUserBeta.setActive(true);
        clientUserBeta = userRepository.saveAndFlush(clientUserBeta);

        projectAcme = projectRepository.saveAndFlush(new Project(orgAcme, "Acme Web App", "ACMWEB", "Acme Core Web App"));
        projectBeta = projectRepository.saveAndFlush(new Project(orgBeta, "Beta Mobile App", "BETMOB", "Beta Mobile App"));

        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme));
        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme2));
        membershipRepository.saveAndFlush(new ProjectMembership(projectBeta, clientUserBeta));

        issueAcme = issueRepository.saveAndFlush(new Issue(
                projectAcme, null, clientUserAcme,
                "Acme Bug", "Acme Bug Description",
                IssueType.BUG, IssuePriority.HIGH
        ));

        issueBeta = issueRepository.saveAndFlush(new Issue(
                projectBeta, null, clientUserBeta,
                "Beta Feature", "Beta Feature Description",
                IssueType.NEW_FEATURE, IssuePriority.MEDIUM
        ));
    }

    // =========================================================================
    // 1. Valid Linear Transitions (APP_ADMIN & CLIENT_ADMIN)
    // =========================================================================

    @Test
    @DisplayName("APP_ADMIN can transition issue through complete valid linear lifecycle")
    void testAppAdminCompleteLinearLifecycle() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        // SUBMITTED -> RECEIVED
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.RECEIVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("RECEIVED")));

        // RECEIVED -> UNDER_DEVELOPMENT
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.UNDER_DEVELOPMENT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("UNDER_DEVELOPMENT")));

        // UNDER_DEVELOPMENT -> TESTING
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.TESTING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("TESTING")));

        // TESTING -> DEPLOYED
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.DEPLOYED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("DEPLOYED")));
    }

    @Test
    @DisplayName("Valid terminal transitions SUBMITTED -> DECLINED and TESTING -> RESOLVED")
    void testValidTerminalTransitions() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        // SUBMITTED -> DECLINED
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.DECLINED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("DECLINED")));

        // Reset issue to TESTING for RESOLVED test
        issueBeta.setStage(IssueStage.TESTING);
        issueRepository.saveAndFlush(issueBeta);

        String appAdminToken = jwtTokenProvider.generateToken(appAdmin);

        // TESTING -> RESOLVED
        mockMvc.perform(patch("/api/issues/" + issueBeta.getId() + "/stage")
                        .header("Authorization", "Bearer " + appAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.RESOLVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("RESOLVED")));
    }

    // =========================================================================
    // 2. Invalid Transition Rejections
    // =========================================================================

    @Test
    @DisplayName("Skipping stages (SUBMITTED -> DEPLOYED) returns 400 Bad Request")
    void testSkippingStagesRejected() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.DEPLOYED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid stage transition")));
    }

    @Test
    @DisplayName("Backward stage transitions (TESTING -> RECEIVED) are rejected")
    void testBackwardTransitionRejected() throws Exception {
        issueAcme.setStage(IssueStage.TESTING);
        issueRepository.saveAndFlush(issueAcme);

        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.RECEIVED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid stage transition")));
    }

    @Test
    @DisplayName("Outgoing transitions from terminal states (DEPLOYED, DECLINED, RESOLVED) are rejected")
    void testTerminalStateOutgoingTransitionsRejected() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        // DEPLOYED -> TESTING
        issueAcme.setStage(IssueStage.DEPLOYED);
        issueRepository.saveAndFlush(issueAcme);

        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.TESTING))))
                .andExpect(status().isBadRequest());

        // DECLINED -> SUBMITTED
        issueAcme.setStage(IssueStage.DECLINED);
        issueRepository.saveAndFlush(issueAcme);

        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.SUBMITTED))))
                .andExpect(status().isBadRequest());

        // RESOLVED -> TESTING
        issueAcme.setStage(IssueStage.RESOLVED);
        issueRepository.saveAndFlush(issueAcme);

        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.TESTING))))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 3. Role-Based Permissions (CLIENT_USER & CLIENT_ADMIN)
    // =========================================================================

    @Test
    @DisplayName("CLIENT_USER cannot drive developer workflow transitions (RECEIVED, UNDER_DEV, DEPLOYED)")
    void testClientUserDeveloperTransitionsForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        // SUBMITTED -> RECEIVED
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.RECEIVED))))
                .andExpect(status().isForbidden());

        // RECEIVED -> UNDER_DEVELOPMENT
        issueAcme.setStage(IssueStage.RECEIVED);
        issueRepository.saveAndFlush(issueAcme);

        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.UNDER_DEVELOPMENT))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_USER can transition own issue for permitted transitions (SUBMITTED->DECLINED, TESTING->RESOLVED)")
    void testClientUserPermittedTransitionsSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        // SUBMITTED -> DECLINED
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.DECLINED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("DECLINED")));

        // TESTING -> RESOLVED
        issueAcme.setStage(IssueStage.TESTING);
        issueRepository.saveAndFlush(issueAcme);

        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.RESOLVED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage", is("RESOLVED")));
    }

    @Test
    @DisplayName("CLIENT_USER cannot change stage of another user's issue")
    void testClientUserCannotChangeOtherUserIssueStage() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme2);

        // issueAcme was reported by clientUserAcme, not clientUserAcme2
        mockMvc.perform(patch("/api/issues/" + issueAcme.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.DECLINED))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot transition issue of another organization")
    void testClientAdminOtherOrgStageChangeForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(patch("/api/issues/" + issueBeta.getId() + "/stage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateIssueStageRequest(IssueStage.RECEIVED))))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. Protection Against Direct PUT Stage Mutation
    // =========================================================================

    @Test
    @DisplayName("General PUT /api/issues/{id} endpoint cannot modify issue stage")
    void testGeneralPutCannotModifyIssueStage() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        UpdateIssueRequest updateRequest = new UpdateIssueRequest(
                "Updated Title", "Updated Description",
                IssueType.ENHANCEMENT, IssuePriority.URGENT, null
        );

        mockMvc.perform(put("/api/issues/" + issueAcme.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.stage", is("SUBMITTED")));
    }
}
