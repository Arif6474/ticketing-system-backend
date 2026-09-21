package com.ticket.system.service;

import com.ticket.system.dto.request.CreateProjectRequest;
import com.ticket.system.dto.request.UpdateProjectRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.dto.response.ProjectResponse;
import com.ticket.system.entity.ClientOrganization;
import com.ticket.system.entity.Project;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientOrganizationRepository orgRepository;
    private final UserRepository userRepository;
    private final ProjectMembershipRepository membershipRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ClientOrganizationRepository orgRepository,
                          UserRepository userRepository,
                          ProjectMembershipRepository membershipRepository) {
        this.projectRepository = projectRepository;
        this.orgRepository = orgRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public ProjectResponse createProject(UUID currentUserId, CreateProjectRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() != Role.APP_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only APP_ADMIN can create projects");
        }

        ClientOrganization org = orgRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Specified organization does not exist"));

        if (!org.isActive()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot create project for an inactive organization");
        }

        String normalizedCode = request.getShortCode().trim().toUpperCase();
        if (projectRepository.existsByShortCodeIgnoreCase(normalizedCode)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Project short code already exists: " + normalizedCode);
        }

        Project project = new Project(org, request.getName(), normalizedCode, request.getDescription());
        Project saved = projectRepository.save(project);

        return ProjectResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getProjects(UUID currentUserId, int page, int size,
                                                     String search, Boolean active, UUID requestedOrgId) {
        User currentUser = getCurrentUser(currentUserId);

        UUID targetOrgId = requestedOrgId;
        UUID memberUserId = null;

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            targetOrgId = currentUser.getOrganization().getId();
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            memberUserId = currentUser.getId();
            targetOrgId = currentUser.getOrganization().getId();
        }

        Specification<Project> spec = ProjectSpecification.filterProjects(search, active, targetOrgId, memberUserId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").ascending()));

        Page<ProjectResponse> dtoPage = projectRepository.findAll(spec, pageable)
                .map(ProjectResponse::fromEntity);

        return PageResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID currentUserId, UUID projectId) {
        User currentUser = getCurrentUser(currentUserId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot access projects of another organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only access projects where they hold membership");
            }
        }

        return ProjectResponse.fromEntity(project);
    }

    @Transactional
    public ProjectResponse updateProject(UUID currentUserId, UUID projectId, UpdateProjectRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() != Role.APP_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only APP_ADMIN can update projects");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

        String normalizedCode = request.getShortCode().trim().toUpperCase();
        if (projectRepository.existsByShortCodeIgnoreCaseAndIdNot(normalizedCode, projectId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Project short code already exists: " + normalizedCode);
        }

        project.setName(request.getName());
        project.setShortCode(normalizedCode);
        project.setDescription(request.getDescription());
        if (request.getActive() != null) {
            project.setActive(request.getActive());
        }

        Project updated = projectRepository.save(project);
        return ProjectResponse.fromEntity(updated);
    }

    @Transactional
    public GenericResponse deleteProject(UUID currentUserId, UUID projectId) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() != Role.APP_ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only APP_ADMIN can delete projects");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

        projectRepository.delete(project);
        return new GenericResponse("Project deleted successfully");
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
