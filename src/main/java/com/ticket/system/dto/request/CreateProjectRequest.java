package com.ticket.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 255, message = "Project name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Project short code is required")
    @Size(max = 50, message = "Project short code must not exceed 50 characters")
    private String shortCode;

    private String description;

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    public CreateProjectRequest() {
    }

    public CreateProjectRequest(String name, String shortCode, String description, UUID organizationId) {
        this.name = name;
        this.shortCode = shortCode;
        this.description = description;
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
