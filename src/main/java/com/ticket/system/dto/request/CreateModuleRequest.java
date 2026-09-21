package com.ticket.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateModuleRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Module name is required")
    @Size(max = 255, message = "Module name must not exceed 255 characters")
    private String name;

    private String description;

    public CreateModuleRequest() {
    }

    public CreateModuleRequest(UUID projectId, String name, String description) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
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
}
