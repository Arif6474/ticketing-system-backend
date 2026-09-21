package com.ticket.system.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddProjectMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    public AddProjectMemberRequest() {
    }

    public AddProjectMemberRequest(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
