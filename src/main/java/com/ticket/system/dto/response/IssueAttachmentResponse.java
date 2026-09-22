package com.ticket.system.dto.response;

import com.ticket.system.entity.IssueAttachment;

import java.time.OffsetDateTime;
import java.util.UUID;

public class IssueAttachmentResponse {

    private UUID id;
    private UUID issueId;
    private ActorResponse uploadedBy;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private OffsetDateTime createdAt;

    public IssueAttachmentResponse() {
    }

    public IssueAttachmentResponse(UUID id, UUID issueId, ActorResponse uploadedBy, String originalFilename,
                                  String contentType, long fileSize, OffsetDateTime createdAt) {
        this.id = id;
        this.issueId = issueId;
        this.uploadedBy = uploadedBy;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }

    public static IssueAttachmentResponse fromEntity(IssueAttachment attachment) {
        if (attachment == null) {
            return null;
        }
        return new IssueAttachmentResponse(
                attachment.getId(),
                attachment.getIssue() != null ? attachment.getIssue().getId() : null,
                ActorResponse.fromEntity(attachment.getUploadedBy()),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getCreatedAt()
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

    public ActorResponse getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(ActorResponse uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
