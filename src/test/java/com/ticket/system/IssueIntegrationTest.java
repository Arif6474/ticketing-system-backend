package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.CreateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueRequest;
import com.ticket.system.entity.*;
import com.ticket.system.entity.Module;
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
public class IssueIntegrationTest {

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
    private ModuleRepository moduleRepository;

    @Autowired
    private IssueRepository issueRepository;

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
    private User clientUserAcme2;
    private User clientUserBeta;

    private ClientOrganization orgAcme;
    private ClientOrganization orgBeta;

    private Project projectAcme;
    private Project projectBeta;

    private Module moduleAcmeAuth;
    private Module moduleAcmePay;
    private Module moduleBetaMobile;

    private Issue issueAcmeBug;
    private Issue issueBetaFeature;

    @BeforeEach
    void setUp() {
        issueRepository.deleteAll();
        moduleRepository.deleteAll();
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

        // Assign memberships
        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme));
        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme2));
        membershipRepository.saveAndFlush(new ProjectMembership(projectBeta, clientUserBeta));

        moduleAcmeAuth = moduleRepository.saveAndFlush(new Module(projectAcme, "Auth Module", "Authentication System"));
        moduleAcmePay = moduleRepository.saveAndFlush(new Module(projectAcme, "Payment Module", "Payment System"));
        moduleBetaMobile = moduleRepository.saveAndFlush(new Module(projectBeta, "Mobile UI", "Mobile Interface"));

        issueAcmeBug = issueRepository.saveAndFlush(new Issue(
                projectAcme, moduleAcmeAuth, clientUserAcme,
                "Login Crash", "Application crashes on login button click",
                IssueType.BUG, IssuePriority.HIGH
        ));

        issueBetaFeature = issueRepository.saveAndFlush(new Issue(
                projectBeta, moduleBetaMobile, clientUserBeta,
                "Dark Mode", "Add dark mode toggle to mobile dashboard",
                IssueType.NEW_FEATURE, IssuePriority.MEDIUM
        ));
    }

    // =========================================================================
    // 1. Issue Creation Tests
    // =========================================================================

    @Test
    @DisplayName("APP_ADMIN can create an issue for any active project")
    void testAppAdminCreateIssueSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateIssueRequest request = new CreateIssueRequest(
                "System Slowdown", "Performance issues during peak hours",
                IssueType.BUG, IssuePriority.URGENT, projectBeta.getId(), moduleBetaMobile.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("System Slowdown")))
                .andExpect(jsonPath("$.reporterId", is(appAdmin.getId().toString())))
                .andExpect(jsonPath("$.stage", is("SUBMITTED")))
                .andExpect(jsonPath("$.verificationStatus", is("PENDING_VERIFICATION")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN can create an issue for project in own organization")
    void testClientAdminCreateIssueOwnOrgSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        CreateIssueRequest request = new CreateIssueRequest(
                "Payment Failure", "Gateway timeout on checkout",
                IssueType.BUG, IssuePriority.HIGH, projectAcme.getId(), moduleAcmePay.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Payment Failure")))
                .andExpect(jsonPath("$.reporterId", is(clientAdminAcme.getId().toString())));
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot create an issue in another organization's project")
    void testClientAdminCreateIssueOtherOrgForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        CreateIssueRequest request = new CreateIssueRequest(
                "Hack Attempt", "Unauthorized creation",
                IssueType.BUG, IssuePriority.HIGH, projectBeta.getId(), null
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_USER can create an issue for active member project")
    void testClientUserCreateIssueMemberProjectSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);
        CreateIssueRequest request = new CreateIssueRequest(
                "UI Typo", "Spelling mistake on landing page",
                IssueType.ENHANCEMENT, IssuePriority.LOW, projectAcme.getId(), null
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("UI Typo")))
                .andExpect(jsonPath("$.reporterId", is(clientUserAcme.getId().toString())));
    }

    @Test
    @DisplayName("CLIENT_USER cannot create an issue for a non-member project")
    void testClientUserCreateIssueNonMemberProjectForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);
        CreateIssueRequest request = new CreateIssueRequest(
                "Beta Issue", "Non-member creation",
                IssueType.BUG, IssuePriority.MEDIUM, projectBeta.getId(), null
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Creating issue under an inactive project fails with 400 Bad Request")
    void testCreateIssueInactiveProjectFails() throws Exception {
        projectAcme.setActive(false);
        projectRepository.saveAndFlush(projectAcme);

        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateIssueRequest request = new CreateIssueRequest(
                "Test Issue", "Desc",
                IssueType.BUG, IssuePriority.LOW, projectAcme.getId(), null
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inactive project")));
    }

    // =========================================================================
    // 2. Module Relationship & Integrity Tests
    // =========================================================================

    @Test
    @DisplayName("Creating issue with module from a different project fails")
    void testCreateIssueModuleFromDifferentProjectFails() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        // projectAcme with moduleBetaMobile
        CreateIssueRequest request = new CreateIssueRequest(
                "Mismatched Module", "Desc",
                IssueType.BUG, IssuePriority.MEDIUM, projectAcme.getId(), moduleBetaMobile.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to the selected project")));
    }

    @Test
    @DisplayName("Creating issue with an inactive module fails")
    void testCreateIssueInactiveModuleFails() throws Exception {
        moduleAcmeAuth.setActive(false);
        moduleRepository.saveAndFlush(moduleAcmeAuth);

        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateIssueRequest request = new CreateIssueRequest(
                "Inactive Module Issue", "Desc",
                IssueType.BUG, IssuePriority.MEDIUM, projectAcme.getId(), moduleAcmeAuth.getId()
        );

        mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inactive module")));
    }

    // =========================================================================
    // 3. Issue Listing & Filtering Tests
    // =========================================================================

    @Test
    @DisplayName("APP_ADMIN sees all issues across organizations")
    void testAppAdminListAllIssues() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(get("/api/issues")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("CLIENT_ADMIN sees issues only in own organization")
    void testClientAdminListIssuesOwnOrgOnly() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(get("/api/issues")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Login Crash")));
    }

    @Test
    @DisplayName("CLIENT_USER sees issues only in member projects")
    void testClientUserListIssuesMemberProjectOnly() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        mockMvc.perform(get("/api/issues")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Login Crash")));
    }

    @Test
    @DisplayName("Filter issues by type, priority, stage, and search query")
    void testFilterIssuesByMultipleCriteria() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(get("/api/issues")
                        .header("Authorization", "Bearer " + token)
                        .param("type", "BUG")
                        .param("priority", "HIGH")
                        .param("stage", "SUBMITTED")
                        .param("search", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Login Crash")));
    }

    // =========================================================================
    // 4. Issue Detail Access Tests
    // =========================================================================

    @Test
    @DisplayName("CLIENT_ADMIN receives 403 when requesting issue from another organization")
    void testClientAdminGetOtherOrgIssueForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(get("/api/issues/" + issueBetaFeature.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_USER receives 403 when requesting issue from non-member project")
    void testClientUserGetNonMemberProjectIssueForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        mockMvc.perform(get("/api/issues/" + issueBetaFeature.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 5. Issue Update & Immutability Tests
    // =========================================================================

    @Test
    @DisplayName("APP_ADMIN can update issue details")
    void testAppAdminUpdateIssueSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        UpdateIssueRequest request = new UpdateIssueRequest(
                "Updated Login Crash", "Detailed crash stacktrace added",
                IssueType.BUG, IssuePriority.VERY_HIGH, moduleAcmePay.getId()
        );

        mockMvc.perform(put("/api/issues/" + issueAcmeBug.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Login Crash")))
                .andExpect(jsonPath("$.priority", is("VERY_HIGH")))
                .andExpect(jsonPath("$.moduleName", is("Payment Module")));
    }

    @Test
    @DisplayName("CLIENT_USER can update an issue they reported in a member project")
    void testClientUserUpdateOwnIssueSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);
        UpdateIssueRequest request = new UpdateIssueRequest(
                "Login Crash Updated by Reporter", "Updated description",
                IssueType.BUG, IssuePriority.HIGH, null
        );

        mockMvc.perform(put("/api/issues/" + issueAcmeBug.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Login Crash Updated by Reporter")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN receives 403 when attempting to edit another organization's issue")
    void testClientAdminUpdateOtherOrgIssueForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        UpdateIssueRequest request = new UpdateIssueRequest(
                "Unauthorized Org Edit", "Hack",
                IssueType.BUG, IssuePriority.LOW, null
        );

        mockMvc.perform(put("/api/issues/" + issueBetaFeature.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Updating issue with module from a different project fails")
    void testUpdateIssueModuleFromDifferentProjectFails() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        UpdateIssueRequest request = new UpdateIssueRequest(
                "Updated Bug", "Desc",
                IssueType.BUG, IssuePriority.HIGH, moduleBetaMobile.getId()
        );

        mockMvc.perform(put("/api/issues/" + issueAcmeBug.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not belong to the selected project")));
    }

    @Test
    @DisplayName("Updating issue with an inactive module fails")
    void testUpdateIssueInactiveModuleFails() throws Exception {
        moduleAcmePay.setActive(false);
        moduleRepository.saveAndFlush(moduleAcmePay);

        String token = jwtTokenProvider.generateToken(appAdmin);
        UpdateIssueRequest request = new UpdateIssueRequest(
                "Updated Bug", "Desc",
                IssueType.BUG, IssuePriority.HIGH, moduleAcmePay.getId()
        );

        mockMvc.perform(put("/api/issues/" + issueAcmeBug.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inactive module")));
    }

    @Test
    @DisplayName("CLIENT_USER receives 403 when attempting to edit another user's issue")
    void testClientUserUpdateOtherUserIssueForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme2);
        UpdateIssueRequest request = new UpdateIssueRequest(
                "Unauthorized Edit", "Hack",
                IssueType.BUG, IssuePriority.LOW, null
        );

        // issueAcmeBug was reported by clientUserAcme, not clientUserAcme2
        mockMvc.perform(put("/api/issues/" + issueAcmeBug.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 6. Issue Delete Tests
    // =========================================================================

    @Test
    @DisplayName("APP_ADMIN can delete any issue")
    void testAppAdminDeleteIssueSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(delete("/api/issues/" + issueBetaFeature.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        assertTrue(issueRepository.findById(issueBetaFeature.getId()).isEmpty());
    }

    @Test
    @DisplayName("CLIENT_ADMIN can delete issue in own organization")
    void testClientAdminDeleteOwnOrgIssueSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(delete("/api/issues/" + issueAcmeBug.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertTrue(issueRepository.findById(issueAcmeBug.getId()).isEmpty());
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot delete issue in another organization")
    void testClientAdminDeleteOtherOrgIssueForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(delete("/api/issues/" + issueBetaFeature.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_USER receives 403 Forbidden when attempting to delete an issue")
    void testClientUserDeleteIssueForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        mockMvc.perform(delete("/api/issues/" + issueAcmeBug.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
