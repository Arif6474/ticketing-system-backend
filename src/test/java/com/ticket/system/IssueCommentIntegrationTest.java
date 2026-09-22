package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.CreateIssueCommentRequest;
import com.ticket.system.dto.request.UpdateIssueCommentRequest;
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
public class IssueCommentIntegrationTest {

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
    private IssueCommentRepository commentRepository;

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
    private User clientAdminBeta;
    private User clientUserBeta;

    private ClientOrganization orgAcme;
    private ClientOrganization orgBeta;

    private Project projAcme;
    private Project projBeta;

    private Issue issueAcme;
    private Issue issueBeta;

    private String tokenAppAdmin;
    private String tokenClientAdminAcme;
    private String tokenClientUserAcme;
    private String tokenClientUserAcme2;
    private String tokenClientAdminBeta;
    private String tokenClientUserBeta;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
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

        clientUserAcme2 = new User();
        clientUserAcme2.setEmail("user2@acme.com");
        clientUserAcme2.setPasswordHash(passwordEncoder.encode("Password123!"));
        clientUserAcme2.setFirstName("Acme2");
        clientUserAcme2.setLastName("User");
        clientUserAcme2.setRole(Role.CLIENT_USER);
        clientUserAcme2.setOrganization(orgAcme);
        clientUserAcme2.setActive(true);
        clientUserAcme2 = userRepository.saveAndFlush(clientUserAcme2);

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
        membershipRepository.saveAndFlush(new ProjectMembership(projAcme, clientUserAcme2));
        membershipRepository.saveAndFlush(new ProjectMembership(projBeta, clientUserBeta));

        issueAcme = issueRepository.saveAndFlush(new Issue(projAcme, null, clientUserAcme,
                "Acme Issue", "Description", IssueType.BUG, IssuePriority.HIGH));
        issueBeta = issueRepository.saveAndFlush(new Issue(projBeta, null, clientUserBeta,
                "Beta Issue", "Description", IssueType.NEW_FEATURE, IssuePriority.MEDIUM));

