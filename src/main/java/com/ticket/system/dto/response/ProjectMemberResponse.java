package com.ticket.system.dto.response;

import com.ticket.system.entity.ProjectMembership;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProjectMemberResponse {

    private UUID id;
    private UserResponse user;
    private OffsetDateTime createdAt;

    public ProjectMemberResponse() {
    }

    public ProjectMemberResponse(UUID id, UserResponse user, OffsetDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.createdAt = createdAt;
    }

    public static ProjectMemberResponse fromEntity(ProjectMembership membership) {
        if (membership == null) {
            return null;
        }
        return new ProjectMemberResponse(
                membership.getId(),
                UserResponse.fromEntity(membership.getUser()),
                membership.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
