package com.ticket.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ticket.system.entity.ClientOrganization;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ClientOrganizationResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;

    @JsonProperty("isActive")
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ClientOrganizationResponse() {
    }

    public ClientOrganizationResponse(UUID id, String name, String code, String description,
                                      boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ClientOrganizationResponse fromEntity(ClientOrganization org) {
        if (org == null) {
            return null;
        }
        return new ClientOrganizationResponse(
                org.getId(),
                org.getName(),
                org.getCode(),
                org.getDescription(),
                org.isActive(),
                org.getCreatedAt(),
                org.getUpdatedAt()
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
