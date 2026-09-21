package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ticket.system.entity.Project;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProjectResponse {

    private UUID id;
    private String name;
    private String shortCode;
    private String description;

    @JsonProperty("isActive")
    private boolean isActive;

    private ClientOrganizationResponse organization;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProjectResponse() {
    }

    public ProjectResponse(UUID id, String name, String shortCode, String description,
                           boolean isActive, ClientOrganizationResponse organization,
                           OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.shortCode = shortCode;
        this.description = description;
        this.isActive = isActive;
        this.organization = organization;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProjectResponse fromEntity(Project project) {
        if (project == null) {
            return null;
        }
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getShortCode(),
                project.getDescription(),
                project.isActive(),
                ClientOrganizationResponse.fromEntity(project.getOrganization()),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public ClientOrganizationResponse getOrganization() {
        return organization;
    }

    public void setOrganization(ClientOrganizationResponse organization) {
        this.organization = organization;
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
