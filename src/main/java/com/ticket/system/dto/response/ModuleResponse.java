package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ticket.system.entity.Module;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ModuleResponse {

    private UUID id;
    private UUID projectId;
    private String projectName;
    private String name;
    private String description;

    @JsonProperty("isActive")
    private boolean isActive;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ModuleResponse() {
    }

    public ModuleResponse(UUID id, UUID projectId, String projectName, String name, String description,
                          boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.projectId = projectId;
        this.projectName = projectName;
        this.name = name;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ModuleResponse fromEntity(Module module) {
        if (module == null) {
            return null;
        }
        return new ModuleResponse(
                module.getId(),
                module.getProject() != null ? module.getProject().getId() : null,
                module.getProject() != null ? module.getProject().getName() : null,
                module.getName(),
                module.getDescription(),
                module.isActive(),
                module.getCreatedAt(),
                module.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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
