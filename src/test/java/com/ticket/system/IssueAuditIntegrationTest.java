package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.CreateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueStageRequest;
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
public class IssueAuditIntegrationTest {

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

    private Module modAcme1;
    private Module modAcme2;

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
        moduleRepository.deleteAll();
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
        clientUserAcme.setEmail("user@acme.com");
        clientUserAcme.setPasswordHash(passwordEncoder.encode("Password123!"));
        clientUserAcme.setFirstName("Acme");
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

        modAcme1 = moduleRepository.saveAndFlush(new Module(projAcme, "Auth Module", "Authentication"));
        modAcme2 = moduleRepository.saveAndFlush(new Module(projAcme, "Billing Module", "Billing"));

        membershipRepository.saveAndFlush(new ProjectMembership(projAcme, clientUserAcme));
        membershipRepository.saveAndFlush(new ProjectMembership(projBeta, clientUserBeta));

        tokenAppAdmin = jwtTokenProvider.generateToken(appAdmin);
        tokenClientAdminAcme = jwtTokenProvider.generateToken(clientAdminAcme);
        tokenClientUserAcme = jwtTokenProvider.generateToken(clientUserAcme);
        tokenClientAdminBeta = jwtTokenProvider.generateToken(clientAdminBeta);
        tokenClientUserBeta = jwtTokenProvider.generateToken(clientUserBeta);
    }

    @Test
    @DisplayName("Issue creation creates ISSUE_CREATED audit record with authenticated actor")
    void issueCreation_createsAuditEntry() throws Exception {
        CreateIssueRequest request = new CreateIssueRequest(
                "Initial Title",
                "Initial Description",
                IssueType.BUG,
                IssuePriority.HIGH,
                projAcme.getId(),
                modAcme1.getId()
        );

        String jsonResponse = mockMvc.perform(post("/api/issues")
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        UUID issueId = UUID.fromString(objectMapper.readTree(jsonResponse).get("id").asText());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueId);
        assertEquals(1, audits.size());

        IssueAudit audit = audits.get(0);
        assertEquals(AuditEventType.ISSUE_CREATED, audit.getAction());
        assertNull(audit.getFieldName());
        assertNull(audit.getOldValue());
        assertNull(audit.getNewValue());
        assertEquals(clientUserAcme.getId(), audit.getActor().getId());
    }

    @Test
    @DisplayName("Single field change creates one FIELD_CHANGED audit entry")
    void singleFieldChange_createsOneAuditEntry() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Old Title", "Same Description", IssueType.BUG, IssuePriority.MEDIUM));

        UpdateIssueRequest request = new UpdateIssueRequest(
                "New Title",
                "Same Description",
                IssueType.BUG,
                IssuePriority.MEDIUM,
                modAcme1.getId()
        );

        mockMvc.perform(put("/api/issues/" + issue.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issue.getId());
        assertEquals(1, audits.size());

        IssueAudit audit = audits.get(0);
        assertEquals(AuditEventType.FIELD_CHANGED, audit.getAction());
        assertEquals("title", audit.getFieldName());
        assertEquals("Old Title", audit.getOldValue());
        assertEquals("New Title", audit.getNewValue());
        assertEquals(clientUserAcme.getId(), audit.getActor().getId());
    }

    @Test
    @DisplayName("Multiple field changes create separate audit entries")
    void multipleFieldChanges_createsMultipleAuditEntries() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Old Title", "Old Description", IssueType.BUG, IssuePriority.LOW));

        UpdateIssueRequest request = new UpdateIssueRequest(
                "New Title",
                "Old Description",
                IssueType.NEW_FEATURE,
                IssuePriority.HIGH,
                modAcme2.getId()
        );

        mockMvc.perform(put("/api/issues/" + issue.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issue.getId());
        assertEquals(4, audits.size()); // title, type, priority, module

        assertTrue(audits.stream().anyMatch(a -> "title".equals(a.getFieldName()) && "Old Title".equals(a.getOldValue()) && "New Title".equals(a.getNewValue())));
        assertTrue(audits.stream().anyMatch(a -> "type".equals(a.getFieldName()) && "BUG".equals(a.getOldValue()) && "NEW_FEATURE".equals(a.getNewValue())));
        assertTrue(audits.stream().anyMatch(a -> "priority".equals(a.getFieldName()) && "LOW".equals(a.getOldValue()) && "HIGH".equals(a.getNewValue())));
        assertTrue(audits.stream().anyMatch(a -> "module".equals(a.getFieldName()) && "Auth Module".equals(a.getOldValue()) && "Billing Module".equals(a.getNewValue())));
    }

    @Test
    @DisplayName("Unchanged fields do not create audit entries")
    void unchangedFields_doNotCreateAuditEntries() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Exact Title", "Exact Description", IssueType.ENHANCEMENT, IssuePriority.MEDIUM));

        UpdateIssueRequest request = new UpdateIssueRequest(
                "Exact Title",
                "Exact Description",
                IssueType.ENHANCEMENT,
                IssuePriority.MEDIUM,
                modAcme1.getId()
        );

        mockMvc.perform(put("/api/issues/" + issue.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issue.getId());
        assertEquals(0, audits.size());
    }

    @Test
    @DisplayName("Valid stage transition creates STAGE_CHANGED audit entry")
    void validStageTransition_createsStageChangedAudit() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Stage Issue", "Desc", IssueType.BUG, IssuePriority.HIGH));

        UpdateIssueStageRequest request = new UpdateIssueStageRequest(IssueStage.RECEIVED);

        mockMvc.perform(patch("/api/issues/" + issue.getId() + "/stage")
                        .header("Authorization", "Bearer " + tokenClientAdminAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issue.getId());
        assertEquals(1, audits.size());

        IssueAudit audit = audits.get(0);
        assertEquals(AuditEventType.STAGE_CHANGED, audit.getAction());
        assertEquals("stage", audit.getFieldName());
        assertEquals("SUBMITTED", audit.getOldValue());
        assertEquals("RECEIVED", audit.getNewValue());
        assertEquals(clientAdminAcme.getId(), audit.getActor().getId());
    }

    @Test
    @DisplayName("Invalid stage transition creates no audit entry")
    void invalidStageTransition_createsNoAuditEntry() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Stage Issue", "Desc", IssueType.BUG, IssuePriority.HIGH));

        UpdateIssueStageRequest request = new UpdateIssueStageRequest(IssueStage.DEPLOYED); // Invalid from SUBMITTED

        mockMvc.perform(patch("/api/issues/" + issue.getId() + "/stage")
                        .header("Authorization", "Bearer " + tokenClientAdminAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issue.getId());
        assertEquals(0, audits.size());
    }

    @Test
    @DisplayName("Unauthorized stage transition creates no audit entry")
    void unauthorizedStageTransition_createsNoAuditEntry() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Stage Issue", "Desc", IssueType.BUG, IssuePriority.HIGH));

        UpdateIssueStageRequest request = new UpdateIssueStageRequest(IssueStage.RECEIVED);

        // ClientUserBeta attempting to change stage of Acme issue
        mockMvc.perform(patch("/api/issues/" + issue.getId() + "/stage")
                        .header("Authorization", "Bearer " + tokenClientUserBeta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        List<IssueAudit> audits = auditRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issue.getId());
        assertEquals(0, audits.size());
    }

    @Test
    @DisplayName("Audit retrieval authorization enforcement")
    void auditRetrievalAuthorization() throws Exception {
        Issue issueAcme = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Acme Issue", "Desc", IssueType.BUG, IssuePriority.HIGH));

        // APP_ADMIN can read
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/audits")
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk());

        // CLIENT_ADMIN of same org can read
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/audits")
                        .header("Authorization", "Bearer " + tokenClientAdminAcme))
                .andExpect(status().isOk());

        // CLIENT_ADMIN of another org cannot read
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/audits")
                        .header("Authorization", "Bearer " + tokenClientAdminBeta))
                .andExpect(status().isForbidden());

        // CLIENT_USER of member project can read
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/audits")
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk());

        // CLIENT_USER of another org/non-member project cannot read
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/audits")
                        .header("Authorization", "Bearer " + tokenClientUserBeta))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Audit entries are immutable (no PUT/DELETE endpoints exist)")
    void auditImmutability_noMutationEndpoints() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Audit Issue", "Desc", IssueType.BUG, IssuePriority.HIGH));

        UUID fakeAuditId = UUID.randomUUID();

        int putStatus = mockMvc.perform(put("/api/issues/" + issue.getId() + "/audits/" + fakeAuditId)
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andReturn().getResponse().getStatus();
        assertTrue(putStatus >= 400, "PUT audit mutation endpoint must not succeed");

        int deleteStatus = mockMvc.perform(delete("/api/issues/" + issue.getId() + "/audits/" + fakeAuditId)
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andReturn().getResponse().getStatus();
        assertTrue(deleteStatus >= 400, "DELETE audit mutation endpoint must not succeed");
    }

    @Test
    @DisplayName("Audits are returned in chronological order (oldest to newest)")
    void auditsReturnedInChronologicalOrder() throws Exception {
        Issue issue = issueRepository.saveAndFlush(new Issue(projAcme, modAcme1, clientUserAcme,
                "Title 1", "Desc 1", IssueType.BUG, IssuePriority.LOW));

        // Create issue audit
        auditRepository.saveAndFlush(new IssueAudit(issue, clientUserAcme, AuditEventType.ISSUE_CREATED, null, null, null));
        // Field change audit
        auditRepository.saveAndFlush(new IssueAudit(issue, clientUserAcme, AuditEventType.FIELD_CHANGED, "title", "Title 1", "Title 2"));
        // Stage change audit
        auditRepository.saveAndFlush(new IssueAudit(issue, clientAdminAcme, AuditEventType.STAGE_CHANGED, "stage", "SUBMITTED", "RECEIVED"));

        mockMvc.perform(get("/api/issues/" + issue.getId() + "/audits")
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].action", is("ISSUE_CREATED")))
                .andExpect(jsonPath("$[1].action", is("FIELD_CHANGED")))
                .andExpect(jsonPath("$[2].action", is("STAGE_CHANGED")));
    }
}
