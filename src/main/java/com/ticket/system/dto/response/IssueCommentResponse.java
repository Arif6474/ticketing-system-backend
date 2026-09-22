package com.ticket.system.dto.response;

import com.ticket.system.entity.IssueComment;

import java.time.OffsetDateTime;
import java.util.UUID;

public class IssueCommentResponse {

    private UUID id;
    private UUID issueId;
    private ActorResponse author;
    private String content;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public IssueCommentResponse() {
    }

    public IssueCommentResponse(UUID id, UUID issueId, ActorResponse author, String content,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.issueId = issueId;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IssueCommentResponse fromEntity(IssueComment comment) {
        if (comment == null) {
            return null;
        }
        return new IssueCommentResponse(
                comment.getId(),
                comment.getIssue() != null ? comment.getIssue().getId() : null,
                ActorResponse.fromEntity(comment.getAuthor()),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIssueId() {
        return issueId;
    }

    public void setIssueId(UUID issueId) {
        this.issueId = issueId;
    }

    public ActorResponse getAuthor() {
        return author;
    }

    public void setAuthor(ActorResponse author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
