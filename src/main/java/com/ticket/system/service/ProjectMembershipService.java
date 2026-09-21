package com.ticket.system.service;

import com.ticket.system.dto.request.AddProjectMemberRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.dto.response.ProjectMemberResponse;
import com.ticket.system.entity.Project;
import com.ticket.system.entity.ProjectMembership;
import com.ticket.system.entity.Role;
import com.ticket.system.entity.User;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.ProjectMembershipRepository;
import com.ticket.system.repository.ProjectRepository;
import com.ticket.system.repository.UserRepository;
import com.ticket.system.repository.UserSpecification;
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
public class ProjectMembershipService {

    private final ProjectMembershipRepository membershipRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectMembershipService(ProjectMembershipRepository membershipRepository,
                                    ProjectRepository projectRepository,
                                    UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjectMemberResponse addProjectMember(UUID currentUserId, UUID projectId, AddProjectMemberRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to manage project memberships");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

        if (!project.isActive()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot add members to an inactive project");
        }

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only manage memberships for projects in their own organization");
            }
        }

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User to add not found"));

        if (!targetUser.isActive()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot add an inactive user to a project");
        }

        // Cross-Tenant Membership Guard: User must belong to the project's organization
        if (targetUser.getRole() == Role.APP_ADMIN ||
            targetUser.getOrganization() == null ||
            !Objects.equals(targetUser.getOrganization().getId(), project.getOrganization().getId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User must belong to the project's client organization");
        }

        if (membershipRepository.existsByProjectIdAndUserId(projectId, targetUser.getId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "User is already a member of this project");
        }

        ProjectMembership membership = new ProjectMembership(project, targetUser);
        ProjectMembership saved = membershipRepository.save(membership);

        return ProjectMemberResponse.fromEntity(saved);
    }

    @Transactional
    public GenericResponse removeProjectMember(UUID currentUserId, UUID projectId, UUID targetUserId) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to remove project members");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only manage memberships for projects in their own organization");
            }
        }

        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, targetUserId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Membership record not found"));

        membershipRepository.delete(membership);
        return new GenericResponse("Project member removed successfully");
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectMemberResponse> getProjectMembers(UUID currentUserId, UUID projectId,
                                                                 int page, int size, String search) {
        User currentUser = getCurrentUser(currentUserId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only view members of projects in their own organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(projectId, currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only view members of projects where they hold membership");
            }
        }

        // JPA Specification filtering for members belonging to projectId
        Specification<ProjectMembership> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (org.springframework.util.StringUtils.hasText(search)) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                var userRoot = root.get("user");
                var emailPred = cb.like(cb.lower(userRoot.get("email")), searchPattern);
                var firstPred = cb.like(cb.lower(userRoot.get("firstName")), searchPattern);
                var lastPred = cb.like(cb.lower(userRoot.get("lastName")), searchPattern);
                predicates.add(cb.or(emailPred, firstPred, lastPred));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProjectMemberResponse> memberPage = membershipRepository.findAll(spec, pageable)
                .map(ProjectMemberResponse::fromEntity);

        return PageResponse.fromPage(memberPage);
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
