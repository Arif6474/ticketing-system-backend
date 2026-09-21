package com.ticket.system.dto.response;

import com.ticket.system.entity.Issue;
import com.ticket.system.entity.IssuePriority;
import com.ticket.system.entity.IssueStage;
import com.ticket.system.entity.IssueType;
import com.ticket.system.entity.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class IssueResponse {

    private UUID id;
    private String title;
    private String description;
    private IssueType type;
    private IssuePriority priority;
    private IssueStage stage;
    private VerificationStatus verificationStatus;

    private UUID projectId;
    private String projectName;

    private UUID moduleId;
    private String moduleName;

    private UUID reporterId;
    private String reporterName;
    private String reporterEmail;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public IssueResponse() {
    }

    public IssueResponse(UUID id, String title, String description, IssueType type, IssuePriority priority,
                         IssueStage stage, VerificationStatus verificationStatus, UUID projectId, String projectName,
                         UUID moduleId, String moduleName, UUID reporterId, String reporterName, String reporterEmail,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.stage = stage;
        this.verificationStatus = verificationStatus;
        this.projectId = projectId;
        this.projectName = projectName;
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.reporterEmail = reporterEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IssueResponse fromEntity(Issue issue) {
        if (issue == null) {
            return null;
        }

        String reporterFullName = null;
        if (issue.getReporter() != null) {
            String first = issue.getReporter().getFirstName() != null ? issue.getReporter().getFirstName() : "";
            String last = issue.getReporter().getLastName() != null ? issue.getReporter().getLastName() : "";
            reporterFullName = (first + " " + last).trim();
        }

        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getType(),
                issue.getPriority(),
                issue.getStage(),
                issue.getVerificationStatus(),
                issue.getProject() != null ? issue.getProject().getId() : null,
                issue.getProject() != null ? issue.getProject().getName() : null,
                issue.getModule() != null ? issue.getModule().getId() : null,
                issue.getModule() != null ? issue.getModule().getName() : null,
                issue.getReporter() != null ? issue.getReporter().getId() : null,
                reporterFullName,
                issue.getReporter() != null ? issue.getReporter().getEmail() : null,
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
    }

    public IssuePriority getPriority() {
        return priority;
    }

    public void setPriority(IssuePriority priority) {
        this.priority = priority;
    }

    public IssueStage getStage() {
        return stage;
    }

    public void setStage(IssueStage stage) {
        this.stage = stage;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReporterEmail() {
        return reporterEmail;
    }

    public void setReporterEmail(String reporterEmail) {
        this.reporterEmail = reporterEmail;
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
