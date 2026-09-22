package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.entity.*;
import com.ticket.system.repository.*;
import com.ticket.system.security.JwtTokenProvider;
import com.ticket.system.storage.MockStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
public class IssueAttachmentIntegrationTest {

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
    private IssueAttachmentRepository attachmentRepository;

    @Autowired
    private ProjectMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MockStorageService mockStorageService;

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
        mockStorageService.clear();
        attachmentRepository.deleteAll();
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
    // 1. Upload Tests
    // =========================================================================

    @Test
    @DisplayName("Authenticated user can upload valid PNG attachment; uploader set from security context")
    void uploadAttachment_png_success() throws Exception {
        byte[] pngContent = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "screenshot.png", "image/png", pngContent);

        String json = mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.issueId", is(issueAcme.getId().toString())))
                .andExpect(jsonPath("$.uploadedBy.id", is(clientUserAcme.getId().toString())))
                .andExpect(jsonPath("$.originalFilename", is("screenshot.png")))
                .andExpect(jsonPath("$.contentType", is("image/png")))
                .andExpect(jsonPath("$.fileSize", is(pngContent.length)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        List<IssueAttachment> attachments = attachmentRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme.getId());
        assertEquals(1, attachments.size());

        IssueAttachment saved = attachments.get(0);
        assertTrue(mockStorageService.hasObject(saved.getObjectKey()));
        assertTrue(saved.getObjectKey().startsWith("issues/" + issueAcme.getId() + "/"));
        assertNotEquals("screenshot.png", saved.getStoredFilename());
    }

