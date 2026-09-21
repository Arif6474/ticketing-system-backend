package com.ticket.system.dto.request;

import com.ticket.system.entity.IssuePriority;
import com.ticket.system.entity.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class UpdateIssueRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Issue type is required")
    private IssueType type;

    @NotNull(message = "Issue priority is required")
    private IssuePriority priority;

    private UUID moduleId;

    public UpdateIssueRequest() {
    }

    public UpdateIssueRequest(String title, String description, IssueType type, IssuePriority priority, UUID moduleId) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
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

    public UUID getModuleId() {
        return moduleId;
    }

    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
    }
}
