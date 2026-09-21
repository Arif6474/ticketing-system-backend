package com.ticket.system.service;

import com.ticket.system.dto.request.CreateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.IssueResponse;
import com.ticket.system.dto.response.PageResponse;
import com.ticket.system.entity.*;
import com.ticket.system.entity.Module;
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
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final ProjectMembershipRepository membershipRepository;

    public IssueService(IssueRepository issueRepository,
                        ProjectRepository projectRepository,
                        ModuleRepository moduleRepository,
                        UserRepository userRepository,
                        ProjectMembershipRepository membershipRepository) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public IssueResponse createIssue(UUID currentUserId, CreateIssueRequest request) {
        User currentUser = getCurrentUser(currentUserId);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Specified project does not exist"));

        if (!project.isActive()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot create issue for an inactive project");
        }

        // Authorization check for project access
        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only create issues for projects in their own organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(project.getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only create issues for member projects");
            }
        }

        // Validate module if supplied
        Module module = null;
        if (request.getModuleId() != null) {
            module = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Specified module does not exist"));

            if (!module.isActive()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Cannot attach an inactive module to an issue");
            }

            if (!Objects.equals(module.getProject().getId(), project.getId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Specified module does not belong to the selected project");
            }
        }

        Issue issue = new Issue(
                project,
                module,
                currentUser,
                request.getTitle().trim(),
                request.getDescription().trim(),
                request.getType(),
                request.getPriority()
        );

        Issue saved = issueRepository.save(issue);
        return IssueResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getIssues(UUID currentUserId, int page, int size, String search,
                                                 UUID requestedProjectId, UUID requestedModuleId,
                                                 IssueType type, IssuePriority priority, IssueStage stage,
                                                 VerificationStatus verificationStatus, UUID reporterId) {
        User currentUser = getCurrentUser(currentUserId);

        UUID targetOrgId = null;
        UUID memberUserId = null;

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            targetOrgId = currentUser.getOrganization().getId();
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            memberUserId = currentUser.getId();
            targetOrgId = currentUser.getOrganization().getId();
        }

        // If a specific project is requested, verify accessibility
        if (requestedProjectId != null) {
            Project project = projectRepository.findById(requestedProjectId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Project not found"));

            if (currentUser.getRole() == Role.CLIENT_ADMIN) {
                if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId())) {
                    throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot access issues of another organization's project");
                }
            } else if (currentUser.getRole() == Role.CLIENT_USER) {
                if (!Objects.equals(project.getOrganization().getId(), currentUser.getOrganization().getId()) ||
                    !membershipRepository.existsByProjectIdAndUserId(requestedProjectId, currentUser.getId())) {
                    throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only access issues of member projects");
                }
            }
        }

        Specification<Issue> spec = IssueSpecification.filterIssues(
                search, requestedProjectId, requestedModuleId, type, priority,
                stage, verificationStatus, reporterId, targetOrgId, memberUserId
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").ascending()));

        Page<IssueResponse> dtoPage = issueRepository.findAll(spec, pageable)
                .map(IssueResponse::fromEntity);

        return PageResponse.fromPage(dtoPage);
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssueById(UUID currentUserId, UUID issueId) {
        User currentUser = getCurrentUser(currentUserId);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot access issues of another organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(issue.getProject().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only access issues of member projects");
            }
        }

        return IssueResponse.fromEntity(issue);
    }

    @Transactional
    public IssueResponse updateIssue(UUID currentUserId, UUID issueId, UpdateIssueRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only update issues in their own organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(issue.getReporter().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only edit issues that they reported");
            }
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(issue.getProject().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only edit issues in member projects");
            }
        }

        // Validate module if updated
        Module module = null;
        if (request.getModuleId() != null) {
            module = moduleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Specified module does not exist"));

            if (!module.isActive()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Cannot attach an inactive module to an issue");
            }

            if (!Objects.equals(module.getProject().getId(), issue.getProject().getId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "Specified module does not belong to the selected project");
            }
        }

        issue.setTitle(request.getTitle().trim());
        issue.setDescription(request.getDescription().trim());
        issue.setType(request.getType());
        issue.setPriority(request.getPriority());
        issue.setModule(module);

        Issue updated = issueRepository.save(issue);
        return IssueResponse.fromEntity(updated);
    }

    @Transactional
    public GenericResponse deleteIssue(UUID currentUserId, UUID issueId) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to delete issues");
        }

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only delete issues in their own organization");
            }
        }

        issueRepository.delete(issue);
        return new GenericResponse("Issue deleted successfully");
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
