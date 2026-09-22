package com.ticket.system.dto.response;

import com.ticket.system.entity.AuditEventType;
import com.ticket.system.entity.IssueAudit;

import java.time.OffsetDateTime;
import java.util.UUID;

public class IssueAuditResponse {

    private UUID id;
    private AuditEventType action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private ActorResponse actor;
    private OffsetDateTime createdAt;

    public IssueAuditResponse() {
    }

    public IssueAuditResponse(UUID id, AuditEventType action, String fieldName, String oldValue,
                              String newValue, ActorResponse actor, OffsetDateTime createdAt) {
        this.id = id;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    public static IssueAuditResponse fromEntity(IssueAudit audit) {
        if (audit == null) {
            return null;
        }
        return new IssueAuditResponse(
                audit.getId(),
                audit.getAction(),
                audit.getFieldName(),
                audit.getOldValue(),
                audit.getNewValue(),
                ActorResponse.fromEntity(audit.getActor()),
                audit.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AuditEventType getAction() {
        return action;
    }

    public void setAction(AuditEventType action) {
        this.action = action;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public ActorResponse getActor() {
        return actor;
    }

    public void setActor(ActorResponse actor) {
        this.actor = actor;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
