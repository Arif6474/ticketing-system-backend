package com.ticket.system.dto.response;

import com.ticket.system.entity.User;

import java.util.UUID;

public class ActorResponse {

    private UUID id;
    private String name;

    public ActorResponse() {
    }

    public ActorResponse(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public static ActorResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (first + " " + last).trim();
        if (fullName.isEmpty()) {
            fullName = user.getEmail();
        }
        return new ActorResponse(user.getId(), fullName);
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
}
