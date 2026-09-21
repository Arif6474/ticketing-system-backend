package com.ticket.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 255, message = "Project name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Project short code is required")
    @Size(max = 50, message = "Project short code must not exceed 50 characters")
    private String shortCode;

    private String description;

    private Boolean active;

    public UpdateProjectRequest() {
    }

    public UpdateProjectRequest(String name, String shortCode, String description, Boolean active) {
        this.name = name;
        this.shortCode = shortCode;
        this.description = description;
        this.active = active;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
