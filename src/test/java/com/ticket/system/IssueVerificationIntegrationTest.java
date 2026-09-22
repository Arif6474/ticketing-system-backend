package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.UpdateIssueRequest;
import com.ticket.system.dto.request.UpdateVerificationStatusRequest;
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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class IssueVerificationIntegrationTest {

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
    private IssueAuditRepository auditRepository;

    @Autowired
    private ProjectMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User appAdmin;
    private User clientAdminAcme;
    private User clientUserAcme;
    private User clientAdminBeta;
    private User clientUserBeta;

    private ClientOrganization orgAcme;
    private ClientOrganization orgBeta;

    private Project projAcme;
    private Project projBeta;

    private Issue issueAcme1;
    private Issue issueAcme2;
    private Issue issueBeta1;

    private String tokenAppAdmin;
    private String tokenClientAdminAcme;
    private String tokenClientUserAcme;
    private String tokenClientAdminBeta;
    private String tokenClientUserBeta;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        issueRepository.deleteAll();
        membershipRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        orgRepository.deleteAll();

        orgAcme = orgRepository.saveAndFlush(new ClientOrganization("Acme Corp", "ACME", "contact@acme.com"));
        orgBeta = orgRepository.saveAndFlush(new ClientOrganization("Beta Corp", "BETA", "contact@beta.com"));

        appAdmin = new User();
        appAdmin.setEmail("admin@system.com");
        appAdmin.setPasswordHash(passwordEncoder.encode("Password123!"));
        appAdmin.setFirstName("App");
        appAdmin.setLastName("Admin");
        appAdmin.setRole(Role.APP_ADMIN);
        appAdmin.setActive(true);
        appAdmin = userRepository.saveAndFlush(appAdmin);

        clientAdminAcme = new User();
        clientAdminAcme.setEmail("admin@acme.com");
        clientAdminAcme.setPasswordHash(passwordEncoder.encode("Password123!"));
        clientAdminAcme.setFirstName("Acme");
        clientAdminAcme.setLastName("Admin");
        clientAdminAcme.setRole(Role.CLIENT_ADMIN);
        clientAdminAcme.setOrganization(orgAcme);
        clientAdminAcme.setActive(true);
        clientAdminAcme = userRepository.saveAndFlush(clientAdminAcme);

        clientUserAcme = new User();
        clientUserAcme.setEmail("user1@acme.com");
        clientUserAcme.setPasswordHash(passwordEncoder.encode("Password123!"));
        clientUserAcme.setFirstName("Acme1");
        clientUserAcme.setLastName("User");
        clientUserAcme.setRole(Role.CLIENT_USER);
        clientUserAcme.setOrganization(orgAcme);
        clientUserAcme.setActive(true);
        clientUserAcme = userRepository.saveAndFlush(clientUserAcme);

        clientAdminBeta = new User();
        clientAdminBeta.setEmail("admin@beta.com");
        clientAdminBeta.setPasswordHash(passwordEncoder.encode("Password123!"));
        clientAdminBeta.setFirstName("Beta");
        clientAdminBeta.setLastName("Admin");
        clientAdminBeta.setRole(Role.CLIENT_ADMIN);
        clientAdminBeta.setOrganization(orgBeta);
        clientAdminBeta.setActive(true);
        clientAdminBeta = userRepository.saveAndFlush(clientAdminBeta);

        clientUserBeta = new User();
        clientUserBeta.setEmail("user@beta.com");
        clientUserBeta.setPasswordHash(passwordEncoder.encode("Password123!"));
        clientUserBeta.setFirstName("Beta");
        clientUserBeta.setLastName("User");
        clientUserBeta.setRole(Role.CLIENT_USER);
        clientUserBeta.setOrganization(orgBeta);
        clientUserBeta.setActive(true);
        clientUserBeta = userRepository.saveAndFlush(clientUserBeta);

        projAcme = projectRepository.saveAndFlush(new Project(orgAcme, "Acme Web", "PROJ-A", "Acme Web App"));
        projBeta = projectRepository.saveAndFlush(new Project(orgBeta, "Beta Mobile", "PROJ-B", "Beta Mobile App"));

        membershipRepository.saveAndFlush(new ProjectMembership(projAcme, clientUserAcme));
        membershipRepository.saveAndFlush(new ProjectMembership(projBeta, clientUserBeta));

        // Default verificationStatus = PENDING_VERIFICATION, stage = SUBMITTED
        issueAcme1 = issueRepository.saveAndFlush(new Issue(projAcme, null, clientUserAcme,
                "Acme Issue 1 Login Bug", "Description 1", IssueType.BUG, IssuePriority.HIGH));

        issueAcme2 = issueRepository.saveAndFlush(new Issue(projAcme, null, clientUserAcme,
                "Acme Issue 2 Payment Error", "Description 2", IssueType.BUG, IssuePriority.URGENT));

        issueBeta1 = issueRepository.saveAndFlush(new Issue(projBeta, null, clientUserBeta,
                "Beta Issue 1 UI Bug", "Description 3", IssueType.ENHANCEMENT, IssuePriority.MEDIUM));

        tokenAppAdmin = jwtTokenProvider.generateToken(appAdmin);
        tokenClientAdminAcme = jwtTokenProvider.generateToken(clientAdminAcme);
        tokenClientUserAcme = jwtTokenProvider.generateToken(clientUserAcme);
        tokenClientAdminBeta = jwtTokenProvider.generateToken(clientAdminBeta);
        tokenClientUserBeta = jwtTokenProvider.generateToken(clientUserBeta);
    }

    // =========================================================================
    // 1. Verification Queue Tests
    // =========================================================================

    @Test
    @DisplayName("APP_ADMIN can list all pending verification issues across all organizations")
    void getVerificationQueue_appAdmin_returnsAllPending() throws Exception {
        mockMvc.perform(get("/api/issues/verification-queue")
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    @DisplayName("CLIENT_ADMIN can list only own-organization pending verification issues")
    void getVerificationQueue_clientAdmin_returnsOwnOrgPendingOnly() throws Exception {
        mockMvc.perform(get("/api/issues/verification-queue")
                        .header("Authorization", "Bearer " + tokenClientAdminAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(
                        issueAcme1.getId().toString(),
                        issueAcme2.getId().toString()
                )));
    }

    @Test
    @DisplayName("CLIENT_USER receives 403 Forbidden when accessing verification queue")
    void getVerificationQueue_clientUser_returns403() throws Exception {
        mockMvc.perform(get("/api/issues/verification-queue")
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Verification queue excludes issues that are already VERIFIED or REJECTED")
    void getVerificationQueue_excludesDecidedIssues() throws Exception {
        issueAcme1.setVerificationStatus(VerificationStatus.VERIFIED);
        issueRepository.saveAndFlush(issueAcme1);

        mockMvc.perform(get("/api/issues/verification-queue")
                        .header("Authorization", "Bearer " + tokenClientAdminAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(issueAcme2.getId().toString())));
    }

    @Test
    @DisplayName("Verification queue supports filtering by projectId and search term")
    void getVerificationQueue_filteringAndPagination() throws Exception {
        mockMvc.perform(get("/api/issues/verification-queue")
                        .param("projectId", projAcme.getId().toString())
                        .param("search", "Login")
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(issueAcme1.getId().toString())));
    }

    // =========================================================================
    // 2. Verification Decision API Tests
    // =========================================================================

    @Test
    @DisplayName("APP_ADMIN can approve verification (VERIFIED); moves status to VERIFIED and stage SUBMITTED -> RECEIVED")
    void updateVerificationStatus_appAdmin_verified_success() throws Exception {
        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.VERIFIED);

        mockMvc.perform(patch("/api/issues/" + issueAcme1.getId() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(issueAcme1.getId().toString())))
                .andExpect(jsonPath("$.verificationStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.stage", is("RECEIVED")));

        Issue updated = issueRepository.findById(issueAcme1.getId()).orElseThrow();
        assertEquals(VerificationStatus.VERIFIED, updated.getVerificationStatus());
        assertEquals(IssueStage.RECEIVED, updated.getStage());

        // Verify Audit Log creation
        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme1.getId());
        assertTrue(audits.size() >= 1);

        boolean foundVerificationAudit = audits.stream().anyMatch(a ->
                a.getAction() == AuditEventType.VERIFICATION_CHANGED &&
                "verificationStatus".equals(a.getFieldName()) &&
                "PENDING_VERIFICATION".equals(a.getOldValue()) &&
                "VERIFIED".equals(a.getNewValue()) &&
                a.getActor().getId().equals(appAdmin.getId())
        );
        assertTrue(foundVerificationAudit, "VERIFICATION_CHANGED audit log must exist");

        boolean foundStageAudit = audits.stream().anyMatch(a ->
                a.getAction() == AuditEventType.STAGE_CHANGED &&
                "SUBMITTED".equals(a.getOldValue()) &&
                "RECEIVED".equals(a.getNewValue())
        );
        assertTrue(foundStageAudit, "STAGE_CHANGED audit log must exist when stage auto-transitions");
    }

    @Test
    @DisplayName("CLIENT_ADMIN can reject verification (REJECTED); status becomes REJECTED and stage remains SUBMITTED")
    void updateVerificationStatus_clientAdmin_rejected_success() throws Exception {
        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.REJECTED);

        mockMvc.perform(patch("/api/issues/" + issueAcme1.getId() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenClientAdminAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("REJECTED")))
                .andExpect(jsonPath("$.stage", is("SUBMITTED")));

        Issue updated = issueRepository.findById(issueAcme1.getId()).orElseThrow();
        assertEquals(VerificationStatus.REJECTED, updated.getVerificationStatus());
        assertEquals(IssueStage.SUBMITTED, updated.getStage());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme1.getId());
        boolean foundVerificationAudit = audits.stream().anyMatch(a ->
                a.getAction() == AuditEventType.VERIFICATION_CHANGED &&
                "verificationStatus".equals(a.getFieldName()) &&
                "PENDING_VERIFICATION".equals(a.getOldValue()) &&
                "REJECTED".equals(a.getNewValue()) &&
                a.getActor().getId().equals(clientAdminAcme.getId())
        );
        assertTrue(foundVerificationAudit);
    }

    @Test
    @DisplayName("CLIENT_ADMIN modifying another organization's issue returns 403 Forbidden")
    void updateVerificationStatus_clientAdmin_crossOrg_returns403() throws Exception {
        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.VERIFIED);

        mockMvc.perform(patch("/api/issues/" + issueBeta1.getId() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenClientAdminAcme))
                .andExpect(status().isForbidden());

        assertEquals(0, auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueBeta1.getId()).size());
    }

    @Test
    @DisplayName("CLIENT_USER attempting verification decision returns 403 Forbidden")
    void updateVerificationStatus_clientUser_returns403() throws Exception {
        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.VERIFIED);

        mockMvc.perform(patch("/api/issues/" + issueAcme1.getId() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Attempting decision on an issue that is already VERIFIED returns 400 Bad Request")
    void updateVerificationStatus_alreadyVerified_returns400() throws Exception {
        issueAcme1.setVerificationStatus(VerificationStatus.VERIFIED);
        issueRepository.saveAndFlush(issueAcme1);

        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.REJECTED);

        mockMvc.perform(patch("/api/issues/" + issueAcme1.getId() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not pending verification")));
    }

    @Test
    @DisplayName("Attempting decision on an issue that is already REJECTED returns 400 Bad Request")
    void updateVerificationStatus_alreadyRejected_returns400() throws Exception {
        issueAcme1.setVerificationStatus(VerificationStatus.REJECTED);
        issueRepository.saveAndFlush(issueAcme1);

        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.VERIFIED);

        mockMvc.perform(patch("/api/issues/" + issueAcme1.getId() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not pending verification")));
    }

    @Test
    @DisplayName("Passing PENDING_VERIFICATION in decision request returns 400 Bad Request and creates no audit")
    void updateVerificationStatus_invalidStatusPending_returns400AndNoAudit() throws Exception {
        int initialAuditCount = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme1.getId()).size();
        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.PENDING_VERIFICATION);

        mockMvc.perform(patch("/api/issues/" + issueAcme1.getId() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must be VERIFIED or REJECTED")));

        int finalAuditCount = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme1.getId()).size();
        assertEquals(initialAuditCount, finalAuditCount, "Failed decision must create no audit log");
    }

    @Test
    @DisplayName("Reworking a REJECTED issue via PUT /api/issues/{id} resets status to PENDING_VERIFICATION and returns to queue")
    void reworkRejectedIssue_resetsToPendingVerificationAndAppearsInQueue() throws Exception {
        // 1. Reject issueAcme1
        issueAcme1.setVerificationStatus(VerificationStatus.REJECTED);
        issueRepository.saveAndFlush(issueAcme1);

        // 2. Reporter edits/reworks issue
        UpdateIssueRequest updateRequest = new UpdateIssueRequest(
                "Acme Issue 1 Login Bug Reworked",
                "Updated description with more details for verification",
                IssueType.BUG,
                IssuePriority.HIGH,
                null
        );

        mockMvc.perform(put("/api/issues/" + issueAcme1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus", is("PENDING_VERIFICATION")));

        Issue updated = issueRepository.findById(issueAcme1.getId()).orElseThrow();
        assertEquals(VerificationStatus.PENDING_VERIFICATION, updated.getVerificationStatus());

        // 3. Verify audit log REJECTED -> PENDING_VERIFICATION
        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme1.getId());
        boolean foundReworkAudit = audits.stream().anyMatch(a ->
                a.getAction() == AuditEventType.VERIFICATION_CHANGED &&
                "verificationStatus".equals(a.getFieldName()) &&
                "REJECTED".equals(a.getOldValue()) &&
                "PENDING_VERIFICATION".equals(a.getNewValue()) &&
                a.getActor().getId().equals(clientUserAcme.getId())
        );
        assertTrue(foundReworkAudit, "Rework audit log REJECTED -> PENDING_VERIFICATION must exist");

        // 4. Verify issue reappears in verification queue
        mockMvc.perform(get("/api/issues/verification-queue")
                        .header("Authorization", "Bearer " + tokenClientAdminAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(issueAcme1.getId().toString())));
    }

    @Test
    @DisplayName("Non-existent issue ID returns 404 Not Found")
    void updateVerificationStatus_nonExistentIssue_returns404() throws Exception {
        UpdateVerificationStatusRequest request = new UpdateVerificationStatusRequest(VerificationStatus.VERIFIED);

        mockMvc.perform(patch("/api/issues/" + UUID.randomUUID() + "/verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isNotFound());
    }
}
