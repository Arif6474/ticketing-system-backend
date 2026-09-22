package com.ticket.system.service;

import com.ticket.system.dto.request.CreateIssueCommentRequest;
import com.ticket.system.dto.request.UpdateIssueCommentRequest;
import com.ticket.system.dto.response.GenericResponse;
import com.ticket.system.dto.response.IssueCommentResponse;
import com.ticket.system.entity.*;
import com.ticket.system.exception.AppException;
import com.ticket.system.repository.IssueCommentRepository;
import com.ticket.system.repository.IssueRepository;
import com.ticket.system.repository.ProjectMembershipRepository;
import com.ticket.system.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IssueCommentService {

    private final IssueCommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final ProjectMembershipRepository membershipRepository;

    public IssueCommentService(IssueCommentRepository commentRepository,
                               IssueRepository issueRepository,
                               UserRepository userRepository,
                               ProjectMembershipRepository membershipRepository) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public IssueCommentResponse createComment(UUID currentUserId, UUID issueId, CreateIssueCommentRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        Issue issue = verifyIssueAccess(currentUser, issueId);

        String trimmedContent = request.getContent() != null ? request.getContent().trim() : "";
        if (trimmedContent.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Comment content cannot be blank");
        }

        IssueComment comment = new IssueComment(issue, currentUser, trimmedContent);
        IssueComment saved = commentRepository.save(comment);

        return IssueCommentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<IssueCommentResponse> getComments(UUID currentUserId, UUID issueId) {
        User currentUser = getCurrentUser(currentUserId);
        verifyIssueAccess(currentUser, issueId);

        List<IssueComment> comments = commentRepository.findByIssueIdOrderByCreatedAtAscIdAsc(issueId);
        return comments.stream()
                .map(IssueCommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public IssueCommentResponse updateComment(UUID currentUserId, UUID issueId, UUID commentId, UpdateIssueCommentRequest request) {
        User currentUser = getCurrentUser(currentUserId);
        verifyIssueAccess(currentUser, issueId);

        IssueComment comment = commentRepository.findByIdAndIssueId(commentId, issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Comment not found for specified issue"));

        if (currentUser.getRole() != Role.APP_ADMIN && !Objects.equals(comment.getAuthor().getId(), currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the author or an APP_ADMIN can edit this comment");
        }

        String trimmedContent = request.getContent() != null ? request.getContent().trim() : "";
        if (trimmedContent.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Comment content cannot be blank");
        }

        comment.setContent(trimmedContent);
        IssueComment updated = commentRepository.save(comment);

        return IssueCommentResponse.fromEntity(updated);
    }

    @Transactional
    public GenericResponse deleteComment(UUID currentUserId, UUID issueId, UUID commentId) {
        User currentUser = getCurrentUser(currentUserId);
        verifyIssueAccess(currentUser, issueId);

        IssueComment comment = commentRepository.findByIdAndIssueId(commentId, issueId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Comment not found for specified issue"));

        if (currentUser.getRole() != Role.APP_ADMIN && !Objects.equals(comment.getAuthor().getId(), currentUser.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "Only the author or an APP_ADMIN can delete this comment");
        }

        commentRepository.delete(comment);
        return new GenericResponse("Comment deleted successfully");
    }

    private User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private Issue verifyIssueAccess(User currentUser, UUID issueId) {
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

        return issue;
    }
}