        tokenAppAdmin = jwtTokenProvider.generateToken(appAdmin);
        tokenClientAdminAcme = jwtTokenProvider.generateToken(clientAdminAcme);
        tokenClientUserAcme = jwtTokenProvider.generateToken(clientUserAcme);
        tokenClientUserAcme2 = jwtTokenProvider.generateToken(clientUserAcme2);
        tokenClientAdminBeta = jwtTokenProvider.generateToken(clientAdminBeta);
        tokenClientUserBeta = jwtTokenProvider.generateToken(clientUserBeta);
    }

    // =========================================================================
    // 1. Comment Creation Tests
    // =========================================================================

    @Test
    @DisplayName("Authenticated authorized user can create a comment; author comes from security context")
    void createComment_success() throws Exception {
        CreateIssueCommentRequest request = new CreateIssueCommentRequest("This issue is reproduced.");

        mockMvc.perform(post("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.issueId", is(issueAcme.getId().toString())))
                .andExpect(jsonPath("$.author.id", is(clientUserAcme.getId().toString())))
                .andExpect(jsonPath("$.content", is("This issue is reproduced.")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        List<IssueComment> comments = commentRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme.getId());
        assertEquals(1, comments.size());
        assertEquals(clientUserAcme.getId(), comments.get(0).getAuthor().getId());
    }

    @Test
    @DisplayName("Unauthenticated request to create comment is rejected")
    void createComment_unauthenticated_rejected() throws Exception {
        CreateIssueCommentRequest request = new CreateIssueCommentRequest("Comment content");

        mockMvc.perform(post("/api/issues/" + issueAcme.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Blank content is rejected")
    void createComment_blankContent_rejected() throws Exception {
        CreateIssueCommentRequest request = new CreateIssueCommentRequest("   ");

        mockMvc.perform(post("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Creating comment for non-existent issue returns 404")
    void createComment_issueNotFound() throws Exception {
        CreateIssueCommentRequest request = new CreateIssueCommentRequest("Comment content");

        mockMvc.perform(post("/api/issues/" + UUID.randomUUID() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User unauthorized to access issue cannot create comment")
    void createComment_unauthorizedIssueAccess_rejected() throws Exception {
        CreateIssueCommentRequest request = new CreateIssueCommentRequest("Malicious comment");

        // ClientUserBeta trying to comment on Acme issue
        mockMvc.perform(post("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientUserBeta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. Reading Comments Tests
    // =========================================================================

    @Test
    @DisplayName("Comments are returned in chronological order")
    void getComments_chronologicalOrder() throws Exception {
        commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "First comment"));
        commentRepository.saveAndFlush(new IssueComment(issueAcme, clientAdminAcme, "Second comment"));

        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].content", is("First comment")))
                .andExpect(jsonPath("$[1].content", is("Second comment")));
    }

    @Test
    @DisplayName("APP_ADMIN can view comments for any issue")
    void getComments_appAdmin_success() throws Exception {
        commentRepository.saveAndFlush(new IssueComment(issueBeta, clientUserBeta, "Beta comment"));

        mockMvc.perform(get("/api/issues/" + issueBeta.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("CLIENT_ADMIN can view comments for own org issue but rejected for another org")
    void getComments_clientAdmin_authorization() throws Exception {
        commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Acme comment"));

        // Same org
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientAdminAcme))
                .andExpect(status().isOk());

        // Cross org
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientAdminBeta))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_USER can view comments for member project issue but rejected for non-member")
    void getComments_clientUser_authorization() throws Exception {
        commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Acme comment"));

        // Member project
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk());

        // Non-member project
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/comments")
                        .header("Authorization", "Bearer " + tokenClientUserBeta))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Edit Comment Tests
    // =========================================================================

    @Test
    @DisplayName("Author can edit own comment")
    void editComment_author_success() throws Exception {
        IssueComment comment = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Original comment"));
        UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("Updated comment content");

        mockMvc.perform(put("/api/issues/" + issueAcme.getId() + "/comments/" + comment.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Updated comment content")));
    }

    @Test
    @DisplayName("APP_ADMIN can edit any comment")
    void editComment_appAdmin_success() throws Exception {
        IssueComment comment = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Original comment"));
        UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("Admin updated comment");

        mockMvc.perform(put("/api/issues/" + issueAcme.getId() + "/comments/" + comment.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Admin updated comment")));
    }

    @Test
    @DisplayName("Non-author user cannot edit comment")
    void editComment_nonAuthor_rejected() throws Exception {
        IssueComment comment = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Original comment"));
        UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("Unauthorized update");

        // clientUserAcme2 attempting to edit clientUserAcme's comment
        mockMvc.perform(put("/api/issues/" + issueAcme.getId() + "/comments/" + comment.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Editing comment with wrong issueId returns 404 (ID manipulation guard)")
    void editComment_wrongIssueId_rejected() throws Exception {
        IssueComment commentAcme = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Acme comment"));
        UpdateIssueCommentRequest request = new UpdateIssueCommentRequest("Tampered comment");

        // Supplying issueBeta's ID with commentAcme's ID
        mockMvc.perform(put("/api/issues/" + issueBeta.getId() + "/comments/" + commentAcme.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. Delete Comment Tests
    // =========================================================================

    @Test
    @DisplayName("Author can delete own comment")
    void deleteComment_author_success() throws Exception {
        IssueComment comment = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "To be deleted"));

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId() + "/comments/" + comment.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk());

        assertFalse(commentRepository.existsById(comment.getId()));
    }

    @Test
    @DisplayName("APP_ADMIN can delete any comment")
    void deleteComment_appAdmin_success() throws Exception {
        IssueComment comment = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "To be deleted by admin"));

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId() + "/comments/" + comment.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk());

        assertFalse(commentRepository.existsById(comment.getId()));
    }

    @Test
    @DisplayName("Non-author user cannot delete comment")
    void deleteComment_nonAuthor_rejected() throws Exception {
        IssueComment comment = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Protected comment"));

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId() + "/comments/" + comment.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme2))
                .andExpect(status().isForbidden());

        assertTrue(commentRepository.existsById(comment.getId()));
    }

    @Test
    @DisplayName("Deleting comment with wrong issueId returns 404 (ID manipulation guard)")
    void deleteComment_wrongIssueId_rejected() throws Exception {
        IssueComment commentAcme = commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Acme comment"));

        // Supplying issueBeta's ID with commentAcme's ID
        mockMvc.perform(delete("/api/issues/" + issueBeta.getId() + "/comments/" + commentAcme.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isNotFound());

        assertTrue(commentRepository.existsById(commentAcme.getId()));
    }

    @Test
    @DisplayName("Deleting issue with associated comments is blocked with 400 Bad Request")
    void deleteIssueWithComments_blocked() throws Exception {
        commentRepository.saveAndFlush(new IssueComment(issueAcme, clientUserAcme, "Active comment"));

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("associated comments")));

        assertTrue(issueRepository.existsById(issueAcme.getId()));
    }
}
