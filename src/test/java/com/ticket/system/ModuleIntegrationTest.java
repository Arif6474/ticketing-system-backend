package com.ticket.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.system.dto.request.CreateModuleRequest;
import com.ticket.system.dto.request.UpdateModuleRequest;
import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Module;
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
public class ModuleIntegrationTest {

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
    private Module moduleAuth;
    private Module modulePayment;

    @BeforeEach
    void setUp() {
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

        // Assign clientUserAcme as member of projectAcme
        membershipRepository.saveAndFlush(new ProjectMembership(projectAcme, clientUserAcme));

        moduleAuth = moduleRepository.saveAndFlush(new Module(projectAcme, "Authentication", "Auth Module"));
        modulePayment = moduleRepository.saveAndFlush(new Module(projectBeta, "Payment Gateway", "Payment Module"));
    }

    // =========================================================================
    // 1. APP_ADMIN Operations Tests
    // =========================================================================
    @Test
    @DisplayName("APP_ADMIN can create module across any project")
    void testAppAdminCreateModule() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateModuleRequest request = new CreateModuleRequest(projectAcme.getId(), "User Management", "User Mgmt Module");

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("User Management")))
                .andExpect(jsonPath("$.projectId", is(projectAcme.getId().toString())))
                .andExpect(jsonPath("$.isActive", is(true)));
    }

    @Test
    @DisplayName("APP_ADMIN can list all modules across all organizations")
    void testAppAdminListModules() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(get("/api/modules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("APP_ADMIN can update module")
    void testAppAdminUpdateModule() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        UpdateModuleRequest request = new UpdateModuleRequest("Auth Security", "Updated Auth", false);

        mockMvc.perform(put("/api/modules/" + moduleAuth.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Auth Security")))
                .andExpect(jsonPath("$.isActive", is(false)));
    }

    @Test
    @DisplayName("APP_ADMIN can delete module")
    void testAppAdminDeleteModule() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(delete("/api/modules/" + modulePayment.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));

        assertTrue(moduleRepository.findById(modulePayment.getId()).isEmpty());
    }

    // =========================================================================
    // 2. CLIENT_ADMIN Operations & Cross-Tenant Access Tests
    // =========================================================================
    @Test
    @DisplayName("CLIENT_ADMIN can create module in own organization project")
    void testClientAdminCreateModuleSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        CreateModuleRequest request = new CreateModuleRequest(projectAcme.getId(), "Dashboard", "Dashboard Module");

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Dashboard")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot create module in another organization project")
    void testClientAdminCreateModuleOtherOrgForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);
        CreateModuleRequest request = new CreateModuleRequest(projectBeta.getId(), "Hack Module", "Hack");

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_ADMIN lists modules only for projects in own organization")
    void testClientAdminListModulesOwnOrgOnly() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(get("/api/modules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Authentication")));
    }

    @Test
    @DisplayName("CLIENT_ADMIN cannot access module details of another organization")
    void testClientAdminGetOtherOrgModuleForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientAdminAcme);

        mockMvc.perform(get("/api/modules/" + modulePayment.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. CLIENT_USER Operations & Membership Boundary Tests
    // =========================================================================
    @Test
    @DisplayName("CLIENT_USER can view module of a project they belong to")
    void testClientUserGetMemberProjectModuleSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        mockMvc.perform(get("/api/modules/" + moduleAuth.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Authentication")));
    }

    @Test
    @DisplayName("CLIENT_USER cannot view module of a project they do not belong to")
    void testClientUserGetNonMemberProjectModuleForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        mockMvc.perform(get("/api/modules/" + modulePayment.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENT_USER receives 403 when trying to create, update, or delete module")
    void testClientUserMutationForbidden() throws Exception {
        String token = jwtTokenProvider.generateToken(clientUserAcme);

        CreateModuleRequest createReq = new CreateModuleRequest(projectAcme.getId(), "NewMod", "Desc");
        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());

        UpdateModuleRequest updateReq = new UpdateModuleRequest("UpdatedMod", "Desc", true);
        mockMvc.perform(put("/api/modules/" + moduleAuth.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/modules/" + moduleAuth.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. Validation, Uniqueness & Data Integrity Tests
    // =========================================================================
    @Test
    @DisplayName("Duplicate module name within same project returns 400 Bad Request")
    void testDuplicateModuleNameInSameProjectRejected() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateModuleRequest request = new CreateModuleRequest(projectAcme.getId(), "authentication", "Duplicate");

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists for this project")));
    }

    @Test
    @DisplayName("Same module name in different projects succeeds")
    void testSameModuleNameDifferentProjectsSuccess() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateModuleRequest request = new CreateModuleRequest(projectBeta.getId(), "Authentication", "Same Name Different Project");

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Authentication")));
    }

    @Test
    @DisplayName("Project deletion is blocked when modules exist for the project")
    void testProjectDeletionBlockedWhenModulesExist() throws Exception {
        String token = jwtTokenProvider.generateToken(appAdmin);

        mockMvc.perform(delete("/api/projects/" + projectAcme.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("associated modules")));
    }

    @Test
    @DisplayName("Creating a module for an inactive project fails")
    void testCreateModuleInactiveProjectFails() throws Exception {
        projectAcme.setActive(false);
        projectRepository.saveAndFlush(projectAcme);

        String token = jwtTokenProvider.generateToken(appAdmin);
        CreateModuleRequest request = new CreateModuleRequest(projectAcme.getId(), "New Module", "Desc");

        mockMvc.perform(post("/api/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("inactive project")));
    }
}
