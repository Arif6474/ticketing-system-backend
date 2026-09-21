package com.ticket.system.dto.request;

import com.ticket.system.entity.IssuePriority;
import com.ticket.system.entity.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateIssueRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Issue type is required")
    private IssueType type;

    @NotNull(message = "Issue priority is required")
    private IssuePriority priority;

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    private UUID moduleId;

    public CreateIssueRequest() {
    }

    public CreateIssueRequest(String title, String description, IssueType type, IssuePriority priority, UUID projectId, UUID moduleId) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.projectId = projectId;
        this.moduleId = moduleId;
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

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
    }
}
