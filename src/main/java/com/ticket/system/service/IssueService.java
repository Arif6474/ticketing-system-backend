package com.ticket.system.service;

import com.ticket.system.dto.request.CreateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueRequest;
import com.ticket.system.dto.request.UpdateIssueStageRequest;
import com.ticket.system.dto.request.UpdateVerificationStatusRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.IssueAuditResponse;
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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final IssueStageTransitionService stageTransitionService;
    private final IssueAuditService issueAuditService;
    private final IssueCommentRepository commentRepository;
    private final IssueAttachmentRepository attachmentRepository;

    public IssueService(IssueRepository issueRepository,
                        ProjectRepository projectRepository,
                        ModuleRepository moduleRepository,
                        UserRepository userRepository,
                        ProjectMembershipRepository membershipRepository,
                        IssueStageTransitionService stageTransitionService,
                        IssueAuditService issueAuditService,
                        IssueCommentRepository commentRepository,
                        IssueAttachmentRepository attachmentRepository) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.stageTransitionService = stageTransitionService;
        this.issueAuditService = issueAuditService;
        this.commentRepository = commentRepository;
        this.attachmentRepository = attachmentRepository;
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
        issueAuditService.recordIssueCreated(saved, currentUser);
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

        String oldTitle = issue.getTitle();
        String oldDescription = issue.getDescription();
        IssueType oldType = issue.getType();
        IssuePriority oldPriority = issue.getPriority();
        Module oldModule = issue.getModule();

        String newTitle = request.getTitle().trim();
        String newDescription = request.getDescription().trim();
        IssueType newType = request.getType();
        IssuePriority newPriority = request.getPriority();

        issue.setTitle(newTitle);
        issue.setDescription(newDescription);
        issue.setType(newType);
        issue.setPriority(newPriority);
        issue.setModule(module);

        VerificationStatus oldVerifStatus = issue.getVerificationStatus();
        boolean isResettingRejected = (oldVerifStatus == VerificationStatus.REJECTED);
        if (isResettingRejected) {
            issue.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        }

        Issue updated = issueRepository.save(issue);

        if (!Objects.equals(oldTitle, newTitle)) {
            issueAuditService.recordFieldChange(updated, currentUser, "title", oldTitle, newTitle);
        }
        if (!Objects.equals(oldDescription, newDescription)) {
            issueAuditService.recordFieldChange(updated, currentUser, "description", oldDescription, newDescription);
        }
        if (oldType != newType) {
            issueAuditService.recordFieldChange(updated, currentUser, "type",
                    oldType != null ? oldType.name() : null,
                    newType != null ? newType.name() : null);
        }
        if (oldPriority != newPriority) {
            issueAuditService.recordFieldChange(updated, currentUser, "priority",
                    oldPriority != null ? oldPriority.name() : null,
                    newPriority != null ? newPriority.name() : null);
        }
        UUID oldModuleId = oldModule != null ? oldModule.getId() : null;
        UUID newModuleId = module != null ? module.getId() : null;
        if (!Objects.equals(oldModuleId, newModuleId)) {
            String oldModName = oldModule != null ? oldModule.getName() : null;
            String newModName = module != null ? module.getName() : null;
            issueAuditService.recordFieldChange(updated, currentUser, "module", oldModName, newModName);
        }
        if (isResettingRejected) {
            issueAuditService.recordVerificationChange(updated, currentUser, "REJECTED", "PENDING_VERIFICATION");
        }

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

        if (commentRepository.existsByIssueId(issueId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot delete issue because it has associated comments");
        }

        if (attachmentRepository.existsByIssueId(issueId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cannot delete issue because it has associated attachments");
        }

        issueRepository.delete(issue);
        return new GenericResponse("Issue deleted successfully");
    }

    @Transactional
    public IssueResponse updateIssueStage(UUID currentUserId, UUID issueId, UpdateIssueStageRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot modify stage of issue in another organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(issue.getReporter().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only modify stage of issues that they reported");
            }
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(issue.getProject().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only modify stage of issues in member projects");
            }
        }

        IssueStage oldStage = issue.getStage();
        stageTransitionService.validateTransition(oldStage, request.getStage(), currentUser.getRole());

        issue.setStage(request.getStage());
        Issue updated = issueRepository.save(issue);
        issueAuditService.recordStageChange(updated, currentUser, oldStage.name(), request.getStage().name());
        return IssueResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> getVerificationQueue(UUID currentUserId, int page, int size, String search, UUID projectId) {
        User currentUser = getCurrentUser(currentUserId);

        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to access the verification queue");
        }

        UUID targetOrgId = null;
        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            targetOrgId = currentUser.getOrganization().getId();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Specification<Issue> spec = IssueSpecification.filterIssues(
                search, projectId, null, null, null, null,
                VerificationStatus.PENDING_VERIFICATION, null, targetOrgId, null
        );

        Page<Issue> issuePage = issueRepository.findAll(spec, pageable);

        Page<IssueResponse> responsePage = issuePage.map(IssueResponse::fromEntity);
        return PageResponse.fromPage(responsePage);
    }

    @Transactional
    public IssueResponse updateVerificationStatus(UUID currentUserId, UUID issueId, UpdateVerificationStatusRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser.getRole() == Role.CLIENT_USER) {
            throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER is not authorized to perform issue verification");
        }

        if (request.getStatus() == null || request.getStatus() == VerificationStatus.PENDING_VERIFICATION) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Verification status decision must be VERIFIED or REJECTED");
        }

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN can only perform verification for issues in their own organization");
            }
        }

        if (issue.getVerificationStatus() != VerificationStatus.PENDING_VERIFICATION) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Issue is not pending verification; current status: " + issue.getVerificationStatus());
        }

        VerificationStatus oldStatus = issue.getVerificationStatus();
        VerificationStatus newStatus = request.getStatus();

        issue.setVerificationStatus(newStatus);

        // If VERIFIED and currently in SUBMITTED stage, transition SUBMITTED -> RECEIVED via state machine
        if (newStatus == VerificationStatus.VERIFIED && issue.getStage() == IssueStage.SUBMITTED) {
            IssueStage oldStage = issue.getStage();
            stageTransitionService.validateTransition(oldStage, IssueStage.RECEIVED, currentUser.getRole());
            issue.setStage(IssueStage.RECEIVED);
            issueAuditService.recordStageChange(issue, currentUser, oldStage.name(), IssueStage.RECEIVED.name());
        }

        Issue updated = issueRepository.save(issue);
        issueAuditService.recordVerificationChange(updated, currentUser, oldStatus.name(), newStatus.name());

        return IssueResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public List<IssueAuditResponse> getIssueAudits(UUID currentUserId, UUID issueId) {
        User currentUser = getCurrentUser(currentUserId);
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Issue not found"));

        if (currentUser.getRole() == Role.CLIENT_ADMIN) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_ADMIN cannot access audit history of another organization");
            }
        } else if (currentUser.getRole() == Role.CLIENT_USER) {
            if (!Objects.equals(issue.getProject().getOrganization().getId(), currentUser.getOrganization().getId()) ||
                !membershipRepository.existsByProjectIdAndUserId(issue.getProject().getId(), currentUser.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, "CLIENT_USER can only access audit history of member projects");
            }
        }

        return issueAuditService.getAuditsForIssue(issueId);
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }
}