    @Test
    @DisplayName("Authenticated user can upload valid PDF attachment")
    void uploadAttachment_pdf_success() throws Exception {
        byte[] pdfContent = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '5'};
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfContent);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType", is("application/pdf")));
    }

    @Test
    @DisplayName("Unauthenticated upload request returns 401")
    void uploadAttachment_unauthenticated_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Upload for non-existent issue returns 404")
    void uploadAttachment_nonExistentIssue_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/api/issues/" + UUID.randomUUID() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CLIENT_ADMIN cross-org upload returns 403 Forbidden")
    void uploadAttachment_clientAdmin_crossOrg_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientAdminBeta))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_USER unauthorized project upload returns 403 Forbidden")
    void uploadAttachment_clientUser_unauthorizedProject_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserBeta))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Uploading file exceeding 10MB limit returns 400 Bad Request")
    void uploadAttachment_fileTooLarge_rejected() throws Exception {
        byte[] largeContent = new byte[10 * 1024 * 1024 + 1]; // 10MB + 1 byte
        MockMultipartFile file = new MockMultipartFile("file", "large.txt", "text/plain", largeContent);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds maximum allowed limit")));
    }

    @Test
    @DisplayName("Unsupported MIME type or invalid signature returns 400 Bad Request")
    void uploadAttachment_unsupportedMimeType_rejected() throws Exception {
        // Executable / shell script
        MockMultipartFile file = new MockMultipartFile("file", "script.sh", "application/x-sh", "#!/bin/bash".getBytes());

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unsupported file type")));
    }

    @Test
    @DisplayName("Mismatched format signature / fake magic bytes returns 400 Bad Request")
    void uploadAttachment_fakeMagicBytes_rejected() throws Exception {
        // File claims to be image/png but content is plain text
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", "Not a real PNG".getBytes());

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match PNG format signature")));
    }

    @Test
    @DisplayName("Valid WebP file upload succeeds")
    void uploadAttachment_validWebp_success() throws Exception {
        byte[] webpHeader = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", webpHeader);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType", is("image/webp")));
    }

    @Test
    @DisplayName("WAV file disguised as WebP (RIFF...WAVE) is rejected with 400 Bad Request")
    void uploadAttachment_wavDisguisedAsWebp_rejected() throws Exception {
        byte[] wavHeader = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'};
        MockMultipartFile file = new MockMultipartFile("file", "audio.webp", "image/webp", wavHeader);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match WEBP format signature")));
    }

    @Test
    @DisplayName("Executable file extension disguised with image/png MIME is rejected with 400 Bad Request")
    void uploadAttachment_executableDisguisedAsPng_rejected() throws Exception {
        byte[] pngHeader = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "image/png", pngHeader);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Executable file extensions are strictly forbidden")));
    }

    @Test
    @DisplayName("PNG file content renamed to .pdf extension is rejected with 400 Bad Request")
    void uploadAttachment_pngRenamedToPdf_rejected() throws Exception {
        byte[] pngHeader = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "image.pdf", "application/pdf", pngHeader);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match PDF format signature")));
    }

    @Test
    @DisplayName("Text file containing binary NUL bytes is rejected with 400 Bad Request")
    void uploadAttachment_textWithNulBytes_rejected() throws Exception {
        byte[] binaryText = new byte[]{'H', 'e', 'l', 'l', 'o', 0x00, 'W', 'o', 'r', 'l', 'd'};
        MockMultipartFile file = new MockMultipartFile("file", "binary.txt", "text/plain", binaryText);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("contains binary NUL bytes")));
    }

    @Test
    @DisplayName("Empty file returns 400 Bad Request")
    void uploadAttachment_emptyFile_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 2. List Attachments Tests
    // =========================================================================

    @Test
    @DisplayName("Authorized users can list metadata in chronological order")
    void getAttachments_chronologicalOrder() throws Exception {
        byte[] pdfContent = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '5'};
        attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "first.pdf", "sub1.pdf", "issues/" + issueAcme.getId() + "/1.pdf", "application/pdf", 8));
        attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme2, "second.txt", "sub2.txt", "issues/" + issueAcme.getId() + "/2.txt", "text/plain", 10));

        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/attachments")
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].originalFilename", is("first.pdf")))
                .andExpect(jsonPath("$[1].originalFilename", is("second.txt")));
    }

    @Test
    @DisplayName("Cross-org list attachments returns 403 Forbidden")
    void getAttachments_crossOrg_rejected() throws Exception {
        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/attachments")
                        .header("Authorization", "Bearer " + tokenClientAdminBeta))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Download Tests
    // =========================================================================

    @Test
    @DisplayName("Authorized user can request presigned download URL")
    void generateDownloadUrl_success() throws Exception {
        IssueAttachment att = attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "file.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));
        mockStorageService.upload(att.getObjectKey(), new java.io.ByteArrayInputStream("Hello".getBytes()), 5, "text/plain");

        mockMvc.perform(get("/api/issues/" + issueAcme.getId() + "/attachments/" + att.getId() + "/download")
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachment.id", is(att.getId().toString())))
                .andExpect(jsonPath("$.downloadUrl", containsString("https://mock-r2.storage.com/issues/" + issueAcme.getId())));
    }

    @Test
    @DisplayName("Cross-issue attachment ID manipulation in download returns 404 Not Found")
    void generateDownloadUrl_crossIssue_rejected() throws Exception {
        IssueAttachment attAcme = attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "acme.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));

        // Supplying issueBeta's ID with attAcme's ID
        mockMvc.perform(get("/api/issues/" + issueBeta.getId() + "/attachments/" + attAcme.getId() + "/download")
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. Delete Attachment Tests
    // =========================================================================

    @Test
    @DisplayName("Uploader can delete own attachment")
    void deleteAttachment_uploader_success() throws Exception {
        IssueAttachment att = attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "del.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));
        mockStorageService.upload(att.getObjectKey(), new java.io.ByteArrayInputStream("Hello".getBytes()), 5, "text/plain");

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId() + "/attachments/" + att.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isOk());

        assertFalse(attachmentRepository.existsById(att.getId()));
        assertFalse(mockStorageService.hasObject(att.getObjectKey()));
    }

    @Test
    @DisplayName("APP_ADMIN can delete any attachment")
    void deleteAttachment_appAdmin_success() throws Exception {
        IssueAttachment att = attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "del.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));
        mockStorageService.upload(att.getObjectKey(), new java.io.ByteArrayInputStream("Hello".getBytes()), 5, "text/plain");

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId() + "/attachments/" + att.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isOk());

        assertFalse(attachmentRepository.existsById(att.getId()));
    }

    @Test
    @DisplayName("Non-uploader user cannot delete attachment")
    void deleteAttachment_nonUploader_rejected() throws Exception {
        IssueAttachment att = attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "del.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId() + "/attachments/" + att.getId())
                        .header("Authorization", "Bearer " + tokenClientUserAcme2))
                .andExpect(status().isForbidden());

        assertTrue(attachmentRepository.existsById(att.getId()));
    }

    @Test
    @DisplayName("Deleting attachment with wrong issueId returns 404 (ID manipulation guard)")
    void deleteAttachment_wrongIssueId_rejected() throws Exception {
        IssueAttachment attAcme = attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "acme.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));

        // Supplying issueBeta's ID with attAcme's ID
        mockMvc.perform(delete("/api/issues/" + issueBeta.getId() + "/attachments/" + attAcme.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isNotFound());

        assertTrue(attachmentRepository.existsById(attAcme.getId()));
    }

    // =========================================================================
    // 5. Data Integrity & Storage Failures
    // =========================================================================

    @Test
    @DisplayName("Deleting issue with associated attachments is blocked with 400 Bad Request")
    void deleteIssueWithAttachments_blocked() throws Exception {
        attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "att.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("associated attachments")));

        assertTrue(issueRepository.existsById(issueAcme.getId()));
    }

    @Test
    @DisplayName("R2 storage upload failure does not persist DB metadata")
    void storageUploadFailure_doesNotPersistMetadata() throws Exception {
        mockStorageService.setFailUpload(true);
        MockMultipartFile file = new MockMultipartFile("file", "fail.txt", "text/plain", "Hello".getBytes());

        mockMvc.perform(multipart("/api/issues/" + issueAcme.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenClientUserAcme))
                .andExpect(status().isInternalServerError());

        assertEquals(0, attachmentRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueAcme.getId()).size());
    }

    @Test
    @DisplayName("R2 deletion failure prevents DB metadata deletion")
    void storageDeleteFailure_doesNotDeleteMetadata() throws Exception {
        IssueAttachment att = attachmentRepository.saveAndFlush(new IssueAttachment(issueAcme, clientUserAcme, "del.txt", "stored.txt", "issues/" + issueAcme.getId() + "/stored.txt", "text/plain", 5));
        mockStorageService.upload(att.getObjectKey(), new java.io.ByteArrayInputStream("Hello".getBytes()), 5, "text/plain");

        mockStorageService.setFailDelete(true);

        mockMvc.perform(delete("/api/issues/" + issueAcme.getId() + "/attachments/" + att.getId())
                        .header("Authorization", "Bearer " + tokenAppAdmin))
                .andExpect(status().isInternalServerError());

        assertTrue(attachmentRepository.existsById(att.getId()));
    }
}
