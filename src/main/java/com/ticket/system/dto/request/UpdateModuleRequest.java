package com.ticket.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateModuleRequest {

    @NotBlank(message = "Module name is required")
    @Size(max = 255, message = "Module name must not exceed 255 characters")
    private String name;

    private String description;

    private Boolean active;

    public UpdateModuleRequest() {
    }

    public UpdateModuleRequest(String name, String description, Boolean active) {
        this.name = name;
        this.description = description;
        this.active = active;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
